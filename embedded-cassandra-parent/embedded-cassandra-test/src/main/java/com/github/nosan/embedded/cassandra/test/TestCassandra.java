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

import com.datastax.oss.driver.api.core.CqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.nosan.embedded.cassandra.Cassandra;
import com.github.nosan.embedded.cassandra.CassandraException;
import com.github.nosan.embedded.cassandra.CassandraFactory;
import com.github.nosan.embedded.cassandra.CassandraInterruptedException;
import com.github.nosan.embedded.cassandra.Settings;
import com.github.nosan.embedded.cassandra.Version;
import com.github.nosan.embedded.cassandra.cql.CqlScript;
import com.github.nosan.embedded.cassandra.lang.annotation.Nullable;
import com.github.nosan.embedded.cassandra.local.LocalCassandraFactory;
import com.github.nosan.embedded.cassandra.test.util.CqlUtils;

/**
 * Test {@link Cassandra} that allows the Cassandra to be {@link #start() started} and {@link #stop() stopped}. {@link
 * TestCassandra} does not launch {@link Cassandra} itself, it simply delegates calls to the underlying {@link
 * Cassandra}.
 *
 * @author Dmytro Nosan
 * @see CassandraFactory
 * @see CqlUtils
 * @see CqlScript
 * @since 1.0.0
 */
public class TestCassandra {

	private static final Logger log = LoggerFactory.getLogger(TestCassandra.class);

	private final Object monitor = new Object();

	private final Cassandra cassandra;

	private final List<CqlScript> scripts;

	private final SessionFactory sessionFactory;

	@Nullable
	private volatile CqlSession session;

	/**
	 * Creates a {@link TestCassandra}.
	 */
	public TestCassandra() {
		this(null, null, new CqlScript[0]);
	}

	/**
	 * Creates a {@link TestCassandra}.
	 *
	 * @param scripts CQL scripts to execute
	 */
	public TestCassandra(CqlScript... scripts) {
		this(null, null, scripts);
	}

	/**
	 * Creates a {@link TestCassandra}.
	 *
	 * @param sessionFactory factory to create a {@link CqlSession}
	 * @param scripts CQL scripts to execute
	 */
	public TestCassandra(@Nullable SessionFactory sessionFactory, CqlScript... scripts) {
		this(null, sessionFactory, scripts);
	}

	/**
	 * Creates a {@link TestCassandra}.
	 *
	 * @param cassandraFactory factory to create a {@link Cassandra}
	 * @param scripts CQL scripts to execute
	 */
	public TestCassandra(@Nullable CassandraFactory cassandraFactory, CqlScript... scripts) {
		this(cassandraFactory, null, scripts);
	}

	/**
	 * Creates a {@link TestCassandra}.
	 *
	 * @param cassandraFactory factory to create a {@link Cassandra}
	 * @param sessionFactory factory to create a {@link CqlSession}
	 * @param scripts CQL scripts to execute
	 */
	public TestCassandra(@Nullable CassandraFactory cassandraFactory, @Nullable SessionFactory sessionFactory,
			CqlScript... scripts) {
		Objects.requireNonNull(scripts, "Scripts must not be null");
		Cassandra cassandra = ((cassandraFactory != null) ? cassandraFactory : new LocalCassandraFactory()).create();
		this.cassandra = Objects.requireNonNull(cassandra, "Cassandra must not be null");
		this.sessionFactory = (sessionFactory != null) ? sessionFactory : new DefaultSessionFactory();
		this.scripts = Collections.unmodifiableList(Arrays.asList(scripts));
	}

	/**
	 * Starts the underlying {@link Cassandra}. Calling this method on an already started {@code Cassandra} has no
	 * effect. Causes the current thread to wait, until the {@code Cassandra} has started.
	 *
	 * @throws CassandraException if the underlying {@code Cassandra} cannot be started
	 * @throws CassandraInterruptedException if the current thread is {@link Thread#interrupt() interrupted} by another
	 * thread
	 */
	public void start() throws CassandraException {
		synchronized (this.monitor) {
			try {
				startInternal();
			}
			catch (CassandraException ex) {
				stopInternalSilently();
				throw ex;
			}
			catch (Exception ex) {
				stopInternalSilently();
				throw new CassandraException("Unable to start Test Cassandra", ex);
			}
		}
	}

	/**
	 * Stops the underlying {@link Cassandra}. Calling this method on an already stopped
	 * {@code Cassandra} has no effect. Causes the current thread to wait, until the {@code Cassandra} has stopped.
	 *
	 * @throws CassandraException if the underlying {@code Cassandra} cannot be stopped
	 * @throws CassandraInterruptedException if the current thread is {@link Thread#interrupt() interrupted}
	 * by another thread
	 */
	public void stop() throws CassandraException {
		synchronized (this.monitor) {
			try {
				stopInternal();
			}
			catch (CassandraException ex) {
				throw ex;
			}
			catch (Exception ex) {
				throw new CassandraException("Unable to stop Test Cassandra", ex);
			}
		}

	}

	/**
	 * Returns the settings of the underlying {@code Cassandra}.
	 *
	 * @return the settings
	 * @throws IllegalStateException If the underlying {@link Cassandra} is not running
	 */
	public Settings getSettings() throws IllegalStateException {
		synchronized (this.monitor) {
			return this.cassandra.getSettings();
		}
	}

	/**
	 * Returns the {@link Version version} of this {@code Cassandra}.
	 *
	 * @return a version
	 * @since 2.0.0
	 */
	public Version getVersion() {
		return this.cassandra.getVersion();
	}

	/**
	 * Initializes a singleton {@link CqlSession} using a {@link SessionFactory}.
	 *
	 * @return a session
	 */
	public CqlSession getSession() {
		CqlSession session = this.session;
		if (session == null) {
			synchronized (this.monitor) {
				session = this.session;
				if (session == null) {
					session = this.sessionFactory.create(getVersion(), getSettings());
					Objects.requireNonNull(session, "Session must not be null");
					this.session = session;
				}
			}
		}
		return session;
	}

	@Override
	public String toString() {
		return String.format("Test Cassandra '%s'", this.cassandra);
	}

	private void startInternal() {
		if (log.isDebugEnabled()) {
			log.debug("Starts Test Cassandra '{}'", this.cassandra);
		}
		this.cassandra.start();
		if (!this.scripts.isEmpty()) {
			CqlUtils.execute(getSession(), this.scripts.toArray(new CqlScript[0]));
		}
		if (log.isDebugEnabled()) {
			log.debug("Test Cassandra '{}' is started", this.cassandra);
		}
	}

	private void stopInternal() {
		try {
			CqlSession session = this.session;
			if (session != null) {
				session.close();
			}
		}
		catch (Exception ex) {
			log.error(String.format("Session '%s' is not closed", this.session), ex);
		}
		this.session = null;
		this.cassandra.stop();
		Cassandra.State state = this.cassandra.getState();
		if (log.isDebugEnabled() && state == Cassandra.State.STOPPED) {
			log.debug("Test Cassandra '{}' is stopped", this.cassandra);
		}
	}

	private void stopInternalSilently() {
		try {
			stopInternal();
		}
		catch (CassandraInterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		catch (Exception ex) {
			if (log.isDebugEnabled()) {
				log.error("Unable to stop Test Cassandra", ex);
			}
		}
	}

}
