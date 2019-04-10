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
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import com.github.nosan.embedded.cassandra.CassandraFactory;
import com.github.nosan.embedded.cassandra.Version;
import com.github.nosan.embedded.cassandra.cql.CqlScript;
import com.github.nosan.embedded.cassandra.cql.StaticCqlScript;
import com.github.nosan.embedded.cassandra.cql.UrlCqlScript;
import com.github.nosan.embedded.cassandra.lang.annotation.Nullable;
import com.github.nosan.embedded.cassandra.local.LocalCassandraFactory;
import com.github.nosan.embedded.cassandra.local.WorkingDirectoryCustomizer;
import com.github.nosan.embedded.cassandra.local.artifact.ArtifactFactory;
import com.github.nosan.embedded.cassandra.local.artifact.RemoteArtifactFactory;
import com.github.nosan.embedded.cassandra.local.artifact.UrlFactory;
import com.github.nosan.embedded.cassandra.test.SessionFactory;
import com.github.nosan.embedded.cassandra.test.TestCassandra;

/**
 * {@link ContextCustomizer} used to create {@link EmbeddedCassandraFactoryBean}.
 *
 * @author Dmytro Nosan
 * @since 1.0.0
 */
class EmbeddedCassandraContextCustomizer implements ContextCustomizer {

