/*
 * Copyright 2018-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.nosan.embedded.cassandra.local;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import com.github.nosan.embedded.cassandra.Cassandra;
import com.github.nosan.embedded.cassandra.Settings;
import com.github.nosan.embedded.cassandra.Version;
import com.github.nosan.embedded.cassandra.util.MDCUtils;
import com.github.nosan.embedded.cassandra.util.NetworkUtils;
import com.github.nosan.embedded.cassandra.util.PortUtils;
import com.github.nosan.embedded.cassandra.util.StringUtils;
import com.github.nosan.embedded.cassandra.util.SystemProperty;
import com.github.nosan.embedded.cassandra.util.annotation.Nullable;

/**
 * The common implementation of the {@link CassandraNode}.
 *
 * @author Dmytro Nosan
 * @since 1.4.1
 */
abstract class AbstractCassandraNode implements CassandraNode {

	private static final AtomicLong instanceCounter = new AtomicLong();

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final long instance = instanceCounter.incrementAndGet();

	private final ThreadFactory threadFactory = runnable -> {
		Map<String, String> context = MDCUtils.getContext();
		Thread thread = new Thread(() -> {
			MDCUtils.setContext(context);
			runnable.run();
		}, String.format("cassandra-%d", this.instance));
		thread.setDaemon(true);
		return thread;
	};

	private final Path workingDirectory;

	private final Version version;

	private final Duration timeout;

	private final List<String> jvmOptions;

	private final int jmxPort;

	@Nullable
	private final Path javaHome;

	@Nullable
	private ProcessId processId;

	/**
	 * Creates a {@link AbstractCassandraNode}.
	 *
	 * @param workingDirectory a configured base directory
	 * @param version a version
	 * @param timeout a startup timeout
	 * @param jvmOptions additional {@code JVM} options
	 * @param javaHome java home directory
	 * @param jmxPort JMX port
	 */
	AbstractCassandraNode(Path workingDirectory, Version version, Duration timeout, List<String> jvmOptions,
			@Nullable Path javaHome, int jmxPort) {
		this.workingDirectory = workingDirectory;
		this.version = version;
		this.timeout = timeout;
		this.javaHome = javaHome;
		this.jmxPort = jmxPort;
		this.jvmOptions = Collections.unmodifiableList(new ArrayList<>(jvmOptions));
	}

	@Override
	public final Settings start() throws IOException, InterruptedException {
		ProcessBuilder processBuilder = new ProcessBuilder().directory(this.workingDirectory.toFile())
				.redirectErrorStream(true);
		Map<?, ?> properties = getProperties();
		JvmParameters jvmParameters = getJvmParameters(properties);
		RuntimeNodeSettings settings = getSettings(properties, jvmParameters);

		String javaHome = getJavaHome(this.javaHome);
		if (StringUtils.hasText(javaHome)) {
			processBuilder.environment().put("JAVA_HOME", javaHome);
		}
		processBuilder.environment().put("JVM_EXTRA_OPTS", jvmParameters.toString());

		CompositeConsumer<String> consumer = new CompositeConsumer<>();
		NodeReadiness nodeReadiness = new NodeReadiness();
		TransportReadiness transportReadiness = new TransportReadiness(settings);
		BufferedConsumer bufferedConsumer = new BufferedConsumer(10);
		consumer.add(LoggerFactory.getLogger(Cassandra.class)::info);
		consumer.add(bufferedConsumer);
		consumer.add(nodeReadiness);
		consumer.add(new RpcAddressConsumer(settings, consumer));
		consumer.add(new ListenAddressConsumer(settings, consumer));

		ProcessId processId = start(processBuilder, this.threadFactory,
				new FilteredConsumer<>(consumer, new StackTraceFilter()));
		this.processId = processId;
		if (isStarted(this.timeout, processId, bufferedConsumer, transportReadiness, nodeReadiness)) {
			consumer.remove(bufferedConsumer);
			consumer.remove(nodeReadiness);
			this.log.info("Cassandra Node '{}' has been started", processId.getPid());
			return settings;
		}
		throw new IOException(
				String.format("Cassandra Node '%s' has not been started, seems like (%d) milliseconds is not enough.",
						processId.getPid(), this.timeout.toMillis()));
	}

