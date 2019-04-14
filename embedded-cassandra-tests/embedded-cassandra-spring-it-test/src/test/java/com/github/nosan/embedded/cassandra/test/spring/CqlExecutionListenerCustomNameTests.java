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

import com.datastax.oss.driver.api.core.CqlSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.github.nosan.embedded.cassandra.test.junit5.CassandraExtension;
import com.github.nosan.embedded.cassandra.test.util.CqlUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CqlExecutionListener}.
 *
 * @author Dmytro Nosan
 */
@SuppressWarnings("NullableProblems")
@ExtendWith(SpringExtension.class)
@ContextConfiguration
@Cql(session = "customSession", scripts = "/init.cql")
@Cql(session = "customSession", statements = "DROP KEYSPACE test",
		executionPhase = Cql.ExecutionPhase.AFTER_TEST_METHOD)
class CqlExecutionListenerCustomNameTests {

	@RegisterExtension
	public static final CassandraExtension cassandra = new CassandraExtension();

	@Autowired
	private CqlSession customSession;

	@Test
	@Cql(session = "customSession", scripts = "/users-data.cql")
	void shouldHaveUser() {
		assertThat(CqlUtils.getCount(this.customSession, "test.users")).isEqualTo(1);
	}

	@Test
	void shouldNotHaveUser() {
		assertThat(CqlUtils.getCount(this.customSession, "test.users")).isZero();
	}

	@Configuration
	static class TestConfiguration {

		@Bean
		public CqlSession customSession() {
			return cassandra.getSession();
		}

	}

}
