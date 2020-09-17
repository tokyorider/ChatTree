package Nodes;

import Utility.Messages.Message;
import Utility.Messages.MessageType;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.UUID;

class Sender {

    private DatagramSocket socket;

    Sender(DatagramSocket socket) {
        this.socket = socket;
    }

    void sendStandard(Message message, InetSocketAddress dest) throws IOException {
        byte[] result = message.toByteArray();
        socket.send(new DatagramPacket(result, result.length, dest));
    }

    void sendAcknowledge(UUID id, InetSocketAddress dest) throws IOException {
        final int UUID_SIZE = 2 * Long.BYTES;
        int offset = 0;
        byte[] result = new byte[1 + UUID_SIZE];
        result[0] = MessageType.ACK.getValue();
        offset += 1;

        ByteBuffer buf = ByteBuffer.allocate(UUID_SIZE);
        buf.putLong(id.getMostSignificantBits());
        buf.putLong(id.getLeastSignificantBits());
        System.arraycopy(buf.array(), 0, result, offset, UUID_SIZE);

        socket.send(new DatagramPacket(result, result.length, dest));
    }

    void sendHello(InetSocketAddress dest) throws IOException {
        socket.send(new DatagramPacket(new byte[]{MessageType.HELLO.getValue()}, 1, dest));
    }

    void sendHelloBack(InetSocketAddress dest, InetSocketAddress substitute) throws IOException {
        byte[] sub = socketAddressToByteArr(substitute);
        socket.send(new DatagramPacket(sub, sub.length, dest));
    }

    void sendKeepAlive(InetSocketAddress dest) throws IOException {
        socket.send(new DatagramPacket(new byte[]{MessageType.KEEP_ALIVE.getValue()}, 1, dest));
    }

    void sendDisconnect(InetSocketAddress disconnected) throws IOException {
        byte[] disc = socketAddressToByteArr(disconnected);
        byte[] result = new byte[1 + disc.length];
        result[0] = MessageType.DISCONNECT.getValue();
        System.arraycopy(disc, 0, result, 1, disc.length);

        socket.send(new DatagramPacket(result, result.length, socket.getLocalSocketAddress()));
    }

    private byte[] socketAddressToByteArr(InetSocketAddress socketAddress) {
        int ipLength = socketAddress.getAddress().getAddress().length;
        byte[] result = new byte[ipLength + Short.BYTES];

        System.arraycopy(socketAddress.getAddress().getAddress(), 0, result, 0, ipLength);
        byte[] port = ByteBuffer.allocate(Short.BYTES).putShort((short) socketAddress.getPort()).array();
        System.arraycopy(port, 0, result, ipLength, Short.BYTES);

        return result;
    }

}
