package org.team54.server;

import org.team54.model.Peer;
import org.team54.model.PeerConnection;
import org.team54.service.MessageServices;
import org.team54.utils.Constants;
import org.team54.utils.StringUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Xiaotian
 * @program distributed-ass2-Decentralized-Chat
 * @description container for holding neighbors.
 * @create 2021-10-06 12:20
 */
public class NeighborPeerManager {

    private static NeighborPeerManager instance;

    /** TODO this is master peer, 就是当这个本Peer A以成员身份加入Peer B的聊天室, masterPeer就用来保存下上游Peer B的地址的文本信息,
     *  主要为了获得到本peer加入的房间server的域名文本信息,这样list neighbor就能正常显示上游的peer节点的地址了 */
    /** masterPeer 是唯一的,因为一个peer同时只能加入一个peer,和其一个聊天室 */
    /** 如果master peer 是这个服务器自己, isSelfPeer设为true, 防止list neighbor时重复list出来自己 */
    /** masterPeer 不用存peerConnection字段,因为不会在server这里以server身份给masterPeer发信息 */
    /** masterPeer 其余的字段设置好; 如果和这个masterPeer断开了,就把masterPeer设置为""; 如果又连接新的Peer准备加房,再设置上它 */
    private Peer masterPeer;

    /** These are member peer */
    private final ConcurrentHashMap<SocketChannel, Peer> neighborMemberPeerMap;
    private final HashSet<String> memberPeerConnectionTextBlackList;
    private final ConcurrentHashMap<String, Peer> livingMemberPeers;

    private ChatRoomManager chatRoomManager;

    public static synchronized NeighborPeerManager getInstance() {
        if (instance == null) {
            instance = new NeighborPeerManager();
        }

        return instance;
    }

    private NeighborPeerManager() {
        neighborMemberPeerMap = new ConcurrentHashMap<>();
        memberPeerConnectionTextBlackList = new HashSet<>();
        livingMemberPeers = new ConcurrentHashMap<>();
    }

    public void setChatRoomManager(ChatRoomManager chatRoomManager) {
        this.chatRoomManager = chatRoomManager;
    }

    /**
     * // TODO 获得masterPeer
     * @return masterPeer
     */
    public Peer getMasterPeer() {
        return masterPeer;
    }

    /**
     * // TODO 不要忘记设置masterPeer,如果当本Peer主动连接到另一个Peer, 设置那个Peer的各种文本信息(主要是域名信息)实例化成Peer对象,放进来; 当本peer quit了那个peer, 置masterPeer为"",
     * @param masterPeer
     */
    public void setMasterPeer(Peer masterPeer) {
        this.masterPeer = masterPeer;
    }

    /**
     * in initial setup, the peer's identity's id's port is NOT listen port
     * we need to get listen port
     *
     * @param peer        peer
     * @param incomingHost new host text
     */
    public void updatePeerWithHostChange(Peer peer, String incomingHost, String incomingHashId) {

        int listenPort = 0;
        String localHostString = "";
        if (incomingHost != null) {
            listenPort = StringUtils.parsePortNumFromHostText(incomingHost);
            localHostString = StringUtils.parseHostStringFromHostText(incomingHost);
            // System.out.println("[debug] incomingHost: " + incomingHost);
            // System.out.println("[debug] server got listen port: " + listenPort);
        }

        // incomingHost is private address,
        // so we use combination: peer original connection's hostString + newport
//        String newPeerIdentity = publicHostString + ":" + listenPort;
//        System.out.println("[debug]new peer port: " + newPeerIdentity);

        /* judge whether the peer is the local peer or not */
//        if (chatServer != null) {
//
//            // get this server's local listening address
//            String localAddress = chatServer.getLocalListeningHostText();
//
//            if (incomingHost != null && incomingHost.equals(localAddress)) {
//                System.out.println("[debug] connect to myself");
//                peer.setSelfPeer(true);
//            }
//        }

        /* judge whether the incoming peer is this local peer or not */
        if (incomingHashId.equals(Constants.THIS_PEER_HASH_ID)) {
            peer.setSelfPeer(true);
        }

        // update peer info
        peer.setListenPort(listenPort);
        peer.setLocalHostName(localHostString);
        peer.setGotListenPort(true);
//        synchronized (livingMemberPeers) {
//
//            Peer peerObj = livingMemberPeers.get(originalPeerId);
//            if (!livingMemberPeers.containsKey(newPeerId)) {
//                livingMemberPeers.put(newPeerId, peerObj);
//                livingMemberPeers.remove(originalPeerId);
//                // not temp id, then can be searched out
//                peer.setIdentity(newPeerId);
//                peer.setListenPort(listenPort);
//                peer.setLocalHostName(localHostString);
//
//                peer.setGotListenPort(true);
//            } else {
//                System.out.println("[debug] update peer indentity failed, already connected");
//            }
//        }
    }

