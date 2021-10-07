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
    private String id;
    private String formerRoomId = "";
    private String roomId = "";

    private PeerConnection peerConnection;

    /** c2s host info, get from "hostchange" message that is sent upon connection */
    private String hostName;
    private int hostPort;

    /** whether this peer is the peer himself, or is a remote peer */
    private boolean isSelfPeer;
}