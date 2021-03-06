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

import java.lang.annotation.Annotation;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.datastax.driver.core.Cluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
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

import com.github.nosan.embedded.cassandra.CassandraFactory;
import com.github.nosan.embedded.cassandra.Version;
import com.github.nosan.embedded.cassandra.cql.CqlScript;
import com.github.nosan.embedded.cassandra.local.LocalCassandraFactory;
import com.github.nosan.embedded.cassandra.local.artifact.ArtifactFactory;
import com.github.nosan.embedded.cassandra.local.artifact.RemoteArtifactFactory;
import com.github.nosan.embedded.cassandra.local.artifact.UrlFactory;
import com.github.nosan.embedded.cassandra.test.ClusterFactory;
import com.github.nosan.embedded.cassandra.test.TestCassandra;
import com.github.nosan.embedded.cassandra.util.StringUtils;
import com.github.nosan.embedded.cassandra.util.annotation.Nullable;

/**
 * {@link ContextCustomizer} used to create {@link EmbeddedCassandraFactoryBean}, {@link LocalCassandraFactoryBean} and
 * {@link EmbeddedClusterFactoryBean} beans.
 *
 * @author Dmytro Nosan
 * @since 1.0.0
 */
class EmbeddedCassandraContextCustomizer implements ContextCustomizer {

	private static final Logger log = LoggerFactory.getLogger(EmbeddedCassandraContextCustomizer.class);

	private static final String LOCAL_CASSANDRA_FACTORY_BEAN_NAME = "localCassandraFactory";

	private static final String EMBEDDED_CLUSTER_BEAN_NAME = "embeddedCluster";

	private static final String EMBEDDED_CASSANDRA_BEAN_NAME = "embeddedCassandra";

	@Override
	public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
		Class<?> testClass = mergedConfig.getTestClass();
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		if (beanFactory instanceof BeanDefinitionRegistry) {
			BeanDefinitionRegistry registry = ((BeanDefinitionRegistry) beanFactory);
			ifAnnotationPresent(testClass, EmbeddedLocalCassandra.class,
					annotation -> registerPrimaryBeanDefinition(registry, LOCAL_CASSANDRA_FACTORY_BEAN_NAME,
							BeanDefinitionBuilder.rootBeanDefinition(LocalCassandraFactoryBean.class)
									.addConstructorArgValue(testClass).addConstructorArgValue(annotation)
									.getBeanDefinition()));
			ifAnnotationPresent(testClass, EmbeddedCassandra.class, annotation -> {
				registerPrimaryBeanDefinition(registry, EMBEDDED_CASSANDRA_BEAN_NAME,
						BeanDefinitionBuilder.rootBeanDefinition(EmbeddedCassandraFactoryBean.class)
								.addConstructorArgValue(testClass).addConstructorArgValue(annotation)
								.getBeanDefinition());
				if (annotation.replace() == EmbeddedCassandra.Replace.ANY) {
					registerPrimaryBeanDefinition(registry, EMBEDDED_CLUSTER_BEAN_NAME,
							BeanDefinitionBuilder.rootBeanDefinition(EmbeddedClusterFactoryBean.class)
									.getBeanDefinition());
				}
			});
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

	private static <A extends Annotation> void ifAnnotationPresent(Class<?> testClass, Class<A> annotationClass,
			Consumer<A> consumer) {
		Optional.ofNullable(AnnotatedElementUtils.findMergedAnnotation(testClass, annotationClass)).ifPresent(consumer);
	}

	private static void registerPrimaryBeanDefinition(BeanDefinitionRegistry registry, String beanName,
			BeanDefinition beanDefinition) {
		if (registry.containsBeanDefinition(beanName)) {
			registry.removeBeanDefinition(beanName);
		}
		beanDefinition.setPrimary(true);
		registry.registerBeanDefinition(beanName, beanDefinition);
		log.info("The @Primary '{}' bean has been registered.", beanName);
	}

	/**
	 * {@link FactoryBean} used to create and configure a {@link TestCassandra}.
	 */
	static class EmbeddedCassandraFactoryBean
			implements FactoryBean<TestCassandra>, InitializingBean, DisposableBean, ApplicationContextAware {

		private final Class<?> testClass;

		private final EmbeddedCassandra annotation;

		@Nullable
		private TestCassandra cassandra;

		@Nullable
		private ApplicationContext applicationContext;

		EmbeddedCassandraFactoryBean(Class<?> testClass, EmbeddedCassandra annotation) {
			this.testClass = testClass;
			this.annotation = annotation;
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
			}
		}

		@Override
		public void afterPropertiesSet() {
			ApplicationContext applicationContext = Objects
					.requireNonNull(this.applicationContext, "Application Context must not be null");
			EmbeddedCassandra annotation = this.annotation;
			CqlConfig config = new CqlConfig(this.testClass, annotation.scripts(), annotation.statements(),
					annotation.encoding());
			CqlScript[] scripts = CqlResourceUtils.getScripts(applicationContext, config);
			TestCassandra cassandra = new TestCassandra(annotation.registerShutdownHook(),
					BeanFactoryUtils.getIfUnique(applicationContext, CassandraFactory.class),
					BeanFactoryUtils.getIfUnique(applicationContext, ClusterFactory.class), scripts);
			this.cassandra = cassandra;
			cassandra.start();
		}

		@Override
		public boolean isSingleton() {
			return true;
		}

	}

