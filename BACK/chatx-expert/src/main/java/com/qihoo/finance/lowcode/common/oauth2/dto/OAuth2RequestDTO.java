package com.qihoo.finance.lowcode.common.oauth2.dto;


import lombok.Builder;
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
@Builder
public class OAuth2RequestDTO implements Serializable {
    private String code;
    private String state;
    private String nonce;
    private String redirectUri;
    private String clientId;
    private String scope;
}
