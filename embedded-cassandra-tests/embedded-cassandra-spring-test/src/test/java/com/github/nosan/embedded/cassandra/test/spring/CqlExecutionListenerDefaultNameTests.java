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

import com.datastax.driver.core.Cluster;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.github.nosan.embedded.cassandra.local.LocalCassandraFactory;
import com.github.nosan.embedded.cassandra.local.LocalCassandraFactoryBuilder;
import com.github.nosan.embedded.cassandra.test.jupiter.CassandraExtension;
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
@Cql(scripts = "/init.cql")
@Cql(statements = "DROP KEYSPACE test", executionPhase = Cql.ExecutionPhase.AFTER_TEST_METHOD)
class CqlExecutionListenerDefaultNameTests {

	@RegisterExtension
	static final CassandraExtension cassandra = new CassandraExtension(getFactory());

	@Autowired
	private Cluster cluster;

	@Test
	@Cql(scripts = "/users-data.cql")
	void shouldHaveUser() {
		assertThat(CqlUtils.getRowCount(this.cluster.connect(), "test.users")).isEqualTo(1);
	}

	@Test
	void shouldNotHaveUser() {
		assertThat(CqlUtils.getRowCount(this.cluster.connect(), "test.users")).isZero();
	}

	private static LocalCassandraFactory getFactory() {
		return new LocalCassandraFactoryBuilder().setDeleteWorkingDirectory(true).build();
	}

	@Configuration
	static class TestConfiguration {

		@Bean
		public Cluster customCluster() {
			return Cluster.builder().withPort(9000).addContactPoint("localhost").build();
		}

		@Bean
		public Cluster cluster() {
			return cassandra.getCluster();
		}

	}

}
