package Utility.Infos;

import Utility.Messages.Message;

import java.net.InetSocketAddress;

public class MessageInfo {

    public Message message;

    public boolean isActive;

    public int sendTries;

    public InetSocketAddress dest;

    public MessageInfo(Message message, boolean isActive, int sendTries, InetSocketAddress dest) {
        this.message = message;
        this.isActive = isActive;
        this.sendTries = sendTries;
        this.dest = dest;
    }

}
