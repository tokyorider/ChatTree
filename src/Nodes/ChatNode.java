package Nodes;

import Exceptions.UnknownMessageTypeException;
import Utility.Infos.MessageInfo;
import Utility.Infos.NeighbourInfo;
import Utility.Messages.Message;
import Utility.Messages.MessageType;
import Utility.RandomInRange;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatNode {

    private class MessageResend extends TimerTask {

        private static final int TRIES_LIMIT = 3;

        @Override
        public void run() {
            standardMessages.forEach((id, info) -> {
                synchronized (standardMessages) {
                    if (!info.isActive && info.sendTries < TRIES_LIMIT) {
                        addStandardMessageTask(info.message);
                    } else if (!info.isActive) {
                        standardMessages.remove(id);
                    }
                }
            });
        }

    }

    private class KeepInTouch extends TimerTask {

        @Override
        public void run() {
            neighbours.forEach((neighbour, info) -> threadPool.submit(() -> {
                try {
                    sender.sendKeepAlive(neighbour);
                } catch(IOException e) {
                    throw new UncheckedIOException(e);
                }
            }));
        }

    }

    public class CheckForDisconnects extends TimerTask {

        private static final int DISCONNECT_TIMEOUT = 10000;

        @Override
        public void run() {
            neighbours.forEach((neighbour, info) -> {
                if (new Date().getTime() - info.lastActivity.getTime() > DISCONNECT_TIMEOUT) {
                    try {
                        sender.sendDisconnect(neighbour);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            });
        }

    }

    private String name;

    private final DatagramSocket socket;

    private short lossPercentage;

    private final ConcurrentHashMap<InetSocketAddress, NeighbourInfo> neighbours = new ConcurrentHashMap<>();

    private ExecutorService threadPool = Executors.newFixedThreadPool(2 * Runtime.getRuntime().availableProcessors());

    private Sender sender;

    private final ConcurrentHashMap<UUID, MessageInfo> standardMessages = new ConcurrentHashMap<>();

    private static final int TIMEOUT = 10000;
    private static final int RESEND_DELAY = 300;
    private static final int RESEND_PERIOD = RESEND_DELAY;
    private static final int KEEP_IN_TOUCH_DELAY = 1000;
    private static final int KEEP_IN_TOUCH_PERIOD = 3000;
    private static final int CHECK_FOR_DISCONNECTS_DELAY = 10000;
    private static final int CHECK_FOR_DISCONNECTS_PERIOD = 2000;
    private static final int PACKET_SIZE = 65536;

    public ChatNode(NodeArgs args) throws IOException {
        name = args.name;
        socket = new DatagramSocket(args.port);
        lossPercentage = args.lossPercentage;
        sender = new Sender(socket);

        if (args.neighbourIp != null) {
            InetSocketAddress neighbour = new InetSocketAddress(args.neighbourIp, args.neighbourPort);

            socket.setSoTimeout(TIMEOUT);
            while (true) {
                try {
                    sender.sendHello(neighbour);
                    addNeighbour(neighbour, receiveSubstitute(neighbour));
                    break;
                } catch (SocketTimeoutException e) {

                }
            }
            socket.setSoTimeout(0);
        }
        System.out.println(name + " was born!");
    }

    public void chat() throws IOException {
        new StandardInputListener(this).listen();
        setTimer();
        listenSocket();
    }

    void addNeighbour(InetSocketAddress newNeighbour, InetSocketAddress substitute) {
        System.out.println(newNeighbour + " was added))))");
        synchronized (neighbours) {
            neighbours.put(newNeighbour, new NeighbourInfo(substitute, new Date()));
        }
    }

    void fixNeighbourInfo(InetSocketAddress neighbour) {
        synchronized (neighbours) {
            if (neighbours.get(neighbour) != null) {
                neighbours.get(neighbour).lastActivity = new Date();
            }
        }
    }

    void removeNeighbour(InetSocketAddress neighbour) {
        System.out.println(neighbour + " was removed((((");
        synchronized (neighbours) {
            neighbours.remove(neighbour);
        }
    }

    void removeMessage(UUID id) {
        synchronized (standardMessages) {
            standardMessages.remove(id);
        }
    }

    private void setTimer() {
        Timer timer = new Timer(true);
        timer.schedule(new MessageResend(), RESEND_DELAY, RESEND_PERIOD);
        timer.schedule(new KeepInTouch(), KEEP_IN_TOUCH_DELAY, KEEP_IN_TOUCH_PERIOD);
        timer.schedule(new CheckForDisconnects(), CHECK_FOR_DISCONNECTS_DELAY, CHECK_FOR_DISCONNECTS_PERIOD);
    }

    private void listenSocket() throws IOException {
        MessageHandler handler = new MessageHandler(sender, this, threadPool);

        while (true) {
            DatagramPacket packet = new DatagramPacket(new byte[PACKET_SIZE], PACKET_SIZE);
            socket.receive(packet);
            if (RandomInRange.random(0, 100) >= lossPercentage) {
                Message recvMessage;
                try {
                    recvMessage = new Message(Arrays.copyOf(packet.getData(), packet.getLength()));
                } catch (UnknownMessageTypeException e) {
                    continue;
                }
                InetSocketAddress source = (InetSocketAddress) packet.getSocketAddress();
                switch (recvMessage.getType()) {
                    case STANDARD:
                        handler.handleStandardMessage(recvMessage, source);
                        break;
                    case ACK:
                        handler.handleAcknowledgeMessage(recvMessage.getId(), source);
                        break;
                    case HELLO:
                        InetSocketAddress sub = (neighbours.isEmpty()) ? source : neighbours.keys().nextElement();
                        handler.handleHelloMessage(source, sub, (InetSocketAddress) socket.getLocalSocketAddress());
                        break;
                    case KEEP_ALIVE:
                        handler.handleKeepAliveMessage(source);
                        break;
                    case DISCONNECT:
                        int ipLength = recvMessage.getContent().length - Short.BYTES;
                        InetAddress ip = InetAddress.getByAddress(Arrays.copyOf(recvMessage.getContent(), ipLength));
                        short port = ByteBuffer.wrap(recvMessage.getContent(), ipLength, Short.BYTES).getShort();
                        InetSocketAddress disconnected = new InetSocketAddress(ip, port);
                        sub = neighbours.get(disconnected).substitute;

                        handler.handleDisconnectMessage(disconnected, sub, socket);
                }
            }
        }
    }

    InetSocketAddress receiveSubstitute(InetSocketAddress address) throws IOException {
        DatagramPacket packet = new DatagramPacket(new byte[18], 18);
        socket.receive(packet);
        while (!(packet.getSocketAddress().equals(address) && RandomInRange.random(0, 100) >= lossPercentage
                && packet.getLength() > 1)) {
            socket.receive(packet);
        }

        int ipLength = packet.getLength() - Short.BYTES;
        InetAddress sub = InetAddress.getByAddress(Arrays.copyOfRange(packet.getData(), 0, ipLength));
        short port = ByteBuffer.wrap(packet.getData(), ipLength, Short.BYTES).getShort();

        return new InetSocketAddress(sub, port);
    }

    void addStandardMessage(InetSocketAddress source, Message msg) {
            neighbours.forEach((neighbour, info) -> {
                if (!neighbour.equals(source)) {
                    UUID id = UUID.randomUUID();
                    Message messageToSend = new Message(id, MessageType.STANDARD, msg.getNodeName(), msg.getContent());
                    synchronized (standardMessages) {
                        standardMessages.put(id, new MessageInfo(messageToSend, true, 0, neighbour));
                    }
                    addStandardMessageTask(messageToSend);
                }
            });
    }

    public void addStandardMessageTask(Message msg) {
        threadPool.submit(() -> {
            try {
                synchronized (standardMessages) {
                    sender.sendStandard(msg, standardMessages.get(msg.getId()).dest);
                    standardMessages.get(msg.getId()).isActive = false;
                    standardMessages.get(msg.getId()).sendTries++;
                }
            } catch(IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    public String getName() {
        return name;
    }

}