package com.qihoo.finance.lowcode.common.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.constants.ServiceErrorCode;
import com.qihoo.finance.lowcode.common.entity.base.Result;
import com.qihoo.finance.lowcode.common.exception.ServiceException;
import com.qihoo.finance.lowcode.gentracker.tool.JSON;
import com.qihoo.finance.lowcode.gentracker.tool.PluginUtils;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import com.qihoo.finance.lowcode.timetracker.configuration.TimeTrackerPersistentState;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author weiyichao
 * @date 2023-07-10
 */
@Slf4j
public class RestTemplateUtil {
    protected static final Gson GSON = new Gson();

    private static String mac;
    private static final int DEFAULT_CONNECT_TIMEOUT = 10000;
    private static final int DEFAULT_SOCKET_TIMEOUT = 30000;

    protected static RequestConfig requestConfig(int socketTimeout) {
        return RequestConfig.custom().setConnectTimeout(DEFAULT_CONNECT_TIMEOUT).setSocketTimeout(socketTimeout).build();
    }

    public static Map<String, String> baseHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("x-token-p", getLocalMac());
        headers.put(Constants.Headers.MAC, getLocalMac());
        headers.put(Constants.Headers.EMAIL, UserUtils.getUserEmail());
        headers.put(Constants.Headers.USERNAME, UserUtils.getUserNo());
        headers.put(Constants.Headers.TOKEN, UserUtils.getToken());
        headers.put(Constants.Headers.VERSION, PluginUtils.getPluginVersion());
        headers.put(Constants.Headers.IDE_VERSION, ApplicationInfo.getInstance().getFullVersion());

