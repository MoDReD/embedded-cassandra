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
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apiguardian.api.API;

import com.github.nosan.embedded.cassandra.Version;
import com.github.nosan.embedded.cassandra.local.artifact.ArtifactFactory;
import com.github.nosan.embedded.cassandra.local.artifact.RemoteArtifactFactory;
import com.github.nosan.embedded.cassandra.util.annotation.Nullable;

/**
 * Builder to create a {@link LocalCassandraFactory}.
 *
 * @author Dmytro Nosan
 * @since 1.0.0
 */
@API(since = "1.0.0", status = API.Status.STABLE)
public final class LocalCassandraFactoryBuilder {

	private final List<String> jvmOptions = new ArrayList<>();

	@Nullable
	private Version version;

	@Nullable
	private ArtifactFactory artifactFactory;

	@Nullable
	private Duration startupTimeout;

	@Nullable
	private Path workingDirectory;

	@Nullable
	private Path artifactDirectory;

	@Nullable
	private URL configurationFile;

	@Nullable
	private URL logbackFile;

	@Nullable
	private URL rackFile;

	@Nullable
	private URL topologyFile;

	@Nullable
	private Path javaHome;

	@Nullable
	private URL commitLogArchivingFile;

	private int jmxPort = 7199;

	private boolean allowRoot = false;

	private boolean registerShutdownHook = true;

	private boolean deleteWorkingDirectory = false;

	/**
	 * Initializes the value for the {@link LocalCassandraFactory#isAllowRoot() allowRoot} attribute.
	 *
	 * @param allowRoot The value for allowRoot
	 * @return {@code this} builder for use in a chained invocation
	 * @since 1.2.1
	 */
	public LocalCassandraFactoryBuilder setAllowRoot(boolean allowRoot) {
		this.allowRoot = allowRoot;
		return this;
	}

	/**
	 * Initializes the value for the {@link LocalCassandraFactory#getJmxPort() jmxPort} attribute.
	 *
	 * @param jmxPort The value for jmxPort
	 * @return {@code this} builder for use in a chained invocation
	 * @since 1.1.1
	 */
	public LocalCassandraFactoryBuilder setJmxPort(int jmxPort) {
		this.jmxPort = jmxPort;
		return this;
	}

	/**
	 * Initializes the value for the {@link LocalCassandraFactory#getJavaHome() javaHome} attribute.
	 *
	 * @param javaHome The value for javaHome
	 * @return {@code this} builder for use in a chained invocation
	 * @since 1.0.9
	 */
	public LocalCassandraFactoryBuilder setJavaHome(@Nullable Path javaHome) {
		this.javaHome = javaHome;
		return this;
	}

	/**
	 * Initializes the value for the {@link LocalCassandraFactory#getJavaHome() javaHome} attribute.
	 *
	 * @param javaHome The value for javaHome
	 * @return {@code this} builder for use in a chained invocation
	 * @since 1.0.9
	 */
	public LocalCassandraFactoryBuilder setJavaHome(@Nullable File javaHome) {
		return setJavaHome((javaHome != null) ? javaHome.toPath() : null);
	}

	/**
	 * Initializes the value for the {@link LocalCassandraFactory#getStartupTimeout() timeout} attribute.
	 *
	 * @param startupTimeout The value for startupTimeout
	 * @return {@code this} builder for use in a chained invocation
	 */
	public LocalCassandraFactoryBuilder setStartupTimeout(@Nullable Duration startupTimeout) {
		this.startupTimeout = startupTimeout;
		return this;
	}

	/**
	 * Initializes the value for the {@link LocalCassandraFactory#getVersion() version} attribute.
	 *
	 * @param version The value for version
	 * @return {@code this} builder for use in a chained invocation
	 */
	public LocalCassandraFactoryBuilder setVersion(@Nullable Version version) {
		this.version = version;
		return this;
	}

	/**
	 * Initializes the value for the {@link LocalCassandraFactory#getVersion() version} attribute.
	 *
	 * @param version The value for version
	 * @return {@code this} builder for use in a chained invocation
	 * @since 1.4.3
	 */
	public LocalCassandraFactoryBuilder setVersion(@Nullable String version) {
		return setVersion((version != null) ? Version.parse(version) : null);
	}

