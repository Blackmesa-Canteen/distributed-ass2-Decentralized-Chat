package org.team54.service;

import com.google.gson.Gson;
import org.team54.messageBean.*;
import org.team54.model.Peer;
import org.team54.model.Room;
import org.team54.server.ChatRoomManager;
import org.team54.server.NeighborPeerManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Xiaotian
 * @program distributed-ass2-Decentralized-Chat
 * @description
 * @create 2021-10-08 11:21
 */
public class RoomMessageService {
    private static final ChatRoomManager chatRoomManager = ChatRoomManager.getInstance();
    private static final NeighborPeerManager neighborPeerManager = NeighborPeerManager.getInstance();

    /** used by client to join a room */
    public static String genJoinRoomMsg(String roomId) {
        JoinRoomMessage jsonObject = JoinRoomMessage.builder()
                .roomid(roomId)
                .build();

        return new Gson().toJson(jsonObject) + "\n";
    }

    public static String genRoomChangeResponseMsg(String identity, String former, String roomId) {
        RoomChangeMessage jsonObject = RoomChangeMessage.builder()
                .roomid(roomId)
                .former(former)
                .identity(identity)
                .build();

        return new Gson().toJson(jsonObject) + "\n";
    }

    /** used by client to gen who query message */
    public static String genWhoQueryMsg(String roomId) {
        WhoMessage jsonObject = WhoMessage.builder()
                .roomid(roomId)
                .build();

        return new Gson().toJson(jsonObject) + "\n";
    }

    public static String genRoomContentResponseMsg(String roomId) {
        Room room = chatRoomManager.findRoomById(roomId);
        List<String> identities = new ArrayList<>();

        if (room != null) {
            for (Peer peer : room.getPeers()) {
                identities.add(peer.getId());
            }
        }

        RoomContentsMessage jsonObject = RoomContentsMessage.builder()
                .roomid(roomId)
                .identities(identities)
                .build();

        return new Gson().toJson(jsonObject) + "\n";
    }

    /** used by client to gen chat message to server */
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

    /** used by client to gen list message to get Room list from server */
    public static String genListMessage(String roomId) {
        ListMessage jsonObject = ListMessage.builder().build();

        return new Gson().toJson(jsonObject) + "\n";
    }

    public static String genRoomListResponseMsg() {
        List<Room> allRooms = chatRoomManager.findAllRooms();
        List<RoomDTO> roomDTOList = new ArrayList<>();

        for (Room room : allRooms) {
            if (room != null) {
                String roomId = room.getRoomId();
                int count = room.getPeers().size();

                RoomDTO roomDTO = RoomDTO.builder()
                        .roomid(roomId)
                        .count(count)
                        .build();

                roomDTOList.add(roomDTO);
            }
        }

        RoomListMessage jsonObject = RoomListMessage.builder()
                .rooms(roomDTOList)
                .build();

        return new Gson().toJson(jsonObject) + "\n";
    }
}