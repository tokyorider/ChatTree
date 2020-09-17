package Utility.Messages;

import Exceptions.UnknownMessageTypeException;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

public class Message {

    private MessageType type;

    private UUID id = null;

    private String nodeName = null;

    private byte[] content = null;

    public Message(byte[] data) throws UnknownMessageTypeException {
        int offset = 0;
        type = MessageType.getType(data[0]);
        offset += 1;

        switch (type) {
            case STANDARD:
            case ACK:
                long mostBits, leastBits;
                ByteBuffer buf = ByteBuffer.wrap(data, offset, Long.BYTES);
                mostBits = buf.getLong();
                offset += Long.BYTES;

                buf = ByteBuffer.wrap(data, offset, Long.BYTES);
                leastBits = buf.getLong();
                offset += Long.BYTES;
                id = new UUID(mostBits, leastBits);
        }

        if (type == MessageType.STANDARD) {
            short nodeNameLength = ByteBuffer.allocate(Short.BYTES).put(
                    Arrays.copyOfRange(data, offset, offset + Short.BYTES)).clear().getShort();
            offset += Short.BYTES;
            nodeName = new String(Arrays.copyOfRange(data, offset, offset + nodeNameLength), StandardCharsets.UTF_8);
            offset += nodeNameLength;
        }

        if (type == MessageType.STANDARD || type == MessageType.DISCONNECT) {
            content = Arrays.copyOfRange(data, offset, data.length);
        }
    }

    public Message(UUID id, MessageType type, String nodeName, byte[] content) {
        this.id = id;
        this.type = type;
        this.nodeName = nodeName;
        this.content = content;
    }

    public UUID getId() {
        return id;
    }

    public MessageType getType() {
        return type;
    }

    public String getNodeName() {
        return nodeName;
    }

    public byte[] getContent() {
        return content;
    }

    public byte[] toByteArray() {
        final int UUID_SIZE = 2 * Long.BYTES;
        byte[] result = new byte[1 + UUID_SIZE + ((type == MessageType.STANDARD) ?
                Short.BYTES + nodeName.length() + content.length : 0)];
        int offset = 0;
        result[0] = type.getValue();
        offset += 1;

        switch (type) {
            case STANDARD:
            case ACK:
                ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
                byte[] bits = buffer.putLong(id.getMostSignificantBits()).array();
                System.arraycopy(bits, 0, result, offset, Long.BYTES);
                offset += Long.BYTES;
                bits = buffer.clear().putLong(id.getLeastSignificantBits()).array();
                System.arraycopy(bits, 0, result, offset, Long.BYTES);
                offset += Long.BYTES;
        }

        if (type == MessageType.STANDARD) {
            byte[] nodeNameLength = ByteBuffer.allocate(Short.BYTES).putShort(
                    (short) nodeName.getBytes(StandardCharsets.UTF_8).length).array();
            System.arraycopy(nodeNameLength, 0, result, offset, Short.BYTES);
            offset += Short.BYTES;

            byte[] name = nodeName.getBytes(StandardCharsets.UTF_8);
            System.arraycopy(name, 0, result, offset, name.length);
            offset += name.length;

            System.arraycopy(content, 0, result, offset, content.length);
        }

        return result;
    }

}
