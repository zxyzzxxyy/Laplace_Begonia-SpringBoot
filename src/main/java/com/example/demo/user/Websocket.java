package com.example.demo.user;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.ContextLoader;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.alibaba.fastjson.JSON.parseObject;

@EnableScheduling
@RestController
@CrossOrigin
@Component
@ServerEndpoint("/api/message/{userid}")
public class Websocket {
    private static UserMapper mapper;
    @Autowired
    public void setActionLogMapper(UserMapper mapper) {
        Websocket.mapper = mapper;
    }


    private static List list = new ArrayList();
    //静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。
    private static AtomicInteger onlineNum = new AtomicInteger();

    //concurrent包的线程安全Set，用来存放每个客户端对应的WebSocketServer对象。
    private static ConcurrentHashMap<String, Session> sessionPools = new ConcurrentHashMap<>();


    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "xb.removeuserchat"),
            exchange = @Exchange(name="xb.removeuserchat",type = ExchangeTypes.DIRECT)
    ))
    public void removeuserchat (String msg){
        HashMap map = new HashMap();
        map = parseObject(msg,HashMap.class);
        sessionPools.remove(map.get("userid").toString());
        sessionPools.remove(map.get("userid").toString()+"p");

    };
    //发送消息
    public void sendMessage(Session session, String message) throws IOException {

        if(session != null){
            synchronized (session) {

                session.getBasicRemote().sendText(message);
            }
        }
    }
    //给指定用户发送信息
    public void sendInfo(String userid, String message){
        Session session = sessionPools.get(userid);
        //  System.out.println(session);
        try {
            sendMessage(session, message);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    //建立连接成功调用
    @OnOpen
    public void onOpen(Session session, @PathParam(value = "userid") String userid){
        sessionPools.put(userid, session);
        addOnlineCount();
        //System.out.println(userName + "加入webSocket！当前人数为" + onlineNum);
    }

    //关闭连接时调用
    @OnClose
    public void onClose(@PathParam(value = "userid") String userid){
        sessionPools.remove(userid);
        subOnlineCount();
        //System.out.println(userName + "断开webSocket连接！当前人数为" + onlineNum);
    }

    //收到客户端信息
    @OnMessage
    public void onMessage(String message) throws IOException{

        List temp = new ArrayList();
        JSONObject obj=JSON.parseObject(message);
        Map<String, Object> params = (Map)JSONObject.parseObject(obj.toJSONString());

        if(params.get("type").equals("back")){
            Long current=System.currentTimeMillis();
            if((int)(current/1000)>Integer.valueOf(params.get("image").toString())+2*60)return;
            params.put("message",Integer.valueOf(params.get("message").toString()));
            HashMap map1 = mapper.getmessage1((Integer)params.get("message"));
            mapper.backmessage((Integer)params.get("message"));
            params.put("type","back");
            sendMessage(sessionPools.get(params.get("a").toString()), params.toString());
            sendMessage(sessionPools.get(params.get("a").toString()+"p"), params.toString());
            sendMessage(sessionPools.get(params.get("b").toString()), params.toString());
            sendMessage(sessionPools.get(params.get("b").toString()+"p"), params.toString());
            return;
        }
        if(params.get("type").equals("ping"))
            return;
        String str = "";
        if(params.get("type").equals("pc")){
            str=(String)params.get("a");

            str=str.substring(0,str.length()-1);

            params.put("a",Integer.valueOf(str));
        }
         {
             StringBuffer cookie = new StringBuffer();
             Random random = new Random();
             String str1="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
             for(int j=0;j<=18;j++) {
                 int sub = random.nextInt(61);
                 cookie.append(str1.substring(sub, sub + 1));
             }
             params.put("cookie",cookie.toString());
             newchat(params);
             params.put("id",(Integer)mapper.getmessage2(cookie.toString()).get("id"));

            sendMessage(sessionPools.get(params.get("a").toString()), params.toString());
            sendMessage(sessionPools.get(params.get("a").toString()+"p"), params.toString());
            sendMessage(sessionPools.get(params.get("b").toString()), params.toString());
            sendMessage(sessionPools.get(params.get("b").toString()+"p"), params.toString());
        }
    }


    //错误时调用
    @OnError
    public void onError(Session session, Throwable throwable){

        throwable.printStackTrace();
    }

    public void newchat(Map params){
        params.put("time",(int)(new Date().getTime()/1000));
        mapper.newchat(params);
    }

    public static void addOnlineCount(){
        onlineNum.incrementAndGet();
    }

    public static void subOnlineCount() {
        onlineNum.decrementAndGet();
    }

}