        return headers;
    }

    //------------------------------------------------------------------------------------------------------------------

    public static <T> T get(String url, Map<String, Object> data, Map<String, String> headers, TypeReference<T> returnType) {
        log.debug("RestTemplateUtil get {}", url);

        // 参数组装
        if (MapUtils.isNotEmpty(data)) {
            StringBuilder urlBuilder = new StringBuilder(url).append("?");
            for (String key : data.keySet()) {
                urlBuilder.append(key).append("=").append(data.get(key)).append("&");
            }
            urlBuilder.deleteCharAt(urlBuilder.length() - 1);
            url = urlBuilder.toString();
        }

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(url);
            httpGet.setConfig(requestConfig(DEFAULT_SOCKET_TIMEOUT));

            // setHeader
            baseHeaders().forEach(httpGet::setHeader);
            if (MapUtils.isNotEmpty(headers)) headers.forEach(httpGet::setHeader);

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                return JSON.parse(responseBody, returnType);
            } catch (SocketTimeoutException | org.apache.http.conn.ConnectTimeoutException e) {
                log.warn("接口请求超时: url: {}, param: {}, headers: {}, errMsg: {}", url, JSON.toJson(data), JSON.toJson(headers), e.getMessage());
                throw new ServiceException(ServiceErrorCode.SOCKET_TIMEOUT);
            } catch (Exception e) {
                log.error("接口请求异常: url: {}, param: {}, headers: {}, errMsg: {}", url, JSON.toJson(data), JSON.toJson(headers), e.getMessage());
                throw new RuntimeException(e);
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("接口请求异常: url: {}, param: {}, headers: {}, errMsg: {}", url, JSON.toJson(data), JSON.toJson(headers), e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static <T> T post(String url, Object data, Class<T> returnType) {
        String send = post(url, data);
        return JSON.parse(send, returnType);
    }

    public static <T> T
    post(String url, Object data, Map<String, String> headers, TypeReference<T> returnType) {
        String send = post(url, data, headers);
        return JSON.parse(send, returnType);
    }

    public static <T> T
    post(String url, Object data, Map<String, String> headers, TypeReference<T> returnType,
         int socketTimeout) {
        String send = post(url, data, headers, socketTimeout);
        return JSON.parse(send, returnType);
    }

    public static <T> T post(String url, Object data, Map<String, String> headers, Class<T> returnType) {
        String send = post(url, data, headers);
        return JSON.parse(send, returnType);
    }

    public static <T> T post(String url, Object data, TypeReference<T> tTypeReference) {
        String send = post(url, data);
        return JSON.parse(send, tTypeReference);
    }

    public static <T> T post(String url, Object data, TypeReference<T> tTypeReference, int socketTimeout) {
        String send = post(url, data, socketTimeout);
        return JSON.parse(send, tTypeReference);
    }

    public static <T> String post(String url, T data) {
        return post(url, data, new HashMap<>());
    }

    public static <T> String post(String url, T data, int socketTimeout) {
        return post(url, data, new HashMap<>(), socketTimeout);
    }

    public static <T> String post(String url, T data, Map<String, String> headers) {
        return post(url, data, headers, DEFAULT_SOCKET_TIMEOUT);
    }

    public static <T> String post(String url, T data, Map<String, String> headers, int socketTimeout) {
        log.debug("RestTemplateUtil post {}", url);

        try {
            CloseableHttpClient httpClient = HttpClients.custom()
                    .setRetryHandler(new DefaultHttpRequestRetryHandler(0, false))
                    .setDefaultRequestConfig(requestConfig(socketTimeout)).build();

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Gson gson = new GsonBuilder().registerTypeAdapter(Date.class, (JsonSerializer<Date>) (date, type, jsonSerializationContext) -> jsonSerializationContext.serialize(dateFormat.format(date)))
                    .create();

            HttpPost httpPost = new HttpPost(url);
            httpPost.setConfig(requestConfig(socketTimeout));

            StringEntity requestEntity = new StringEntity(gson.toJson(data), StandardCharsets.UTF_8);
            requestEntity.setContentType("application/json");
            httpPost.setEntity(requestEntity);

            // setHeader
            baseHeaders().forEach(httpPost::setHeader);
            if (MapUtils.isNotEmpty(headers)) headers.forEach(httpPost::setHeader);
            if (!httpPost.containsHeader("x-token-p")) httpPost.setHeader("x-token-p", getLocalMac());

            CloseableHttpResponse response = httpClient.execute(httpPost);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                // 处理非 200 状态码的响应
                log.warn("接口请求失败: {}，响应状态码：{}", url, response.getStatusLine().getStatusCode());
            }

            String responseBody = EntityUtils.toString(response.getEntity());
            response.close();
            return responseBody;
        } catch (SocketTimeoutException | org.apache.http.conn.ConnectTimeoutException e) {
            log.warn("接口请求超时: url: {}, param: {}, headers: {}, errMsg: {}", url, JSON.toJson(data), JSON.toJson(headers), e.getMessage());
            throw new ServiceException(ServiceErrorCode.SOCKET_TIMEOUT);
        } catch (Exception e) {
            log.warn("接口请求异常: url: {}, param: {}, headers: {}, errMsg: {}", url, JSON.toJson(data), JSON.toJson(headers), e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static <T> T uploadFile(String url, Map<String, String> headers, InputStream inputStream, String fileName, TypeReference<T> tTypeReference) {
        // 创建 HttpClient 实例
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // 创建 HttpPost 对象
            HttpPost httpPost = new HttpPost(url);
            // 使用 MultipartEntityBuilder 构建请求体
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addTextBody("name", URLEncoder.encode(fileName, StandardCharsets.UTF_8), ContentType.TEXT_PLAIN);
            builder.addBinaryBody("file", inputStream, ContentType.APPLICATION_OCTET_STREAM, URLEncoder.encode(fileName, StandardCharsets.UTF_8));

            // 构建 HttpEntity 对象
            HttpEntity multipart = builder.build();
            httpPost.setEntity(multipart);

            // setHeader
            baseHeaders().forEach(httpPost::setHeader);
            if (MapUtils.isNotEmpty(headers)) headers.forEach(httpPost::setHeader);
            if (!httpPost.containsHeader("x-token-p")) httpPost.setHeader("x-token-p", getLocalMac());

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                // 打印响应状态
                String responseBody = EntityUtils.toString(response.getEntity());
                log.info("call uploadFile status: {}, response: {}", response.getStatusLine(), responseBody);
                return JSON.parse(responseBody, tTypeReference);
            } catch (IOException e) {
                log.warn("接口请求异常: url: {}, headers: {}, errMsg: {}", url, JSON.toJson(headers), e.getMessage());
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            log.warn("接口请求异常: url: {}, headers: {}, errMsg: {}", url, JSON.toJson(headers), e.getMessage());
            throw new RuntimeException(e);
        }
    }
    //------------------------------------------------------------------------------------------------------------------

    public static TimeTrackerPersistentState getConfig() {
        try (CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(requestConfig(DEFAULT_SOCKET_TIMEOUT)).build()) {
            String url = String.format("%s/config", Constants.Url.TIME_TRACK);
            HttpGet httpGet = new HttpGet(url);
            baseHeaders().forEach(httpGet::setHeader);

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        String responseBody = EntityUtils.toString(entity);
                        TimeTrackerPersistentState timeTrackerPersistentState = null;
                        if (StringUtils.isNotBlank(responseBody)) {
                            Type resultType = new TypeToken<Result<TimeTrackerPersistentState>>() {
                            }.getType();
                            Result<TimeTrackerPersistentState> result = GSON.fromJson(responseBody, resultType);
                            if (result.isSuccess()) {
                                timeTrackerPersistentState = result.getData();
                            }
                        }
                        return timeTrackerPersistentState;
                    }
                } else {
                    // 处理非 200 状态码的响应
                    log.warn("接口请求失败: {}，响应状态码：{}", url, response.getStatusLine().getStatusCode());
                }
            } catch (IOException e) {
                // 处理HTTP请求异常
                log.error("获取配置异常: {}，msg：{}", url, e.getMessage());
            }
        } catch (Exception e) {
            // 处理其他异常
            log.error("获取配置异常：" + e.getMessage());
        }
        return null;
    }

    public static String getLocalMac() {
        String userMac = UserUtils.getMac();
        if (StringUtils.isNotEmpty(userMac)) {
            return userMac;
        }

        if (StringUtils.isEmpty(mac)) {
            mac = generateLocalMac();
        }

        return mac;
    }

    private static String generateLocalMac() {
        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            // 获取网卡，获取地址
            byte[] mac = NetworkInterface.getByInetAddress(inetAddress).getHardwareAddress();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mac.length; i++) {
                if (i != 0) {
                    sb.append("-");
                }
                // 字节转换为整数
                int temp = mac[i] & 0xff;
                String str = Integer.toHexString(temp);
                if (str.length() == 1) {
                    sb.append("0").append(str);
                } else {
                    sb.append(str);
                }
            }
            return sb.toString();
        } catch (Exception exception) {
            log.error("randomMac.... error");
            return randomMac();
        }

    }

    @Deprecated
    public static void asyncPostTimeTrack(String projectName, String userName, String date, long writeTimeSeconds, long readTimeSeconds) {
        Task.Backgroundable task = new Task.Backgroundable(ProjectUtils.getCurrProject(), "PostTimeTrack") {
            @Override
            public void run(ProgressIndicator progressIndicator) {
                // 用户读写时间上报
                progressIndicator.setText("TimeTrack Task in progress...");
                postTimeTrack(projectName, userName, date, writeTimeSeconds, readTimeSeconds);
            }
        };

        // 启动异步任务
        ProgressManager.getInstance().run(task);
    }

    public static void postTimeTrack(String projectName, String userName, String date, long writeTimeSeconds, long readTimeSeconds) {

        String url = String.format("%s/tracker", Constants.Url.TIME_TRACK);
        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setRetryHandler(new DefaultHttpRequestRetryHandler(0, false))
                .setDefaultRequestConfig(requestConfig(DEFAULT_SOCKET_TIMEOUT)).build()) {

            Gson gson = new GsonBuilder().create();
            HttpPost httpPost = new HttpPost(url);
            PostEntity postEntity = new PostEntity();
            postEntity.setProjectName(projectName);
            postEntity.setUserName(userName);
            postEntity.setReportDatetime(date);
            postEntity.setWriteTimeSeconds(writeTimeSeconds);
            postEntity.setReadTimeSeconds(readTimeSeconds);
            StringEntity requestEntity = new StringEntity(gson.toJson(postEntity));
            requestEntity.setContentType("application/json");
            httpPost.setEntity(requestEntity);
            // header
            baseHeaders().forEach(httpPost::setHeader);

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                    // 处理非 200 状态码的响应
                    log.warn("接口请求失败: {}，响应状态码：{}", url, response.getStatusLine().getStatusCode());
                }
            } catch (IOException e) {
                // 处理非 200 状态码的响应
                log.error("接口请求失败{}, msg：", url, e.getMessage());
            }

            CloseableHttpResponse response = httpClient.execute(httpPost);
            String responseBody = EntityUtils.toString(response.getEntity());
            log.debug("Response: " + responseBody);
            response.close();
        } catch (Exception e) {
            log.error("接口请求失败{}, msg：", url, e.getMessage());
        }
    }

    public static String postForm(String url, Map<String, String> formData, Map<String, String> headers, int socketTimeout) {
        log.debug("RestTemplateUtil postForm {}", url);

        try {
            CloseableHttpClient httpClient = HttpClients.custom()
                    .setRetryHandler(new DefaultHttpRequestRetryHandler(0, false))
                    .setDefaultRequestConfig(requestConfig(socketTimeout)).build();

            HttpPost httpPost = new HttpPost(url);
            httpPost.setConfig(requestConfig(socketTimeout));

            // 构建表单数据
            List<NameValuePair> params = new ArrayList<>();
            if (formData != null) {
                for (Map.Entry<String, String> entry : formData.entrySet()) {
                    params.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
                }
            }
            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(params, StandardCharsets.UTF_8);
            httpPost.setEntity(formEntity);

            // 设置 Content-Type
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");

            // 设置 headers
            baseHeaders().forEach(httpPost::setHeader);
            if (MapUtils.isNotEmpty(headers)) headers.forEach(httpPost::setHeader);
            if (!httpPost.containsHeader("x-token-p")) httpPost.setHeader("x-token-p", getLocalMac());

            CloseableHttpResponse response = httpClient.execute(httpPost);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                log.warn("接口请求失败: {}，响应状态码：{}", url, response.getStatusLine().getStatusCode());
            }

            String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            response.close();
            return responseBody;
        } catch (SocketTimeoutException | org.apache.http.conn.ConnectTimeoutException e) {
            log.warn("接口请求超时: url: {}, param: {}, headers: {}, errMsg: {}", url, JSON.toJson(formData), JSON.toJson(headers), e.getMessage());
            throw new ServiceException(ServiceErrorCode.SOCKET_TIMEOUT);
        } catch (Exception e) {
            log.warn("接口请求异常: url: {}, param: {}, headers: {}, errMsg: {}", url, JSON.toJson(formData), JSON.toJson(headers), e.getMessage());
            throw new RuntimeException(e);
        }
    }


    private static String randomMac() {
        // c0-00-00-1c-1a-00
        return String.format("%s-%s-%s-%s-%s-%s", getRandomValue(2), getRandomValue(2),
                getRandomValue(2), getRandomValue(2), getRandomValue(2), getRandomValue(2));
    }


    public static String getRandomValue(int numSize) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < numSize; i++) {
            char temp = 0;
            int key = (int) (Math.random() * 2);
            switch (key) {
                case 0:
                    //产生随机数字
                    temp = (char) (Math.random() * 10 + 48);
                    break;
                case 1:
                    //产生a-f
                    temp = (char) (Math.random() * 6 + 'a');
                    break;
                default:
                    break;
            }
            str.append(temp);
        }
        return str.toString();
    }

    @Getter
    @Setter
    static class PostEntity {
        private String projectName;
        private String userName;
        private String reportDatetime;
        private long totalTimeSeconds;

        private Long writeTimeSeconds;
        private Long readTimeSeconds;
    }
}
