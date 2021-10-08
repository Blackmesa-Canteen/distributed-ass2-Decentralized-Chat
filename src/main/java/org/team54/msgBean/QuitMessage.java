package org.team54.msgBean;

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
    private String type = Constants.QUIT_JSON_TYPE;
}