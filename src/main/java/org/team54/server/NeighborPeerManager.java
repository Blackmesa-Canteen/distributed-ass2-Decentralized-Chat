package org.team54.server;

import org.team54.model.Peer;
import org.team54.model.PeerConnection;
import org.team54.utils.Constants;
import org.team54.utils.StringUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
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

    private final ConcurrentHashMap<SocketChannel, Peer> neighborPeerMap;
    private final HashSet<String> peerHostNameBlackList;
    private final ChatRoomManager chatRoomManager;

    public static synchronized NeighborPeerManager getInstance() {
        if (instance == null) {
            instance = new NeighborPeerManager();
        }

        return instance;
    }

    private NeighborPeerManager() {
        neighborPeerMap = new ConcurrentHashMap<>();
        peerHostNameBlackList = new HashSet<>();
        chatRoomManager = ChatRoomManager.getInstance();
    }

    /**
     * will be called when accept a new incoming connection
     *
     * @param newSocketChannel
     */
    public void registerNewSocketChannelAsNeighbor(SocketChannel newSocketChannel) {
        try {

            // get socketChannel host info for peer id
            InetSocketAddress remoteAddress = (InetSocketAddress) newSocketChannel.getRemoteAddress();
            String hostText = remoteAddress.getHostString() + ":" + remoteAddress.getPort();

            Peer peerInstance = Peer.builder()
                    .id(hostText)
                    .formerRoomId("")
                    .roomId("")
                    .hostName(remoteAddress.getHostString())
                    .hostPort(Constants.NON_PORT_DESIGNATED)
                    .build();

            PeerConnection connection = PeerConnection.builder()
                    .peer(peerInstance)
                    .socketChannel(newSocketChannel)
                    .build();

            peerInstance.setPeerConnection(connection);

            // check blacklist
            if (isHostNameInBlackList(hostText)) {
                // TODO handle connect fail message

                // close connection
                newSocketChannel.close();
                connection = null;
                peerInstance = null;
                return;
            }

            // TODO check whether the peer is local peer himself: can be handled in parsing hostchange message!

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
     * find peer by peer id
     *
     * low performance.
     *
     * @param id stirng peer id
     * @return peer, null if not found
     */
    public Peer getPeerByPeerId(String id) {

        if (id != null && id.length() > 0) {

            synchronized (neighborPeerMap) {
                ArrayList<Peer> peers = new ArrayList<>(neighborPeerMap.values());
                for (Peer peer : peers) {
                    if (peer.getId().equals(id)) {
                        return peer;
                    }
                }
            }
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

        // remove the peer from neighbor peer map
        synchronized (neighborPeerMap) {
            neighborPeerMap.remove(socketChannel);
        }


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
            synchronized (neighborPeerMap) {
                neighborPeerMap.remove(socketChannel);
            }

            // TODO: leave local room and broadcast leaving msg in local room, if peer has

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
     *
     * @return peers arraylist
     */
    public List<Peer> getAllNeighborPeers() {
        ArrayList<Peer> res = new ArrayList<>();
        synchronized (neighborPeerMap) {
            Collection<Peer> values = neighborPeerMap.values();
            res.addAll(values);
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
            remoteAddress = (InetSocketAddress) kickedPeer.getPeerConnection().getSocketChannel().getRemoteAddress();
            String hostString = remoteAddress.getHostString();

            synchronized (peerHostNameBlackList) {
                if (hostString != null) {
                    peerHostNameBlackList.add(hostString);
                }
            }
        } catch (IOException e) {
            System.out.println("err in addPeerHostNameToBlackList");
            e.printStackTrace();
        }
    }

    /**
     * check whether a hostname is in blacklist or not
     *
     * @param hostname string
     * @return true if in the blacklist
     */
    public boolean isHostNameInBlackList(String hostname) {
        synchronized (peerHostNameBlackList) {
            if (hostname != null) {
                return peerHostNameBlackList.contains(hostname);
            }
        }

        return false;
    }
}