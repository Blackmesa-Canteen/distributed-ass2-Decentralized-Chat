package org.team54.server;

import org.team54.model.Peer;
import org.team54.model.ServerIncomingTextMessage;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Xiaotian
 * @program distributed-ass2-Decentralized-Chat
 * @description MQ for async incoming message handling
 * @create 2021-10-06 13:01
 */
public class MessageQueueWorker implements Runnable {
    /** a queue FIFO */
    private BlockingQueue<ServerIncomingTextMessage> queue = new LinkedBlockingQueue<>();

    public void handleIncomingTextMessage(ChatServer server, Peer sourcePeer, String text) {

        // handle exception: sometimes, multiple JSON object in one packet
        String[] jsonStrings = text.split("\n");
        for (String jsonString : jsonStrings) {

            ServerIncomingTextMessage message = ServerIncomingTextMessage.builder()
                    .chatServer(server)
                    .sourcePeer(sourcePeer)
                    .text(jsonString + "\n")
                    .build();

            // System.out.println("[debug] a new incoming message is recieved in server. from " + sourcePeer.getIdentity());
            queue.offer(message);
        }
    }

    @Override
    public void run() {
        ServerIncomingTextMessage incomingMessage;
        while (true) {
            synchronized (queue) {
                try {
                    // will wait if there is no message in the head
                    incomingMessage = queue.take();
                } catch (InterruptedException e) {
                    // Thrown when the thread is waiting, sleeping,
                    // or otherwise occupied, and the thread is interrupted
                    continue;
                }
            }

            // handle incoming text request
            incomingMessage.getChatServer().handleRequestCallback(incomingMessage.getSourcePeer(), incomingMessage.getText());
        }
    }
}