	@Override
	public final void stop() throws IOException, InterruptedException {
		ProcessId processId = this.processId;
		Process process = (processId != null) ? processId.getProcess() : null;
		if (processId != null && process.isAlive()) {
			long pid = processId.getPid();
			this.log.info("Stops Cassandra Node '{}'", pid);
			if (!isTerminating(pid)) {
				process.destroy();
			}
			if (!isShutdown(Duration.ofSeconds(5), process)) {
				if (!isTerminating(pid)) {
					process.destroy();
				}
				if (!isShutdown(Duration.ofSeconds(5), process)) {
					if (!isKilling(pid)) {
						process.destroyForcibly();
					}
					if (!isShutdown(Duration.ofSeconds(5), process)) {
						process.destroyForcibly();
					}
				}
			}
			if (process.isAlive()) {
				throw new IOException(String.format("Casandra Node '%s' has not been stopped.", pid));
			}
			this.processId = null;
			this.log.info("Cassandra Node '{}' has been stopped", pid);
		}
	}

	/**
	 * Starts the Apache Cassandra.
	 *
	 * @param builder the almost configured builder (does not have a command)
	 * @param threadFactory thread factory to create a process reader.
	 * @param consumer the output consumer
	 * @return the process
	 * @throws IOException in the case of I/O errors
	 */
	protected abstract ProcessId start(ProcessBuilder builder, ThreadFactory threadFactory, Consumer<String> consumer)
			throws IOException;

	/**
	 * Terminates the Apache Cassandra.
	 *
	 * @param pid the pid
	 * @param builder the almost configured builder (does not have a command)
	 * @param threadFactory thread factory to create a process reader.
	 * @param consumer the output consumer
	 * @return the exit value of the termination subprocess.
	 * @throws IOException in the case of I/O errors
	 * @throws InterruptedException if the current thread is {@link Thread#interrupt() interrupted} by another thread
	 */
	protected abstract int terminate(long pid, ProcessBuilder builder, ThreadFactory threadFactory,
			Consumer<String> consumer) throws IOException, InterruptedException;

	/**
	 * Kills the Apache Cassandra.
	 *
	 * @param pid the pid
	 * @param builder the almost configured builder (does not have command)
	 * @param threadFactory thread factory to create a process reader.
	 * @param consumer the output consumer
	 * @return the exit value of the kill subprocess.
	 * @throws IOException in the case of I/O errors
	 * @throws InterruptedException if the current thread is {@link Thread#interrupt() interrupted} by another thread
	 */
	protected abstract int kill(long pid, ProcessBuilder builder, ThreadFactory threadFactory,
			Consumer<String> consumer) throws IOException, InterruptedException;

	private Map<?, ?> getProperties() throws IOException {
		try (InputStream is = Files.newInputStream(this.workingDirectory.resolve("conf/cassandra.yaml"))) {
			Yaml yaml = new Yaml();
			Map properties = yaml.loadAs(is, Map.class);
			return (properties != null) ? properties : Collections.emptyMap();
		}
	}

	@Nullable
	private String getJavaHome(@Nullable Path javaHome) {
		return Optional.ofNullable(javaHome).map(Path::toString)
				.orElseGet(new SystemProperty("java.home"));
	}

	private RuntimeNodeSettings getSettings(Map<?, ?> properties, JvmParameters jvmParameters) {
		return new RuntimeNodeSettings(this.version, properties, jvmParameters);
	}

	private JvmParameters getJvmParameters(Map<?, ?> properties) {
		return new JvmParameters(this.jvmOptions, this.jmxPort, new NodeSettings(this.version, properties));
	}

	private boolean isStarted(Duration timeout, ProcessId processId, BufferedConsumer bufferedConsumer,
			TransportReadiness transportReadiness, NodeReadiness nodeReadiness) throws IOException,
			InterruptedException {
		Process process = processId.getProcess();
		long start = System.nanoTime();
		long rem = timeout.toNanos();
		do {
			long pid = processId.getPid();
			if (!process.isAlive()) {
				int exitValue = process.exitValue();
				throw new IOException(String.format("Cassandra Node '%s' is not alive. Exit code is '%s'. "
						+ "Please see logs for more details.%n%s", pid, exitValue, bufferedConsumer));
			}
			if (nodeReadiness.get() && transportReadiness.get()) {
				return true;
			}
			if (rem > 0) {
				Thread.sleep(Math.min(TimeUnit.NANOSECONDS.toMillis(rem) + 1, 100));
			}
			rem = timeout.toNanos() - (System.nanoTime() - start);
		} while (rem > 0);
		return false;
	}