    /**
     * will be called when accept a new incoming connection,
     * the peer is downward peer, which means these peers are members
     *
     * @param newSocketChannel
     */
    public void registerNewSocketChannelAsNeighbor(SocketChannel newSocketChannel) {
        try {

            // get socketChannel host info for peer id

            /* public ip + outgoing port */
            InetSocketAddress remoteAddress = (InetSocketAddress) newSocketChannel.getRemoteAddress();

            // public host address + connection port, NOT listening port
//            String hostText = remoteAddress.getHostString() + ":" + remoteAddress.getPort();
            String hostText = StringUtils.getHostTextFromInetSocketAddress(remoteAddress);

            // System.out.println("[debug] new Peer identity: " + hostText);

            Peer peerInstance = Peer.builder()
                    .identity(hostText)
                    .originalConnectionHostText(hostText)
                    .formerRoomId("")
                    .roomId("")
                    .isGotListenPort(false)
                    .localHostName("")
                    .listenPort(Constants.NON_PORT_DESIGNATED)
                    .outgoingPort(StringUtils.parsePortNumFromHostText(hostText))
                    .publicHostName(StringUtils.parseHostStringFromHostText(hostText))
                    .build();

            PeerConnection connection = PeerConnection.builder()
                    .peerId(hostText)
                    .socketChannel(newSocketChannel)
                    .build();

            peerInstance.setPeerConnection(connection);

            // check blacklist: blacklist contains host + port
            // compare original connect host
            if (isHostNameInBlackList(hostText)) {
                System.out.println("Rejected kicked connection from: " + hostText);
                // close connection
                newSocketChannel.close();
                connection = null;
                peerInstance = null;
                return;
            }

            // put the new member peer in hashmap
            // put、remove and clear need to get a lock
            synchronized (neighborMemberPeerMap) {
                if (neighborMemberPeerMap.containsKey(newSocketChannel)) {
                    // System.out.println("[debug] please don't connect for twice.");
                    return;
                } else {
                    neighborMemberPeerMap.put(newSocketChannel, peerInstance);
                }
            }

            synchronized (livingMemberPeers) {
                livingMemberPeers.putIfAbsent(peerInstance.getIdentity(), peerInstance);
            }

            // System.out.println("[debug] server has put new peer in neighbor");
            // System.out.println("[debug] server has put new connection " + peerInstance.toString() + " in server living peers.");

        } catch (IOException e) {
            System.out.println("err in registerNewSocketChannelAsNeighbor");
            e.printStackTrace();
        }
    }

    /**
     * get peer model with a specific socketChannel
     *
     * @param socketChannel key
     * @return peer model
     */
    public Peer getPeerWithSocketChannel(SocketChannel socketChannel) {
        // using concurrentHashMap, read don't need to get lock.
        return neighborMemberPeerMap.get(socketChannel);
    }

    /**
     * check whether the peer is living or not
     *
     * @param peerId string peer id
     */
    public boolean isPeerLivingByPeerId(String peerId) {
        if (peerId != null) {
            synchronized (livingMemberPeers) {
                return livingMemberPeers.containsKey(peerId);
            }
        }

        return false;
    }

    /**
     * find peer by peer id
     *
     * @param id stirng peer id
     * @return peer, null if not found
     */
    public Peer getPeerByPeerId(String id) {

        if (id != null && id.length() > 0) {

            synchronized (livingMemberPeers) {
                return livingMemberPeers.get(id);
            }

//            synchronized (neighborPeerMap) {
//                ArrayList<Peer> peers = new ArrayList<>(neighborPeerMap.values());
//                for (Peer peer : peers) {
//                    if (peer.getId().equals(id)) {
//                        return peer;
//                    }
//                }
//            }
        }

        return null;
    }

