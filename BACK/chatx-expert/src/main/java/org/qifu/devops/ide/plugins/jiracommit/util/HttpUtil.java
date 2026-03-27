package org.qifu.devops.ide.plugins.jiracommit.util;

import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.util.EntityUtils;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Set;

public class HttpUtil {

    public String commonPostJsonWithoutAuthByHeaders(JSONObject content, String apiUrl, Map headerMap) {
        String postResult = "";
        HttpPost post = null;
        try {

            HttpClient httpClient = new DefaultHttpClient();
            httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 60000);
            httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 60000);
            post = new HttpPost(apiUrl);
            // 构造消息头
            post.setHeader("Content-type", "application/json; charset=utf-8");
            post.setHeader("Connection", "Close");

            if (headerMap!=null){
                Set<String> headerKeySet = headerMap.keySet();
                for (String headerKey:headerKeySet) {
                    post.setHeader(headerKey,String.valueOf(headerMap.get(headerKey)));
                }
            }

            // 构建消息实体
            StringEntity entity = new StringEntity(content.toString(), Charset.forName("UTF-8"));
            entity.setContentEncoding("UTF-8");
            // 发送Json格式的数据请求
            entity.setContentType("application/json");
            post.setEntity(entity);

            HttpResponse response = httpClient.execute(post);

            // 检验返回码
            // int statusCode = response.getStatusLine().getStatusCode();
            if (response != null  && response.getEntity()!= null) {
                postResult = EntityUtils.toString(response.getEntity());
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (post != null) {
                try {
                    post.releaseConnection();
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        return postResult;
    }


    public String commonPostMapWithoutAuthByHeaders(Map<String, ?> params, String apiUrl, Map headerMap) {
        String postResult = "";
        HttpPost post = null;
        try {

            HttpClient httpClient = new DefaultHttpClient();
            httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 60000);
            httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 60000);


            URIBuilder builder = new URIBuilder(apiUrl);
            if (params != null) {
                for (String key : params.keySet()) {
                    builder.addParameter(key, String.valueOf(params.get(key)));
                }
            }
            URI uri = builder.build();

            post = new HttpPost(uri);
            // 构造消息头
            post.setHeader("Content-type", "application/json; charset=utf-8");
            post.setHeader("Connection", "Close");

            if (headerMap!=null){
                Set<String> headerKeySet = headerMap.keySet();
                for (String headerKey:headerKeySet) {
                    post.setHeader(headerKey,String.valueOf(headerMap.get(headerKey)));
                }
            }

            HttpResponse response = httpClient.execute(post);

            // 检验返回码
            // int statusCode = response.getStatusLine().getStatusCode();
            if (response != null  && response.getEntity()!= null) {
                postResult = EntityUtils.toString(response.getEntity());
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (post != null) {
                try {
                    post.releaseConnection();
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        return postResult;
    }

}
