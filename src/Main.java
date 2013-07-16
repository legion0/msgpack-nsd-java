import java.io.IOException;
import java.net.InetSocketAddress;

import org.msgpack.nsd.Client;
import org.msgpack.nsd.ResponseReader;
import org.msgpack.type.RawValue;
import org.msgpack.type.Value;
import org.msgpack.type.ValueFactory;

public class Main {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		MyResponseReader response = new MyResponseReader();
		Client client = new Client(response);
		client.setTimeout(5);
		InetSocketAddress serverAddress = client.findFirst("GM");
		System.out.println(serverAddress);
		System.out.println(response.machineName);
	}

	static class MyResponseReader implements ResponseReader {

		public String machineName;
		private final RawValue key_machine_name = ValueFactory.createRawValue("machine_name".getBytes());

		public void read(Value extra_info) {
			machineName = extra_info.asMapValue().get(key_machine_name).asRawValue().getString();
		}
	};
}
