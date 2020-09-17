package Utility.Infos;

import java.net.InetSocketAddress;
import java.util.Date;

public class NeighbourInfo {

    public InetSocketAddress substitute;

    public Date lastActivity;

    public NeighbourInfo(InetSocketAddress substitute, Date lastActivity) {
        this.substitute = substitute;
        this.lastActivity = lastActivity;
    }

}