	@Override
	public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
		Class<?> testClass = mergedConfig.getTestClass();
		EmbeddedCassandra annotation = AnnotatedElementUtils.findMergedAnnotation(testClass, EmbeddedCassandra.class);
		if (annotation != null) {
			ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
			if (beanFactory instanceof BeanDefinitionRegistry) {
				BeanDefinitionRegistry registry = ((BeanDefinitionRegistry) beanFactory);
				registry.registerBeanDefinition(EmbeddedCassandra.class.getName(),
						BeanDefinitionBuilder.rootBeanDefinition(EmbeddedCassandraFactoryBean.class)
								.addConstructorArgValue(testClass)
								.getBeanDefinition());
			}
		}
	}

	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other != null && getClass() == other.getClass()));
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

	/**
	 * {@link FactoryBean} used to create and configure a {@link TestCassandra}.
	 */
	static class EmbeddedCassandraFactoryBean implements FactoryBean<TestCassandra>, InitializingBean,
			DisposableBean, ApplicationContextAware {

		private final Class<?> testClass;

		@Nullable
		private TestCassandra cassandra;

		@Nullable
		private ApplicationContext applicationContext;

		EmbeddedCassandraFactoryBean(Class<?> testClass) {
			this.testClass = testClass;
		}

		@Override
		public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
			this.applicationContext = applicationContext;
		}

		@Override
		public TestCassandra getObject() {
			return Objects.requireNonNull(this.cassandra, "Cassandra must not be null");
		}

		@Override
		public Class<?> getObjectType() {
			return TestCassandra.class;
		}

		@Override
		public void destroy() {
			TestCassandra cassandra = this.cassandra;
			if (cassandra != null) {
				cassandra.stop();
				this.cassandra = null;
			}
		}

		@Override
		public void afterPropertiesSet() {
			TestCassandra cassandra = new TestCassandra(getCassandraFactory(), getSessionFactory(), getScripts());
			this.cassandra = cassandra;
			cassandra.start();
		}

		@Override
		public boolean isSingleton() {
			return true;
		}

		@Nullable
		private CassandraFactory getCassandraFactory() {
			CassandraFactory cassandraFactory = getContext().getBeanProvider(CassandraFactory.class).getIfUnique();
			if (cassandraFactory != null) {
				return cassandraFactory;
			}
			EmbeddedLocalCassandra annotation = AnnotatedElementUtils.findMergedAnnotation(getTestClass(),
					EmbeddedLocalCassandra.class);
			return (annotation != null) ? getCassandraFactory(annotation) : null;
		}

		@Nullable
		private SessionFactory getSessionFactory() {
			return getContext().getBeanProvider(SessionFactory.class).getIfUnique();
		}

		private CqlScript[] getScripts() {
			EmbeddedCassandra annotation = AnnotatedElementUtils.findMergedAnnotation(getTestClass(),
					EmbeddedCassandra.class);
			List<CqlScript> scripts = new ArrayList<>();
			if (annotation != null) {
				String encoding = annotation.encoding();
				Charset charset = StringUtils.hasText(encoding) ? Charset.forName(encoding) : null;
				for (URL url : SpringResourceUtils.getResources(getContext(), getTestClass(), annotation.scripts())) {
					scripts.add(new UrlCqlScript(url, charset));
				}
				if (!ObjectUtils.isEmpty(annotation.statements())) {
					scripts.add(new StaticCqlScript(annotation.statements()));
				}
			}
			return scripts.toArray(new CqlScript[0]);
		}

		private CassandraFactory getCassandraFactory(EmbeddedLocalCassandra annotation) {
			Environment environment = getContext().getEnvironment();
			String version = environment.resolvePlaceholders(annotation.version());
			LocalCassandraFactory cassandraFactory = new LocalCassandraFactory();
			if (StringUtils.hasText(version)) {
				cassandraFactory.setVersion(Version.parse(version));
			}
			cassandraFactory.setWorkingDirectory(getPath(annotation.workingDirectory()));
			cassandraFactory.setArtifactDirectory(getPath(annotation.artifactDirectory()));
			cassandraFactory.setJavaHome(getPath(annotation.javaHome()));
			cassandraFactory.setConfigurationFile(getURL(annotation.configurationFile()));
			cassandraFactory.setLoggingFile(getURL(annotation.loggingFile()));
			cassandraFactory.setTopologyFile(getURL(annotation.topologyFile()));
			cassandraFactory.setRackFile(getURL(annotation.rackFile()));
			cassandraFactory.getJvmOptions().addAll(Arrays.stream(annotation.jvmOptions())
					.map(environment::resolvePlaceholders).filter(StringUtils::hasText).collect(Collectors.toList()));
			cassandraFactory.setJmxLocalPort(getPort(annotation.jmxLocalPort()));
			cassandraFactory.setPort(getPort(annotation.port()));
			cassandraFactory.setStoragePort(getPort(annotation.storagePort()));
			cassandraFactory.setSslStoragePort(getPort(annotation.sslStoragePort()));
			cassandraFactory.setRpcPort(getPort(annotation.rpcPort()));
			cassandraFactory.setAllowRoot(annotation.allowRoot());
			cassandraFactory.setRegisterShutdownHook(annotation.registerShutdownHook());
			cassandraFactory.setDeleteWorkingDirectory(annotation.deleteWorkingDirectory());
			cassandraFactory.setArtifactFactory(getArtifactFactory(annotation.artifact()));
			cassandraFactory.getCustomizers().addAll(getCustomizers());
			return cassandraFactory;
		}

		private ArtifactFactory getArtifactFactory(EmbeddedLocalCassandra.Artifact annotation) {
			return getContext().getBeanProvider(ArtifactFactory.class).getIfUnique(() -> {
				RemoteArtifactFactory artifactFactory = new RemoteArtifactFactory();
				String proxyHost = getContext().getEnvironment().resolvePlaceholders(annotation.proxyHost());
				int proxyPort = annotation.proxyPort();
				Proxy.Type proxyType = annotation.proxyType();
				if (proxyType != Proxy.Type.DIRECT && StringUtils.hasText(proxyHost) && proxyPort != -1) {
					artifactFactory.setProxy(new Proxy(proxyType, new InetSocketAddress(proxyHost, proxyPort)));
				}
				if (!UrlFactory.class.equals(annotation.urlFactory())) {
					artifactFactory.setUrlFactory(BeanUtils.instantiateClass(annotation.urlFactory()));
				}
				artifactFactory.setDirectory(getPath(annotation.directory()));
				artifactFactory.setReadTimeout(Duration.ofMillis(annotation.readTimeout()));
				artifactFactory.setConnectTimeout(Duration.ofMillis(annotation.connectTimeout()));
				return artifactFactory;
			});
		}

		private List<WorkingDirectoryCustomizer> getCustomizers() {
			return getContext().getBeanProvider(WorkingDirectoryCustomizer.class)
					.orderedStream().collect(Collectors.toList());
		}

		@Nullable
		private URL getURL(String location) {
			Environment environment = getContext().getEnvironment();
			return StringUtils.hasText(location) ? SpringResourceUtils.getResource(getContext(), getTestClass(),
					environment.resolvePlaceholders(location)) : null;
		}

		@Nullable
		private Path getPath(String location) {
			Environment environment = getContext().getEnvironment();
			return StringUtils.hasText(location) ? Paths.get(environment.resolvePlaceholders(location)) : null;
		}

		@Nullable
		private Integer getPort(int port) {
			return (port != -1) ? port : null;
		}

		private Class<?> getTestClass() {
			return this.testClass;
		}

		private ApplicationContext getContext() {
			ApplicationContext context = this.applicationContext;
			Objects.requireNonNull(context, "Application Context must not be null");
			return context;
		}

	}

}