    /**
     * handle neighbor disconnect
     *
     * @param peer
     */
    public void handleDisconnectNeighborPeer(Peer peer) {

        SocketChannel socketChannel = peer.getPeerConnection().getSocketChannel();

        // remove living peer record
        synchronized (livingMemberPeers) {
            livingMemberPeers.remove(peer.getIdentity());
        }

        // if the peer has joined a room, exit
        if (!"".equals(peer.getRoomId())) {
            chatRoomManager.removePeerFromRoomId(peer.getRoomId(), peer);
        } else {
            // if no room joined, send empty room change response to this peer only.
            // to make sure "When the client that is disconnecting receives
            // the RoomChange message, then it can close the connection."
            String roomChangeResponseMsg = MessageServices.genRoomChangeResponseMsg(
                    peer.getIdentity(),
                    "",
                    ""
            );

            peer.getPeerConnection().sendTextMsgToMe(roomChangeResponseMsg);
        }

        // remove the peer from neighbor peer map
        synchronized (neighborMemberPeerMap) {
            neighborMemberPeerMap.remove(socketChannel);
        }

        // try to close this peer connection
        peer.getPeerConnection().closeMe();
        peer.setPeerConnection(null);
    }

    /**
     * handle neighbor disconnect
     *
     * @param socketChannel socketChannel
     */
    public void handleDisconnectNeighborSocketChannel(SocketChannel socketChannel) {

        if (socketChannel != null) {

            // remove the peer from neighbor peer map
            Peer peer = null;
            synchronized (neighborMemberPeerMap) {
                peer = neighborMemberPeerMap.get(socketChannel);
                neighborMemberPeerMap.remove(socketChannel);
            }

            if (peer != null) {
                synchronized (livingMemberPeers) {
                    livingMemberPeers.remove(peer.getIdentity());
                }
            }

            // if the peer has joined a room, exit
            if (peer != null && !"".equals(peer.getRoomId())) {
                chatRoomManager.removePeerFromRoomId(peer.getRoomId(), peer);
            }

            // try to close this peer connection
            try {
                socketChannel.close();
            } catch (IOException e) {
                System.out.println("err in handleDisconnectSocketChannel");
                e.printStackTrace();
            }
        }
    }

    /**
     * get all Neighbor peers
     * <p>
     * Not including temp id one
     *
     * @param peerExcluded the peer that is excluded, e.g.
     *                     address of the client that issued listNeighbors request
     * @return peers arraylist
     */
    public List<Peer> getAllNeighborPeers(Peer peerExcluded) {
        ArrayList<Peer> res = new ArrayList<>();
        synchronized (neighborMemberPeerMap) {
            Collection<Peer> values = neighborMemberPeerMap.values();
            for (Peer peer : values) {

                // peer should have listen port, and not self, and exclude target peer
                if (peer != null
                        && peer.isGotListenPort()
                        && !peer.isSelfPeer()
                        && !peer.equals(peerExcluded)) {

                    res.add(peer);
                }
            }
        }

        // add chatroom server peer that this peer is belongs to and is not this peer himself
        if (masterPeer != null && !masterPeer.isSelfPeer()) {
            res.add(masterPeer);
        }

        // System.out.println("[debug] getAllNeighbors: " + res.toString());
        return res;
    }

    /**
     * put a peer's hostname in the black list
     *
     * @param kickedPeer peer that is kicked
     */
    public void addPeerHostNameToBlackList(Peer kickedPeer) {
        InetSocketAddress remoteAddress = null;

        // can't kick himself
        if (kickedPeer.isSelfPeer()) {
            return;
        }

        try {

            // add real connection address text in the blacklist
            remoteAddress = (InetSocketAddress) kickedPeer.getPeerConnection().getSocketChannel().getRemoteAddress();
            String hostText = StringUtils.getHostTextFromInetSocketAddress(remoteAddress);

            synchronized (memberPeerConnectionTextBlackList) {
                memberPeerConnectionTextBlackList.add(hostText);
            }

        } catch (IOException e) {
            System.out.println("err in addPeerHostNameToBlackList");
            e.printStackTrace();
        }
    }

    /**
     * check whether a hostname is in blacklist or not
     *
     * @param hostString string
     * @return true if in the blacklist
     */
    public boolean isHostNameInBlackList(String hostString) {
        synchronized (memberPeerConnectionTextBlackList) {
            if (hostString != null) {
                return memberPeerConnectionTextBlackList.contains(hostString);
            }
        }

        return false;
    }
}