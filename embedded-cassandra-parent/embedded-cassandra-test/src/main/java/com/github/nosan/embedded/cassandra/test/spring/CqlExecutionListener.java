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

import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.datastax.oss.driver.api.core.CqlSession;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import com.github.nosan.embedded.cassandra.cql.CqlScript;
import com.github.nosan.embedded.cassandra.cql.StaticCqlScript;
import com.github.nosan.embedded.cassandra.cql.UrlCqlScript;
import com.github.nosan.embedded.cassandra.lang.annotation.Nullable;
import com.github.nosan.embedded.cassandra.test.spring.Cql.ExecutionPhase;
import com.github.nosan.embedded.cassandra.test.util.CqlUtils;

/**
 * {@code TestExecutionListener} that provides support for executing CQL {@link Cql#scripts scripts} and {@link
 * Cql#statements statements} configured via the {@link Cql @Cql} annotation.
 * <p>Scripts and statements will be executed {@link #beforeTestMethod(TestContext) before}
 * or {@link #afterTestMethod(TestContext) after} execution of the corresponding {@link java.lang.reflect.Method test
 * method}, depending on the configured value of the {@link Cql#executionPhase executionPhase} flag.
 *
 * @author Dmytro Nosan
 * @see Cql
 * @see CqlGroup
 * @since 1.0.0
 */
public final class CqlExecutionListener extends AbstractTestExecutionListener {

	private static final List<String> SESSION_BEANS = Arrays.asList("cassandraCqlSession", "cassandraSession");

	@Override
	public int getOrder() {
		return 5000;
	}

	@Override
	public void beforeTestMethod(TestContext testContext) {
		executeCqlScripts(testContext, ExecutionPhase.BEFORE_TEST_METHOD);
	}

	@Override
	public void afterTestMethod(TestContext testContext) {
		executeCqlScripts(testContext, ExecutionPhase.AFTER_TEST_METHOD);
	}

	private static void executeCqlScripts(TestContext testContext, ExecutionPhase executionPhase) {
		Set<Cql> methodAnnotations = AnnotatedElementUtils
				.findMergedRepeatableAnnotations(testContext.getTestMethod(), Cql.class, CqlGroup.class);
		Set<Cql> classAnnotations = AnnotatedElementUtils
				.findMergedRepeatableAnnotations(testContext.getTestClass(), Cql.class, CqlGroup.class);

		if (executionPhase == ExecutionPhase.BEFORE_TEST_METHOD) {
			executeCqlScripts(classAnnotations, executionPhase, testContext);
			executeCqlScripts(methodAnnotations, executionPhase, testContext);
		}
		else if (executionPhase == ExecutionPhase.AFTER_TEST_METHOD) {
			executeCqlScripts(methodAnnotations, executionPhase, testContext);
			executeCqlScripts(classAnnotations, executionPhase, testContext);
		}
	}

	private static void executeCqlScripts(Set<Cql> cqlAnnotations, ExecutionPhase executionPhase,
			TestContext testContext) {
		for (Cql cql : cqlAnnotations) {
			if (executionPhase == cql.executionPhase()) {
				executeCqlScripts(testContext, cql);
			}
		}
	}

	private static void executeCqlScripts(TestContext testContext, Cql cql) {
		CqlScript[] scripts = getScripts(cql, testContext.getTestClass(), testContext.getApplicationContext());
		if (scripts.length > 0) {
			CqlUtils.execute(getSession(cql.session(), testContext), scripts);
		}
	}

	private static CqlScript[] getScripts(Cql annotation, Class<?> testClass, ApplicationContext context) {
		List<CqlScript> scripts = new ArrayList<>();
		for (URL url : SpringResourceUtils.getResources(context, testClass, annotation.scripts())) {
			scripts.add(new UrlCqlScript(url, getCharset(annotation)));
		}
		if (!ObjectUtils.isEmpty(annotation.statements())) {
			scripts.add(new StaticCqlScript(annotation.statements()));
		}
		return scripts.toArray(new CqlScript[0]);
	}

	@Nullable
	private static Charset getCharset(Cql annotation) {
		return StringUtils.hasText(annotation.encoding()) ? Charset.forName(annotation.encoding()) : null;
	}

	private static CqlSession getSession(String name, TestContext testContext) {
		ApplicationContext applicationContext = testContext.getApplicationContext();
		if (StringUtils.hasText(name)) {
			return applicationContext.getBean(name, CqlSession.class);
		}
		CqlSession session = applicationContext.getBeanProvider(CqlSession.class).getIfUnique();
		if (session != null) {
			return session;
		}
		String[] beanNames = applicationContext.getBeanNamesForType(CqlSession.class);
		for (String sessionBeanName : SESSION_BEANS) {
			for (String beanName : beanNames) {
				if (beanName.equals(sessionBeanName)) {
					return applicationContext.getBean(sessionBeanName, CqlSession.class);
				}
			}
		}
		throw new NoSuchBeanDefinitionException(CqlSession.class);
	}

}
