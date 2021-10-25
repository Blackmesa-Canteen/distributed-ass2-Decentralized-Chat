package org.team54.client;

import org.team54.app.ChatPeer;
import org.team54.messageBean.*;
import org.team54.model.Peer;
import org.team54.server.ChatRoomManager;
import org.team54.service.MessageServices;
import org.team54.utils.Constants;


import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import com.google.gson.Gson;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import org.team54.utils.StringUtils;

public class ScannerWorker implements Runnable{
    public static AtomicBoolean alive = new AtomicBoolean();
    public static AtomicBoolean waitingInput = new AtomicBoolean();

    private Scanner scanner;
    private SocketChannel socketChannel;
    private Client client;
    private ByteBuffer writeBuffer = ByteBuffer.allocate(1024);
    private Peer localPeer;
    private final ChatRoomManager chatRoomManager;

    public ScannerWorker( Client client, Peer localPeer) {
        this.scanner = new Scanner(System.in);
        this.client = client;
        this.localPeer = localPeer;
        this.chatRoomManager = ChatRoomManager.getInstance();
    }

    @Override
    public void run(){
        alive.set(true);
        while(alive.get()){
            if(client.connectLocal.get() == true){ // if connect locally, hide identity util join a room
                if("".equals(this.localPeer.getRoomId()) && this.localPeer.getServerSideIdentity() == null){
                    System.out.printf(">");
                }else if("".equals(this.localPeer.getRoomId()) && this.localPeer.getServerSideIdentity() != null){
                    System.out.printf(">");
                } else if(!"".equals(this.localPeer.getRoomId()) && this.localPeer.getServerSideIdentity() != null){
                    System.out.printf("[%s] %s>", this.localPeer.getRoomId(), this.localPeer.getServerSideIdentity());
                }
            }else{
                if("".equals(this.localPeer.getRoomId()) && this.localPeer.getServerSideIdentity() == null){
                    System.out.printf(">");
                }else{
                    System.out.printf("[%s] %s>", this.localPeer.getRoomId(), this.localPeer.getServerSideIdentity());
                }
            }
            
            waitingInput.set(true);
            String message = scanner.nextLine();

            if("".equals(message)){continue;}

            if(isCommand(message)){// if the user input is command, switch cases based on the first word of input
                String [] arr = message.substring(1).split("\\s+");
                switch (arr[0]){
                    case Constants.Connect_TYPE:
                        handleConnect(arr);
                        break;
                    case Constants.ROOM_CREATE_TYPE: // local command
                        handleCreateRoom(arr);
                        break;
                    case Constants.KICK_TYPE: // local command
                        handleKick(arr);
                        break;
                    case Constants.ROOM_Delete_TYPE: // local command, but local server will send out message
                        handleDelete(arr);
                        break;
                    case Constants.WHO_JSON_TYPE:
                        handleRoomContent(arr);
                        break;
                    case Constants.JOIN_JSON_TYPE:
                        handleJoin(arr);
                        break;
                    case Constants.LIST_JSON_TYPE:
                        handleListMessage(arr);
                        break;
                    case Constants.QUIT_JSON_TYPE:
                        handleQuit();;
                        break;
                    case Constants.LIST_NEIGHBORS_JSON_TYPE:
                        handleListNeighbors(arr);
                        break;
                    case Constants.Search_Net_TYPE:
                        handleSearchNet(arr);
                        break;
                    case Constants.SHOUT_JSON_TYPE:
                        handleShout(arr);
                        break;
                    default:
                        System.out.println("no such command, please check");
                }
            }else{
                handleMessage(message);
            }



        }

    }

//    public void setSocketChannel(SocketChannel socketChannel) {
//        this.socketChannel = socketChannel;
//    }

    public void setClient(Client client){
        this.client = client;
    }

    // if the input String starts with #, return true
    public boolean isCommand (String message){
        return(message.substring(0,1).equals("#"));
    }


