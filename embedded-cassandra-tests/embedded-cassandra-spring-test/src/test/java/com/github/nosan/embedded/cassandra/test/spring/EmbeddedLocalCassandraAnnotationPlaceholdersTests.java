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

package com.github.nosan.embedded.cassandra.test.spring;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.file.Paths;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.github.nosan.embedded.cassandra.Version;
import com.github.nosan.embedded.cassandra.local.LocalCassandraFactory;
import com.github.nosan.embedded.cassandra.local.artifact.ArtifactFactory;
import com.github.nosan.embedded.cassandra.local.artifact.DefaultUrlFactory;
import com.github.nosan.embedded.cassandra.local.artifact.RemoteArtifactFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EmbeddedLocalCassandra}.
 *
 * @author Dmytro Nosan
 */
@SuppressWarnings("NullableProblems")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ExcludeCassandraBeanDefinitionRegistryPostProcessor.class)
@TestPropertySource("classpath:application.properties")
@EmbeddedLocalCassandra(version = "${version}", configurationFile = "${configurationFile}",
		logbackFile = "${logbackFile}", rackFile = "${rackFile}", workingDirectory = "${workingDirectory}",
		artifactDirectory = "${artifactDirectory}", javaHome = "${javaHome}", jvmOptions = {"${jvmOptions}"},
		topologyFile = "${topologyFile}", commitLogArchivingFile = "${commitLogArchivingFile}", startupTimeout = 240000,
		jmxPort = 8000, registerTestShutdownHook = false, allowRoot = true, registerShutdownHook = false,
		deleteWorkingDirectory = true,
		artifact = @EmbeddedLocalCassandra.Artifact(directory = "${artifactDirectory}", proxyHost = "${proxyHost}",
				proxyPort = 8080, readTimeout = 15000, connectTimeout = 20000, proxyType = Proxy.Type.SOCKS,
				urlFactory = DefaultUrlFactory.class))
class EmbeddedLocalCassandraAnnotationPlaceholdersTests {

	@Autowired
	private LocalCassandraFactory factory;

	@Test
	void shouldRegisterLocalFactoryBean() {
		LocalCassandraFactory factory = this.factory;
		ArtifactFactory artifactFactory = factory.getArtifactFactory();
		assertThat(artifactFactory).isInstanceOf(RemoteArtifactFactory.class);
		RemoteArtifactFactory af = (RemoteArtifactFactory) artifactFactory;
		assertThat(af).isNotNull();
		assertThat(af.getDirectory()).isEqualTo(Paths.get("target/artifact"));
		assertThat(af.getReadTimeout()).isEqualTo(Duration.ofSeconds(15));
		assertThat(af.getConnectTimeout()).isEqualTo(Duration.ofSeconds(20));
		assertThat(af.getUrlFactory()).isInstanceOf(DefaultUrlFactory.class);
		assertThat(af.getProxy()).isNotNull();
		assertThat(af.getProxy().address()).isEqualTo(new InetSocketAddress("localhost", 8080));
		assertThat(af.getProxy().type()).isEqualTo(Proxy.Type.SOCKS);
		assertThat(factory.getVersion()).isEqualTo(Version.parse("3.11.3"));
		assertThat(factory.getWorkingDirectory()).isEqualTo(Paths.get("target/cassandra"));
		assertThat(factory.getArtifactDirectory()).isEqualTo(Paths.get("target/artifact"));
		assertThat(factory.getJavaHome()).isEqualTo(Paths.get("target/java"));
		assertThat(factory.getStartupTimeout()).isEqualTo(Duration.ofMinutes(4));
		assertThat(factory.getLogbackFile()).isEqualTo(getClass().getResource("/logback-test.xml"));
		assertThat(factory.getTopologyFile()).isEqualTo(getClass().getResource("/topology.properties"));
		assertThat(factory.getCommitLogArchivingFile())
				.isEqualTo(getClass().getResource("/commit_log_archiving.properties"));
		assertThat(factory.getRackFile()).isEqualTo(getClass().getResource("/rack.properties"));
		assertThat(factory.getConfigurationFile()).isEqualTo(getClass().getResource("/cassandra.yaml"));
		assertThat(factory.getJvmOptions()).containsExactly("-Dtest.property=property");
		assertThat(factory.getJmxPort()).isEqualTo(8000);
		assertThat(factory.isAllowRoot()).isTrue();
		assertThat(factory.isRegisterShutdownHook()).isFalse();
		assertThat(factory.isDeleteWorkingDirectory()).isTrue();
	}

}
