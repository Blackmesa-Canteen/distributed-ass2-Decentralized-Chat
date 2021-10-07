package org.team54.server;

import org.team54.model.Peer;
import org.team54.model.PeerConnection;
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
    private ChatServer chatServer;

    private final ConcurrentHashMap<SocketChannel, Peer> neighborPeerMap;
    private final HashSet<String> peerConnectionTextBlackList;
    private final ConcurrentHashMap<String, Peer> livingPeers;
    private final ChatRoomManager chatRoomManager;

    public static synchronized NeighborPeerManager getInstance() {
        if (instance == null) {
            instance = new NeighborPeerManager();
        }

        return instance;
    }

    private NeighborPeerManager() {
        neighborPeerMap = new ConcurrentHashMap<>();
        peerConnectionTextBlackList = new HashSet<>();
        chatRoomManager = ChatRoomManager.getInstance();
        livingPeers = new ConcurrentHashMap<>();
    }

    public void setChatServer(ChatServer chatServer) {
        this.chatServer = chatServer;
    }

    /**
     * in initial setup, the peer's identity's id's port is NOT listen port
     * now change it to the listen port
     * <p>
     * After this, the peer will be accepted.
     *
     * @param peer        peer
     * @param newIdentity new host text
     */
    public void updatePeerIdentityIdPort(Peer peer, String newIdentity) {

        String originalPeerId = peer.getId();
        int newPort = 0;
        if (newIdentity != null) {
            System.out.println("[debug] newIdentity text received: " + newIdentity);
            newPort = StringUtils.parsePortNumFromHostText(newIdentity);
            System.out.println("[debug]update peer to new port:" + newPort);
        }

        // newIdentity is private address,
        // so we use combination: peer original connection's hostString + newport
        String newPeerId = StringUtils.parseHostnameFromHostText(originalPeerId)
                + ":" + newPort;

        System.out.println("[debug]new peer port: " + newPeerId);

        // judge whether the peer is the local peer or not
        if (chatServer != null) {

            // get this server's local listening address
            String localAddress = chatServer.getLocalListeningHostText();

            if (newIdentity != null && newIdentity.equals(localAddress)) {
                System.out.println("[debug] connect to myself");
                peer.setSelfPeer(true);
            }
        }

        // update hashmap and so on
        synchronized (livingPeers) {

            Peer peerObj = livingPeers.get(originalPeerId);
            if (!livingPeers.containsKey(newPeerId)) {
                livingPeers.put(newPeerId, peerObj);
                livingPeers.remove(originalPeerId);
                // not temp id, then can be searched out
                peer.setTempId(false);
            } else {
                System.out.println("[debug] update peer indentity failed");
            }
        }
    }

    /**
     * will be called when accept a new incoming connection
     *
     * @param newSocketChannel
     */
    public void registerNewSocketChannelAsNeighbor(SocketChannel newSocketChannel) {
        try {

            // get socketChannel host info for peer id

            /* !! ALERT : this is just a temp id! will be changed soon !!*/
            InetSocketAddress remoteAddress = (InetSocketAddress) newSocketChannel.getRemoteAddress();

            // public host address + connection port, NOT listening port
//            String hostText = remoteAddress.getHostString() + ":" + remoteAddress.getPort();
            String hostText = StringUtils.getHostTextFromInetSocketAddress(remoteAddress);

            System.out.println("[debug] new Peer id: " + hostText);

            Peer peerInstance = Peer.builder()
                    .id(hostText)
                    .originalConnectionHostText(hostText)
                    .formerRoomId("")
                    .roomId("")
                    .isTempId(true)
                    .hostName(remoteAddress.getHostString())
                    .hostPort(Constants.NON_PORT_DESIGNATED)
                    .build();

            PeerConnection connection = PeerConnection.builder()
                    .peer(peerInstance)
                    .socketChannel(newSocketChannel)
                    .build();

            peerInstance.setPeerConnection(connection);

            // check blacklist: blacklist contains host + port
            // compare original connect host
            if (isHostNameInBlackList(hostText)) {
                // TODO handle connect fail message

                // close connection
                newSocketChannel.close();
                connection = null;
                peerInstance = null;
                return;
            }

            // TODO check whether the peer is local peer himself: can be handled in parsing hostchange message!

            synchronized (livingPeers) {
                livingPeers.put(peerInstance.getId(), peerInstance);
            }

            // put the new peer in hashmap
            // put、remove and clear need to get a lock
            synchronized (neighborPeerMap) {
                neighborPeerMap.put(newSocketChannel, peerInstance);
            }
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
        return neighborPeerMap.get(socketChannel);
    }

    /**
     * check whether the peer is living or not
     *
     * @param peerId string peer id
     */
    public boolean isPeerLivingByPeerId(String peerId) {
        if (peerId != null) {
            synchronized (livingPeers) {
                return livingPeers.containsKey(peerId);
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

            synchronized (livingPeers) {
                return livingPeers.get(id);
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
        synchronized (livingPeers) {
            livingPeers.remove(peer.getId());
        }

        // remove the peer from neighbor peer map
        synchronized (neighborPeerMap) {
            neighborPeerMap.remove(socketChannel);
        }

        // if the peer has joined a room, exit
        if (!"".equals(peer.getRoomId())) {
            chatRoomManager.removePeerFromRoomId(peer.getRoomId(), peer);
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
            synchronized (neighborPeerMap) {
                peer = neighborPeerMap.get(socketChannel);
                neighborPeerMap.remove(socketChannel);
            }

            synchronized (livingPeers) {
                if (peer != null) {
                    livingPeers.remove(peer.getId());
                }
            }

            // if the peer has joined a room, exit
            if (peer != null && !"".equals(peer.getRoomId())) {
                chatRoomManager.removePeerFromRoomId(peer.getRoomId(), peer);
                // TODO: leave local room and broadcast leaving msg in local room, if peer has
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
     * @return peers arraylist
     */
    public List<Peer> getAllNeighborPeers() {
        ArrayList<Peer> res = new ArrayList<>();
        synchronized (neighborPeerMap) {
            Collection<Peer> values = neighborPeerMap.values();
            for (Peer peer : values) {

                // not including temp id one
                if (!peer.isTempId()) {
                    res.add(peer);
                }
            }
        }

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

            synchronized (peerConnectionTextBlackList) {
                peerConnectionTextBlackList.add(hostText);
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
        synchronized (peerConnectionTextBlackList) {
            if (hostString != null) {
                return peerConnectionTextBlackList.contains(hostString);
            }
        }

        return false;
    }
}