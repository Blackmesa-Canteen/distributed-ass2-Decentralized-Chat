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

    /** 公网ip "xxx.xxx.xxx.xxx", 不包含端口, 本机自己的peer得不到公网ip,就先放着本地ip吧 */
    private String publicHostName;
    /** outgoing */
    private int outgoingPort;
    /** 这用来存从client发出的hostchange内容里的 123.123.123.123:4444 的 123.123.123.123, 因为本机自己的peer得不到公网ip所以hostchange里只能够得到本地ip */
    private String localHostName;
    /** listening port, 这用来存从client发出的hostchange内容里的 123.123.123.123:4444 的 4444 */
    private int listenPort;

    /** whether this peer is the peer himself, or is a remote peer */
    private volatile boolean isSelfPeer;

    /** at first connection, the id's port is not accept port, need to be altered in hostchange packet */
    private volatile boolean isGotListenPort = true;
}