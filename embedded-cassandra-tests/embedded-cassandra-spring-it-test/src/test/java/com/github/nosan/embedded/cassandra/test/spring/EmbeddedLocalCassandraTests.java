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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.github.nosan.embedded.cassandra.Version;
import com.github.nosan.embedded.cassandra.local.WorkingDirectoryCustomizer;
import com.github.nosan.embedded.cassandra.test.TestCassandra;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EmbeddedLocalCassandra}.
 *
 * @author Dmytro Nosan
 */
@SuppressWarnings("NullableProblems")
@ExtendWith(SpringExtension.class)
@ContextConfiguration
@EmbeddedLocalCassandra(version = "3.11.3", scripts = "/init.cql",
		statements = "CREATE TABLE IF NOT EXISTS test.roles (   id text PRIMARY  KEY );")
class EmbeddedLocalCassandraTests {

	private static boolean invoked = false;

	@Autowired
	private TestCassandra cassandra;

	@Autowired
	private CqlSession session;

	@Test
	void shouldOverrideVersion() {
		assertThat(invoked).describedAs("Customizer is not invoked").isTrue();
		assertThat(this.cassandra.getVersion()).isEqualTo(Version.parse("3.11.3"));
		assertThat(this.session.execute("SELECT * FROM test.roles").wasApplied()).isTrue();
	}

	@Configuration
	@Import(SessionConfiguration.class)
	static class TestConfiguration {

		@Bean
		public WorkingDirectoryCustomizer customizer() {
			return (workingDirectory, version) -> invoked = true;
		}

	}

}
