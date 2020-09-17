package Nodes;

import java.net.InetAddress;

public class NodeArgs {

    public final String name;

    public final short port;

    public final short lossPercentage;

    public final InetAddress neighbourIp;

    public final Short neighbourPort;

    public NodeArgs(String name, short port, short lossPercentage) {
        this.name = name;
        this.port = port;
        this.lossPercentage = lossPercentage;
        this.neighbourIp = null;
        this.neighbourPort = null;
    }

    public NodeArgs(String name, short port, short lossPercentage, InetAddress neighbourIp, Short neighbourPort) {
        this.name = name;
        this.port = port;
        this.lossPercentage = lossPercentage;
        this.neighbourIp = neighbourIp;
        this.neighbourPort = neighbourPort;
    }

}
