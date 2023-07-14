package com.example.demo.user;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;

@Component public class jiami {
    public String jiami(String url){

        url=url.substring(42,url.length());
        Integer time = (int)(System.currentTimeMillis()/1000);
        String md5 = DigestUtils.md5Hex("oGO63AraJpkJyq6fWrAG0ZxKaVP331"+url+time.toString());
        return "?sign="+md5+"&t="+time;
    };
    public String jiami1(String url){
        return url;
    };
}