	private boolean isShutdown(Duration timeout, Process process) throws InterruptedException {
		long start = System.nanoTime();
		long rem = timeout.toNanos();
		do {
			if (!process.isAlive()) {
				return true;
			}
			if (rem > 0) {
				Thread.sleep(Math.min(TimeUnit.NANOSECONDS.toMillis(rem) + 1, 250));
			}
			rem = timeout.toNanos() - (System.nanoTime() - start);
		} while (rem > 0);
		return false;
	}

	private boolean isTerminating(long pid) throws InterruptedException {
		try {
			ProcessBuilder builder = new ProcessBuilder().directory(this.workingDirectory.toFile())
					.redirectErrorStream(true);
			return terminate(pid, builder, this.threadFactory, this.log::info) == 0;
		}
		catch (IOException ex) {
			this.log.error(String.format("The terminated signal has not been sent to Cassandra Node '%s'.", pid), ex);
			return false;
		}
	}

	private boolean isKilling(long pid) throws InterruptedException {
		try {
			ProcessBuilder builder = new ProcessBuilder().directory(this.workingDirectory.toFile())
					.redirectErrorStream(true);
			return kill(pid, builder, this.threadFactory, this.log::info) == 0;
		}
		catch (IOException ex) {
			this.log.error(String.format("The kill signal has not been sent to Cassandra Node '%s'.", pid), ex);
			return false;
		}
	}

	private static final class TransportReadiness implements Supplier<Boolean> {

		private final Settings settings;

		TransportReadiness(Settings settings) {
			this.settings = settings;
		}

		@Override
		public Boolean get() {
			Settings settings = this.settings;
			Predicate<Settings> condition = (ignore) -> true;
			if (settings.isStartNativeTransport()) {
				condition = condition.and(TransportReadiness::isNativeTransportReady);
			}
			if (settings.isStartRpc()) {
				condition = condition.and(TransportReadiness::isRpcTransportReady);
			}
			return condition.test(settings);
		}

		private static boolean isNativeTransportReady(Settings settings) {
			return isPortBusy(settings.getRealAddress(), settings.getPort()) && isPortBusy(settings.getRealAddress(),
					settings.getSslPort());
		}

		private static boolean isRpcTransportReady(Settings settings) {
			return isPortBusy(settings.getRealAddress(), settings.getRpcPort());
		}

		private static boolean isPortBusy(InetAddress address, @Nullable Integer port) {
			return (port == null || port == -1) || PortUtils.isPortBusy(address, port);
		}

	}

	private static final class NodeReadiness implements Consumer<String>, Supplier<Boolean> {

		private volatile boolean ready = false;

		private long start = System.currentTimeMillis();

		@Override
		public void accept(String line) {
			if (match(line)) {
				this.ready = true;
			}
		}

		@Override
		public Boolean get() {
			boolean ready = this.ready;
			if (!ready) {
				long elapsed = System.currentTimeMillis() - this.start;
				return TimeUnit.MILLISECONDS.toSeconds(elapsed) > 20;
			}
			return true;
		}

		private static boolean match(String line) {
			return line.matches("(?i).*listening.*clients.*") || line
					.matches("(?i).*not\\s*starting.*as\\s*requested.*");
		}

	}

	private static final class RpcAddressConsumer implements Consumer<String> {

		private static final Logger log = LoggerFactory.getLogger(RpcAddressConsumer.class);

		private final RuntimeNodeSettings settings;

		private final Pattern regex;

		private final CompositeConsumer<String> consumer;

		RpcAddressConsumer(RuntimeNodeSettings settings, CompositeConsumer<String> consumer) {
			this.settings = settings;
			this.regex = Pattern
					.compile(String.format(".*/(.+):\\s*(%d|%d).*", settings.getPort(), settings.getSslPort()));
			this.consumer = consumer;
		}

		@Override
		public void accept(String line) {
			Matcher matcher = this.regex.matcher(line);
			if (matcher.matches()) {
				String address = matcher.group(1).trim();
				try {
					this.settings.setRealAddress(NetworkUtils.getInetAddress(address.trim()));
					this.consumer.remove(this);
				}
				catch (Throwable ex) {
					if (log.isDebugEnabled()) {
						log.error(String.format("Could not parse an InetAddress '%s'", address), ex);
					}
				}
			}
		}

	}

