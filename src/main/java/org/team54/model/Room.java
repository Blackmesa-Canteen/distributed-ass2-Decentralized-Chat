package org.team54.model;

import lombok.*;

import java.util.ArrayList;

/**
 * @author Xiaotian
 * @program distributed-ass2-Decentralized-Chat
 * @description
 * @create 2021-10-06 12:23
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Room {

    private String roomId;
    private ArrayList<Peer> peers;

    /* owner will always be creator */
//    private Peer owner;
}