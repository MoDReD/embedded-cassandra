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

import org.apiguardian.api.API;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.nosan.embedded.cassandra.util.annotation.Nullable;

/**
 * Utility methods for dealing with classes.
 *
 * @author Dmytro Nosan
 * @since 1.2.1
 */
@API(since = "1.2.1", status = API.Status.INTERNAL)
public abstract class ClassUtils {

	private static final Logger log = LoggerFactory.getLogger(ClassUtils.class);

	/**
	 * Return the one of the  {@code ClassLoader} to use.
	 * <ol>
	 * <li>Thread.currentThread().getContextClassLoader()</li>
	 * <li>ClassUtils.class.getClassLoader()</li>
	 * <li>ClassLoader.getSystemClassLoader()</li>
	 * </ol>
	 *
	 * @return the {@code ClassLoader} (only {@code null} if even the system {@code ClassLoader}  isn't accessible)
	 * @see Thread#getContextClassLoader()
	 * @see ClassLoader#getSystemClassLoader()
	 */
	@Nullable
	public static ClassLoader getClassLoader() {
		ClassLoader cl = null;
		try {
			cl = Thread.currentThread().getContextClassLoader();
		}
		catch (Throwable ex) {
			if (log.isDebugEnabled()) {
				log.error(String.format("Could not get '%s' context loader", Thread.currentThread()), ex);
			}
		}
		if (cl == null) {
			try {
				cl = ClassUtils.class.getClassLoader();
			}
			catch (Throwable ex) {
				if (log.isDebugEnabled()) {
					log.error(String.format("Could not get '%s' class loader", ClassUtils.class), ex);
				}
			}
		}
		if (cl == null) {
			try {
				cl = ClassLoader.getSystemClassLoader();
			}
			catch (Throwable ex) {
				if (log.isDebugEnabled()) {
					log.error("Could not get a system class loader", ex);
				}
			}
		}
		return cl;
	}

	/**
	 * Determines the package name for the given class.
	 *
	 * @param source a class
	 * @return package name or empty string
	 * @since 1.2.2
	 */
	public static String getPackageName(@Nullable Class<?> source) {
		while (source != null && source.isArray()) {
			source = source.getComponentType();
		}
		String name = (source != null) ? source.getName() : "";
		int i = name.lastIndexOf('.');
		return (i > 0) ? name.substring(0, i) : "";
	}

}
