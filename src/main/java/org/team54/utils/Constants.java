package org.team54.utils;

/**
 * @author Xiaotian
 * @program distributed-ass2-Decentralized-Chat
 * @description some constants
 * @create 2021-10-06 10:35
 */
public class Constants {
    /** status consts */
    public static int NON_PORT_DESIGNATED = -1;

    /** config consts*/
    public static int CHANNEL_BUFFER_SIZE = 8192;

    /**message type*/
    public static String HOST_CHANGE_JSON_TYPE = "hostchange";
    public static String LIST_NEIGHBORS_JSON_TYPE = "listneighbors";
    public static String LIST_NEIGHBORS_RESPOND_JSON_TYPE = "listneighborsrespond";
    public static String SHOUT_JSON_TYPE = "shout";

    public static String JOIN_JSON_TYPE = "join";
    public static String ROOM_CHANGE_JSON_TYPE = "roomchange";
    public static String ROOM_CONTENTS_JSON_TYPE = "roomcontents";
    public static String WHO_JSON_TYPE = "who";
    public static String ROOM_LIST_JSON_TYPE = "roomlist";
    public static String LIST_JSON_TYPE = "list";
    public static String CREATE_ROOM_JSON_TYPE = "createroom";
    public static String DELETE_JSON_TYPE = "delete";
    public static String MESSAGE_JSON_TYPE = "message";
    public static String QUIT_JSON_TYPE = "quit";
}