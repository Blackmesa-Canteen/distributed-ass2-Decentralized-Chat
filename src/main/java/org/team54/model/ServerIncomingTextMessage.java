package org.team54.model;

import lombok.*;
import org.team54.server.ChatServer;

import java.nio.channels.SocketChannel;

/**
 * @author Xiaotian
 * @program distributed-ass2-Decentralized-Chat
 * @description used in MQ
 * @create 2021-10-06 13:06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ServerIncomingTextMessage {
    private ChatServer chatServer;
    private Peer sourcePeer;
    private String text;
}