package com.example.demo.user;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class send {
    @Autowired
    private RabbitTemplate rabbit;

    public void loginmessage(String msg){
        String queueName="xb.login";
        rabbit.convertAndSend(queueName,"success",msg);
    }

    public void removeuserchat(String msg){
        String queueName="xb.removeuserchat";
        rabbit.convertAndSend(queueName,msg);
    }
}
