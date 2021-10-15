package org.team54.client;

import org.team54.messageBean.*;
import org.team54.service.MessageServices;
import org.team54.utils.Constants;


import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import com.google.gson.Gson;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;

public class ScannerWorker implements Runnable{
    public AtomicBoolean alive = new AtomicBoolean();
    public static AtomicBoolean waitingInput = new AtomicBoolean();
    private Scanner scanner;
    private ClientWorker clientWorker;
    private SocketChannel socketChannel;
    private NIOClient client;
    private Thread clientThread;
    private Thread workerThread;
    private ByteBuffer writeBuffer = ByteBuffer.allocate(1024);


    public ScannerWorker( NIOClient client, SocketChannel socketChannel, ClientWorker clientWorker) throws IOException {
        this.scanner = new Scanner(System.in);
        this.clientWorker = clientWorker;
        this.socketChannel = socketChannel;
        this.client = client;
    }
    @Override
    public void run(){
        alive.set(true);
        while(alive.get()){
            System.out.print("[input] in scannerworker ready to get input>>  ");
            waitingInput.set(true);
            String message = scanner.nextLine();
            if(isCommand(message)){// if the user input is command, switch cases based on the first word of input
                String [] arr = message.substring(1).split("\\s+");
                switch (arr[0]){
                    case Constants.Connect_TYPE:
                        handleConnect(arr);
                        break;
                    case Constants.JOIN_JSON_TYPE:
                        break;
                    case Constants.QUIT_JSON_TYPE:
                        handleQuit();;
                        break;

                }
            }else{
                handleMessage(message);
            }



        }

    }

    public void setSocketChannel(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    public void setClient(NIOClient client){
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
                // if the client has already connect to a server, return immediately
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
                this.client.setInetAddress(address);
                this.client.setPort(port);
                // if the command is connect, starts the client thread and the clientworker thread
                this.client.startInit();
                workerThread = new Thread(clientWorker);
                clientThread = new Thread(client);
                if(!workerThread.isAlive()){
                    workerThread.start();
                }
                if(!clientThread.isAlive()){
                    clientThread.start();
                }
                // send identity to server
                String message = MessageServices.genHostChangeRequestMessage(client.getIdentity()[0]);
                //String message = genHostChangeMessage(client.getIdentity()[0]);
                sendMessage(socketChannel,message);
                //socketChannel.write(ByteBuffer.wrap(message.getBytes()));
            } catch (IOException | ArrayIndexOutOfBoundsException e){
                System.out.println("bad #connect command");
            }

        }else if(arr.length == 3){
            try {
                // if the client has already connect to a server, return immediately
                if(client.connectNum == 1){
                    System.out.println("already connect to " + client.getServer());
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
                // pass variables to client
                this.client.setInetAddress(address);
                this.client.setPort(port);
                this.client.setLocalport(localport);
                // if the command is connect, starts the client and client worker thread
                //if(client.selector)
                this.client.startInit();
                workerThread = new Thread(clientWorker);
                clientThread = new Thread(client);
                if(!workerThread.isAlive()){
                    workerThread.start();
                }
                if(!clientThread.isAlive()){
                    clientThread.start();
                }
                // send identity to server
                String message = MessageServices.genHostChangeRequestMessage(client.getIdentity()[0]);
                //String message = MessageServices.genHostChangeMessage(client.getIdentity()[0]);
                sendMessage(socketChannel,message);
            } catch (IOException | ArrayIndexOutOfBoundsException e){
                System.out.println("bad #connect command");
            }
        }
    }

    public void handleQuit(){
        try{
            clientWorker.alive.set(false);
            client.alive.set(false);
            //socketChannel.socket().close();
            //socketChannel.close();
            //client.closeSelector();
            client.stop();
            workerThread.interrupt();
            clientThread.interrupt();
            //System.out.println("After quit workerthread is interrupted "+workerThread.isInterrupted());
            //System.out.println("Afete quit clientthread is interrupted " + clientThread.isInterrupted());
        }catch(IOException e){
            System.out.println(e.getMessage());
            System.out.println("close fail, try again later");
        }

    }

    // send user's common message to server
    private void handleMessage(String message){
        try{
            //ByteBuffer.wrap(data)
            if(socketChannel==null){
                // sockets not connected
                System.out.println("not connected yet. try #connect operation");
            }else{
                String relayMessage = MessageServices.genRelayMessage(client.getIdentity()[0],message);
                //String relayMessage = genRelayMessage(client.getIdentity()[0],message);
                sendMessage(socketChannel,relayMessage);
            }
        }catch(IOException e){

        }
    }

    private void sendMessage(SocketChannel socketChannel, String message) throws IOException {
        writeBuffer.clear();
        writeBuffer.put(message.getBytes(StandardCharsets.UTF_8));
        writeBuffer.flip();
        socketChannel.write(writeBuffer);
    }

    public static String genQuitMessage(String host){
        QuitMessage jsonObject = QuitMessage.builder()
                .build();
        return new Gson().toJson(jsonObject) + "\n";
    }


    // JSONObject requestDataObject = JSONObject.parseObject(text);

//    public static String genRelayMessage(String identity, String content) {
//        ServerRelayMessage jsonObject = ServerRelayMessage.builder()
//                .content(content)
//                .identity(identity)
//                .build();
//
//        return new Gson().toJson(jsonObject) + "\n";
//    }

//    public static String genHostChangeMessage(String host){
//        HostChangeMessage jsonObject = HostChangeMessage.builder()
//                .host(host)
//                .hashId(Constants.THIS_PEER_HASH_ID)
//                .build();
//        return new Gson().toJson(jsonObject) + "\n";
//    }



//    public static String genHashId() {
//        int LENGTH = 30;
//        StringBuilder val = new StringBuilder();
//        Random random = new Random();
//
//        for (int i = 0; i < LENGTH; i++) {
//
//            String charOrNum = random.nextInt(2) % 2 == 0 ? "char" : "num";
//            //
//            if ("char".equalsIgnoreCase(charOrNum)) {
//                // Output letters or numbers
//                int temp = random.nextInt(2) % 2 == 0 ? 65 : 97;
//                val.append((char) (random.nextInt(26) + temp));
//            } else {
//                val.append(String.valueOf(random.nextInt(10)));
//            }
//        }
//        return val.toString();
//    }
}
