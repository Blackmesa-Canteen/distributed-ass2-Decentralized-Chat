package org.team54.messageBean;

import lombok.*;
import org.team54.utils.Constants;

/**
 * @author Xiaotian
 * @program distributed-ass2-Decentralized-Chat
 * @description
 * @create 2021-10-08 10:44
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class HostChangeMessage {
    private String type = Constants.HOST_CHANGE_JSON_TYPE;
    private String host;
}