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

package com.github.nosan.embedded.cassandra.test.testng;

import com.datastax.oss.driver.api.core.CqlSession;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import com.github.nosan.embedded.cassandra.Cassandra;
import com.github.nosan.embedded.cassandra.CassandraException;
import com.github.nosan.embedded.cassandra.CassandraFactory;
import com.github.nosan.embedded.cassandra.cql.CqlScript;
import com.github.nosan.embedded.cassandra.lang.annotation.Nullable;
import com.github.nosan.embedded.cassandra.test.SessionFactory;
import com.github.nosan.embedded.cassandra.test.TestCassandra;

/**
 * Base {@code test class} that allows the Cassandra to be {@link Cassandra#start() started} and {@link Cassandra#stop()
 * stopped}.
 * <p>
 * The typical usage is:
 * <pre>
 * public class CassandraTests extends CassandraTestNG {
 * public CassandraTests(){
 *     super(&#47;* constructor parameters *&#47;);
 * }
 * &#64;Test
 * public void test() {
 * //
 * }
 * }
 * </pre>
 *
 * @author Dmytro Nosan
 * @see CqlScript
 * @see SessionFactory
 * @see CassandraFactory
 * @since 1.0.0
 */
public class CassandraTestNG extends TestCassandra {

	/**
	 * Creates a {@link CassandraTestNG}.
	 */
	public CassandraTestNG() {
		super();
	}

	/**
	 * Creates a {@link CassandraTestNG}.
	 *
	 * @param scripts CQL scripts to execute
	 */
	public CassandraTestNG(CqlScript... scripts) {
		super(scripts);
	}

	/**
	 * Creates a {@link CassandraTestNG}.
	 *
	 * @param sessionFactory factory to create a {@link CqlSession}
	 * @param scripts CQL scripts to execute
	 */
	public CassandraTestNG(@Nullable SessionFactory sessionFactory, CqlScript... scripts) {
		super(sessionFactory, scripts);
	}

	/**
	 * Creates a {@link CassandraTestNG}.
	 *
	 * @param cassandraFactory factory to create a {@link Cassandra}
	 * @param scripts CQL scripts to execute
	 */
	public CassandraTestNG(@Nullable CassandraFactory cassandraFactory, CqlScript... scripts) {
		super(cassandraFactory, scripts);
	}

	/**
	 * Creates a {@link CassandraTestNG}.
	 *
	 * @param cassandraFactory factory to create a {@link Cassandra}
	 * @param sessionFactory factory to create a {@link CqlSession}
	 * @param scripts CQL scripts to execute
	 */
	public CassandraTestNG(@Nullable CassandraFactory cassandraFactory, @Nullable SessionFactory sessionFactory,
			CqlScript... scripts) {
		super(cassandraFactory, sessionFactory, scripts);
	}

	@BeforeClass(alwaysRun = true)
	@Override
	public void start() throws CassandraException {
		super.start();
	}

	@AfterClass(alwaysRun = true)
	@Override
	public void stop() throws CassandraException {
		super.stop();
	}

}
