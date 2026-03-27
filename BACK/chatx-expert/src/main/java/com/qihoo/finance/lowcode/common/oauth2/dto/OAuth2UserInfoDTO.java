package com.qihoo.finance.lowcode.common.oauth2.dto;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author weiyichao
 * @date 2025-09-23
 **/
@Getter
@Setter
@ToString
public class OAuth2UserInfoDTO implements Serializable {
    private String email;
    private String nickName;
    private String name;
}
