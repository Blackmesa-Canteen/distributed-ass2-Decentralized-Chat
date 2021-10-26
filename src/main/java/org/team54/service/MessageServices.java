package org.team54.service;

import com.google.gson.Gson;
import org.team54.messageBean.*;
import org.team54.model.Peer;
import org.team54.model.Room;
import org.team54.server.ChatRoomManager;
import org.team54.server.NeighborPeerManager;
import org.team54.utils.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Xiaotian
 * @program distributed-ass2-Decentralized-Chat
 * @description message generator
 * @create 2021-10-08 11:21
 */
public class MessageServices {
    private static final ChatRoomManager chatRoomManager = ChatRoomManager.getInstance();
    private static final NeighborPeerManager neighborPeerManager = NeighborPeerManager.getInstance();

    /**
     * TODO used by client to gen hostChange Message right after the connect attempt
     *
     * @param host hostString:listenPort
     * @return json request for sending
     */
    public static String genHostChangeRequestMessage(String host) {
        HostChangeMessage jsonObject = HostChangeMessage.builder()
                .host(host)
                .hashId(Constants.THIS_PEER_HASH_ID)
                .build();
        return new Gson().toJson(jsonObject) + "\n";
    }

    /** TODO used by client to gen quit request */
    public static String genQuitRequestMessage() {
        QuitMessage jsonObject = QuitMessage.builder().build();

        return new Gson().toJson(jsonObject) + "\n";
    }

    /**
     * TODO used by client gen join a room request
     *
     * @param roomId room Id want to join
     * @return json request for sending
     */
    public static String genJoinRoomRequestMessage(String roomId) {
        JoinRoomMessage jsonObject = JoinRoomMessage.builder()
                .roomid(roomId)
                .build();

        return new Gson().toJson(jsonObject) + "\n";
    }

    public static String genRoomChangeResponseMsg(String identity, String former, String roomId, String peerHashId) {
        RoomChangeMessage jsonObject = RoomChangeMessage.builder()
                .roomid(roomId)
                .former(former)
                .identity(identity)
                .peerHashId(peerHashId)
                .build();

        return new Gson().toJson(jsonObject) + "\n";
    }

    /**
     * TODO used by client to gen who query message request
     *
     * @param roomId room Id that want to issue who command
     * @return request json to send
     */
    public static String genWhoQueryRequestMessage(String roomId) {
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
                identities.add(peer.getIdentity());
            }
        }

        RoomContentsMessage jsonObject = RoomContentsMessage.builder()
                .roomid(roomId)
                .identities(identities)
                .build();

        return new Gson().toJson(jsonObject) + "\n";
    }

    /** TODO used by client to gen chat message request to server */
    public static String genClientChatMessage(String content) {
        ClientSendMessage jsonObject = ClientSendMessage.builder()
                .content(content)
                .build();

        return new Gson().toJson(jsonObject) + "\n";
    }

    /**
     * TODO used by client to gen shout message to server
     * only need content, the rest of attributes will be handled by server
     *
     * @return shout message json
     */
    public static String genRootShoutChatRequestMessage(String hashId, String content) {
        ShoutMessage jsonObject = ShoutMessage.builder()
                .content(content)
                .rootIdentity("")
                .shoutMessageHashId(hashId)
                .build();

        return new Gson().toJson(jsonObject) + "\n";
    }

    public static String genRelayShoutChatMessage(String hashId, String content, String rootIdentity) {
        ShoutMessage jsonObject = ShoutMessage.builder()
                .content(content)
                .rootIdentity(rootIdentity)
                .shoutMessageHashId(hashId)
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

    /** TODO used by client to gen list message to get Room list request from server */
    public static String genListRequestMessage(String roomId) {
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

    /** TODO used by client to get list neighbors request of a remote server */
    public static String genListNeighborsRequestMessage() {
        ClientListNeighborsMessage jsonObject = ClientListNeighborsMessage.builder().build();
        return new Gson().toJson(jsonObject) + "\n";
    }

    public static String genListNeighborsResponseMsg(Peer peerRequester,  List<Peer> allNeighborPeers) {

        // String res: public ip : listening port
        List<String> res = new ArrayList<>();
        for (Peer peer : allNeighborPeers) {
            String hostText = peer.getPublicHostName() + ":" + peer.getListenPort();
            // System.out.println("[debug] response neighbor peer hostname: " + hostText);
            res.add(hostText);
        }

        ServerRespondNeighborsMessage jsonObject = ServerRespondNeighborsMessage.builder()
                .neighbors(res)
                .build();

        return new Gson().toJson(jsonObject) + "\n";
    }

}