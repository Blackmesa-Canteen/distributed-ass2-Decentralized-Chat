package org.team54.server;

import org.team54.model.Peer;
import org.team54.model.PeerConnection;
import org.team54.utils.Constants;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
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

    public static synchronized NeighborPeerManager getInstance() {
        if (instance == null) {
            instance = new NeighborPeerManager();
        }

        return instance;
    }

    private NeighborPeerManager() {
        neighborPeerMap = new ConcurrentHashMap<>();
    }

    /**
     * will be called when accept a new incoming connection
     *
     * @param newSocketChannel
     */
    public void registerNewSocketChannelAsNeighbor(SocketChannel newSocketChannel) {
        try {
            InetSocketAddress remoteAddress = (InetSocketAddress) newSocketChannel.getRemoteAddress();
            String hostText = remoteAddress.getHostString() + ":" + remoteAddress.getPort();

            Peer peerInstance = Peer.builder()
                    .id(hostText)
                    .formerRoomId(null)
                    .roomId(null)
                    .hostName(remoteAddress.getHostString())
                    .hostPort(Constants.NON_PORT_DESIGNATED)
                    .build();

            PeerConnection connection = PeerConnection.builder()
                    .peer(peerInstance)
                    .socketChannel(newSocketChannel)
                    .build();

            peerInstance.setPeerConnection(connection);

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

        // TODO: leave local room and broadcast leaving msg in local room, if peer has

        // try to close this peer connection
        peer.getPeerConnection().closeMe();
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
        Collection<Peer> values = null;

        synchronized (neighborPeerMap) {
            values = neighborPeerMap.values();
        }

        Iterator<Peer> iterator = values.iterator();
        ArrayList<Peer> res = new ArrayList<>();

        while (iterator.hasNext()) {
            res.add(iterator.next());
        }

        return res;
    }
}