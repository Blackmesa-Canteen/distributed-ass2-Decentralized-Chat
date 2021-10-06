package org.team54.server;

import org.team54.model.Room;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Xiaotian
 * @program distributed-ass2-Decentralized-Chat
 * @description
 * @create 2021-10-06 12:21
 */
public class ChatRoomManager {
    private static ChatRoomManager instance;

    private final Map<String, Room> liveRoomMap;

    public static synchronized ChatRoomManager getInstance() {
        if (instance == null) {
            instance = new ChatRoomManager();
        }

        return instance;
    }

    private ChatRoomManager() {
        this.liveRoomMap = new HashMap<>();
    }
}