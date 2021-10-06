package org.team54.server;

import org.team54.model.Peer;

import java.nio.channels.SocketChannel;
import java.util.HashMap;

/**
 * @author Xiaotian
 * @program distributed-ass2-Decentralized-Chat
 * @description
 * @create 2021-10-06 12:20
 */
public class NeighborManager {

    private HashMap<SocketChannel, Peer> channelPeerMap;
}