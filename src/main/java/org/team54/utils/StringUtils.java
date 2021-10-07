package org.team54.utils;

import java.net.InetSocketAddress;

/**
 * @author Xiaotian
 * @program distributed-ass2-Decentralized-Chat
 * @description utils for string, like parser and verifier.
 * @create 2021-10-06 15:57
 */
public class StringUtils {

    /**
     * verify whether the room id is valid or not
     * @param roomId string
     * @return is valid name?
     */
    public static boolean isValidRoomId(String roomId) {
        if (roomId != null && roomId.length() >= 3 && roomId.length() <= 32) {
            String regex = "^[a-zA-Z]+[A-Za-z0-9]+$";
            return roomId.matches(regex);
        }

        return false;
    }

    /**
     * parse "192.168.1.9" out of "192.168.1.9:5000"
     * @param hostText str like "192.168.1.9:5000"
     * @return string hostname
     */
    public static String parseHostStringFromHostText(String hostText) {
        int i = 0;
        int size = hostText.length();
        String res = null;

        for (i = 0; i < size; i++) {
            if (hostText.charAt(i) == ':') {
                res = hostText.substring(0, i);
                break;
            }
        }

        return res;
    }

    /**
     * parse int 5000 out of "192.168.1.9:5000"
     * @param hostText str like "192.168.1.9:5000"
     * @return int port number
     */
    public static int parsePortNumFromHostText(String hostText) {
        int size = hostText.length();
        int res = Constants.NON_PORT_DESIGNATED;

        for (int i = size - 1; i > 0; i--){
            if (hostText.charAt(i) == ':') {
                String portString = hostText.substring(i + 1);
                if (portString != null && portString.length() > 0) {
                    long num = Long.parseLong(portString);
                    if (num < 1 || num > 65535) {
                        // port too large
                        System.out.println("port should in [1,65535]");
                    } else {
                        res = (int) num;
                    }
                }

                break;
            }
        }

        return res;
    }

    /**
     * simple method to gen "192.168.1.1:8080" like string from InetSocketAddress obj
     *
     * @param inetSocketAddress InetSocketAddress obj
     * @return hosttext
     */
    public static String getHostTextFromInetSocketAddress(InetSocketAddress inetSocketAddress) {
        return inetSocketAddress.getHostString() + ":" + inetSocketAddress.getPort();
    }

    /**
     * tester
     * @param args
     */
    public static void main(String[] args) {
        String test = "202.96.140.77:2345";
        String res = parseHostStringFromHostText(test);
        System.out.println(res);
        System.out.println(parsePortNumFromHostText(test));
    }
}