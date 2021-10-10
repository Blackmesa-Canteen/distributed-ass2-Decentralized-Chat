package org.team54.server;

import org.team54.model.Peer;
import org.team54.model.Room;
import org.team54.service.MessageServices;
import org.team54.utils.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
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

    private final NeighborPeerManager neighborPeerManager;

    public static synchronized ChatRoomManager getInstance() {
        if (instance == null) {
            instance = new ChatRoomManager();
        }

        return instance;
    }

    private ChatRoomManager() {
        this.liveRoomMap = new ConcurrentHashMap<>();
        this.neighborPeerManager = NeighborPeerManager.getInstance();
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
     * @param peer   peer requesting
     */
    public void joinPeerToRoom(String roomId, Peer peer) {
        Room targetRoom = null;
        String previousRoomId = "";

        boolean isSuccessful = false;

//        // make sure the peer's id is updated by hostchange
//        if (peer.isGotListenPort()) {

        // handle "" empty room, just remove the peer from current room
        if ("".equals(roomId)) {
            synchronized (liveRoomMap) {
                previousRoomId = peer.getRoomId();
                Room room = liveRoomMap.get(previousRoomId);

                if (room != null) {
                    // notify client leave the room
                    if (room.getPeers().contains(peer)) {
                        isSuccessful = true;

                        broadcastMessageInRoom(peer,
                                previousRoomId,
                                MessageServices.genRoomChangeResponseMsg(
                                        peer.getIdentity(),
                                        previousRoomId,
                                        roomId),
                                null
                        );

                        room.getPeers().remove(peer);

                        // update current peer info
                        peer.setFormerRoomId(previousRoomId);
                        peer.setRoomId("");
                    }
                }
            }

        } else {
            // if target room is not ""
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

                        // rm peer from prev room
                        if (previousRoomId != null) {
                            if (!"".equals(previousRoomId)) {
                                Room prevRoom = liveRoomMap.get(previousRoomId);

                                // notify old room, except peer itself (prevent send roomchage twice to this peer)
                                broadcastMessageInRoom(
                                        peer,
                                        previousRoomId,
                                        MessageServices.genRoomChangeResponseMsg(
                                                peer.getIdentity(),
                                                previousRoomId,
                                                roomId),
                                        peer
                                );
                                prevRoom.getPeers().remove(peer);
                            }
                        }

                        // update current peer info
                        peer.setFormerRoomId(previousRoomId);
                        peer.setRoomId(roomId);

                        // notify new room (only the user is in the room, can notify with this method)
                        broadcastMessageInRoom(
                                peer,
                                roomId,
                                MessageServices.genRoomChangeResponseMsg(
                                        peer.getIdentity(),
                                        previousRoomId,
                                        roomId),
                                null
                        );

                        isSuccessful = true;
                    }
                }
            }
        }