	/**
	 * Initializes the value for the {@link LocalCassandraFactory#getArtifactFactory() artifactFactory} attribute.
	 *
	 * @param artifactFactory The value for artifactFactory
	 * @return {@code this} builder for use in a chained invocation
	 * @see RemoteArtifactFactory
	 */
	public LocalCassandraFactoryBuilder setArtifactFactory(@Nullable ArtifactFactory artifactFactory) {
		this.artifactFactory = artifactFactory;
		return this;
	}

	/**
	 * Initializes the value for the {@link LocalCassandraFactory#getArtifactDirectory() artifactDirectory} attribute.
	 *
	 * @param artifactDirectory The value for artifactDirectory
	 * @return {@code this} builder for use in a chained invocation
	 * @since 1.3.0
	 */
	public LocalCassandraFactoryBuilder setArtifactDirectory(@Nullable Path artifactDirectory) {
		this.artifactDirectory = artifactDirectory;
		return this;
	}

	/**
	 * Initializes the value for the {@link LocalCassandraFactory#getArtifactDirectory() artifactDirectory} attribute.
	 *
	 * @param artifactDirectory The value for artifactDirectory
	 * @return {@code this} builder for use in a chained invocation
	 * @since 1.3.0
	 */
	public LocalCassandraFactoryBuilder setArtifactDirectory(@Nullable File artifactDirectory) {
		return setArtifactDirectory((artifactDirectory != null) ? artifactDirectory.toPath() : null);
	}

	/**
	 * Initializes the value for the {@link LocalCassandraFactory#getWorkingDirectory() workingDirectory} attribute.
	 *
	 * @param workingDirectory The value for workingDirectory
	 * @return {@code this} builder for use in a chained invocation
	 */
	public LocalCassandraFactoryBuilder setWorkingDirectory(@Nullable Path workingDirectory) {
		this.workingDirectory = workingDirectory;
		return this;
	}

	/**
	 * Initializes the value for the {@link LocalCassandraFactory#getWorkingDirectory() workingDirectory} attribute.
	 *
	 * @param workingDirectory The value for workingDirectory
	 * @return {@code this} builder for use in a chained invocation
	 */
	public LocalCassandraFactoryBuilder setWorkingDirectory(@Nullable File workingDirectory) {
		return setWorkingDirectory((workingDirectory != null) ? workingDirectory.toPath() : null);
	}

	/**
	 * Initializes the value for the {@link LocalCassandraFactory#getConfigurationFile() configurationFile} attribute.
	 *
	 * @param configurationFile The value for configurationFile
	 * @return {@code this} builder for use in a chained invocation
	 */
	public LocalCassandraFactoryBuilder setConfigurationFile(@Nullable Path configurationFile) {
		try {
			return setConfigurationFile((configurationFile != null) ? configurationFile.toUri().toURL() : null);
		}
		catch (MalformedURLException ex) {
			throw new IllegalArgumentException(ex);
		}
	}

	/**
	 * Initializes the value for the {@link LocalCassandraFactory#getConfigurationFile() configurationFile} attribute.
	 *
	 * @param configurationFile The value for configurationFile
	 * @return {@code this} builder for use in a chained invocation
	 */
	public LocalCassandraFactoryBuilder setConfigurationFile(@Nullable File configurationFile) {
		try {
			return setConfigurationFile((configurationFile != null) ? configurationFile.toURI().toURL() : null);
		}
		catch (MalformedURLException ex) {
			throw new IllegalArgumentException(ex);
		}
	}

	/**
	 * Initializes the value for the {@link LocalCassandraFactory#getConfigurationFile() configurationFile} attribute.
	 *
	 * @param configurationFile The value for configurationFile
	 * @return {@code this} builder for use in a chained invocation
	 */
	public LocalCassandraFactoryBuilder setConfigurationFile(@Nullable URL configurationFile) {
		this.configurationFile = configurationFile;
		return this;
	}

	/**
	 * Initializes the value for the {@link LocalCassandraFactory#getLogbackFile() logbackFile} attribute.
	 *
	 * @param logbackFile The value for logbackFile
	 * @return {@code this} builder for use in a chained invocation
	 */
	public LocalCassandraFactoryBuilder setLogbackFile(@Nullable Path logbackFile) {
		try {
			return setLogbackFile((logbackFile != null) ? logbackFile.toUri().toURL() : null);
		}
		catch (MalformedURLException ex) {
			throw new IllegalArgumentException(ex);
		}
	}

