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

package com.github.nosan.embedded.cassandra.local;

import java.io.InputStream;
import java.io.Writer;

import org.yaml.snakeyaml.Yaml;

import com.github.nosan.embedded.cassandra.lang.annotation.Nullable;

/**
 * Utility class for dealing with YAML.
 *
 * @author Dmytro Nosan
 * @since 2.0.0
 */
abstract class YamlUtils {

	private static final boolean YAML_PRESENT;

	static {
		boolean yaml;
		try {
			Class.forName("org.yaml.snakeyaml.Yaml", false, YamlUtils.class.getClassLoader());
			yaml = true;
		}
		catch (Throwable ex) {
			yaml = false;
		}
		YAML_PRESENT = yaml;
	}

	static boolean isPresent() {
		return YAML_PRESENT;
	}

	/**
	 * Parses the YAML document in a stream and produce the corresponding
	 * Java object.
	 *
	 * @param <T> the type
	 * @param input data to load from
	 * @param type class of the object to be created
	 * @return parsed object
	 */
	@Nullable
	static <T> T loadAs(InputStream input, Class<T> type) {
		return new Yaml().loadAs(input, type);
	}

	/**
	 * Serialize a Java object into a YAML stream.
	 *
	 * @param data object to be serialized to YAML
	 * @param output stream to write to
	 */
	static void dump(Object data, Writer output) {
		new Yaml().dump(data, output);
	}

}