//        }

        if (!isSuccessful) {

            // only send unchanged roomchange msg to that peer
            peer.getPeerConnection().sendTextMsgToMe(
                    MessageServices.genRoomChangeResponseMsg(peer.getIdentity(), peer.getRoomId(), peer.getRoomId()));
        }
    }

    /**
     * boradcast text message within a room
     *
     * @param srcPeer      peer that want to broadcast
     * @param roomId       string room id
     * @param message      text message
     * @param peerExcluded Peer that is excluded
     */
    public void broadcastMessageInRoom(Peer srcPeer, String roomId, String message, Peer peerExcluded) {

        // lock until done message sending
        synchronized (liveRoomMap) {
            Room room = liveRoomMap.get(roomId);

            // make sure room is not null, and the peer is in this room
            if (room != null && srcPeer.getRoomId().equals(roomId)) {
                ArrayList<Peer> peers = room.getPeers();
                for (Peer peer : peers) {
                    if (peerExcluded == null || !peerExcluded.equals(peer)) {
                        peer.getPeerConnection().sendTextMsgToMe(message);
                    }
                }
            }
        }
    }

    /**
     * broadcast a message among all rooms
     *
     * @param message String message
     */
    public void broadcastMessageInAllRoom(String message) {
        synchronized (liveRoomMap) {
            for (Room room : liveRoomMap.values()) {
                // make sure room is not null, and the peer is in this room
                if (room != null) {
                    ArrayList<Peer> peers = room.getPeers();
                    for (Peer peer : peers) {
                        peer.getPeerConnection().sendTextMsgToMe(message);
                    }
                }
            }
        }
    }

    /**
     * remove a peer in a room **politely**
     * <p>
     * !! THIS IS NOT KICK !!
     *
     * @param roomId     string roomid
     * @param targetPeer Peer need to be removed
     */
    public void removePeerFromRoomId(String roomId, Peer targetPeer) {

        // if the roomId is "", don't do anything
        if ("".equals(roomId)) {
            return;
        }

        // not atomic, so lock
        synchronized (liveRoomMap) {
            // if user current in this room
            if (targetPeer.getRoomId().equals(roomId)) {
                Room room = liveRoomMap.get(roomId);
                if (room != null) {

                    // broadcast room change message in this room
                    broadcastMessageInRoom(
                            targetPeer,
                            roomId,
                            MessageServices.genRoomChangeResponseMsg(
                                    targetPeer.getIdentity(),
                                    targetPeer.getRoomId(),
                                    ""
                            ),
                            null);

                    // remove the targetPeer from room
                    room.getPeers().remove(targetPeer);
                }

                // update peer info
                targetPeer.setRoomId("");
                targetPeer.setFormerRoomId(roomId);
            }
        }
    }

    /**
     * !! THIS IS KICK, WILL ALSO BAN THE USER !!
     * <p>
     * kick and ban a peer, call by the owner
     *
     * @param peerId Peer ID
     */
    public void kickPeerByPeerId(String peerId) {
        Peer kickedPeer = neighborPeerManager.getPeerByPeerId(peerId);
        if (kickedPeer != null) {
            // remove from the room (don't need below because handle disconnect will remove him)
            // removePeerFromRoomId(roomId, kickedPeer);

            // add black list
            neighborPeerManager.addPeerHostNameToBlackList(kickedPeer);

            // disconnect this peer
            neighborPeerManager.handleDisconnectNeighborPeer(kickedPeer);
        }
    }

    /**
     * remove a room by Id
     * <p>
     * owner of a room can at any time issue a Delete command.
     * <p>
     * Similarly to the create room and kick user, this command does not
     * generate messages and the logic is simply executed locally.
     * <p>
     * The peer will first treat this as if all users of the room had sent a
     * RoomChange message to the empty room "". Then the peer will delete the room.
     * <p>
     * The peer can print whether the room was successfully deleted or not.
     *
     * @param roomId String room Id
     */
    public void deleteRoomById(String roomId) {

        synchronized (liveRoomMap) {
            Room room = liveRoomMap.get(roomId);
            if (room != null) {

                // get all peers reference
                ArrayList<Peer> peers = room.getPeers();
                // unload this room's peer's reference with a new empty arraylist
                room.setPeers(new ArrayList<>());

                System.out.println("[debug] deleteRoom original peers size: " + peers.size());

                for (Peer peer : peers) {
                    if (peer.getRoomId().equals(roomId)) {

                        // send message to this peer that he is quit to "" room
                        peer.getPeerConnection().sendTextMsgToMe(
                                MessageServices.genRoomChangeResponseMsg(
                                        peer.getIdentity(),
                                        roomId,
                                        ""
                                )
                        );

                        peer.setRoomId("");
                        peer.setFormerRoomId(roomId);
                    }
                }

                // remove the room from map
                liveRoomMap.remove(roomId);
            } else {
                System.out.println("Room delete failed: No such room.");
            }
        }
    }

    /**
     * safely gen room content msg and send to peer
     *
     * @param peer   Peer
     * @param roomId target room id
     */
    public void sendRoomContentMsgToPeer(Peer peer, String roomId) {
        synchronized (liveRoomMap) {
            peer.getPeerConnection().sendTextMsgToMe(MessageServices.genRoomContentResponseMsg(roomId));
        }
    }

    /**
     * safely send roomlist msg to peer
     *
     * @param peer Peer
     */
    public void sendRoomListMsgToPeer(Peer peer) {
        synchronized (liveRoomMap) {
            peer.getPeerConnection().sendTextMsgToMe(MessageServices.genRoomListResponseMsg());
        }
    }
}