	/**
	 * Initializes the value for the {@link LocalCassandraFactory#getLogbackFile() logbackFile} attribute.
	 *
	 * @param logbackFile The value for logbackFile
	 * @return {@code this} builder for use in a chained invocation
	 */
	public LocalCassandraFactoryBuilder setLogbackFile(@Nullable File logbackFile) {
		try {
			return setLogbackFile((logbackFile != null) ? logbackFile.toURI().toURL() : null);
		}
		catch (MalformedURLException ex) {
			throw new IllegalArgumentException(ex);
		}
	}

	/**
	 * Initializes the value for the {@link LocalCassandraFactory#getLogbackFile() logbackFile} attribute.
	 *
	 * @param logbackFile The value for logbackFile
	 * @return {@code this} builder for use in a chained invocation
	 */
	public LocalCassandraFactoryBuilder setLogbackFile(@Nullable URL logbackFile) {
		this.logbackFile = logbackFile;
		return this;
	}

	/**
	 * Initializes the value for the {@link LocalCassandraFactory#getJvmOptions() jvmOptions} attribute.
	 *
	 * @param jvmOptions The value for jvmOptions
	 * @return {@code this} builder for use in a chained invocation
	 */
	public LocalCassandraFactoryBuilder setJvmOptions(@Nullable String... jvmOptions) {
		this.jvmOptions.clear();
		if (jvmOptions != null) {
			this.jvmOptions.addAll(Arrays.asList(jvmOptions));
		}
		return this;
	}

	/**
	 * Add additional {@link LocalCassandraFactory#getJvmOptions() jvmOptions}.
	 *
	 * @param jvmOptions The value for jvmOptions
	 * @return {@code this} builder for use in a chained invocation
	 * @since 1.0.5
	 */
	public LocalCassandraFactoryBuilder addJvmOptions(@Nullable String... jvmOptions) {
		if (jvmOptions != null) {
			this.jvmOptions.addAll(Arrays.asList(jvmOptions));
		}
		return this;
	}

	/**
	 * Initializes the value for the {@link LocalCassandraFactory#getRackFile() rackFile} attribute.
	 *
	 * @param rackFile The value for rackFile
	 * @return {@code this} builder for use in a chained invocation
	 */
	public LocalCassandraFactoryBuilder setRackFile(@Nullable Path rackFile) {
		try {
			return setRackFile((rackFile != null) ? rackFile.toUri().toURL() : null);
		}
		catch (MalformedURLException ex) {
			throw new IllegalArgumentException(ex);
		}
	}

	/**
	 * Initializes the value for the {@link LocalCassandraFactory#getRackFile() rackFile} attribute.
	 *
	 * @param rackFile The value for rackFile
	 * @return {@code this} builder for use in a chained invocation
	 */
	public LocalCassandraFactoryBuilder setRackFile(@Nullable File rackFile) {
		try {
			return setRackFile((rackFile != null) ? rackFile.toURI().toURL() : null);
		}
		catch (MalformedURLException ex) {
			throw new IllegalArgumentException(ex);
		}
	}

	/**
	 * Initializes the value for the {@link LocalCassandraFactory#getRackFile() rackFile} attribute.
	 *
	 * @param rackFile The value for rackFile
	 * @return {@code this} builder for use in a chained invocation
	 */
	public LocalCassandraFactoryBuilder setRackFile(@Nullable URL rackFile) {
		this.rackFile = rackFile;
		return this;
	}

	/**
	 * Initializes the value for the {@link LocalCassandraFactory#getTopologyFile() topologyFile} attribute.
	 *
	 * @param topologyFile The value for topologyFile
	 * @return {@code this} builder for use in a chained invocation
	 */
	public LocalCassandraFactoryBuilder setTopologyFile(@Nullable Path topologyFile) {
		try {
			return setTopologyFile((topologyFile != null) ? topologyFile.toUri().toURL() : null);
		}
		catch (MalformedURLException ex) {
			throw new IllegalArgumentException(ex);
		}
	}

	/**
	 * Initializes the value for the {@link LocalCassandraFactory#getTopologyFile() topologyFile} attribute.
	 *
	 * @param topologyFile The value for topologyFile
	 * @return {@code this} builder for use in a chained invocation
	 */
	public LocalCassandraFactoryBuilder setTopologyFile(@Nullable File topologyFile) {
		try {
			return setTopologyFile((topologyFile != null) ? topologyFile.toURI().toURL() : null);
		}
		catch (MalformedURLException ex) {
			throw new IllegalArgumentException(ex);
		}
	}

