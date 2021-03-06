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

package com.github.nosan.embedded.cassandra.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apiguardian.api.API;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.github.nosan.embedded.cassandra.util.annotation.Nullable;

/**
 * Utility class for dealing with {@link MDC}.
 *
 * @author Dmytro Nosan
 * @since 1.2.8
 */
@API(since = "1.2.8", status = API.Status.INTERNAL)
public abstract class MDCUtils {

	private static final Logger log = LoggerFactory.getLogger(MDCUtils.class);

	/**
	 * Return a copy of the current thread's context map, with keys and values of type String.
	 *
	 * @return A copy of the current thread's context map.
	 */
	public static Map<String, String> getContext() {
		try {
			Map<String, String> context = MDC.getCopyOfContextMap();
			return (context != null) ? Collections.unmodifiableMap(context) : Collections.emptyMap();
		}
		catch (Throwable ex) {
			if (log.isDebugEnabled()) {
				log.error("Could not get MDC context", ex);
			}
			return new LinkedHashMap<>();
		}
	}

	/**
	 * Set the current thread's context map by first clearing any existing map and then copying the map passed as
	 * parameter. The context map passed as parameter must only contain keys and values of type String.
	 *
	 * @param context must contain only keys and values of type String
	 */
	public static void setContext(@Nullable Map<String, String> context) {
		if (context == null) {
			context = new LinkedHashMap<>();
		}
		try {
			MDC.setContextMap(context);
		}
		catch (Throwable ex) {
			if (log.isDebugEnabled()) {
				log.error("Could not set MDC context", ex);
			}
		}
	}

}
