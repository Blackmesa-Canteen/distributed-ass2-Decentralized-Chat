package org.team54.client;

import org.team54.model.Peer;
import org.team54.server.ChatRoomManager;
import org.team54.server.NeighborPeerManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public class BFS {
    private final NeighborPeerManager neighborPeerManager =  NeighborPeerManager.getInstance();

    // close list to store the hashID of visited peers
    private List<String> closeList = new ArrayList<String>();
    // queue
    private LinkedBlockingQueue<Peer> queue = new LinkedBlockingQueue(128);

    public void search(Peer localPeer) throws InterruptedException {
        // add root node, which is the localPeer
        queue.offer(localPeer);

        while(!queue.isEmpty()){
            // take the first peer from the queue, find children
            Peer curPeer = queue.take();
            // judge if we have visted the current peer before
            if(closeList.contains(curPeer.getHashId())){
                // visited before, finish this iteration
                continue;
            }else{
                // add current peer to close list
                closeList.add(curPeer.getHashId());
            }

            // get children of current Peer, working on it
            List<Peer> allNeighborPeers = neighborPeerManager.getAllNeighborPeers(curPeer);
            for(Peer peer: allNeighborPeers){
                System.out.println(peer.getIdentity());
            }

        }

    }

    public static void main(String[] args){

        //BFS(localPeer);
    }
}
