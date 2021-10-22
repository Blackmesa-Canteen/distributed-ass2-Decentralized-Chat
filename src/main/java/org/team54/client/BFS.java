package org.team54.client;

import com.alibaba.fastjson.JSONObject;
import org.team54.model.BFSTuple;
import org.team54.messageBean.RoomDTO;
import org.team54.messageBean.RoomListMessage;
import org.team54.messageBean.ServerRespondNeighborsMessage;
import org.team54.model.Peer;
import org.team54.server.ChatRoomManager;
import org.team54.server.NeighborPeerManager;
import org.team54.service.MessageServices;
import org.team54.utils.Constants;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class BFS implements Runnable {
    private final NeighborPeerManager neighborPeerManager =  NeighborPeerManager.getInstance();
    private SocketChannel searchChannel;
    public AtomicBoolean alive = new AtomicBoolean();
    private ByteBuffer writeBuffer = ByteBuffer.allocate(2048);
    private ByteBuffer readBuffer = ByteBuffer.allocate(2048);


    @Override
    public void run(){

    }

    public HashMap search(Peer localPeer) throws InterruptedException, IOException {
        HashMap<String,HashMap> BFSResult = new HashMap<>();
        // HashMap<String,String> roomContent = new HashMap<>();

        // close list to store the hashID of visited peers
        // cannot get hashID from listneighbor option, use identity instead.
        //List<String> closeList = new ArrayList<String>();
        Set<String> closeSet = new HashSet<String>();

        // queue, store identity of Peer
        LinkedBlockingQueue<String> queue = new LinkedBlockingQueue(128);

        // add root node, which is the localPeer
        // queue.offer(localPeer);
        // should not connect to localPeer
        // thus, get the local neighborPeerManager to get localPeer's neighbor
        // push them into the queue
        List<Peer> allNeighborPeers = neighborPeerManager.getAllNeighborPeers(localPeer);
        for(Peer peer: allNeighborPeers){
            // get identity
            String hostText = peer.getPublicHostName() + ":" + peer.getListenPort();
            // push child to queue
            queue.offer(hostText);
            // System.out.println("[debug client] pushed peer is: " + hostText);
        }

        while(!queue.isEmpty()){
            // take the first peer from the queue, find children
            String curPeer = queue.take();
            // judge if we have visted the current peer before
            if(closeSet.contains(curPeer)){
                // visited before, finish this iteration
                continue;
            }else{
                // add current peer to close list
                closeSet.add(curPeer);

                // add current peer to result dict
                BFSResult.put(curPeer,null);
            }

            // get children of current Peer
            BFSTuple bfsTuple = findChild(curPeer);
            // update roomlist of current peer
            BFSResult.put(curPeer,bfsTuple.getRoomMap());
            // push child into queue
            List<String> neighborList = bfsTuple.getNeighborList();
            for(String peer:neighborList){
                if(!queue.contains(peer)){
                    queue.offer(peer);
                }
            }

        }
        return BFSResult;
    }

    private BFSTuple findChild(String peer) throws IOException, InterruptedException {
        //HashMap<String,HashMap> peerMap = new HashMap<>();
        BFSTuple bfsTuple = BFSTuple.builder().build();
        List<String> neighborList = new ArrayList<>();

        InetAddress address = InetAddress.getByName(peer.split(":")[0]);
        //InetAddress address = InetAddress.getByName(peer.getLocalHostName());
        int port = Integer.parseInt(peer.split(":")[1]);

        if(connect(port,address)){// if connect success, send list neighbor request to server
            String message = MessageServices.genListNeighborsRequestMessage();
            writeBuffer.clear();
            writeBuffer.put(message.getBytes(StandardCharsets.UTF_8));
            writeBuffer.flip();
            searchChannel.write(writeBuffer);
        }

        // wait for neighbor message from server
        alive.set(true);
        while(alive.get()){
            if(!searchChannel.isConnected()){
                // System.out.println("[debug client] in BFS reading, not connected yet");
                return null;
            }
            readBuffer.clear();

            int readNum = searchChannel.read(readBuffer);

            if(readNum == -1){
                // System.out.println("[debug client] in client BFS read, bad read, client close");
                stop();
                return null;
            }
            if(readNum!=0){ // get response
                // deal with received message, byte[] -> JSONObject
                byte[] data = readBuffer.array();
                String message = new String(data,0,readNum);
                JSONObject replyDataObject = JSONObject.parseObject(message);

                // get type
                String type = replyDataObject.getString("type");
                // switch based on response type
                if(type.equals(Constants.LIST_NEIGHBORS_RESPOND_JSON_TYPE)){
                    // is neighbor message, return list of neighbors
                    ServerRespondNeighborsMessage SRNM = replyDataObject.toJavaObject(replyDataObject,ServerRespondNeighborsMessage.class);
                    neighborList = SRNM.getNeighbors();
                    bfsTuple.setNeighborList(neighborList);
//                    for(String peerName:neighbors){
//                        peerMap.put(peerName,null);
//                    }
                    // send room list message
                    String roomListMessage = MessageServices.genListRequestMessage(null);
                    writeBuffer.clear();
                    writeBuffer.put(roomListMessage.getBytes(StandardCharsets.UTF_8));
                    writeBuffer.flip();
                    searchChannel.write(writeBuffer);
                }else if(type.equals(Constants.ROOM_LIST_JSON_TYPE)){ // room content response
                    //handle room list message
                    HashMap<String,Integer> roomMap = new HashMap<>();

                    RoomListMessage RLM = replyDataObject.toJavaObject(replyDataObject,RoomListMessage.class);

                    for(RoomDTO RDTO: RLM.getRooms()){
                        roomMap.put(RDTO.getRoomid(),RDTO.getCount());
                    }
                    bfsTuple.setRoomMap(roomMap);
                    //return result.trim();

                    // already get neighbor info, send quit request
                    String quitMessaage = MessageServices.genQuitRequestMessage();
                    writeBuffer.clear();
                    writeBuffer.put(quitMessaage.getBytes(StandardCharsets.UTF_8));
                    writeBuffer.flip();
                    searchChannel.write(writeBuffer);
                }else if(type.equals(Constants.ROOM_CHANGE_JSON_TYPE)){// quit response
                    // already get neighbor info, stop reading

                    Thread.sleep(1000); // wait server to close socketchannel
                    // System.out.println("[debug client] in search network, get room change response, close");
                    stop();
                } else{ // not the list neighbor message, keep waiting
                    continue;
                }


            }
        }

        return bfsTuple;
    }

    private boolean connect(int port, InetAddress address) throws IOException  {
        // init a socketchannel and set to NIO mode
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);

        // server's address, address+port e.g. 127.0.0.1:1234,
        InetSocketAddress isa = new InetSocketAddress(address,port);

        socketChannel.connect(isa);
        socketChannel.finishConnect();

        if(socketChannel.isConnected()){
            this.searchChannel = socketChannel;
            return true;
        }else{
            // System.out.println("[debug client] fail to connect to " +  isa + " in BFS connect");
            return false;
        }
    }

    private boolean stop() throws IOException{
        if(!this.searchChannel.isConnected()){
            // System.out.println("[debug client] searchChannel not connected yet");
            return false;
        }
        alive.set(false);
        searchChannel.close();
        return true;
    }



}