	/**
	 * Initializes the value for the {@link LocalCassandraFactory#getTopologyFile() topologyFile} attribute.
	 *
	 * @param topologyFile The value for topologyFile
	 * @return {@code this} builder for use in a chained invocation
	 */
	public LocalCassandraFactoryBuilder setTopologyFile(@Nullable URL topologyFile) {
		this.topologyFile = topologyFile;
		return this;
	}

	/**
	 * Initializes the value for the {@link LocalCassandraFactory#isRegisterShutdownHook() registerShutdownHook}
	 * attribute.
	 *
	 * @param registerShutdownHook The value for registerShutdownHook
	 * @return {@code this} builder for use in a chained invocation
	 * @since 1.2.3
	 */
	public LocalCassandraFactoryBuilder setRegisterShutdownHook(boolean registerShutdownHook) {
		this.registerShutdownHook = registerShutdownHook;
		return this;
	}

	/**
	 * Initializes the value for the {@link LocalCassandraFactory#getCommitLogArchivingFile() commitLogArchivingFile}
	 * attribute.
	 *
	 * @param commitLogArchivingFile The value for commitLogArchivingFile
	 * @return {@code this} builder for use in a chained invocation
	 * @since 1.2.8
	 */
	public LocalCassandraFactoryBuilder setCommitLogArchivingFile(@Nullable URL commitLogArchivingFile) {
		this.commitLogArchivingFile = commitLogArchivingFile;
		return this;
	}

	/**
	 * Initializes the value for the {@link LocalCassandraFactory#getCommitLogArchivingFile() commitLogArchivingFile}
	 * attribute.
	 *
	 * @param commitLogArchivingFile The value for commitLogArchivingFile
	 * @return {@code this} builder for use in a chained invocation
	 * @since 1.2.8
	 */
	public LocalCassandraFactoryBuilder setCommitLogArchivingFile(@Nullable Path commitLogArchivingFile) {
		try {
			return setCommitLogArchivingFile(
					(commitLogArchivingFile != null) ? commitLogArchivingFile.toUri().toURL() : null);
		}
		catch (MalformedURLException ex) {
			throw new IllegalArgumentException(ex);
		}
	}

	/**
	 * Initializes the value for the {@link LocalCassandraFactory#getCommitLogArchivingFile() commitLogArchivingFile}
	 * attribute.
	 *
	 * @param commitLogArchivingFile The value for commitLogArchivingFile
	 * @return {@code this} builder for use in a chained invocation
	 * @since 1.2.8
	 */
	public LocalCassandraFactoryBuilder setCommitLogArchivingFile(@Nullable File commitLogArchivingFile) {
		try {
			return setCommitLogArchivingFile(
					(commitLogArchivingFile != null) ? commitLogArchivingFile.toURI().toURL() : null);
		}
		catch (MalformedURLException ex) {
			throw new IllegalArgumentException(ex);
		}
	}

	/**
	 * Initializes the value for the {@link LocalCassandraFactory#isDeleteWorkingDirectory} attribute.
	 *
	 * @param deleteWorkingDirectory The value for deleteWorkingDirectory
	 * @return {@code this} builder for use in a chained invocation
	 * @since 1.4.3
	 */
	@API(since = "1.4.3", status = API.Status.MAINTAINED)
	public LocalCassandraFactoryBuilder setDeleteWorkingDirectory(boolean deleteWorkingDirectory) {
		this.deleteWorkingDirectory = deleteWorkingDirectory;
		return this;
	}

	/**
	 * Builds a new {@link LocalCassandraFactory}.
	 *
	 * @return a new instance
	 */
	public LocalCassandraFactory build() {
		LocalCassandraFactory factory = new LocalCassandraFactory();
		factory.setVersion(this.version);
		factory.setWorkingDirectory(this.workingDirectory);
		factory.setArtifactFactory(this.artifactFactory);
		factory.setConfigurationFile(this.configurationFile);
		factory.setLogbackFile(this.logbackFile);
		factory.setTopologyFile(this.topologyFile);
		factory.setRackFile(this.rackFile);
		factory.setStartupTimeout(this.startupTimeout);
		factory.getJvmOptions().addAll(this.jvmOptions);
		factory.setJavaHome(this.javaHome);
		factory.setJmxPort(this.jmxPort);
		factory.setAllowRoot(this.allowRoot);
		factory.setRegisterShutdownHook(this.registerShutdownHook);
		factory.setCommitLogArchivingFile(this.commitLogArchivingFile);
		factory.setArtifactDirectory(this.artifactDirectory);
		factory.setDeleteWorkingDirectory(this.deleteWorkingDirectory);
		return factory;
	}

}
