package org.team54.client;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
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
        pint2Console(data);
        JSONObject replyDataObject = JSONObject.parseObject(data);

        String type = replyDataObject.getString("type");
        switch (type){
            case Constants.ROOM_CHANGE_JSON_TYPE:
                pint2Console(handleRoomChangeMessage(replyDataObject));
                break;
            case Constants.ROOM_CONTENTS_JSON_TYPE:
                pint2Console(handleRoomContentsMessage(replyDataObject));
                break;
            case Constants.ROOM_LIST_JSON_TYPE:
                break;
            case Constants.MESSAGE_JSON_TYPE:
                break;

        }
        // start to handle json messages from server, hasn't finished yet;
    }

    private void pint2Console(String text){
        if("".equals(text)){// if text is null, do not print
            return;
        }
        if(ScannerWorker.waitingInput.get()){
            System.out.println("\n" + "[debug client] received data is " + text);
            ScannerWorker.waitingInput.set(false);
        }else{
            System.out.println("[debug client] received data is " + text);
        }
    }

    private String handleRoomChangeMessage(JSONObject replyDataObject){
        String identity = replyDataObject.getString("identity");
        String roomid = replyDataObject.getString("roomid");
        String former = replyDataObject.getString("former");
        String result = "";

        if(identity.equals(this.localPeer.getIdentity())){//if the current client is the one who changes room
            if(roomid.length()==0){//current client quit from server
                result = identity + " leaves " + this.localPeer.getRoomId();
                result += "\nDisconnected from " + this.localPeer.getPublicHostName();
                //this.localPeer.setStatus(Constants.CLOSE);
            }else if(former.equals(roomid)){//current client fails to join
                result = "The request room is invalid or nonexistent";
                //client.setStatus(Constants.COMMON_STATUS);
            }else{//current client joins successfully
                result = identity + " moves from "+ former +" to "+roomid;
                //update local client variables
                this.localPeer.setFormerRoomId(this.localPeer.getRoomId());
                this.localPeer.setRoomId(roomid);
                //client.setStatus(Constants.COMMON_STATUS);
            }
        }else{//if the current client is not the one who changes room
            if(roomid.length()==0){//other client quit from server
                result = identity + " leaves " + this.localPeer.getRoomId();
            }else if(former.length()==0){//other client fails to join
                result = identity + " moves to " + roomid;
            }else{//other client joins successfully
                result = identity + " moves from "+ former +" to "+roomid;
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

//    public String handleRoomListMessag(JSONArray replyDataObject){
//        //Gson gson = new Gson();
//        //RoomListMessage RLM = gson.fromJson(JsonMessage,RoomListMessage.class);
//        boolean success = DeleteRoomSuccess(client,RLM);
//        if(success){
//            result = "Room " + client.getTempRoomName() + " delete success";
//        }else{
//            result = "Room " + client.getTempRoomName() + " delete fails";
//        }
//        //update local client variables
//        client.setRoomDTOList(RLM.getRooms());
//        client.setTempRoomName(null);
//        //roomcreate request has been processed, back to commonstatus
//        client.setStatus(Constants.COMMON_STATUS);
//        break;
//    }
//
//    private boolean DeleteRoomSuccess(Peer peer, RoomListMessage RLM){
//        ArrayList<String> newlist = new ArrayList<>();
//        for(RoomDTO RDTO:RLM.getRooms()){
//            newlist.add(RDTO.getRoomid());//add roomlist to local room_list variable
//        }
//        ArrayList<String> oldlist = client.printRoomList();
//        if(!newlist.contains(client.getTempRoomName()) && oldlist.contains(client.getTempRoomName())){
//            return  true;
//        }else{
//            return  false;
//        }
//    }
}
