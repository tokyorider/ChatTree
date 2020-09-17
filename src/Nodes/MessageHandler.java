package Nodes;

import Utility.Messages.Message;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

class MessageHandler {

    private Sender sender;

    private ChatNode node;

    private ExecutorService threadPool;

    private final int TIMEOUT = 10000;

    MessageHandler(Sender sender, ChatNode node, ExecutorService threadPool) {
        this.sender = sender;
        this.node = node;
        this.threadPool = threadPool;
    }

    void handleStandardMessage(Message message, InetSocketAddress messageSource) {
        node.fixNeighbourInfo(messageSource);
        threadPool.submit(() -> {
            try {
                sender.sendAcknowledge(message.getId(), messageSource);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        System.out.println(message.getNodeName() + ": " +
                new String(message.getContent(), StandardCharsets.UTF_8));

        node.addStandardMessage(messageSource, message);
    }

    void handleAcknowledgeMessage(UUID id, InetSocketAddress messageSource) {
        node.removeMessage(id);
        node.fixNeighbourInfo(messageSource);
    }

    void handleHelloMessage(InetSocketAddress messageSource, InetSocketAddress sub, InetSocketAddress local)
            throws IOException
    {
        sender.sendHelloBack(messageSource, sub);
        System.out.println(sub);
        node.addNeighbour(messageSource, local);
    }

    void handleKeepAliveMessage(InetSocketAddress messageSource) {
        node.fixNeighbourInfo(messageSource);
    }

    void handleDisconnectMessage(InetSocketAddress disconnected, InetSocketAddress sub, DatagramSocket socket)
            throws IOException
    {
        node.removeNeighbour(disconnected);

        if (!sub.equals(socket.getLocalSocketAddress())) {
            socket.setSoTimeout(TIMEOUT);
            while (true) {
                try {
                    sender.sendHello(sub);
                    node.addNeighbour(sub, node.receiveSubstitute(sub));
                    break;
                } catch (SocketTimeoutException e) {

                }
            }
            socket.setSoTimeout(0);
        }
    }

}
