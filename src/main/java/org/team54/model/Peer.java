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
    private volatile String identity; // local hostname + local peer's listenting port
    private volatile String serverSideIdentity; // public hostname + local peer's output port

    @Builder.Default
    private String originalConnectionHostText = "";

    /** hashId when a peer is created, and will be trensferred to neiborpeer in hostchange message
     * can be used to detect whether the Peer is this peer self or not */
    @Builder.Default
    private String hashId = "";

    /** use volatile to ensure threads get updated roomId */
    @Builder.Default
    private volatile String formerRoomId = "";
    @Builder.Default
    private volatile String roomId = "";

    private PeerConnection peerConnection;

    /** 公网ip "xxx.xxx.xxx.xxx", 不包含端口, 本机自己的peer得不到公网ip,就先放着本地ip吧 */
    private String publicHostName;
    /** outgoing */
    private int outgoingPort;
    /** 这用来存从client发出的hostchange内容里的 123.123.123.123:4444 的 123.123.123.123, 因为本机自己的peer得不到公网ip所以hostchange里只能够得到本地ip */
    private String localHostName;
    /** listening port, 这用来存从client发出的hostchange内容里的 123.123.123.123:4444 的 4444 */
    private volatile int listenPort;

    /** whether this peer is the peer himself, or is a remote peer */
    @Builder.Default
    private volatile boolean isSelfPeer = false;

    /** at first connection, the id's port is not accept port, need to be altered in hostchange packet */
    @Builder.Default
    private volatile boolean isGotListenPort = false;


}