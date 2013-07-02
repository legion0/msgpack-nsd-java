import java.io.IOException;
import java.net.InetSocketAddress;

import org.msgpack.nsd.Client;

public class Main {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		Client client = new Client();
		client.setTimeout(3);
		InetSocketAddress serverAddress = client.findFirst("GM");
		System.out.println(serverAddress);
	}
}
