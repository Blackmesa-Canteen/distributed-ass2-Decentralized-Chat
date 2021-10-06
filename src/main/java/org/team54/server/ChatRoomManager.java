package org.team54.server;

import org.team54.model.Peer;
import org.team54.model.Room;
import org.team54.utils.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Xiaotian
 * @program distributed-ass2-Decentralized-Chat
 * @description chat Room manager of this peer
 * @create 2021-10-06 12:21
 */
public class ChatRoomManager {
    private static ChatRoomManager instance;

    /**
     * roomId, Room model
     */
    private final ConcurrentHashMap<String, Room> liveRoomMap;

    public static synchronized ChatRoomManager getInstance() {
        if (instance == null) {
            instance = new ChatRoomManager();
        }

        return instance;
    }

    private ChatRoomManager() {
        this.liveRoomMap = new ConcurrentHashMap<>();
    }

    /**
     * find room model by id
     *
     * @param id roomId
     * @return room model
     */
    public Room findRoomById(String id) {
        return liveRoomMap.get(id);
    }

    /**
     * get all room models from the peer
     *
     * @return roomsList
     */
    public List<Room> findAllRooms() {
        ArrayList<Room> res = new ArrayList<>();
        synchronized (liveRoomMap) {
            Collection<Room> values = liveRoomMap.values();
            res.addAll(values);
        }

        return res;
    }

    /**
     * create a empty room without any clients.
     * <p>
     * only this local peer can create Room.
     * <p>
     * The peer out puts either e.g. “Room jokes created.”
     * or “Room jokes is invalid or already in use.”
     * depending on whether the local command worked or not.
     *
     * @param roomId roomId must contain alphanumeric characters only ,
     *               start with an upper or lower case letter,
     *               have at least 3 characters and at most 32 characters.
     *               And not used already.
     */
    public void createNewEmptyRoom(String roomId) {

        // check room name: legal
        if (StringUtils.isValidRoomId(roomId)) {
            Room room = Room.builder()
                    .roomId(roomId)
                    .peers(new ArrayList<>())
                    .build();

            // put(), remove(), clear() need lock in ConcurrentHashMap
            synchronized (liveRoomMap) {
                Room prevRoom = liveRoomMap.putIfAbsent(roomId, room);

                // check whether already exists
                if (prevRoom != null) {
                    // has prev room, show failed info
                    System.out.println("Room " + roomId + " is invalid or already in use.");
                } else {
                    // this is new room, show success info
                    System.out.println("Room " + roomId + " created.");
                }
            }
        } else {

            // illegal name, show failed info
            System.out.println("Room " + roomId + " is invalid or already in use.");
        }
    }

    /**
     * Join the peer to the room
     * <p>
     * If roomid is invalid or non existent then client’s current room will not change.
     * <p>
     * The special room given by "" indicates that the client wants to leave the room
     * but not join another room. i.e. stay connected but not be in any room.
     * <p>
     * Otherwise the client’s current room will change to the requested room.
     * <p>
     * If the room did not change then the server will send a RoomChange message only to
     * the client that requested the room change.
     * <p>
     * If the room did change, then the server will send a RoomChange message to all clients
     * currently in the requesting client’s current room and the requesting client’s requested room.
     *
     * @param roomId targetRoom ID
     * @param peer peer requesting
     */
    public void joinPeerToRoom(String roomId, Peer peer) {
        Room targetRoom = null;
        String previousRoomId = "";

        boolean isSuccessful = false;

        // lock to prevent join a deleted null room
        synchronized (liveRoomMap) {
            targetRoom = liveRoomMap.get(roomId);
            if (targetRoom != null) {
                // if target Room exists
                // check peer existence
                if (!targetRoom.getPeers().contains(peer)) {
                    // if peer not joined the room

                    // get prev roomId of peer
                    previousRoomId = peer.getRoomId();

                    // add peer to new room
                    targetRoom.getPeers().add(peer);

                    // TODO notify new room

                    // kick peer from prev room
                    if (previousRoomId != null) {
                        Room prevRoom = liveRoomMap.get(previousRoomId);

                        // TODO notify old room

                        prevRoom.getPeers().remove(peer);
                    }

                    // TODO other room info

                    // update current peer info
                    peer.setFormerRoomId(previousRoomId);
                    peer.setRoomId(targetRoom.getRoomId());

                    isSuccessful = true;
                }
            }
        }

        if (!isSuccessful) {
            // TODO send failed info
        }
    }
}