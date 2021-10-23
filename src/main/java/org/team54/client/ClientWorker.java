package org.team54.client;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import org.team54.messageBean.RoomDTO;
import org.team54.messageBean.RoomListMessage;
import org.team54.messageBean.ServerRespondNeighborsMessage;
import org.team54.messageBean.ShoutMessage;
import org.team54.model.Peer;
import org.team54.utils.Constants;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClientWorker implements Runnable{
    public AtomicBoolean alive = new AtomicBoolean();
    public AtomicBoolean waitingQuitResponse = new AtomicBoolean();

    // mission queue to store mission from client thread
    private LinkedBlockingQueue<ClientDataEvent> queue = new LinkedBlockingQueue(128);
    private Peer localPeer;

    public ClientWorker(Peer localPeer){
        this.localPeer = localPeer;
    }

    @Override
    public void run(){
        alive.set(true);
        ClientDataEvent dataEvent;
        while(alive.get()){
            try {
                dataEvent = queue.take();
            } catch (InterruptedException e) {
                continue;
            }

            // try to handle json message here
            handleData(dataEvent);
        }
    }

    /**
     * add the received event to quest queue
     * this method will be called when socket channel receives a new message
     * after that, the soket channel's thread can continue without block
     * client worker takes the message from the quest queue and dealing with them in another thread
     * @param nioClient
     * @param socketChannel the socket channel data comes from
     * @param data
     * @param size data size
     */
    public void processEvent(NIOClient nioClient, SocketChannel socketChannel, byte[] data, int size){
        byte[] copiedData = new byte[size];
        System.arraycopy(data,0,copiedData,0,size);
        ClientDataEvent clientdataEvent = new ClientDataEvent(nioClient,socketChannel,copiedData);
        queue.offer(clientdataEvent);
    }



    private void handleData(ClientDataEvent clientdataEvent){
        //get the received data
        String data = new String(clientdataEvent.data,0,clientdataEvent.data.length);
        // print2Console("[debug client] received data is "+data);
        JSONObject replyDataObject = JSONObject.parseObject(data);

        String type = replyDataObject.getString("type");
        switch (type){
            case Constants.ROOM_CHANGE_JSON_TYPE:
                print2Console(handleRoomChangeMessage(replyDataObject));
                break;
            case Constants.ROOM_CONTENTS_JSON_TYPE:
                print2Console(handleRoomContentsMessage(replyDataObject));
                break;
            case Constants.ROOM_LIST_JSON_TYPE:
                print2Console(handleRoomListMessage(replyDataObject));
                break;
            case Constants.LIST_NEIGHBORS_RESPOND_JSON_TYPE:
                print2Console(handleListNeighborMessage(replyDataObject));
                break;
            case Constants.MESSAGE_JSON_TYPE:
                print2Console(handleMessage(replyDataObject));
                break;
            case Constants.SHOUT_JSON_TYPE:
                print2Console(handleShoutMessage(replyDataObject));

        }
        // start to handle json messages from server, hasn't finished yet;
    }

    private void print2Console(String text){
        if("".equals(text)){// if text is null, do not print
            return;
        }
        if(ScannerWorker.waitingInput.get()){
            System.out.println("\n" +  text);
            ScannerWorker.waitingInput.set(false);
        }else{
            System.out.println(text);
        }
    }

    private String handleMessage(JSONObject replyDataObject){
        String result = "";
        String speakerIdentity = replyDataObject.getString("identity");
        String content = replyDataObject.getString("content");
        result = "[" + speakerIdentity + "]" + ": " + content;
        return result;
    }

    private String handleRoomChangeMessage(JSONObject replyDataObject){
        String identity = replyDataObject.getString("identity");
        String roomid = replyDataObject.getString("roomid");
        String former = replyDataObject.getString("former");
        String result = "";
        if(identity.equals(this.localPeer.getServerSideIdentity())){//if the current client is the one who changes room
            if(waitingQuitResponse.get() == true){ // if the current peer is waiting for quit response
                if(this.localPeer.getRoomId().length() != 0) {//quit without joining a room
                    result = identity + " leaves " + this.localPeer.getRoomId();
                    // server will close the socketchannel, client will have a bad read and close in NIOClient
                }
                // set waiting for quit to false
                waitingQuitResponse.set(false);
            }else{ // if the current peer is waiting for room change response
                if(former.equals(roomid)){ // fail to join
                    result = "The request room is invalid or non existent";
                }else{ // join success
                    if("".equals(former)){ // join room first time
                        result = identity + " moved to " + roomid;
                    }else{
                        if(roomid.length() == 0){ // leave room
                            result = identity + " leaves "+ former;
                        }else{ // change room
                            result = identity + " moved from "+ former +" to "+roomid;
                        }

                    }
                    //update local client variables
                    this.localPeer.setFormerRoomId(this.localPeer.getRoomId());
                    this.localPeer.setRoomId(roomid);
                }

            }
        }else{//if the current client is not the one who changes room
            if(roomid.length()==0){//other client quit from server
                result = identity + " leaves " + this.localPeer.getRoomId();
            }else if(former.length()==0){//other client fails to join
                result = identity + " moved to " + roomid;
            }else{//other client joins successfully
                result = identity + " moved from "+ former +" to "+roomid;
            }
        }

        return result;

    }

    public String handleRoomContentsMessage(JSONObject replyDataObject){
        //Gson gson = new Gson();
        //JSONArray jsonArray = (JSONArray)replyDataObject.get("identities");
        //List<String> identities = (List<String>)JSONArray.parseArray(jsonArray.toString(),String.class);
        //RoomContentsMessage RCM = gson.fromJson(JsonMessage,RoomContentsMessage.class);
        List<String> idList = replyDataObject.getObject("identities",List.class);

        String identities = "";
        for(String identity:idList){
            identities += identity+" ";
        }
        String result = replyDataObject.get("roomid") +" contains " + identities;
        return result;

    }

    public String handleRoomListMessage(JSONObject replyDataObject){
        String result = "";
        //replyDataObject.getObject("rooms",RoomDTO.class);
        RoomListMessage RLM = replyDataObject.toJavaObject(replyDataObject,RoomListMessage.class);

        for(RoomDTO RDTO: RLM.getRooms()){
            result += RDTO.getRoomid()+": "+RDTO.getCount()+" guests\n";
        }
        return result.trim();
    }

    public String handleListNeighborMessage(JSONObject replyDataObject){
        String result = "";

        ServerRespondNeighborsMessage SRNM = replyDataObject.toJavaObject(replyDataObject,ServerRespondNeighborsMessage.class);
        List<String> neighbors = SRNM.getNeighbors();

        if(neighbors.size() == 0){
            result = "no neighbors found";
        }else{
            for(String neighbor: neighbors){
                result+= neighbor + " ";
            }
        }

        return result;
    }

    private String handleShoutMessage(JSONObject replyDataObject){
        String result = "";
        ShoutMessage SM = replyDataObject.toJavaObject(replyDataObject,ShoutMessage.class);

        String identity = SM.getRootIdentity();
        String content = SM.getContent();
        result = "[" + identity + " shouted]: " + content;
        return result;
    }

}