    // handle the #connect commad
    public void handleConnect(String[] arr){
        if(arr.length == 1){
            System.out.println("missing connect parameter");
        }else if(arr.length == 2){
            try {
                this.client.inConnectProcess.set(true);
                // if connect locally, stop local connect first
                if(this.client.connectLocal.get() == true){
                    if(localPeer.getRoomId().length()!=0){
                        System.out.println("currently in a local room, please use #join \"\" to quit room first");
                        return;
                    }else{
                        String quitMessaage = MessageServices.genQuitRequestMessage();
                        this.client.Write(quitMessaage);
                        this.client.waitingQuitResponse.set(true);
                    }

                }

                // wait until disconnect from local finish
                while( client.connectLocal.get()==true || this.client.waitingQuitResponse.get()==true){

                }
                // System.out.println("[debug client] connect local is false");

                // if the client has already connected to a server, return immediately
                if(client.connectNum == 1){
                    System.out.println("already connect to " + client.getServer());
                    return;
                }

                // the input is like #connect 192.168.0.0:1234
                // arr[0] = connect, arr[1] = 192.168.0.0:1234
                // addressArr[0] = 192.168.0.0
                // addressArr[1] = 1234
                String [] addressArr = arr[1].split(":");
                InetAddress address = InetAddress.getByName(addressArr[0]);
                int port = Integer.parseInt(addressArr[1]);
                // strat connection
                this.client.startConn(-1,port,address);
                // send hostchange message to server
                String message = MessageServices.genHostChangeRequestMessage(client.getIdentity()[0]);
                this.client.Write(message);
                this.client.inConnectProcess.set(false);
            } catch (IOException | ArrayIndexOutOfBoundsException e){
                System.out.println("bad #connect command");
            }

        }else if(arr.length == 3){
            try {
                this.client.inConnectProcess.set(true);
                // if connect locally, stop local connect first
                if(this.client.connectLocal.get() == true){
                    if(localPeer.getRoomId().length()!=0){
                        System.out.println("currently in a local room, please use #join \"\" to quit room first");
                        return;
                    }else{
                        String quitMessaage = MessageServices.genQuitRequestMessage();
                        this.client.Write(quitMessaage);
                        this.client.waitingQuitResponse.set(true);
                    }

                }

                // wait until disconnect from local finish
                while( client.connectLocal.get()==true || this.client.waitingQuitResponse.get()==true){

                }
                // System.out.println("[debug client] connect local is false");

                // if the client has already connected to a server, return immediately
                if(client.connectNum == 1){
                    System.out.println("already connect to " + client.getServer() );
                    return;
                }

                // the input is like #connect 192.168.0.0:1234 5000
                // arr[0] = connect, arr[1] = 192.168.0.0:1234, arr[2] = 5000
                // addressArr[0] = 192.168.0.0
                // addressArr[1] = 1234
                String [] addressArr = arr[1].split(":");
                InetAddress address = InetAddress.getByName(addressArr[0]);
                int port = Integer.parseInt(addressArr[1]);
                int localport = Integer.parseInt(arr[2]);
                // start connection
                this.client.startConn(localport,port,address);
                // send hostchange message to server
                String message = MessageServices.genHostChangeRequestMessage(client.getIdentity()[0]);
                this.client.Write(message);
                this.client.inConnectProcess.set(false);
            } catch (IOException | ArrayIndexOutOfBoundsException e){
                System.out.println("bad #connect command");
            }
        }
    }

    public void handleQuit(){

        if(this.client.connectLocal.get() == true){
            System.out.println("not connected yet, try #connect command");
        }else{
            try{
                String quitMessaage = MessageServices.genQuitRequestMessage();
                this.client.Write(quitMessaage);
                client.waitingQuitResponse.set(true);
            }catch(IOException e){
                System.out.println(e.getMessage());
                System.out.println("close fail, try again later");
            }
        }
    }

    // send user's common message to server
    private void handleMessage(String message){
        try{
            if("".equals(localPeer.getRoomId())){
                System.out.println("please join a room to send message");
            } else{
                String CommonMessage = MessageServices.genClientChatMessage(message);
                this.client.Write(CommonMessage);
            }
        }catch(IOException e){
            System.out.println("send message fails, try again later");
        }
    }

    private void handleRoomContent(String[] arr){
        try{
            if(arr.length==1){//if the input command only contains #Who with no following arguments
                System.out.println("invalid command, #who needs 1 argument");
            }else if(arr.length == 2){//correct command paradigm
                String WM = MessageServices.genWhoQueryRequestMessage(arr[1]);
                this.client.Write(WM);
            }else{//other unconsidered situation
                System.out.println("command error");
            }
        }catch(IOException e){
            System.out.println("#who operation fails, try again later");
        }
    }

    private void handleJoin(String[] arr){
        if(arr.length==1){//if the input command only contains #join with no following arguments
            System.out.println("invalid command, #join needs 1 argument");
        }else if(arr.length == 2){//correct command paradigm
            if(arr[1].equals(localPeer.getRoomId())){
                System.out.println("Currently in " + localPeer.getRoomId());
            }else{
                String JRM;
                if("\"\"".equals(arr[1])){
                    JRM = MessageServices.genJoinRoomRequestMessage("");
                }else{
                    JRM = MessageServices.genJoinRoomRequestMessage(arr[1]);
                }

                // set waiting for roomchange response to ture
                client.waitingRoomChangeResponse.set(true);
                try{
                    this.client.Write(JRM);
                }catch (IOException e){
                    System.out.println("join fails, try again later");
                }
            }
        }else{//other unconsidered situation
            System.out.println("command error");
        }


    }

