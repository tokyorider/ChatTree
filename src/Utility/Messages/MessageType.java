package Utility.Messages;

import Exceptions.UnknownMessageTypeException;

public enum MessageType {

    STANDARD(1),
    ACK(2),
    HELLO(3),
    KEEP_ALIVE(4),
    DISCONNECT(5);

    private int value;

    MessageType(int value) {
        this.value = value;
    }

    public byte getValue() {
        return (byte)value;
    }

    public static MessageType getType(int value) throws UnknownMessageTypeException {
        for (MessageType type : MessageType.values()) {
            if (type.value == value) {
                return type;
            }
        }

        throw new UnknownMessageTypeException();
    }

}
