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

package com.github.nosan.embedded.cassandra.cql;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Objects;

/**
 * {@link CqlResource} implementation for {@link URL}.
 *
 * @author Dmytro Nosan
 */
public class UrlCqlResource extends AbstractCqlResource {

	private final URL location;

	public UrlCqlResource(URL location) {
		this(location, null);
	}

	public UrlCqlResource(URL location, Charset charset) {
		super(charset);
		this.location = Objects.requireNonNull(location, "Location must not be null");
	}

	@Override
	public String getName() {
		return String.valueOf(this.location);
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return this.location.openStream();
	}
}