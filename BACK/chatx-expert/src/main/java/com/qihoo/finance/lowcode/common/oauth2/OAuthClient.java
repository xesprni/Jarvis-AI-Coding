package com.qihoo.finance.lowcode.common.oauth2;


import com.intellij.ide.BrowserUtil;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.oauth2.dto.OAuth2RequestDTO;
import com.qihoo.finance.lowcode.common.oauth2.dto.OAuth2ResponseDTO;
import com.qihoo.finance.lowcode.common.oauth2.dto.OAuth2UserInfoDTO;
import com.qihoo.finance.lowcode.common.util.LowCodeAppUtils;
import com.qihoo.finance.lowcode.settings.ChatxApplicationSettings;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static com.qihoo.finance.lowcode.common.oauth2.OAuthCallbackServer.NONCE;
import static com.qihoo.finance.lowcode.common.oauth2.OAuthCallbackServer.STATE;

/**
 * @author weiyichao
 * @date 2025-09-22
 **/
public class OAuthClient {

    public static void startLogin(int port) {
        String redirectUri = String.format("http://127.0.0.1:%d/callback", port);
        String authUrl = Constants.Url.OAUTH2_AUTH
                + "?response_type=code"
                + "&client_id=" + ChatxApplicationSettings.settings().oauth2ClientId
                + "&redirect_uri=" + redirectUri
                + "&state=" + STATE
                + "&nonce=" + NONCE
                + "&scope=read";

        BrowserUtil.browse(authUrl); // 打开系统默认浏览器
    }
    public static String getAccessToken(OAuth2RequestDTO auth) {
        Map<String, String> param = new HashMap<>();
        param.put("grant_type", "authorization_code");
        param.put("client_id", auth.getClientId());
        param.put("client_secret", ChatxApplicationSettings.settings().oauth2ClientSecret);
        param.put("code", auth.getCode());
        param.put("redirect_uri", auth.getRedirectUri());

        String credentials = auth.getClientId() + ":" + ChatxApplicationSettings.settings().oauth2ClientSecret;
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes());

        Map<String, String> header = new HashMap<>();
        header.put("Authorization", "Basic " + encoded);
        OAuth2ResponseDTO oAuth2Response = LowCodeAppUtils.loginWithOAuth2(Constants.Url.OAUTH2_TOKEN_ENDPOINT,param,header);
        return oAuth2Response.getAccessToken();
    }

    public static OAuth2UserInfoDTO getUserInfo(String accessToken, String mac) {
        Map<String, String> header = new HashMap<>();
        header.put("Authorization", "Bearer " + accessToken);
        Map<String,Object> param = new HashMap<>();
        param.put("mac",mac);
        return LowCodeAppUtils.getUserInfoWithOAuth2(Constants.Url.OAUTH2_USERINFO,param,header);
    }
}
