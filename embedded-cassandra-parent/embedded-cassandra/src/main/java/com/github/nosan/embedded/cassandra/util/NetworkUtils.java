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

import java.io.UncheckedIOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import org.apiguardian.api.API;

import com.github.nosan.embedded.cassandra.util.annotation.Nullable;

/**
 * Utility methods for dealing with network.
 *
 * @author Dmytro Nosan
 * @since 1.1.0
 */
@API(since = "1.1.0", status = API.Status.INTERNAL)
public abstract class NetworkUtils {

	/**
	 * Determines the IP address of a host, given the host's name.
	 *
	 * @param address the specified host
	 * @return an IP address for the given host name.
	 * @throws UncheckedIOException if no IP address for the host could be found
	 */
	public static InetAddress getInetAddress(@Nullable String address) {
		try {
			return InetAddress.getByName(address);
		}
		catch (UnknownHostException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	/**
	 * Return the first IPv4 or IPv6 address by the interface name.
	 *
	 * @param interfaceName the interface name (e.g. en0)
	 * @param useIpv6 whether to use IPv6 or not
	 * @return an IP address for the given interface name.
	 * @throws IllegalArgumentException if interface is unknown.
	 * @throws UncheckedIOException if an I/O error occurs.
	 * @since 1.2.7
	 */
	public static Optional<InetAddress> getAddressByInterface(String interfaceName, boolean useIpv6) {
		Objects.requireNonNull(interfaceName, "Interface name must not be null");
		Predicate<InetAddress> condition = useIpv6 ? Inet6Address.class::isInstance : Inet4Address.class::isInstance;
		return getAddressesByInterface(interfaceName).stream().filter(condition).findFirst();
	}

	/**
	 * Return the list of addresses by the interface name.
	 *
	 * @param interfaceName the interface name (e.g. en0)
	 * @return an IP address for the given interface name.
	 * @throws IllegalArgumentException if interface is unknown.
	 * @throws UncheckedIOException if an I/O error occurs.
	 * @since 1.2.7
	 */
	public static List<InetAddress> getAddressesByInterface(String interfaceName) {
		Objects.requireNonNull(interfaceName, "Interface name must not be null");
		try {
			NetworkInterface networkInterface = NetworkInterface.getByName(interfaceName);
			if (networkInterface == null) {
				throw new SocketException(String.format("'%s' interface is not valid", interfaceName));
			}
			Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
			return Collections.unmodifiableList(Collections.list(addresses));
		}
		catch (SocketException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	/**
	 * Return the {@code localhost} address.
	 *
	 * @return the localhost
	 * @throws UncheckedIOException if no IP address for the host could be found
	 */
	public static InetAddress getLocalhost() {
		try {
			return InetAddress.getLocalHost();
		}
		catch (UnknownHostException ex) {
			throw new UncheckedIOException(ex);
		}
	}

}
