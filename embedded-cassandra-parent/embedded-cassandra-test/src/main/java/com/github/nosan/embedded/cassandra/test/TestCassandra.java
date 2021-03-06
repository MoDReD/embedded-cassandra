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

package com.github.nosan.embedded.cassandra.test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleStatement;
import com.datastax.driver.core.Statement;
import org.apiguardian.api.API;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.nosan.embedded.cassandra.Cassandra;
import com.github.nosan.embedded.cassandra.CassandraException;
import com.github.nosan.embedded.cassandra.CassandraFactory;
import com.github.nosan.embedded.cassandra.CassandraInterruptedException;
import com.github.nosan.embedded.cassandra.Settings;
import com.github.nosan.embedded.cassandra.cql.CqlScript;
import com.github.nosan.embedded.cassandra.local.LocalCassandraFactory;
import com.github.nosan.embedded.cassandra.test.util.CqlScriptUtils;
import com.github.nosan.embedded.cassandra.test.util.CqlUtils;
import com.github.nosan.embedded.cassandra.util.annotation.Nullable;

/**
 * Test {@link Cassandra} that allows the Cassandra to be {@link #start() started} and {@link #stop() stopped}. {@link
 * TestCassandra} does not launch {@link Cassandra} itself, it simply delegates calls to the underlying {@link
 * Cassandra}.
 * <p>
 * In addition to the basic functionality includes utility methods to test {@code Cassandra} code.
 *
 * @author Dmytro Nosan
 * @see TestCassandraBuilder
 * @see CassandraFactory
 * @see CqlScriptUtils
 * @see CqlUtils
 * @see CqlScript
 * @since 1.0.0
 */
@API(since = "1.0.0", status = API.Status.STABLE)
public class TestCassandra implements Cassandra {

	private static final Logger log = LoggerFactory.getLogger(TestCassandra.class);

	private final boolean registerShutdownHook;

	private final Object lock = new Object();

	private final List<CqlScript> scripts;

	private final ClusterFactory clusterFactory;

	private final CassandraFactory cassandraFactory;

	private volatile State state = State.NEW;

	@Nullable
	private volatile Cassandra cassandra;

	@Nullable
	private volatile Cluster cluster;

	@Nullable
	private volatile Session session;

	@Nullable
	private volatile Thread startThread;

	private boolean shutdownHookRegistered = false;

	/**
	 * Creates a {@link TestCassandra}.
	 *
	 * @param scripts CQL scripts to execute
	 */
	public TestCassandra(@Nullable CqlScript... scripts) {
		this(true, null, null, scripts);
	}

	/**
	 * Creates a {@link TestCassandra}.
	 *
	 * @param scripts CQL scripts to execute
	 * @param registerShutdownHook whether shutdown hook should be registered or not
	 */
	public TestCassandra(boolean registerShutdownHook, @Nullable CqlScript... scripts) {
		this(registerShutdownHook, null, null, scripts);
	}

	/**
	 * Creates a {@link TestCassandra}.
	 *
	 * @param clusterFactory factory to create a {@link Cluster}
	 * @param scripts CQL scripts to execute
	 */
	public TestCassandra(@Nullable ClusterFactory clusterFactory, @Nullable CqlScript... scripts) {
		this(true, null, clusterFactory, scripts);
	}

	/**
	 * Creates a {@link TestCassandra}.
	 *
	 * @param cassandraFactory factory to create a {@link Cassandra}
	 * @param scripts CQL scripts to execute
	 */
	public TestCassandra(@Nullable CassandraFactory cassandraFactory, @Nullable CqlScript... scripts) {
		this(true, cassandraFactory, null, scripts);
	}

	/**
	 * Creates a {@link TestCassandra}.
	 *
	 * @param clusterFactory factory to create a {@link Cluster}
	 * @param scripts CQL scripts to execute
	 * @param registerShutdownHook whether shutdown hook should be registered or not
	 */
	public TestCassandra(boolean registerShutdownHook, @Nullable ClusterFactory clusterFactory,
			@Nullable CqlScript... scripts) {
		this(registerShutdownHook, null, clusterFactory, scripts);
	}

	/**
	 * Creates a {@link TestCassandra}.
	 *
	 * @param cassandraFactory factory to create a {@link Cassandra}
	 * @param scripts CQL scripts to execute
	 * @param registerShutdownHook whether shutdown hook should be registered or not
	 */
	public TestCassandra(boolean registerShutdownHook, @Nullable CassandraFactory cassandraFactory,
			@Nullable CqlScript... scripts) {
		this(registerShutdownHook, cassandraFactory, null, scripts);
	}

