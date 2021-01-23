package org.alindner.cish.lang;

import lombok.EqualsAndHashCode;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
public class OS {
	public static void main(String... args) {
		System.out.println(getMainIpAddress().getAddress());
		System.out.println(getInterfaces());
		System.out.println(getHostname());
	}

	private static final Map<String, String> infos = new HashMap<>();
	static {
		final String[] lines;
		try {
			lines = execReadToString("hostnamectl").split("\n");
			for (final String line : lines) {
				final String[] l     = line.split(":");
				final String   name  = l[0].replaceAll(" ", "");
				final String   value = l[1].replaceAll(" ", "");
				infos.put(name, value);
			}
		} catch (IOException e) {
			String os = System.getProperty("os.name").toLowerCase();
			try {
				if (os.contains("win")) {
					if (!System.getenv("COMPUTERNAME").isEmpty()) {
						infos.put("Statichostname", System.getenv("COMPUTERNAME"));
					} else {
						infos.put("Statichostname", execReadToString("hostname"));
					}
				} else if (os.contains("nix") || os.contains("nux") || os.contains("mac os x")) {

					if (!System.getenv("HOSTNAME").isEmpty()) {
						infos.put("Statichostname", System.getenv("HOSTNAME"));
					} else {
						try {
							infos.put("Statichostname", execReadToString("hostname"));
						} catch (IOException ignored) {
							infos.put("Statichostname", execReadToString("cat /etc/hostname"));
						}
					}
				}
			} catch (IOException ex) {
				log.error("Couldn't detect hostname", ex);
			}
		}
	}

	public static String getHostname() {
		return infos.getOrDefault("Statichostname", "UNDEFINED");
	}

	public static String execReadToString(String execCommand) throws IOException {
		try (Scanner s = new Scanner(Runtime.getRuntime().exec(execCommand).getInputStream()).useDelimiter("\\A")) {
			return s.hasNext() ? s.next() : "";
		}
	}

	/**
	 * Tries to detect the IP Address where the internet is routed to.
	 *
	 * @return IP Address
	 */
	public static Interfaces.IpAddress getMainIpAddress() {
		try (final DatagramSocket socket = new DatagramSocket()) {
			socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
			final String finalResult = socket.getLocalAddress().getHostAddress();
			return getInterfaces().stream()
			                      .map(Interfaces::getIpAddresses)
			                      .flatMap(Collection::stream)
			                      .filter(ipAddressStream -> Objects.equals(ipAddressStream.getAddress(), finalResult))
			                      .findFirst()
			                      .orElse(Interfaces.IpAddress.DUMMY);
		} catch (SocketException | UnknownHostException e) {
			e.printStackTrace();
		}
		return Interfaces.IpAddress.DUMMY;
	}

	/**
	 * List all interfaces
	 *
	 * @return list of interfaces
	 */
	public static List<Interfaces> getInterfaces() {
		try {
			return Collections.list(NetworkInterface.getNetworkInterfaces())
			                  .stream()
			                  .map(Interfaces::new)
			                  .collect(Collectors.toList());

		} catch (SocketException e) {
			e.printStackTrace();
		}
		return new ArrayList<>();
	}

	/**
	 * internal representation of an interfaces
	 */
	public static class Interfaces {
		@Override
		public String toString() {
			return String.format("Interfaces{ipif=%s, addresses=%s}", iif.toString(), getIpAddresses().toString());
		}

		private final NetworkInterface iif;

		public Interfaces(final NetworkInterface networkInterface) {
			this.iif = networkInterface;
		}

		/**
		 * get all ip addresses of an interface
		 *
		 * @return list of ip addresses
		 */
		public List<IpAddress> getIpAddresses() {
			return Collections.list(iif.getInetAddresses())
			                  .stream()
			                  .map(IpAddress::new)
			                  .collect(Collectors.toList());
		}

		/**
		 * internal representation of ip addresses
		 */
		@EqualsAndHashCode
		public static class IpAddress {
			public static final IpAddress   DUMMY = new IpAddress(null) {
				public String getAddress() {
					return "0.0.0.0";
				}

				@Override
				public boolean equals(final Object o) {
					if (this == o) {
						return true;
					}
					if (!(o instanceof IpAddress)) {
						return false;
					}
					final IpAddress ipAddress = (IpAddress) o;
					return Objects.equals(getAddress(), ipAddress.getAddress());
				}

				@Override
				public int hashCode() {
					return Objects.hash(getAddress());
				}
			};
			private final       InetAddress address;

			public IpAddress(InetAddress address) {
				this.address = address;
			}

			public String getAddress() {
				return address.getHostAddress();
			}

			public String getHostname() {
				return address.getHostName().equals(getAddress()) ? null : address.getHostName();
			}

			@Override
			public String toString() {
				return String.format("IpAddress[address=%s,hostname=%s]", getAddress(), getHostname());
			}
		}
	}
}