    public void handleListNeighbors(String[] arr){
        if(this.client.alive.get()==false){
            // sockets not connected
            System.out.println("not connected yet. try #connect operation");
        }else{
            if(arr.length==1){
                String listNeighborsMessage = MessageServices.genListNeighborsRequestMessage();
                try{
                    this.client.Write(listNeighborsMessage);
                }catch (IOException e){
                    System.out.println("fail to request for neighbour list");
                }
            }else{
                System.out.println("command error");
            }
        }
    }

    private void handleListMessage(String[] arr){
        if(this.client.alive.get()==false){
            // sockets not connected
            System.out.println("not connected yet. try #connect operation");
        }else{
            if(arr.length==1){
                String listMessage = MessageServices.genListRequestMessage(null);
                try{
                    this.client.Write(listMessage);
                }catch (IOException e){
                    System.out.println("fail to request for room list");
                }
            }else{
                System.out.println("command error");
            }
        }
    }





    private void handleCreateRoom(String[] arr){// local command, can only do with no connection with other servers
        if(this.client.alive.get() == true && this.client.connectLocal.get() == false){
            System.out.println("cannot create room when having connections with other server");
            return;
        }
        if(arr.length == 1){
            System.out.println("invalid command, #createroom option needs 1 argument");
        }else if(arr.length == 2){
            // check input validation first
            if(StringUtils.isValidRoomId(arr[1])){
                chatRoomManager.createNewEmptyRoom(arr[1]);
            }else{
                System.out.println("Invalid ROOMID");
            }
        }else{
            System.out.println("command error");
        }
    }

    private void handleDelete(String[] arr){ // local command, can only do with no connection with other servers
        if(this.client.alive.get() == true && this.client.connectLocal.get() == false){
            System.out.println("cannot delete room when having connections with other server");
            return;
        }
        if(arr.length == 1){
            System.out.println("invalid command, #delete option needs 1 argument");
        }else if(arr.length == 2){
            // check input validation first
            if(StringUtils.isValidRoomId(arr[1])){
                chatRoomManager.deleteRoomById(arr[1]);
            }else{
                System.out.println("Invalid ROOMID");
            }
        }else{
            System.out.println("command error");
        }
    }

    private void handleKick(String[] arr){ // local command, can only do with no connection with other servers
        if(this.client.alive.get()==true && this.client.connectLocal.get() == false){
            System.out.println("cannot kick when having connections with other server");
            return;
        }
        if(arr.length == 1){
            System.out.println("invalid command, #kick option needs 1 argument");
        }else if(arr.length == 2){

            chatRoomManager.kickPeerByPeerId(arr[1]); // cannot know if kick is success
        }else{
            System.out.println("command error");
        }
    }

    private void handleSearchNet(String[] arr){
        if(arr.length == 1){
            // start search net
            BFS bfs = new BFS();
            try{
                HashMap<String, HashMap> BFSResult = bfs.search(localPeer);
                for(String peerKey:BFSResult.keySet()){
                    System.out.println(peerKey);
                    HashMap<String, Integer> roomMap =  BFSResult.get(peerKey);
                    for(String roomKey: roomMap.keySet()){
                        System.out.println(roomKey + " " + roomMap.get(roomKey) + " users");
                    }

                }
            }catch(InterruptedException | IOException e){
                e.printStackTrace();
            }

        }else{
            System.out.println("command error");
        }
    }

    private void handleShout(String[] arr){
        try{
            if("".equals(this.localPeer.getRoomId())){
                System.out.println("need to join a room before shout");
            } else {
                if(arr.length==1){//if the input command only contains #shout with no following arguments
                    System.out.println("invalid command, #shout needs 1 argument");
                }else if(arr.length > 1){//correct command paradigm
                    String content = "";
                    for(int i = 1; i<arr.length;i++){
                        content += arr[i] + " ";
                    }
                    String SM = MessageServices.genRootShoutChatRequestMessage(this.localPeer.getHashId(),content.trim());
                    this.client.Write(SM);
                }else{//other unconsidered situation
                    System.out.println("command error");
                }
            }
        }catch(IOException e){
            System.out.println("#shout operation fails, try again later");
        }
    }



}
