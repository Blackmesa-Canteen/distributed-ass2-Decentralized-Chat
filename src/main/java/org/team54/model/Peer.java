package org.team54.model;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @author Xiaotian
 * @program distributed-ass2-Decentralized-Chat
 * @description peer belongs to a connection
 * @create 2021-10-06 12:24
 */
@Data
@Builder
@NoArgsConstructor
@ToString
public class Peer {

    /** id string: 192.168.1.10:3000 */
    private String id;
    private String formerRoomId;
    private String roomId;

    private PeerConnection peerConnection;
}