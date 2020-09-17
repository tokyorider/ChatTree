import Nodes.ChatNode;
import Nodes.NodeArgs;
import Utility.ArgResolver;

public class Main {

    public static void main(String[] args) {
        try {
            NodeArgs nodeArgs = ArgResolver.resolve(args);
            new ChatNode(nodeArgs).chat();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}