package org.team54.model;

import lombok.*;

/**
 * @author Xiaotian
 * @program distributed-ass2-Decentralized-Chat
 * @description peer belongs to a connection
 * @create 2021-10-06 12:24
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Peer {

    /** id string: 192.168.1.10:3000 */
    private volatile String id;
    private String originalConnectionHostText = "";

    /** use volatile to ensure threads get updated roomId */
    private volatile String formerRoomId = "";
    private volatile String roomId = "";

    private PeerConnection peerConnection;

    private String publicHostName;
    private int outgoingPort;
    /** c2s host info, get from "hostchange" message that is sent upon connection */
    private String localHostName;
    /** listening port */
    private int listenPort;

    /** whether this peer is the peer himself, or is a remote peer */
    private volatile boolean isSelfPeer;

    /** at first connection, the id's port is not accept port, need to be altered in hostchange packet */
    private volatile boolean isTempId = true;
}