/*
 * Copyright 2018-2018 the original author or authors.
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

import javax.annotation.Nonnull;

import org.apiguardian.api.API;

import com.github.nosan.embedded.cassandra.local.LocalCassandraFactory;

/**
 * Factory that creates a {@link Cassandra}.
 *
 * @author Dmytro Nosan
 * @see Cassandra
 * @see LocalCassandraFactory
 * @since 1.0.0
 */
@API(since = "1.0.0", status = API.Status.STABLE)
@FunctionalInterface
public interface CassandraFactory {

	/**
	 * Creates a new configured {@link Cassandra}.
	 *
	 * @return a configured {@code Cassandra}
	 */
	@Nonnull
	Cassandra create();
}
