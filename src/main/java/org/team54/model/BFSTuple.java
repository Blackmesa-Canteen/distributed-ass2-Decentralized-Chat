package org.team54.model;

import lombok.*;
import org.team54.utils.Constants;

import java.util.HashMap;
import java.util.List;

/**
 * @author Xiaotian
 * @program distributed-ass2-Decentralized-Chat
 * @description
 * @create 2021-10-08 11:00
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class BFSTuple {
    private List<String> neighborList;
    private HashMap<String,Integer> roomMap;
}