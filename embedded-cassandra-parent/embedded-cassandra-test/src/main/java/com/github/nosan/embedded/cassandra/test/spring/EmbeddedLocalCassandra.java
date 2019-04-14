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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.Proxy;
import java.nio.file.Path;

import org.springframework.core.annotation.AliasFor;
import org.springframework.core.io.Resource;

import com.github.nosan.embedded.cassandra.Cassandra;
import com.github.nosan.embedded.cassandra.CassandraFactory;
import com.github.nosan.embedded.cassandra.Version;
import com.github.nosan.embedded.cassandra.local.LocalCassandraFactory;
import com.github.nosan.embedded.cassandra.local.WorkingDirectoryCustomizer;
import com.github.nosan.embedded.cassandra.local.artifact.ArtifactFactory;
import com.github.nosan.embedded.cassandra.local.artifact.RemoteArtifactFactory;
import com.github.nosan.embedded.cassandra.local.artifact.UrlFactory;
import com.github.nosan.embedded.cassandra.test.SessionFactory;
import com.github.nosan.embedded.cassandra.test.TestCassandra;

/**
 * Annotation that can be specified on a test class that runs {@link Cassandra} based tests. This annotation
 * extends {@link EmbeddedCassandra} annotation and allows to customize {@link RemoteArtifactFactory} and {@link
 * LocalCassandraFactory}.
 * <p>The typical usage of this annotation is like:
 * <pre class="code">
 * &#064;RunWith(SpringRunner.class) //for JUnit4
 * &#064;EmbeddedLocalCassandra(version = "2.2.12", ...)
 * public class CassandraTests {
 * 	&#064;Test
 * 	void testMe(){}
 * }
 * }
 * </pre>
 * It is possible to define you own {@link SessionFactory}, {@link CassandraFactory}, {@link ArtifactFactory},
 * {@link WorkingDirectoryCustomizer} bean(s) to control {@link TestCassandra} instance.
 *
 * @author Dmytro Nosan
 * @see EmbeddedCassandra
 * @see LocalCassandraFactory
 * @see RemoteArtifactFactory
 * @since 1.2.6
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Inherited
@EmbeddedCassandra
public @interface EmbeddedLocalCassandra {

	/**
	 * Sets attribute for {@link LocalCassandraFactory#getVersion()}.
	 * <p>
	 * This value can contain a {@code spring} placeholder.
	 *
	 * @return The value of the {@code version} attribute
	 * @see Version
	 */
	String version() default "";

	/**
	 * Sets attribute for {@link LocalCassandraFactory#getConfigurationFile()}.
	 * <p>
	 * Path will be interpreted as a Spring {@link Resource}.
	 * <p>
	 * This value can contain a {@code spring} placeholder.
	 *
	 * @return The value of the {@code configurationFile} attribute
	 * @see Resource
	 */
	String configurationFile() default "";

	/**
	 * Sets attribute for {@link LocalCassandraFactory#getPort()}.
	 *
	 * @return The value of the {@code port} attribute, or {@code -1 same as null}
	 */
	int port() default -1;

	/**
	 * Sets attribute for {@link LocalCassandraFactory#getStoragePort()}.
	 *
	 * @return The value of the {@code storagePort} attribute, or {@code -1 same as null}
	 */
	int storagePort() default -1;

	/**
	 * Sets attribute for {@link LocalCassandraFactory#getSslStoragePort()}.
	 *
	 * @return The value of the {@code sslStoragePort} attribute, or {@code -1 same as null}
	 */
	int sslStoragePort() default -1;

	/**
	 * Sets attribute for {@link LocalCassandraFactory#getRpcPort()}.
	 *
	 * @return The value of the {@code rpcPort} attribute, or {@code -1 same as null}
	 */
	int rpcPort() default -1;

	/**
	 * Sets attribute for {@link LocalCassandraFactory#getJmxLocalPort()}.
	 *
	 * @return The value of the {@code jmxLocalPort} attribute, or {@code -1 same as null}
	 */
	int jmxLocalPort() default -1;

	/**
	 * Sets attribute for {@link LocalCassandraFactory#getJvmOptions()}.
	 * <p>
	 * This value can contain a {@code spring} placeholder.
	 *
	 * @return The value of the {@code jvmOptions} attribute
	 */
	String[] jvmOptions() default {};

	/**
	 * Sets attribute for {@link LocalCassandraFactory#getWorkingDirectory()}.
	 * <p>
	 * Path will be interpreted as a {@link Path}.
	 * <p>
	 * This value can contain a {@code spring} placeholder.
	 *
	 * @return The value of the {@code workingDirectory} attribute
	 * @see Path
	 */
	String workingDirectory() default "";

	/**
	 * Sets attribute for {@link LocalCassandraFactory#getLoggingFile()}.
	 * <p>
	 * Path will be interpreted as a Spring {@link Resource}.
	 * <p>
	 * This value can contain a {@code spring} placeholder.
	 *
	 * @return The value of the {@code logbackFile} attribute
	 * @see Resource
	 */
	String loggingFile() default "";

	/**
	 * Sets attribute for {@link LocalCassandraFactory#getRackFile()}.
	 * <p>
	 * Path will be interpreted as a Spring {@link Resource}.
	 * <p>
	 * This value can contain a {@code spring} placeholder.
	 *
	 * @return the value of {@code rackFile} attribute
	 * @see Resource
	 */
	String rackFile() default "";

	/**
	 * Sets attribute for {@link LocalCassandraFactory#getTopologyFile()}.
	 * <p>
	 * Path will be interpreted as a Spring {@link Resource}.
	 * <p>
	 * This value can contain a {@code spring} placeholder.
	 *
	 * @return the value of {@code topologyFile} attribute
	 * @see Resource
	 */
	String topologyFile() default "";

	/**
	 * Sets attribute for {@link LocalCassandraFactory#getJavaHome()}.
	 * <p>
	 * Path will be interpreted as a {@link Path}.
	 * <p>
	 * This value can contain a {@code spring} placeholder.
	 *
	 * @return The value of the {@code javaHome} attribute
	 * @see Path
	 */
	String javaHome() default "";

	/**
	 * Sets attribute for {@link LocalCassandraFactory#isAllowRoot()}.
	 *
	 * @return The value of the {@code allowRoot} attribute
	 */
	boolean allowRoot() default false;

	/**
	 * Sets attribute for {@link LocalCassandraFactory#isRegisterShutdownHook()}.
	 *
	 * @return The value of the {@code registerShutdownHook} attribute
	 */
	boolean registerShutdownHook() default true;

	/**
	 * Sets attribute for {@link LocalCassandraFactory#isDeleteWorkingDirectory()}.
	 *
	 * @return The value of the {@code deleteWorkingDirectory} attribute
	 * @since 2.0.0
	 */
	boolean deleteWorkingDirectory() default true;

	/**
	 * Sets attribute for {@link LocalCassandraFactory#getArtifactDirectory()}.
	 * <p>
	 * Path will be interpreted as a {@link Path}.
	 * <p>
	 * This value can contain a {@code spring} placeholder.
	 *
	 * @return The value of the {@code artifactDirectory} attribute
	 * @see Path
	 * @since 1.3.0
	 */
	String artifactDirectory() default "";

	/**
	 * Alias for {@link EmbeddedCassandra#scripts()}.
	 *
	 * @return CQL Scripts
	 */
	@AliasFor(annotation = EmbeddedCassandra.class)
	String[] scripts() default {};

	/**
	 * Alias for {@link EmbeddedCassandra#statements()}.
	 *
	 * @return CQL statements
	 */
	@AliasFor(annotation = EmbeddedCassandra.class)
	String[] statements() default {};

	/**
	 * Alias for {@link EmbeddedCassandra#encoding()}.
	 *
	 * @return CQL scripts encoding.
	 */
	@AliasFor(annotation = EmbeddedCassandra.class)
	String encoding() default "";

	/**
	 * Sets attribute for {@link LocalCassandraFactory#getArtifactFactory()}.
	 *
	 * @return The value of the {@code artifactFactory} attribute
	 */
	Artifact artifact() default @Artifact;

	/**
	 * Annotation that describes {@link RemoteArtifactFactory} attributes.
	 *
	 * @see RemoteArtifactFactory
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target({})
	@Documented
	@interface Artifact {

		/**
		 * Sets attribute for {@link RemoteArtifactFactory#getDirectory()}.
		 * <p>
		 * Path will be interpreted as a {@link Path}.
		 * <p>
		 * This value can contain a {@code spring} placeholder.
		 *
		 * @return The value of the {@code directory} attribute
		 */
		String directory() default "";

		/**
		 * Sets attribute for {@link RemoteArtifactFactory#getUrlFactory()}.
		 *
		 * @return The value of the {@code urlFactory} attribute
		 */
		Class<? extends UrlFactory> urlFactory() default UrlFactory.class;

		/**
		 * Sets proxy host attribute for {@link RemoteArtifactFactory#getProxy()}}.
		 * <p>
		 * This value can contain a {@code spring} placeholder.
		 *
		 * @return The value of the {@code proxyHost} attribute
		 */
		String proxyHost() default "";

		/**
		 * Sets proxy port attribute for {@link RemoteArtifactFactory#getProxy()}}.
		 *
		 * @return The value of the {@code proxyPort} attribute
		 */
		int proxyPort() default -1;

		/**
		 * Sets proxy type attribute for {@link RemoteArtifactFactory#getProxy()}}.
		 *
		 * @return The value of the {@code proxyType} attribute
		 */
		Proxy.Type proxyType() default Proxy.Type.HTTP;

		/**
		 * Sets attribute for {@link RemoteArtifactFactory#getReadTimeout()} in milliseconds.
		 *
		 * @return The value of the {@code readTimeout} attribute
		 */
		long readTimeout() default 30000;

		/**
		 * Sets attribute for {@link RemoteArtifactFactory#getConnectTimeout()} in milliseconds.
		 *
		 * @return The value of the {@code connectTimeout} attribute
		 */
		long connectTimeout() default 30000;

	}

}
