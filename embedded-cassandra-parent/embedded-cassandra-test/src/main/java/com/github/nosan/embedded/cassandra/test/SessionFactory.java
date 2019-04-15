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

import com.datastax.oss.driver.api.core.CqlSession;

import com.github.nosan.embedded.cassandra.Settings;
import com.github.nosan.embedded.cassandra.Version;

/**
 * Factory that creates a {@link CqlSession}.
 *
 * @author Dmytro Nosan
 * @see CqlSession
 * @since 2.0.0
 */
@FunctionalInterface
public interface SessionFactory {

	/**
	 * Creates a new configured {@link CqlSession}.
	 *
	 * @param settings the settings
	 * @param version a version
	 * @return a session
	 */
	CqlSession create(Version version, Settings settings);

}