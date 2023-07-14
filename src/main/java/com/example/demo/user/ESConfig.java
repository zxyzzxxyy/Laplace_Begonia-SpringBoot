package com.example.demo.user;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 定义高版本ES实例对象
 * @author wanghl
 * @date 2020/7/17 9:23
 **/
@Configuration
public class ESConfig {

    @Bean
    public RestHighLevelClient restHighLevelClient() {
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                       new HttpHost("127.0.0.1", 9200, "http")
                        //new HttpHost("49.232.23.79", 9200, "http")
                )
        );
        return client;
    }
}