	/**
	 * {@link FactoryBean} used to create and configure a {@link Cluster}.
	 */
	static class EmbeddedClusterFactoryBean implements FactoryBean<Cluster>, InitializingBean, ApplicationContextAware {

		@Nullable
		private Cluster cluster;

		@Nullable
		private ApplicationContext applicationContext;

		@Override
		public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
			this.applicationContext = applicationContext;
		}

		@Override
		public Cluster getObject() {
			return Objects.requireNonNull(this.cluster, "Cluster must not be null");
		}

		@Override
		public Class<?> getObjectType() {
			return Cluster.class;
		}

		@Override
		public boolean isSingleton() {
			return true;
		}

		@Override
		public void afterPropertiesSet() {
			ApplicationContext applicationContext = Objects
					.requireNonNull(this.applicationContext, "Application Context must not be null");
			TestCassandra cassandra = applicationContext.getBean(EMBEDDED_CASSANDRA_BEAN_NAME, TestCassandra.class);
			this.cluster = cassandra.getCluster();
		}

	}

	/**
	 * {@link FactoryBean} used to create and configure a {@link LocalCassandraFactory}.
	 */
	static class LocalCassandraFactoryBean
			implements FactoryBean<LocalCassandraFactory>, InitializingBean, ApplicationContextAware {

		private final Class<?> testClass;

		private final EmbeddedLocalCassandra annotation;

		@Nullable
		private LocalCassandraFactory cassandraFactory;

		@Nullable
		private ApplicationContext applicationContext;

		LocalCassandraFactoryBean(Class<?> testClass, EmbeddedLocalCassandra annotation) {
			this.testClass = testClass;
			this.annotation = annotation;
		}

		@Override
		public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
			this.applicationContext = applicationContext;
		}

		@Override
		public LocalCassandraFactory getObject() {
			return Objects.requireNonNull(this.cassandraFactory, "Cassandra Factory must not be null");
		}

		@Override
		public Class<?> getObjectType() {
			return LocalCassandraFactory.class;
		}

		@Override
		public boolean isSingleton() {
			return true;
		}

		@Override
		public void afterPropertiesSet() {
			ApplicationContext applicationContext = Objects
					.requireNonNull(this.applicationContext, "Application Context must not be null");
			Environment environment = applicationContext.getEnvironment();
			EmbeddedLocalCassandra annotation = this.annotation;
			Class<?> testClass = this.testClass;
			String configurationFile = environment.resolvePlaceholders(annotation.configurationFile());
			String logbackFile = environment.resolvePlaceholders(annotation.logbackFile());
			String topologyFile = environment.resolvePlaceholders(annotation.topologyFile());
			String rackFile = environment.resolvePlaceholders(annotation.rackFile());
			String commitLogArchivingFile = environment.resolvePlaceholders(annotation.commitLogArchivingFile());
			String workingDirectory = environment.resolvePlaceholders(annotation.workingDirectory());
			String artifactDirectory = environment.resolvePlaceholders(annotation.artifactDirectory());
			String javaHome = environment.resolvePlaceholders(annotation.javaHome());
			String version = environment.resolvePlaceholders(annotation.version());
			Duration startupTimeout = Duration.ofMillis(annotation.startupTimeout());
			int jmxPort = annotation.jmxPort();
			boolean allowRoot = annotation.allowRoot();
			boolean registerShutdownHook = annotation.registerShutdownHook();
			boolean deleteWorkingDirectory = annotation.deleteWorkingDirectory();
			List<String> jvmOptions = Arrays.stream(annotation.jvmOptions()).map(environment::resolvePlaceholders)
					.filter(StringUtils::hasText).collect(Collectors.toList());

			LocalCassandraFactory factory = new LocalCassandraFactory();
			if (StringUtils.hasText(workingDirectory)) {
				factory.setWorkingDirectory(Paths.get(workingDirectory));
			}
			if (StringUtils.hasText(artifactDirectory)) {
				factory.setArtifactDirectory(Paths.get(artifactDirectory));
			}
			if (StringUtils.hasText(javaHome)) {
				factory.setJavaHome(Paths.get(javaHome));
			}
			if (StringUtils.hasText(version)) {
				factory.setVersion(Version.parse(version));
			}
			if (StringUtils.hasText(configurationFile)) {
				factory.setConfigurationFile(CqlResourceUtils.getURL(applicationContext, configurationFile, testClass));
			}
			if (StringUtils.hasText(logbackFile)) {
				factory.setLogbackFile(CqlResourceUtils.getURL(applicationContext, logbackFile, testClass));
			}
			if (StringUtils.hasText(topologyFile)) {
				factory.setTopologyFile(CqlResourceUtils.getURL(applicationContext, topologyFile, testClass));
			}
			if (StringUtils.hasText(rackFile)) {
				factory.setRackFile(CqlResourceUtils.getURL(applicationContext, rackFile, testClass));
			}
			if (StringUtils.hasText(commitLogArchivingFile)) {
				factory.setCommitLogArchivingFile(
						CqlResourceUtils.getURL(applicationContext, commitLogArchivingFile, testClass));
			}
			factory.setStartupTimeout(startupTimeout);
			factory.getJvmOptions().addAll(jvmOptions);
			factory.setJmxPort(jmxPort);
			factory.setAllowRoot(allowRoot);
			factory.setRegisterShutdownHook(registerShutdownHook);
			factory.setDeleteWorkingDirectory(deleteWorkingDirectory);
			ArtifactFactory artifactFactory = BeanFactoryUtils.getIfUnique(applicationContext, ArtifactFactory.class);
			if (artifactFactory != null) {
				factory.setArtifactFactory(artifactFactory);
			}
			else {
				factory.setArtifactFactory(getArtifactFactory(environment, annotation.artifact()));
			}
			this.cassandraFactory = factory;
		}

		private static ArtifactFactory getArtifactFactory(Environment environment,
				EmbeddedLocalCassandra.Artifact annotation) {
			String directory = environment.resolvePlaceholders(annotation.directory());
			String proxyHost = environment.resolvePlaceholders(annotation.proxyHost());
			int proxyPort = annotation.proxyPort();
			Proxy.Type proxyType = annotation.proxyType();
			Class<? extends UrlFactory> urlFactory = annotation.urlFactory();
			Duration readTimeout = Duration.ofMillis(annotation.readTimeout());
			Duration connectTimeout = Duration.ofMillis(annotation.connectTimeout());

			RemoteArtifactFactory factory = new RemoteArtifactFactory();
			if (StringUtils.hasText(directory)) {
				factory.setDirectory(Paths.get(directory));
			}
			if (proxyType != Proxy.Type.DIRECT && StringUtils.hasText(proxyHost) && proxyPort != -1) {
				factory.setProxy(new Proxy(proxyType, new InetSocketAddress(proxyHost, proxyPort)));
			}
			if (!UrlFactory.class.equals(urlFactory)) {
				factory.setUrlFactory(BeanUtils.instantiateClass(urlFactory));
			}
			factory.setReadTimeout(readTimeout);
			factory.setConnectTimeout(connectTimeout);
			return factory;
		}

	}

}
