package com.qihoo.finance.lowcode.common.oauth2;


import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.oauth2.dto.OAuth2RequestDTO;
import com.qihoo.finance.lowcode.settings.ChatxApplicationSettings;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;

/**
 * @author weiyichao
 * @date 2025-09-22
 **/
@Slf4j
public class OAuthCallbackServer {

    public static final String NONCE = randomBase36(7);
    public static final String STATE = randomBase36(7);

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public void start(int port, java.util.function.Consumer<OAuth2RequestDTO> onCodeReceived) {
        this.bossGroup = new NioEventLoopGroup(1);
        this.workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new HttpServerCodec());
                            ch.pipeline().addLast(new HttpObjectAggregator(65536));
                            ch.pipeline().addLast(new SimpleChannelInboundHandler<FullHttpRequest>() {
                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) {
                                    QueryStringDecoder decoder = new QueryStringDecoder(req.uri());
                                    if (decoder.path().equals("/callback")) {
                                        String code = decoder.parameters().get("code").get(0);
                                        String state = decoder.parameters().get("state").get(0);
                                        String nonce = decoder.parameters().get("nonce").get(0);
                                        String redirectUri = String.format("%s/%s", Constants.Url.OAUTH2_REDIRECT_URI, ChatxApplicationSettings.settings().oauth2ClientId);
                                        String clientId = decoder.parameters().get("client_id").get(0);
                                        String scope = decoder.parameters().get("scope").get(0);
                                        if (isBlank(code) || isBlank(state) || isBlank(nonce)) {
                                            sendResponse(ctx, "⚠️ 参数（400） 校验失败，拒绝请求！");
                                            return;
                                        }
                                        if (!state.equals(STATE)) {
                                            sendResponse(ctx, "⚠️ state 校验失败，拒绝请求！");
                                            return;
                                        }
                                        if (!nonce.equals(NONCE)) {
                                            sendResponse(ctx, "⚠️ nonce 校验失败，拒绝请求！");
                                            return;
                                        }
                                        onCodeReceived.accept(OAuth2RequestDTO.builder().code(code).nonce(nonce).state(state)
                                                .clientId(clientId).redirectUri(redirectUri).scope(scope).build());

                                        // 返回带自动唤醒功能的页面
                                        String content = buildSuccessPageWithAutoWake(code, state, nonce);
                                        FullHttpResponse response = new DefaultFullHttpResponse(
                                                HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                                                io.netty.buffer.Unpooled.copiedBuffer(content.getBytes(StandardCharsets.UTF_8))
                                        );
                                        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
                                        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                                    }
                                }
                            });
                        }
                    });

            b.bind(port).sync();
        }
        catch (Exception exception){
            log.error("OAuthCallbackServer start error", exception);
        }

    }

    public void stop() {
        if (bossGroup != null) bossGroup.shutdownGracefully();
        if (workerGroup != null) workerGroup.shutdownGracefully();
    }


    private void sendResponse(ChannelHandlerContext ctx, String message) {
        String content = "<html><head><meta charset=\"UTF-8\"></head><body>" + message + "</body></html>";
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                ctx.alloc().buffer().writeBytes(content.getBytes(StandardCharsets.UTF_8))
        );
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    public int getRandomPort() {
        return 10000 + (int) (Math.random() * 10000); // 10000–19999
    }

    public static String randomBase36(int length) {
        final SecureRandom random = new SecureRandom();
        return new BigInteger(130, random)
                .toString(36)
                .substring(0, length);
    }

    public static boolean isBlank(String content) {
        if (content == null) {
            return true;
        }
        if (content.isEmpty()) {
            return true;
        }
        return content.isBlank();
    }

    /**
     * 构建登录成功页面
     */
    private String buildSuccessPageWithAutoWake(String code, String state, String nonce) {
        return "<!DOCTYPE html>" +
                "<html lang=\"zh-CN\">" +
                "<head>" +
                "    <meta charset=\"UTF-8\">" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "    <title>登录成功</title>" +
                "    <style>" +
                "        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; " +
                "               background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); " +
                "               margin: 0; padding: 0; min-height: 100vh; " +
                "               display: flex; align-items: center; justify-content: center; }" +
                "        .container { background: white; padding: 40px; border-radius: 12px; " +
                "                    box-shadow: 0 10px 25px rgba(0,0,0,0.1); text-align: center; " +
                "                    max-width: 400px; margin: 20px; }" +
                "        .success-icon { font-size: 48px; color: #4CAF50; margin-bottom: 20px; }" +
                "        h1 { color: #333; margin: 0 0 10px 0; font-size: 24px; }" +
                "        p { color: #666; margin: 10px 0; line-height: 1.6; }" +
                "        .close-tip { margin-top: 20px; padding: 15px; background: #f8f9fa; " +
                "                    border-radius: 8px; color: #666; font-size: 14px; }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class=\"container\">" +
                "        <div class=\"success-icon\">✓</div>" +
                "        <h1>登录成功！</h1>" +
                "        <p>授权完成，IDEA 已自动获得焦点</p>" +
                "        <div class=\"close-tip\">" +
                "            请返回 IDEA 查看登录状态<br>" +
                "            当前页面可以关闭" +
                "        </div>" +
                "    </div>" +
                "</body>" +
                "</html>";
    }

}
