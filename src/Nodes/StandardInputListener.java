package Nodes;

import Utility.Messages.Message;
import Utility.Messages.MessageType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

class StandardInputListener extends Thread {

    private ChatNode node;

    StandardInputListener(ChatNode node) {
        this.node = node;
    }

    @Override
    public void run() {
        try (BufferedReader sin = new BufferedReader(new InputStreamReader(System.in))) {
            String string = sin.readLine(), nodeName = node.getName();
            while (string != null) {
                Message msg = new Message(UUID.randomUUID(), MessageType.STANDARD, nodeName,
                        string.getBytes(StandardCharsets.UTF_8));
                node.addStandardMessage(null, msg);
                string = sin.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void listen() {
        this.start();
    }

}
