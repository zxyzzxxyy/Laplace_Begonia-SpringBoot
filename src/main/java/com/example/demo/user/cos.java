package com.example.demo.user;

import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.ObjectMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import com.alibaba.fastjson.JSONObject;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.BasicSessionCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.region.Region;
import com.tencent.cloud.CosStsClient;
import com.tencent.cloud.Response;
import java.net.URL;
import java.util.*;

public class cos {
    // 创建 COSClient 实例，这个实例用来后续调用请求
    public static HashMap sign(){
        TreeMap<String, Object> config = new TreeMap<String, Object>();
        HashMap temp = new HashMap();
        try {
            //这里的 SecretId 和 SecretKey 代表了用于申请临时密钥的永久身份（主账号、子账号等），子账号需要具有操作存储桶的权限。
            // 替换为您的云 api 密钥 SecretId
            config.put("secretId", "AKID6eMp4jCOPBsACyoqvA8pGRRBgoiNKCjf");
            // 替换为您的云 api 密钥 SecretKey
            config.put("secretKey", "qRlqVaiaGDWsxjwJSLnzwdwzlGVZPhQt");

            // 设置域名:
            // 如果您使用了腾讯云 cvm，可以设置内部域名
            //config.put("host", "sts.internal.tencentcloudapi.com");

            // 临时密钥有效时长，单位是秒，默认 1800 秒，目前主账号最长 2 小时（即 7200 秒），子账号最长 36 小时（即 129600）秒
            config.put("durationSeconds", 1800);

            // 换成您的 bucket
            config.put("bucket", "txtzz-1301452902");
            // 换成 bucket 所在地区
            config.put("region", "ap-beijing");

            // 这里改成允许的路径前缀，可以根据自己网站的用户登录态判断允许上传的具体路径
            // 列举几种典型的前缀授权场景：
            // 1、允许访问所有对象："*"
            // 2、允许访问指定的对象："a/a1.txt", "b/b1.txt"
            // 3、允许访问指定前缀的对象："a*", "a/*", "b/*"
            // 如果填写了“*”，将允许用户访问所有资源；除非业务需要，否则请按照最小权限原则授予用户相应的访问权限范围。
            config.put("allowPrefixes", new String[] {
                    "*"
            });

            // 密钥的权限列表。必须在这里指定本次临时密钥所需要的权限。
            // 简单上传、表单上传和分块上传需要以下的权限，其他权限列表请看 https://cloud.tencent.com/document/product/436/31923
            String[] allowActions = new String[] {
                    // 简单上传
                    "name/cos:PutObject",
                    // 表单上传、小程序上传
                    "name/cos:PostObject",
                    // 分块上传
                    "name/cos:InitiateMultipartUpload",
                    "name/cos:ListMultipartUploads",
                    "name/cos:ListParts",
                    "name/cos:UploadPart",
                    "name/cos:CompleteMultipartUpload",
                    "name/cos:GetObject",
                    "*"
            };
            config.put("allowActions", allowActions);

            Response response = CosStsClient.getCredential(config);
            System.out.println(response.credentials.tmpSecretId);
            System.out.println(response.credentials.tmpSecretKey);
            temp.put("tmpSecretId",response.credentials.tmpSecretId);
            temp.put("tmpSecretKey",response.credentials.tmpSecretKey);
            temp.put("sessionToken",response.credentials.sessionToken);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("no valid secret !");
        }finally {
            return temp;
        }
    }
    public static String sout1(String imagesrc){
        // 详细代码参见本页：简单操作 -> 创建 COSClient
        COSClient cosClient = com.example.demo.user.cos.createCOSClient();

        // 存储桶的命名格式为 BucketName-APPID，此处填写的存储桶名称必须为此格式
        String bucketName = "txtzz-1301452902";
        // 对象键(Key)是对象在存储桶中的唯一标识。详情请参见 [对象键](https://cloud.tencent.com/document/product/436/13324)
        String[] key1 = imagesrc.split("/");
        String key = key1[key1.length-1];
        // 设置签名过期时间(可选), 若未进行设置则默认使用 ClientConfig 中的签名过期时间(1小时)
        // 这里设置签名在半个小时后过期
        Date expirationDate = new Date(System.currentTimeMillis() + 30 * 60 * 1000);

        // 填写本次请求的参数，需与实际请求相同，能够防止用户篡改此签名的 HTTP 请求的参数
        //params.put("param1", "value1");

        // 填写本次请求的头部，需与实际请求相同，能够防止用户篡改此签名的 HTTP 请求的头部
        //headers.put("header1", "value1");

        // 请求的 HTTP 方法，上传请求用 PUT，下载请求用 GET，删除请求用 DELETE
        HttpMethodName method = HttpMethodName.GET;

        URL url = cosClient.generatePresignedUrl(bucketName, key, expirationDate, method);
        String url1 = url.toString()+"&imageAve";
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("User-Agent", "PostmanRuntime/7.28.4");
        HttpEntity<String> formEntity = new HttpEntity<>(null, headers);
        //ResponseEntity<Resource> responseEntity = restTemplate.exchange( url1, HttpMethod.GET, formEntity, Resource.class);

        return url1;

    }
    static COSClient createCOSClient() {
        // 设置用户身份信息。
        // SECRETID 和 SECRETKEY 请登录访问管理控制台 https://console.cloud.tencent.com/cam/capi 进行查看和管理
        String secretId = "AKID6eMp4jCOPBsACyoqvA8pGRRBgoiNKCjf";
        String secretKey = "qRlqVaiaGDWsxjwJSLnzwdwzlGVZPhQt";
        HashMap temp =sign();
        COSCredentials cred = new BasicSessionCredentials(temp.get("tmpSecretId").toString(), temp.get("tmpSecretKey").toString(),temp.get("sessionToken").toString());

        // ClientConfig 中包含了后续请求 COS 的客户端设置：
        ClientConfig clientConfig = new ClientConfig();

        // 设置 bucket 的地域
        // COS_REGION 请参照 https://cloud.tencent.com/document/product/436/6224
        clientConfig.setRegion(new Region("ap-beijing"));

        // 设置请求协议, http 或者 https
        // 5.6.53 及更低的版本，建议设置使用 https 协议
        // 5.6.54 及更高版本，默认使用了 https
        clientConfig.setHttpProtocol(HttpProtocol.https);

        // 以下的设置，是可选的：

        // 设置 socket 读取超时，默认 30s
        clientConfig.setSocketTimeout(30*1000);
        // 设置建立连接超时，默认 30s
        clientConfig.setConnectionTimeout(30*1000);

        // 如果需要的话，设置 http 代理，ip 以及 port
        clientConfig.setHttpProxyIp("httpProxyIp");
        clientConfig.setHttpProxyPort(80);

        // 生成 cos 客户端。
        return new COSClient(cred, clientConfig);
    }

}
