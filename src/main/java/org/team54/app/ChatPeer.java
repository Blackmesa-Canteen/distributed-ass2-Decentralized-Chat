package org.team54.app;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.team54.server.ChatServer;
import org.team54.server.MessageQueueWorker;
import org.team54.utils.Constants;

import java.io.IOException;

/**
 * @author Xiaotian
 * @program distributed-ass2-Decentralized-Chat
 * @description
 * @create 2021-10-05 20:32
 */
public class ChatPeer {

    /** server listen port: -p */
    private static int serverListenPort = 4444;

    /** client connect port: -i, if not designated in argument, use OS random port !! */
    private static int clientPort = Constants.NON_PORT_DESIGNATED;

    private static ChatServer server;

    public static void main(String[] args) throws IOException {

        // handle args
        handleArgs(args);

        // init server listening logic
        MessageQueueWorker MQWorker = new MessageQueueWorker();
        new Thread(MQWorker).start();
        server = new ChatServer(null, serverListenPort, MQWorker);
        new Thread(server).start();

        // init client logic

    }

    private static void handleArgs(String[] args) {
        MyCmdOption option = new MyCmdOption();
        CmdLineParser cmdLineParser = new CmdLineParser(option);

        try {
            if (args.length == 0) {
                // no args, default server port is 4444
                serverListenPort = 4444;

                // no arges, default client connection port will be this
                // need to allocate randomly when create connection
                clientPort = Constants.NON_PORT_DESIGNATED;

            } else {
                cmdLineParser.parseArgument(args);

                if (option.listenPort != Constants.NON_PORT_DESIGNATED
                        && (option.listenPort < 1
                        || option.listenPort > 65535)) {
                    // port too large
                    System.out.println("port should in [1,65535]");
                    System.exit(-1);
                }

                if (option.clientPort != Constants.NON_PORT_DESIGNATED
                        && (option.clientPort < 1
                        || option.clientPort > 65535)) {
                    // port too large
                    System.out.println("port should in [1,65535]");
                    System.exit(-1);
                }

                serverListenPort = (int) option.listenPort;
                clientPort = (int) option.clientPort;
            }

        } catch (CmdLineException e) {
            System.out.println("Command line error: " + e.getMessage());
            argHelpInfo(cmdLineParser);
            System.exit(-1);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private static void argHelpInfo(CmdLineParser cmdLineParser) {
        System.out.println("-p [listen port number], -i [client port for connection]");
        cmdLineParser.printUsage(System.out);
    }

    public static int getServerListenPort() {
        return serverListenPort;
    }

    public static int getClientPort() {
        return clientPort;
    }

    public static void setClientPort(int clientPort) {
        ChatPeer.clientPort = clientPort;
    }
}