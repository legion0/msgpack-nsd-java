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
import org.msgpack.type.MapValue;
import org.msgpack.type.RawValue;
import org.msgpack.type.ValueFactory;
import org.msgpack.unpacker.Unpacker;

public class Client {

	private long timeout = -1;
	private int ttl = 3;
	private static final String MULTICAST_GROUP = "224.0.0.1";
	private static final int NSD_PORT = 33333;
	private ResponseReader responseReader = null;
	private static final RawValue key_extra_info = ValueFactory.createRawValue("extra_info".getBytes());
	private static final RawValue key_service = ValueFactory.createRawValue("service".getBytes());
	private static final RawValue key_features = ValueFactory.createRawValue("features".getBytes());
	private static final RawValue key_port = ValueFactory.createRawValue("port".getBytes());

	public Client(ResponseReader responseReader) {
		this.responseReader = responseReader;
	}

	public InetSocketAddress findFirst(String serviceName) throws IOException {
		return findFirst(serviceName, null);
	}

	public void setTimeout(int seconds) {
		timeout = seconds * 1000000000l;
	}

	public InetSocketAddress findFirst(final String serviceName, String[] features) throws IOException {
		@SuppressWarnings("serial")
		Map<Object, Object> msg = new LinkedHashMap<Object, Object>() {
			{
				put(key_service, serviceName);
			}
		};
		if (features != null && features.length > 0) {
			msg.put(key_features, features);
		}
		MessagePack msgpack = new MessagePack();
		byte[] msgBuffer = msgpack.write(msg);
		InetAddress group = InetAddress.getByName(MULTICAST_GROUP);
		MulticastSocket socket = new MulticastSocket();
		socket.setTimeToLive(ttl);
		DatagramPacket outgoingPacket = new DatagramPacket(msgBuffer, msgBuffer.length, group, NSD_PORT);
		DatagramPacket incomingPacket;

		byte[] buf;
		long end = timeout > -1 ? System.nanoTime() + timeout : Long.MAX_VALUE;
		try {
			while (System.nanoTime() < end) {
				try {
					socket.send(outgoingPacket);

					buf = new byte[4096];
					incomingPacket = new DatagramPacket(buf, buf.length);
					socket.setSoTimeout(1000);
					socket.receive(incomingPacket);
					byte[] tempBuffer = new byte[incomingPacket.getLength()];
					System.arraycopy(buf, 0, tempBuffer, 0, incomingPacket.getLength());
					buf = tempBuffer;
					ByteArrayInputStream in = new ByteArrayInputStream(buf);
					Unpacker unpacker = msgpack.createUnpacker(in);
					MapValue returnMsg = unpacker.readValue().asMapValue();
					String returnServiceName = returnMsg.get(key_service).asRawValue().getString();
					if (!serviceName.equals(returnServiceName)) {
						throw new MessageTypeException("Invalid Service Name");
					}
					int port = returnMsg.get(key_port).asIntegerValue().getInt();
					if (returnMsg.containsKey(key_extra_info)) {
						responseReader.read(returnMsg.get(key_extra_info));
					}
					return new InetSocketAddress(incomingPacket.getAddress(), port);
				} catch (SocketTimeoutException ex) {
					// System.out.println("timeout");
				} catch (NullPointerException ex) {
					ex.printStackTrace();
				} catch (MessageTypeException ex) {
					ex.printStackTrace();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		} finally {
			socket.close();
		}
		return null;
	}
}
