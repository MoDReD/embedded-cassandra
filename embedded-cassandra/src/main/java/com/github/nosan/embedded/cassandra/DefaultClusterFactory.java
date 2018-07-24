/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.nosan.embedded.cassandra;

import java.util.Objects;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.HostDistance;
import com.datastax.driver.core.PoolingOptions;
import com.datastax.driver.core.QueryOptions;

/**
 * {@link ClusterFactory} with a default strategy.
 *
 * @author Dmytro Nosan
 */
public class DefaultClusterFactory implements ClusterFactory {

	@Override
	public Cluster getCluster(Config config, Version version) {
		Objects.requireNonNull(config, "Config must not be null");
		Objects.requireNonNull(version, "Version must not be null");

		QueryOptions queryOptions = new QueryOptions();
		queryOptions.setRefreshNodeIntervalMillis(0);
		queryOptions.setRefreshNodeListIntervalMillis(0);
		queryOptions.setRefreshSchemaIntervalMillis(0);

		PoolingOptions poolingOptions = new PoolingOptions()
				.setPoolTimeoutMillis(30000)
				.setMaxRequestsPerConnection(HostDistance.LOCAL, 32768)
				.setMaxRequestsPerConnection(HostDistance.REMOTE, 32768);

		return configure(Cluster.builder()
				.addContactPoint(config.getRpcAddress())
				.withQueryOptions(queryOptions)
				.withPoolingOptions(poolingOptions)
				.withoutJMXReporting()
				.withoutMetrics()
				.withPort(config.getNativeTransportPort()))
				.build();
	}

	protected Cluster.Builder configure(Cluster.Builder builder) {
		return builder;
	}
}
