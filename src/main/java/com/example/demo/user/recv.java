package com.example.demo.user;

import com.alibaba.fastjson.JSONObject;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;

import static com.alibaba.fastjson.JSON.parseObject;

@Component
public class recv {
    @Autowired
    private JavaMailSender javaMailSender;
    @Autowired
    private UserMapper mapper;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "xb.login1"),
            exchange = @Exchange(name="xb.login",type = ExchangeTypes.DIRECT),
            key = {"success"}
    ))
    public void login1 (String msg){
        email(parseObject(msg,HashMap.class));
    };
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "xb.login2"),
            exchange = @Exchange(name="xb.login",type = ExchangeTypes.DIRECT),
            key = {"success"}
    ))
    public void login2 (String msg){
        HashMap map = new HashMap();
        map = parseObject(msg,HashMap.class);
        map.put("createtime",(int)(new Date().getTime()/1000));
        map.put("show1",1);
        map.put("systemheadimage","https://txtzz-1301452902.file.myqcloud.com/RqH32RQWblpBddrwujizvGJLy.jpeg");
        map.put("src","");
        map.put("images","");
        map.put("message",map.get("msg").toString());
        mapper.newsystemmessage(map);
    };

    public void email(HashMap map){
        // 构建一个邮件对象
        SimpleMailMessage message = new SimpleMailMessage();
        // 设置邮件主题
        message.setSubject(map.get("title").toString());
        // 设置邮件发送者，这个跟application.yml中设置的要一致
        message.setFrom("2483242070@qq.com");
        // 设置邮件接收者，可以有多个接收者，中间用逗号隔开，以下类似
        // message.setTo("10*****16@qq.com","12****32*qq.com");
        message.setTo(map.get("email").toString());
        // 设置邮件抄送人，可以有多个抄送人
        //message.setCc("12****32*qq.com");
        // 设置隐秘抄送人，可以有多个
        //message.setBcc("7******9@qq.com");
        // 设置邮件发送日期
        message.setSentDate(new Date());
        // 设置邮件的正文
        message.setText(map.get("msg").toString());
        // 发送邮件
        javaMailSender.send(message);
    }
}
