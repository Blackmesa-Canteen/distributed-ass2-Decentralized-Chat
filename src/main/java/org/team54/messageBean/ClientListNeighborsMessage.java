package org.team54.messageBean;

import lombok.*;
import org.team54.utils.Constants;

/**
 * @author Xiaotian
 * @program distributed-ass2-Decentralized-Chat
 * @description
 * @create 2021-10-08 11:09
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class ClientListNeighborsMessage {
    @Builder.Default
    private String type = Constants.LIST_NEIGHBORS_JSON_TYPE;
}