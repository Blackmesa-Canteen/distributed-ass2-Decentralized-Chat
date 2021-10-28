package org.team54.messageBean;

import lombok.*;
import org.team54.utils.Constants;

/**
 * @author Xiaotian
 * @program distributed-ass2-Decentralized-Chat
 * @description
 * @create 2021-10-08 11:15
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class ShoutMessage {
    @Builder.Default
    private String type = Constants.SHOUT_JSON_TYPE;

    private String content;

    @Builder.Default
    private String rootIdentity = "";

    private String shoutMessageHashId;

}