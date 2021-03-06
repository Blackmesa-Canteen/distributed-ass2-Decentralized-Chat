package org.team54.messageBean;

import lombok.*;
import org.team54.utils.Constants;

/**
 * @author Xiaotian
 * @program distributed-ass2-Decentralized-Chat
 * @description message that send by client
 * @create 2021-10-08 11:05
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class ClientSendMessage {
    @Builder.Default
    private String type = Constants.MESSAGE_JSON_TYPE;
    private String content;
}