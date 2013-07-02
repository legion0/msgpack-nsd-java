package org.msgpack.nsd;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.msgpack.MessagePack;
import org.msgpack.MessageTypeException;
import org.msgpack.type.Value;
import org.msgpack.type.ValueFactory;
import org.msgpack.unpacker.Unpacker;

public class Client {

	private Integer timeout = null;
	private int ttl = 3;
	private static final String MULTICAST_GROUP = "224.0.0.1";
	private static final int NSD_PORT = 33333;

	public Client() {

	}

	public InetSocketAddress findFirst(String serviceName) throws IOException {
		return findFirst(serviceName, null);
	}

	public void setTimeout(int seconds) {
		timeout = seconds;
	}

	public InetSocketAddress findFirst(String serviceName, String[] features) throws IOException {
		InetSocketAddress server = null;
		@SuppressWarnings("serial")
		Map<Object, Object> msg = new LinkedHashMap<Object, Object>() {{
			put("service", "GM");
		}};
		if (features != null && features.length > 0) {
			msg.put("features", features);
		}
		MessagePack msgpack = new MessagePack();
		byte[] msgBuffer = msgpack.write(msg);
		InetAddress group = InetAddress.getByName(MULTICAST_GROUP);
		MulticastSocket socket = new MulticastSocket();
		socket.setTimeToLive(ttl);
		DatagramPacket packet = new DatagramPacket(msgBuffer, msgBuffer.length, group, NSD_PORT);
		socket.send(packet);

		byte[] buf = new byte[4096];
		packet = new DatagramPacket(buf, buf.length);
		if (timeout != null) {
			socket.setSoTimeout(timeout);
		}
		try {
			socket.receive(packet);
			byte [] tempBuffer = new byte[packet.getLength()];
			System.arraycopy(buf, 0, tempBuffer, 0, packet.getLength());
			buf = tempBuffer;
			ByteArrayInputStream in = new ByteArrayInputStream(buf);
	        Unpacker unpacker = msgpack.createUnpacker(in);
	        Value returnMsg = unpacker.readValue();
			System.out.println(returnMsg);
        	int port = returnMsg.asMapValue().get(ValueFactory.createRawValue("port".getBytes())).asIntegerValue().getInt();
        	server = new InetSocketAddress(packet.getAddress(), port);
		} catch (SocketTimeoutException ex) {
			System.out.println("catch");
			ex.printStackTrace();
		} catch (MessageTypeException ex) {
			System.out.println("catch");
			ex.printStackTrace();
		} finally {
			socket.close();
		}
		return server;
	}
}