	/**
	 * Creates a {@link TestCassandra}.
	 *
	 * @param cassandraFactory factory to create a {@link Cassandra}
	 * @param clusterFactory factory to create a {@link Cluster}
	 * @param scripts CQL scripts to execute
	 */
	public TestCassandra(@Nullable CassandraFactory cassandraFactory, @Nullable ClusterFactory clusterFactory,
			@Nullable CqlScript... scripts) {
		this(true, cassandraFactory, clusterFactory, scripts);
	}

	/**
	 * Creates a {@link TestCassandra}.
	 *
	 * @param cassandraFactory factory to create a {@link Cassandra}
	 * @param clusterFactory factory to create a {@link Cluster}
	 * @param scripts CQL scripts to execute
	 * @param registerShutdownHook whether shutdown hook should be registered or not
	 */
	public TestCassandra(boolean registerShutdownHook, @Nullable CassandraFactory cassandraFactory,
			@Nullable ClusterFactory clusterFactory, @Nullable CqlScript... scripts) {
		this.cassandraFactory = (cassandraFactory != null) ? cassandraFactory : new LocalCassandraFactory();
		this.scripts = Collections.unmodifiableList(Arrays.asList((scripts != null) ? scripts : new CqlScript[0]));
		this.clusterFactory = (clusterFactory != null) ? clusterFactory : new DefaultClusterFactory();
		this.registerShutdownHook = registerShutdownHook;
	}

	@Override
	public void start() throws CassandraException {
		synchronized (this.lock) {
			if (this.state != State.STARTED) {
				try {
					registerShutdownHook();
				}
				catch (Throwable ex) {
					throw new CassandraException("Unable to register a shutdown hook for Test Cassandra", ex);
				}
				try {
					this.startThread = Thread.currentThread();
					this.state = State.STARTING;
					startInternal();
					this.state = State.STARTED;
					this.startThread = null;
				}
				catch (CassandraInterruptedException ex) {
					this.startThread = null;
					this.state = State.START_INTERRUPTED;
					boolean interrupted = Thread.interrupted();
					stopInternalSilently();
					if (interrupted) {
						Thread.currentThread().interrupt();
					}
					throw ex;
				}
				catch (Throwable ex) {
					this.startThread = null;
					this.state = State.START_FAILED;
					stopInternalSilently();
					throw new CassandraException("Unable to start Test Cassandra", ex);
				}
			}
		}
	}

	@Override
	public void stop() throws CassandraException {
		synchronized (this.lock) {
			if (this.state != State.STOPPED) {
				try {
					this.state = State.STOPPING;
					stopInternal();
					this.state = State.STOPPED;
				}
				catch (CassandraInterruptedException ex) {
					this.state = State.STOP_INTERRUPTED;
					throw ex;
				}
				catch (Throwable ex) {
					this.state = State.STOP_FAILED;
					throw new CassandraException("Unable to stop Test Cassandra", ex);
				}
			}
		}
	}

	@Override
	public Settings getSettings() throws CassandraException {
		return getCassandra().getSettings();
	}

	@Override
	public State getState() {
		return this.state;
	}

	/**
	 * Initializes a singleton {@link Cluster} via {@link ClusterFactory}. This {@link Cluster} will be closed by this
	 * {@code Cassandra}.
	 *
	 * @return a {@link Cluster} instance
	 */
	public Cluster getCluster() {
		Cluster cluster = this.cluster;
		if (cluster == null) {
			synchronized (this.lock) {
				cluster = this.cluster;
				if (cluster == null) {
					cluster = this.clusterFactory.create(getSettings());
					Objects.requireNonNull(cluster, "Cluster must not be null.");
					this.cluster = cluster;
				}
			}
		}
		return cluster;
	}

	/**
	 * Initializes a singleton {@link Session} using a {@link #getCluster() Cluster}. This {@link Session } will be
	 * closed by this {@code Cassandra}.
	 *
	 * @return a non-initialized {@link Session} on this {@link #getCluster() Cluster}.
	 */
	public Session getSession() {
		Session session = this.session;
		if (session == null) {
			synchronized (this.lock) {
				session = this.session;
				if (session == null) {
					session = getCluster().newSession();
					this.session = session;
				}
			}
		}
		return session;
	}

	/**
	 * Delete all rows from the specified tables.
	 *
	 * @param tableNames the names of the tables to delete from
	 * @see CqlUtils#deleteFromTables(Session, String...)
	 * @since 1.0.6
	 */
	public void deleteFromTables(String... tableNames) {
		CqlUtils.deleteFromTables(getSession(), tableNames);
	}

	/**
	 * Drop the specified tables.
	 *
	 * @param tableNames the names of the tables to drop
	 * @see CqlUtils#dropTables(Session, String...)
	 * @since 1.0.6
	 */
	public void dropTables(String... tableNames) {
		CqlUtils.dropTables(getSession(), tableNames);
	}

