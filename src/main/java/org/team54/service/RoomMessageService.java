package org.team54.service;

import com.google.gson.Gson;
import org.team54.messageBean.*;
import org.team54.server.ChatRoomManager;
import org.team54.server.NeighborPeerManager;

/**
 * @author Xiaotian
 * @program distributed-ass2-Decentralized-Chat
 * @description
 * @create 2021-10-08 11:21
 */
public class RoomMessageService {
    private static final ChatRoomManager chatRoomManager = ChatRoomManager.getInstance();
    private static final NeighborPeerManager neighborPeerManager = NeighborPeerManager.getInstance();

    public static String genJoinRoomMsg(String roomId) {
        JoinRoomMessage jsonObject = JoinRoomMessage.builder()
                .roomid(roomId)
                .build();

        return new Gson().toJson(jsonObject) + "\n";
    }

    public static String genRoomChangeMsg(String identity, String former, String roomId) {
        RoomChangeMessage jsonObject = RoomChangeMessage.builder()
                .roomid(roomId)
                .former(former)
                .identity(identity)
                .build();

        return new Gson().toJson(jsonObject) + "\n";
    }

    public static String genWhoMsg(String roomId) {
        WhoMessage jsonObject = WhoMessage.builder()
                .roomid(roomId)
                .build();

        return new Gson().toJson(jsonObject) + "\n";
    }

    // used by client to direct communicate with server
    public static String genClientMessage(String content) {
        ClientSendMessage jsonObject = ClientSendMessage.builder()
                .content(content)
                .build();

        return new Gson().toJson(jsonObject) + "\n";
    }

    public static String genRelayMessage(String identity, String content) {
        ServerRelayMessage jsonObject = ServerRelayMessage.builder()
                .content(content)
                .identity(identity)
                .build();

        return new Gson().toJson(jsonObject) + "\n";
    }
}