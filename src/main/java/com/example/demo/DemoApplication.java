package com.example.demo;

import com.example.demo.user.Config;
import com.example.demo.user.Websocket;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SpringBootApplication
@MapperScan("com.example.demo")
@Controller

public class DemoApplication {

    public static void main(String[] args) {
     //   System.setProperty("es.set.netty.runtime.available.processors","false");
        SpringApplication.run(DemoApplication.class, args);
    }

}
