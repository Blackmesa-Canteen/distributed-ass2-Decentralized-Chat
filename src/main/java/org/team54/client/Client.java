package org.team54.client;

import com.alibaba.fastjson.JSONObject;
import org.team54.app.ChatPeer;
import org.team54.messageBean.RoomDTO;
import org.team54.messageBean.RoomListMessage;
import org.team54.messageBean.ServerRespondNeighborsMessage;
import org.team54.messageBean.ShoutMessage;
import org.team54.model.Peer;
import org.team54.utils.Constants;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Client implements Runnable{

    public AtomicBoolean alive = new AtomicBoolean();
    public AtomicBoolean connectLocal = new AtomicBoolean();
    public AtomicBoolean waitingQuitResponse = new AtomicBoolean();
    public AtomicBoolean waitingRoomChangeResponse = new AtomicBoolean();
    public AtomicBoolean inConnectProcess = new AtomicBoolean();
    // record the number of connect the client established
    // connectNum should in [0,1], cannot connect to more than 1 server
    public int connectNum = 0;

    private Peer localPeer;
    private SocketChannel socketChannel;
    private ByteBuffer writeBuffer = ByteBuffer.allocate(2048);
    private ByteBuffer readBuffer = ByteBuffer.allocate(2048);

    public Client(Peer localPeer){
        this.localPeer = localPeer;
    }

    @Override
    public void run(){
        alive.set(true);

        // the initial state should be connect locally
        connectLocal();

        // spin to get available socketchannel
        while(alive.get()){

            try{
                if(inConnectProcess.get() == false && (socketChannel == null || !socketChannel.isConnected())){ // if no connection, connect locally
                    connectLocal();
                }
                Read(socketChannel);
                //Thread.sleep(100);
            }catch (IOException e){
                e.printStackTrace();
            }

        }
    }

    public void disConnect() throws IOException{
        if(!this.socketChannel.isConnected()){
            System.out.println("not connected yet");
            return;
        }

        socketChannel.close();
        // disconnect, connectNum -1
        connectNum -= 1;

        if(connectLocal.get() == false){ // should be silent if disconnect from local server
            print2Console("Disconnected from " + this.localPeer.getPublicHostName());
            //alive.set(false);
        }

        // need to set ClientBind port to -1 for next connection
        ChatPeer.setClientPort(-1);
        // reset values in localPeer
        resetPeer();
    }

    public SocketChannel startConn(int localport, int port, InetAddress address) throws IOException{
        // init a socketchannel and set to NIO mode
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        // bind local port to the socket
        // if localport is -1, do not bind, establish the connect with random localport
        if(localport!=-1){
            // init localaddress for socket to bind with
            InetSocketAddress localaddress = new InetSocketAddress("127.0.0.1",localport);
            // let the bond port can be used in TIME_WAIT status after disconnect
            socketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            socketChannel.bind(localaddress);

        }

        // server's address, address+port e.g. 127.0.0.1:1234,
        InetSocketAddress isa = new InetSocketAddress(address,port);

        socketChannel.connect(isa);
        socketChannel.finishConnect();

        this.socketChannel = socketChannel;

        if(socketChannel.isConnected()){
            // set changes to localPeer

            localPeer.setPublicHostName(socketChannel.getRemoteAddress().toString().split(":")[0].replace("localhost","").replace("/",""));
            localPeer.setOutgoingPort(socketChannel.socket().getLocalPort());
            localPeer.setLocalHostName(address.toString().split(":")[0].replace("localhost","").replace("/",""));
            localPeer.setIdentity(localPeer.getLocalHostName()+":"+localPeer.getListenPort());
            localPeer.setServerSideIdentity(localPeer.getPublicHostName()+":"+localPeer.getOutgoingPort());

            // System.out.println("[debug client] localhostName : " + localPeer.getLocalHostName());
            // System.out.println("[debug client] outgoingPort : " + localPeer.getOutgoingPort());
            // System.out.println("[debug client] listeningPort : " + localPeer.getListenPort());
            // System.out.println("[debug client] identity : " + localPeer.getIdentity());
            // System.out.println("[debug client] serverSideIdentity : " + localPeer.getServerSideIdentity());

            // record the success connection, connect to sever, connectNum +1
            connectNum += 1;
        }else{
            System.out.println("connect fails, maybe bad server address");
        }
        // System.out.println("[debug client] finish connect");
        return socketChannel;
    }

    public void connectLocal(){
        if(socketChannel == null || !socketChannel.isConnected()){
            // if no connection, try to connect local peer
            try{
                connectLocal.set(true);
                InetAddress address = InetAddress.getByName("localhost");
                startConn(this.localPeer.getOutgoingPort(),this.localPeer.getListenPort(),address);
            } catch (UnknownHostException e) {
                System.out.println("[debug client] localhost not found when connecting itself");
            } catch (IOException e) {
                System.out.println("[debug client] connect local fails");
            }

        }
    }

    public void Write(String message) throws IOException{
        if(!this.socketChannel.isConnected()){
            System.out.println("not connected yet");
            return;
        }
        writeBuffer.clear();
        writeBuffer.put(message.getBytes(StandardCharsets.UTF_8));
        writeBuffer.flip();
        this.socketChannel.write(writeBuffer);
    }


    /**
     * read message from the socketchannel
     * and then, pass it to worker's queue
     * @param socketChannel
     * @throws IOException
     */
    private void Read(SocketChannel socketChannel) throws IOException{
        if(!socketChannel.isConnected()){
            //System.out.println("not connected yet");
            return;
        }
        readBuffer.clear();
        //System.out.println("connected " +socketChannel.isConnected());
        // get the length of data in readbuffer
        int readNum = socketChannel.read(readBuffer);

        if(readNum == -1){
            // System.out.println("[debug client] in client, bad read, socketchannel close");
            disConnect();
            return;
        }
        // pass the message to client worker
        if(readNum!=0){
            //get the received data
            String data = new String(readBuffer.array(),0,readNum);

            // print2Console("[debug client] received data is "+data);
            JSONObject replyDataObject = JSONObject.parseObject(data);

            //start to handle data
            handleData(replyDataObject);
        }


    }

    /** need to reset values in Peer after #quit operation */
    public void resetPeer(){

        this.localPeer.setServerSideIdentity(null);
        // HashID will not change
        this.localPeer.setFormerRoomId("");
        this.localPeer.setRoomId("");
        this.localPeer.setPublicHostName(null);
        this.localPeer.setOutgoingPort(-1);
        // local host name will not change
        // local listen port will not change

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

    private void handleData(JSONObject replyDataObject){
        // print2Console("[debug client] received data is "+data);
        //JSONObject replyDataObject = JSONObject.parseObject(data);

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
    }

    private String handleMessage(JSONObject replyDataObject){
        String result = "";
        String speakerIdentity = replyDataObject.getString("identity");
        String content = replyDataObject.getString("content");
        result = "[" + speakerIdentity + "]" + ": " + content;
        return result;
    }

    private String handleRoomContentsMessage(JSONObject replyDataObject){
        String result = "";
        List<String> idList = replyDataObject.getObject("identities",List.class);

        String identities = "";
        for(String identity:idList){
            identities += identity+" ";
        }
        if ("".equals(identities)){
            result = replyDataObject.get("roomid") + " is empty";
        }else{
            result = replyDataObject.get("roomid") + " contains " + identities;
        }

        return result;
    }

    private String handleRoomListMessage(JSONObject replyDataObject){
        String result = "";
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

    private String handleRoomChangeMessage(JSONObject replyDataObject){
        String identity = replyDataObject.getString("identity");
        String roomid = replyDataObject.getString("roomid");
        String former = replyDataObject.getString("former");
        String result = "";
        if(identity.equals(this.localPeer.getServerSideIdentity())){//if the current client is the one who changes room
            if(waitingQuitResponse.get() == true){ // if the current peer is waiting for quit response
                if(this.localPeer.getRoomId().length() != 0) {//quit when in a room
                    result = identity + " leaves " + this.localPeer.getRoomId();
                    // server will close the socketchannel, client will have a bad read and close in NIOClient
                }
                try {
                    disConnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // set waiting for quit to false
                waitingQuitResponse.set(false);
                connectLocal.set(false);

            }else if(waitingRoomChangeResponse.get() == true){ // if the current peer send #join and is waiting for response
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
                // finish handling roomchange message, back to common state
                waitingRoomChangeResponse.set(false);
            } else{ // if the current peer is just listening
                //在没有请求roomchange的情况下，收到本机的roomchange信息，
                // 可能被kick或这room被删除
                if(former.equals(roomid) && "".equals(former)){ //未加入房间时被kick

                }else if(former.length() != 0 && "".equals(roomid)){ // 在房间时被踢出 或 房间被删除
                    result = identity + " leaves "+ former;
                }


            }
        }else{//if the current client is not the one who changes room
            if(roomid.length()==0){//other client quit the room
                result = identity + " leaves " + this.localPeer.getRoomId();
            }else if(former.length()==0){//other client fails to join
                result = identity + " moved to " + roomid;
            }else{//other client joins successfully
                result = identity + " moved from "+ former +" to "+roomid;
            }
        }

        return result;

    }

    public String getServer(){
        try{
            return this.socketChannel.getRemoteAddress().toString();
        }catch (IOException e){
            e.printStackTrace();
        }
        return null;
    }


    /**
     * arr to store identity
     * {"peer server's address and port","peer's hash ID"}
     * e.g. {"192.168.0.0:1234","791P7i7pspC9U1tq27Q8O76yX41lYZ"}
     * @return
     */
    public String[] getIdentity(){
        String[] arr = {"",""};
        String identity = localPeer.getIdentity();
        String hashID = localPeer.getHashId();
        //String identity = address.toString()+":"+ ChatPeer.getServerListenPort();
        //String hashID = Constants.THIS_PEER_HASH_ID;
        // System.out.println("[debug client] in client, getIdnetity, identity: " + identity + " hashID: " + hashID);
        arr[0] = identity;
        arr[1] = hashID;
        return arr;
    }




}
