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

package com.github.nosan.embedded.cassandra.local.artifact;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.github.nosan.embedded.cassandra.Version;

/**
 * {@link UrlFactory} to create {@code URL}.
 *
 * @author Dmytro Nosan
 * @since 1.0.0
 */
public class DefaultUrlFactory implements UrlFactory {

	@Nonnull
	@Override
	public URL[] create(@Nonnull Version version) throws MalformedURLException {
		Objects.requireNonNull(version, "Version must not be null");
		return new URL[]{new URL(String.format("https://www.apache.org/dyn/closer.cgi" +
				"?action=download&filename=cassandra/%1$s/apache-cassandra-%1$s-bin.tar.gz", version)),
				new URL(String.format("http://archive.apache.org/dist/cassandra/%1$s/apache-cassandra-%1$s-bin.tar.gz",
						version))};
	}
}