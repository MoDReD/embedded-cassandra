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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.nosan.embedded.cassandra.test.DefaultSessionFactory;
import com.github.nosan.embedded.cassandra.test.TestCassandra;

/**
 * {@link Configuration} for configuring {@link com.datastax.oss.driver.api.core.CqlSession}.
 *
 * @author Dmytro Nosan
 */
@Configuration
public class SessionConfiguration {

	@Bean(destroyMethod = "close")
	public CqlSession cqlSession(TestCassandra testCassandra) {
		return new DefaultSessionFactory().create(testCassandra.getVersion(), testCassandra.getSettings());
	}

}
