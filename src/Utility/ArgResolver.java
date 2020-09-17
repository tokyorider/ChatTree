package Utility;

import Exceptions.IllegalNumberOfArgumentsException;
import Nodes.NodeArgs;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ArgResolver {

    public static NodeArgs resolve(String[] args) throws UnknownHostException, IllegalNumberOfArgumentsException {
        if (args.length == 3) {
            return new NodeArgs(args[0], Short.parseShort(args[1]), Short.parseShort(args[2]));
        } else if(args.length == 5) {
            return new NodeArgs(args[0], Short.parseShort(args[1]), Short.parseShort(args[2]),
                                InetAddress.getByName(args[3]), Short.parseShort(args[4]));
        } else {
            throw new IllegalNumberOfArgumentsException();
        }
    }

}
