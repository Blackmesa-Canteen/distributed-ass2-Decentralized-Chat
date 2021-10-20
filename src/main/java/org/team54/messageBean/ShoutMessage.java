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

    // TODO 可能还会有其他的变量,你需要啥就添加啥:

    // TODO client peer产生的第一条shotMessage请求,该项置"", 见Message services的genRootShoutChatMessage方法
    // server peer会判断,当此字段为"",说明是来自根的shotmessage,就会给它赋值为来源root的公网identity.
    // 用serber peer 赋值主要是因为member peer不知道自己的公网ip,得不到公网identity
    @Builder.Default
    private String rootIdentity = "";

    // TODO member  client产生的第一条shotMessage请求里赋值随机hashId, 当server peer发现两次收到同一个hashId的shout,将不会给下游peer转发第二遍
    // 防止死循环
    private String shoutMessageHashId;

}