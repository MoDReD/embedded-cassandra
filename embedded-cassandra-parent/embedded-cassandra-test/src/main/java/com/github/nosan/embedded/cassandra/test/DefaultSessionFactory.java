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

import java.net.InetSocketAddress;
import java.time.Duration;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;

import com.github.nosan.embedded.cassandra.Settings;
import com.github.nosan.embedded.cassandra.Version;

/**
 * {@link SessionFactory} with a default strategy.
 *
 * @author Dmytro Nosan
 * @since 2.0.0
 */
public class DefaultSessionFactory implements SessionFactory {

	private static final String DATACENTER = "datacenter1";

	@Override
	public CqlSession create(Version version, Settings settings) {
		return CqlSession.builder()
				.addContactPoint(new InetSocketAddress(settings.getAddress(), settings.getPort()))
				.withLocalDatacenter(DATACENTER)
				.withConfigLoader(DriverConfigLoader.programmaticBuilder()
						.withBoolean(DefaultDriverOption.RECONNECT_ON_INIT, true)
						.withDuration(DefaultDriverOption.RECONNECTION_MAX_DELAY, Duration.ofSeconds(30))
						.withDuration(DefaultDriverOption.REQUEST_TIMEOUT, Duration.ofSeconds(30))
						.build())
				.build();
	}

}
