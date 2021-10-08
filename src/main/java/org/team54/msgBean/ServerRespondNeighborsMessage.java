package org.team54.msgBean;

import lombok.*;
import org.team54.utils.Constants;

import java.util.List;

/**
 * @author Xiaotian
 * @program distributed-ass2-Decentralized-Chat
 * @description
 * @create 2021-10-08 11:11
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class ServerRespondNeighborsMessage {
    private String type = Constants.LIST_NEIGHBORS_RESPOND_JSON_TYPE;
    private List<String> neighbors;
}