	/**
	 * Drop the specified keyspaces.
	 *
	 * @param keyspaceNames the names of the keyspaces to drop
	 * @see CqlUtils#dropKeyspaces(Session, String...)
	 * @since 1.0.6
	 */
	public void dropKeyspaces(String... keyspaceNames) {
		CqlUtils.dropKeyspaces(getSession(), keyspaceNames);
	}

	/**
	 * Count the rows in the given table.
	 *
	 * @param tableName name of the table to count rows in
	 * @return the number of rows in the table
	 * @see CqlUtils#getRowCount(Session, String)
	 * @since 1.0.6
	 */
	public long getRowCount(String tableName) {
		return CqlUtils.getRowCount(getSession(), tableName);
	}

	/**
	 * Executes the given scripts.
	 *
	 * @param scripts the CQL scripts to execute.
	 * @see CqlScriptUtils#executeScripts(Session, CqlScript...)
	 * @since 1.0.6
	 */
	public void executeScripts(CqlScript... scripts) {
		CqlScriptUtils.executeScripts(getSession(), scripts);
	}

	/**
	 * Executes the provided query using the provided values.
	 *
	 * @param statement the CQL query to execute.
	 * @param args values required for the execution of {@code query}. See {@link
	 * SimpleStatement#SimpleStatement(String, Object...)} for more details.
	 * @return the result of the query. That result will never be null but can be empty (and will be for any non
	 * SELECT query).
	 * @see CqlUtils#executeStatement(Session, String, Object...)
	 * @since 1.0.6
	 */
	public ResultSet executeStatement(String statement, @Nullable Object... args) {
		return CqlUtils.executeStatement(getSession(), statement, args);
	}

	/**
	 * Executes the provided statement.
	 *
	 * @param statement the CQL statement to execute
	 * @return the result of the query. That result will never be null but can be empty (and will be for any non SELECT
	 * query).
	 * @see CqlUtils#executeStatement(Session, Statement)
	 * @since 1.2.8
	 */
	public ResultSet executeStatement(Statement statement) {
		return CqlUtils.executeStatement(getSession(), statement);
	}

	@Override
	public String toString() {
		return String.format("Test Cassandra '%s'", getCassandra());
	}

	/**
	 * Returns the underlying {@link Cassandra}.
	 *
	 * @return the underlying {@link Cassandra}.
	 * @since 1.4.1
	 */
	@API(since = "1.4.1", status = API.Status.EXPERIMENTAL)
	public Cassandra getCassandra() {
		Cassandra cassandra = this.cassandra;
		if (cassandra == null) {
			synchronized (this.lock) {
				cassandra = this.cassandra;
				if (cassandra == null) {
					cassandra = this.cassandraFactory.create();
					Objects.requireNonNull(cassandra, "Cassandra must not be null");
					this.cassandra = cassandra;
				}
			}
		}
		return cassandra;
	}

	private void startInternal() {
		Cassandra cassandra = getCassandra();
		if (log.isDebugEnabled()) {
			log.debug("Starts Test Cassandra '{}'", cassandra);
		}
		cassandra.start();
		if (!this.scripts.isEmpty()) {
			executeScripts(this.scripts.toArray(new CqlScript[0]));
		}
		if (log.isDebugEnabled()) {
			log.debug("Test Cassandra '{}' has been started", cassandra);
		}
	}

	private void stopInternal() {
		try {
			Session session = this.session;
			if (session != null) {
				if (log.isDebugEnabled()) {
					log.debug("Closes a session '{}'", session);
				}
				session.close();
			}
		}
		catch (Throwable ex) {
			log.error(String.format("Session '%s' has not been closed", this.session), ex);
		}
		this.session = null;

		try {
			Cluster cluster = this.cluster;
			if (cluster != null) {
				if (log.isDebugEnabled()) {
					log.debug("Closes a cluster '{}'", cluster);
				}
				cluster.close();
			}
		}
		catch (Throwable ex) {
			log.error(String.format("Cluster '%s' has not been closed", this.cluster), ex);
		}
		this.cluster = null;

		Cassandra cassandra = this.cassandra;
		if (cassandra != null) {
			cassandra.stop();
			if (log.isDebugEnabled() && cassandra.getState() == State.STOPPED) {
				log.debug("Test Cassandra '{}' has been stopped", cassandra);
			}
		}
		this.cassandra = null;
	}

	private void registerShutdownHook() {
		if (this.registerShutdownHook && !this.shutdownHookRegistered) {
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				Optional.ofNullable(this.startThread).ifPresent(Thread::interrupt);
				stop();
			}, "Test Cassandra Shutdown Hook"));
			this.shutdownHookRegistered = true;
		}
	}

	private void stopInternalSilently() {
		try {
			stopInternal();
		}
		catch (CassandraInterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		catch (Throwable ex) {
			if (log.isDebugEnabled()) {
				log.error("Unable to stop Test Cassandra", ex);
			}
		}
	}

}