	private static final class ListenAddressConsumer implements Consumer<String> {

		private static final Logger log = LoggerFactory.getLogger(ListenAddressConsumer.class);

		private final RuntimeNodeSettings settings;

		private final Pattern regex;

		private final CompositeConsumer<String> consumer;

		ListenAddressConsumer(RuntimeNodeSettings settings, CompositeConsumer<String> consumer) {
			this.settings = settings;
			this.regex = Pattern.compile(
					String.format(".*/(.+):\\s*(%d|%d).*", settings.getStoragePort(), settings.getSslStoragePort()));
			this.consumer = consumer;
		}

		@Override
		public void accept(String line) {
			Matcher matcher = this.regex.matcher(line);
			if (matcher.matches()) {
				String address = matcher.group(1).trim();
				try {
					this.settings.setRealListenAddress(NetworkUtils.getInetAddress(address.trim()));
					this.consumer.remove(this);
				}
				catch (Throwable ex) {
					if (log.isDebugEnabled()) {
						log.error(String.format("Could not parse an InetAddress '%s'", address), ex);
					}
				}
			}
		}

	}

	private static final class RuntimeNodeSettings extends NodeSettings {

		private final JvmParameters jvmParameters;

		@Nullable
		private volatile InetAddress realListenAddress;

		@Nullable
		private volatile InetAddress realAddress;

		RuntimeNodeSettings(Version version, @Nullable Map<?, ?> properties, JvmParameters jvmParameters) {
			super(version, properties);
			this.jvmParameters = jvmParameters;
		}

		@Override
		public int getStoragePort() {
			return this.jvmParameters.getStoragePort().orElseGet(super::getStoragePort);
		}

		@Override
		public int getSslStoragePort() {
			return this.jvmParameters.getSslStoragePort().orElseGet(super::getSslStoragePort);
		}

		@Override
		public boolean isStartNativeTransport() {
			return this.jvmParameters.isStartNativeTransport().orElseGet(super::isStartNativeTransport);
		}

		@Override
		public int getPort() {
			return this.jvmParameters.getPort().orElseGet(super::getPort);
		}

		@Override
		public boolean isStartRpc() {
			if (getVersion().getMajor() < 4) {
				return this.jvmParameters.isStartRpc().orElseGet(super::isStartRpc);
			}
			return super.isStartRpc();
		}

		@Override
		public int getRpcPort() {
			if (getVersion().getMajor() < 4) {
				return this.jvmParameters.getRpcPort().orElseGet(super::getRpcPort);
			}
			return super.getRpcPort();
		}

		@Override
		public InetAddress getRealAddress() {
			InetAddress address = this.realAddress;
			if (address != null) {
				return address;
			}
			return super.getRealAddress();
		}

		/**
		 * Initializes the value for the {@link #getRealAddress()} attribute.
		 *
		 * @param realAddress The value for realAddress
		 */
		void setRealAddress(@Nullable InetAddress realAddress) {
			this.realAddress = realAddress;
		}

		@Override
		public InetAddress getRealListenAddress() {
			InetAddress address = this.realListenAddress;
			if (address != null) {
				return address;
			}
			return super.getRealListenAddress();
		}

		/**
		 * Initializes the value for the {@link #getRealListenAddress()}  attribute.
		 *
		 * @param realListenAddress The value for realListenAddress
		 */
		void setRealListenAddress(@Nullable InetAddress realListenAddress) {
			this.realListenAddress = realListenAddress;
		}

	}

	private static final class BufferedConsumer implements Consumer<String> {

		private final Deque<String> lines = new ConcurrentLinkedDeque<>();

		private final int count;

		BufferedConsumer(int count) {
			this.count = count;
		}

		@Override
		public void accept(String line) {
			if (this.lines.size() > this.count) {
				this.lines.removeFirst();
			}
			this.lines.addLast(line);
		}

		@Override
		public String toString() {
			return this.lines.stream().collect(Collectors.joining(System.lineSeparator()));
		}

	}

	private static final class StackTraceFilter implements Predicate<String> {

		private static final Pattern STACKTRACE_PATTERN = Pattern.compile("\\s+(at|\\.{3})\\s+.*");

		@Override
		public boolean test(String line) {
			return !STACKTRACE_PATTERN.matcher(line).matches();
		}

	}

}
