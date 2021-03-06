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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.FileLockInterruptionException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.nosan.embedded.cassandra.Cassandra;
import com.github.nosan.embedded.cassandra.CassandraException;
import com.github.nosan.embedded.cassandra.CassandraInterruptedException;
import com.github.nosan.embedded.cassandra.Settings;
import com.github.nosan.embedded.cassandra.Version;
import com.github.nosan.embedded.cassandra.local.artifact.Artifact;
import com.github.nosan.embedded.cassandra.local.artifact.ArtifactFactory;
import com.github.nosan.embedded.cassandra.util.FileUtils;
import com.github.nosan.embedded.cassandra.util.StringUtils;
import com.github.nosan.embedded.cassandra.util.SystemProperty;
import com.github.nosan.embedded.cassandra.util.annotation.Nullable;

/**
 * This {@link Cassandra} implementation just a wrapper on the {@link CassandraNode}.
 *
 * @author Dmytro Nosan
 * @see LocalCassandraFactory
 * @since 1.0.0
 */
class LocalCassandra implements Cassandra {

	private static final Logger log = LoggerFactory.getLogger(LocalCassandra.class);

	private final boolean registerShutdownHook;

	private final boolean deleteWorkingDirectory;

	private final int jmxPort;

	private final boolean allowRoot;

	private final Object lock = new Object();

	private final Version version;

	private final ArtifactFactory artifactFactory;

	private final Path workingDirectory;

	private final Path artifactDirectory;

	private final Duration startupTimeout;

	@Nullable
	private final URL configurationFile;

	@Nullable
	private final URL logbackFile;

	@Nullable
	private final URL rackFile;

	@Nullable
	private final URL topologyFile;

	@Nullable
	private final URL commitLogArchivingFile;

	private final List<String> jvmOptions;

	@Nullable
	private final Path javaHome;

	private volatile State state = State.NEW;

	@Nullable
	private volatile Thread startThread;

	@Nullable
	private CassandraNode node;

	@Nullable
	private Settings settings;

	private boolean shutdownHookRegistered = false;

	/**
	 * Creates a new {@link LocalCassandra}.
	 *
	 * @param version a version
	 * @param artifactFactory a factory to create {@link Artifact}
	 * @param workingDirectory a directory to keep data/logs/etc... (must be writable)
	 * @param artifactDirectory a directory to extract an {@link Artifact} (must be writable)
	 * @param startupTimeout a startup timeout
	 * @param configurationFile URL to {@code cassandra.yaml}
	 * @param logbackFile URL to {@code logback.xml}
	 * @param rackFile URL to {@code cassandra-rackdc.properties}
	 * @param topologyFile URL to {@code cassandra-topology.properties}
	 * @param commitLogArchivingFile URL to {@code commitlog_archiving.properties}
	 * @param jvmOptions additional {@code JVM} options
	 * @param javaHome java home directory
	 * @param jmxPort JMX port
	 * @param allowRoot allow running as a root
	 * @param registerShutdownHook whether shutdown hook should be registered or not
	 * @param deleteWorkingDirectory delete the working directory after success Cassandra stop
	 */
	LocalCassandra(Version version, ArtifactFactory artifactFactory, Path workingDirectory, Path artifactDirectory,
			Duration startupTimeout, @Nullable URL configurationFile, @Nullable URL logbackFile, @Nullable URL rackFile,
			@Nullable URL topologyFile, @Nullable URL commitLogArchivingFile, List<String> jvmOptions,
			@Nullable Path javaHome, int jmxPort, boolean allowRoot, boolean registerShutdownHook,
			boolean deleteWorkingDirectory) {
		this.artifactFactory = artifactFactory;
		this.workingDirectory = workingDirectory;
		this.artifactDirectory = artifactDirectory;
		this.startupTimeout = startupTimeout;
		this.configurationFile = configurationFile;
		this.logbackFile = logbackFile;
		this.rackFile = rackFile;
		this.topologyFile = topologyFile;
		this.commitLogArchivingFile = commitLogArchivingFile;
		this.jvmOptions = jvmOptions;
		this.javaHome = javaHome;
		this.jmxPort = jmxPort;
		this.allowRoot = allowRoot;
		this.version = version;
		this.registerShutdownHook = registerShutdownHook;
		this.deleteWorkingDirectory = deleteWorkingDirectory;
	}

	@Override
	public void start() throws CassandraException {
		synchronized (this.lock) {
			if (this.state != State.STARTED) {
				try {
					registerShutdownHook();
				}
				catch (Throwable ex) {
					throw new CassandraException("Unable to register a shutdown hook for Cassandra", ex);
				}
				try {
					this.startThread = Thread.currentThread();
					this.state = State.STARTING;
					initialize();
					startInternal();
					this.state = State.STARTED;
					this.startThread = null;
				}
				catch (InterruptedException | ClosedByInterruptException | FileLockInterruptionException ex) {
					this.startThread = null;
					this.state = State.START_INTERRUPTED;
					boolean interrupted = Thread.interrupted();
					stopInternalSilently();
					if (interrupted) {
						Thread.currentThread().interrupt();
					}
					throw new CassandraInterruptedException(ex);
				}
				catch (Throwable ex) {
					this.startThread = null;
					this.state = State.START_FAILED;
					stopInternalSilently();
					throw new CassandraException("Unable to start Cassandra", ex);
				}
			}
		}
	}

