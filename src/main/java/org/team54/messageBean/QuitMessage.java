package org.team54.messageBean;

import lombok.*;
import org.team54.utils.Constants;

/**
 * @author Xiaotian
 * @program assignment1
 * @description
 * @create 2021-08-18 00:40
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class QuitMessage {
    @Builder.Default
    private String type = Constants.QUIT_JSON_TYPE;
}