	@Override
	public void stop() throws CassandraException {
		synchronized (this.lock) {
			if (this.state != State.STOPPED) {
				try {
					this.state = State.STOPPING;
					stopInternal();
					this.state = State.STOPPED;
				}
				catch (InterruptedException | ClosedByInterruptException | FileLockInterruptionException ex) {
					this.state = State.STOP_INTERRUPTED;
					throw new CassandraInterruptedException(ex);
				}
				catch (Throwable ex) {
					this.state = State.STOP_FAILED;
					throw new CassandraException("Unable to stop Cassandra", ex);
				}
			}
		}
	}

	@Override
	public Settings getSettings() throws CassandraException {
		synchronized (this.lock) {
			if (this.state == State.STARTED) {
				Settings settings = this.settings;
				if (settings != null) {
					return settings;
				}
				throw new IllegalStateException("Settings cannot be null if Cassandra is running.");
			}
			throw new CassandraException("Cassandra is not started. Please start it before calling this method.");
		}
	}

	@Override
	public State getState() {
		return this.state;
	}

	@Override
	public String toString() {
		return String.format("%s [%s]", getClass().getSimpleName(), this.version);
	}

	private static boolean isWindows() {
		String os = new SystemProperty("os.name").get();
		if (StringUtils.hasText(os)) {
			return os.toLowerCase(Locale.ENGLISH).contains("windows");
		}
		return File.separatorChar == '\\';
	}

	private void initialize() throws IOException {
		Version version = this.version;
		log.info("Initialize Apache Cassandra '{}'. It takes a while...", version);
		long start = System.currentTimeMillis();
		List<Initializer> initializers = new ArrayList<>();
		initializers.add(new WorkingDirectoryInitializer(this.artifactFactory, this.artifactDirectory));
		initializers.add(new ConfigurationFileInitializer(this.configurationFile));
		initializers.add(new LogbackFileInitializer(this.logbackFile));
		initializers.add(new RackFileInitializer(this.rackFile));
		initializers.add(new TopologyFileInitializer(this.topologyFile));
		initializers.add(new CommitLogFileInitializer(this.commitLogArchivingFile));
		initializers.add(new ConfigurationFileRandomPortInitializer());
		if (!isWindows()) {
			initializers.add(new CassandraFileExecutableInitializer());
		}
		for (Initializer initializer : initializers) {
			initializer.initialize(this.workingDirectory, version);
		}
		long elapsed = System.currentTimeMillis() - start;
		log.info("Apache Cassandra '{}' has been initialized ({} ms)", version, elapsed);
	}

	private void startInternal() throws IOException, InterruptedException {
		Version version = this.version;
		log.info("Starts Apache Cassandra '{}'", version);
		long start = System.currentTimeMillis();
		CassandraNode node = createNode();
		this.node = node;
		this.settings = node.start();
		long elapsed = System.currentTimeMillis() - start;
		log.info("Apache Cassandra '{}' has been started ({} ms)", version, elapsed);
	}

	private void stopInternal() throws IOException, InterruptedException {
		Version version = this.version;
		CassandraNode node = this.node;
		if (node != null) {
			long start = System.currentTimeMillis();
			log.info("Stops Apache Cassandra '{}'", version);
			node.stop();
			this.node = null;
			if (this.deleteWorkingDirectory) {
				Path workingDirectory = this.workingDirectory;
				FileUtils.delete(workingDirectory);
				log.info("The '{}' directory has been deleted.", workingDirectory);
			}
			long elapsed = System.currentTimeMillis() - start;
			log.info("Apache Cassandra '{}' has been stopped ({} ms)", version, elapsed);
		}
	}

	private CassandraNode createNode() {
		if (isWindows()) {
			return new WindowsCassandraNode(this.workingDirectory, this.version, this.startupTimeout, this.jvmOptions,
					this.javaHome, this.jmxPort);
		}
		return new UnixCassandraNode(this.workingDirectory, this.version, this.startupTimeout, this.jvmOptions,
				this.javaHome, this.jmxPort, this.allowRoot);
	}

	private void registerShutdownHook() {
		if (this.registerShutdownHook && !this.shutdownHookRegistered) {
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				Optional.ofNullable(this.startThread).ifPresent(Thread::interrupt);
				stop();
			}, "Cassandra Shutdown Hook"));
			this.shutdownHookRegistered = true;
		}
	}

	private void stopInternalSilently() {
		try {
			stopInternal();
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		catch (Throwable ex) {
			if (log.isDebugEnabled()) {
				log.error("Unable to stop Cassandra", ex);
			}
		}
	}

}
