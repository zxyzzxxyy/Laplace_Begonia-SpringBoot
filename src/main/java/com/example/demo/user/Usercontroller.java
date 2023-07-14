package com.example.demo.user;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

//导入可选配置类

// 导入对应SMS模块的client

// 导入要请求接口对应的request response类


import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.*;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.http.HttpMethodName;
import net.dreamlu.mica.ip2region.core.Ip2regionSearcher;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;




@EnableScheduling
@RestController
@CrossOrigin
@Component
public class Usercontroller {
    private String pixiv_time;
    @Autowired
    private UserMapper mapper;
    @Autowired
    JavaMailSender javaMailSender;
    @Autowired
    jiami j;
    @Autowired
    private Ip2regionSearcher ip2regionSearcher;
    @Autowired
    private send send;
    @Autowired
    private RedisTemplate redisTemplate;



    @Configuration
    public class RestTemplateConfig {
        @Bean
        public RestTemplate restTemplate(@Qualifier("simpleClientHttpRequestFactory") SimpleClientHttpRequestFactory factory){
            return new RestTemplate(factory);
        }

        @Bean
        public SimpleClientHttpRequestFactory simpleClientHttpRequestFactory(){
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(6000000); //毫秒
            factory.setReadTimeout(6000000); //毫秒
            return factory;
        }
    }

    @PostMapping("/api/newtheme")
    @ResponseBody
    @CrossOrigin
    public HashMap newtheme(@RequestBody HashMap map) throws IOException {
        HashMap map1 = new HashMap();

        if(mapper.jugeuserban(map)!=0){
            map1.put("code",1002);
            return  map1;
        }
        StringBuffer themecookie = new StringBuffer();
        Random random = new Random();
        String str1="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        for(int j=0;j<=18;j++) {
            int sub = random.nextInt(61);
            themecookie.append(str1.substring(sub, sub + 1));
        }
        mapper.newtheme((Integer)map.get("userid"),(String)map.get("title"),(String)map.get("data"),
                (String)map.get("text"),(Integer)map.get("createtime"),(Integer)map.get("createtime"),(String)map.get("image"),(String)map.get("tags"),
                (Integer)map.get("type"),
                (String)map.get("ycsrc"),
                (String)map.get("fenlei"),
                themecookie.toString(),
                (String)map.get("lal"),
                (String)map.get("position")
        );
        mapper.themenumadd((Integer)map.get("userid"));
        String str=(String)map.get("tags");
        if(null!=str&&str.length()>0) {
            String[] arr=(str.split(";"));
            for(int i=0;i<arr.length;i++)
            mapper.addtag(themecookie.toString(),arr[i]);
        }
        HashMap map2 = mapper.getthemeby_themecookie(themecookie.toString());
        IndexRequest request = new IndexRequest("theme").id(map2.get("themeid").toString());
        request.source(JSONObject.toJSONString(map2), XContentType.JSON);
        IndexResponse response = client1.index(request, RequestOptions.DEFAULT);
        map1.put("code",1001);
        map1.put("data","success");
        return map1;
    }
    @PostMapping("/api/changetheme")
    @ResponseBody
    @CrossOrigin
    public HashMap changetheme(@RequestBody HashMap map) throws IOException {
        HashMap map1 = new HashMap();
        map.put("changetime",(int)(System.currentTimeMillis()/1000));
        if(mapper.jugeuserban(map)!=0){
            map1.put("code",1002);
            return  map1;
        }
        mapper.deletetags((String)map.get("themecookie").toString());
        mapper.changetheme(map);
        String str=(String)map.get("tags");
        if(null!=str&&str.length()>0) {
            String[] arr=(str.split(";"));
            for(int i=0;i<arr.length;i++)
                mapper.addtag((String)map.get("themecookie").toString(),arr[i]);
        }
        HashMap map2 = mapper.getthemeby_themecookie(map.get("themecookie").toString());
        UpdateRequest request = new UpdateRequest("theme", String.valueOf(map2.get("themeid")));
        request.doc(JSONObject.toJSONString(map2), XContentType.JSON);
        UpdateResponse response = client1.update(request, RequestOptions.DEFAULT);
        map1.put("code",1001);
        map1.put("data","success");
        return map1;
    }
    @PostMapping("/api/gettheme")
    @ResponseBody
    public HashMap gettheme(@RequestBody HashMap map) throws IOException {
        SearchRequest request = new SearchRequest("theme");

        //2、设置搜索条件，使用该构建器进行查询
        SearchSourceBuilder builder = new SearchSourceBuilder();//生成构建器

        //查询条件我们可以用工具类QueryBuilders来构建
        //QueryBuilders.termQuery()：精确匹配
        //QueryBuilders.matchAllQuery()：全文匹配

        //构建精确匹配查询条件
        //构建精确匹配查询条件
        TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery("themeid", map.get("themeid").toString());
        //MatchQueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("title","Pixiv");
//  WildcardQueryBuilder wildcardQueryBuilder = QueryBuilders.wildcardQuery("username", "张");
        builder.query(termQueryBuilder);
        builder.size(50);
        //3、将搜索条件放入搜索请求中
        request.source(builder);
        //4、客户端执行搜索请求
        SearchResponse response = client1.search(request, RequestOptions.DEFAULT);
        //5、打印测试
        SearchHit[] hits = response.getHits().getHits();
        JSONObject map1=JSONObject.parseObject(hits[0].getSourceAsString());
        if((Integer)map1.get("show1")==0){
            HashMap map2 = new HashMap();
            map2.put("code",1002);
            return map2;
        }
        HashMap  userinfo= new HashMap();
        userinfo = mapper.getheadborder((Integer)map1.get("userid"));
        String url1="";
        HashMap pianyi = new HashMap();
        if(userinfo!=null) {
            url1=userinfo.get("url").toString();
            pianyi=mapper.getpianyi(url1);
        }
        map1.put("headborder",url1);
        map1.put("pianyi",pianyi);
        map1.put("userheadimage",j.jiami1(mapper.getuserheadimage((Integer)map1.get("userid"))));
        map1.put("username",mapper.getusername((Integer)map1.get("userid")));
        map1.put("sign",mapper.getsign((Integer)map1.get("userid")));
        map1.put("admin",mapper.getadminbyuserid((Integer)map1.get("userid")));
        String text=(String)map1.get("text");
        List imagelistt = getImgSrc(text);
        for(int i=0;i<imagelistt.size();i++){
            int x=text.indexOf((String)imagelistt.get(i));
            int y=((String)imagelistt.get(i)).length();

            text=text.substring(0,x)+j.jiami1((String)imagelistt.get(i))+text.substring(x+y,text.length());
        }
        map1.put("text",text);
        List list = getmyshenfen3((Integer)map1.get("userid"));
        String str="";
        for(int i=0;i<list.size();i++){
            HashMap temp=(HashMap)list.get(i);
            str+=temp.get("name");
            if(i<list.size()-1)
                str+="、";
        }
        map1.put("shenfen",str);
        UpdateRequest request1 = new UpdateRequest();
        {
            request1.index("theme").id(map.get("themeid").toString());
            // 拓展：局部更新也可以这样写：
            request1.doc(XContentType.JSON, "look",(Integer)map1.get("look")+1);
            // 3、发送请求到ES
            UpdateResponse response1 = client1.update(request1, RequestOptions.DEFAULT);
        }
        map1.put("activity",mapper.jugeactivity((Integer)map.get("themeid")));
        HashMap map2= new HashMap();
        map2.put("code",1001);
        map2.put("data",map1);
        return map2;
    }
    @PostMapping("/api/tuijianlist")
    @ResponseBody
    @CrossOrigin
    public HashMap tuijianlist(@RequestBody HashMap map) throws IOException{
        List list = new ArrayList();
        {
            //list = mapper.getthemelist();
            map=mapper.gettheme((Integer)map.get("themeid"));
            SearchSourceBuilder builder = new SearchSourceBuilder();
            //条件搜索
            BoolQueryBuilder boolQueryBuilder=new BoolQueryBuilder();
            boolQueryBuilder.must(QueryBuilders.termQuery("show1", 1));
            boolQueryBuilder.must(QueryBuilders.termQuery("fenlei", map.get("fenlei")));


            //must_not 查询条件
            //should 查询条件
            String[] tags= (map.get("tags").toString()+";").split(";");
            for(int i=0;i<tags.length;i++) {
                BoolQueryBuilder boolQueryBuilder1=new BoolQueryBuilder();
                boolQueryBuilder1.must(QueryBuilders.matchQuery("tags",tags[i]));
                boolQueryBuilder.should(boolQueryBuilder1);
            };
            builder.query(boolQueryBuilder);
            //结果集合分页
            builder.from(0).size(20);
            //排序
            builder.sort("hot",SortOrder.DESC);
            //搜索
            SearchRequest searchRequest = new SearchRequest();
            searchRequest.indices("theme");
            searchRequest.source(builder);
            // 执行请求
            SearchResponse response = client1.search(searchRequest, RequestOptions.DEFAULT);

            SearchHits hits = response.getHits();
            SearchHit[] searchHits = hits.getHits();

            for(SearchHit hit : searchHits)
            {
                Map<String,Object> sourceAsMap = hit.getSourceAsMap();
                if(sourceAsMap.get("title")!=null&&sourceAsMap.get("title").toString().length()>0&&!sourceAsMap.get("themeid").equals(map.get("themeid")))
                list.add(sourceAsMap);
                if(list.size()==3)break;
            }
            Integer size = 3;
            size =size-list.size();
            SearchSourceBuilder builder1 = new SearchSourceBuilder();
            BoolQueryBuilder boolQueryBuilder1=new BoolQueryBuilder();
            boolQueryBuilder1.must(QueryBuilders.termQuery("show1", 1));
            boolQueryBuilder1.must(QueryBuilders.termQuery("fenlei", "默认分类"));
            builder1.query(boolQueryBuilder1);
            //结果集合分页
            builder1.from(0).size(20);
            //排序
            builder1.sort("hot",SortOrder.DESC);
            //搜索
            SearchRequest searchRequest1 = new SearchRequest();
            searchRequest1.indices("theme");
            searchRequest1.source(builder1);
            // 执行请求
            if(size>0) {
                SearchResponse response1 = client1.search(searchRequest1, RequestOptions.DEFAULT);

                SearchHits hits1 = response1.getHits();
                SearchHit[] searchHits1 = hits1.getHits();
                for (SearchHit hit : searchHits1) {
                    Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                    if(sourceAsMap.get("title")!=null&&sourceAsMap.get("title").toString().length()>0)
                    list.add(sourceAsMap);
                    if(list.size()==3)break;
                }
            }
        }
        for(int i=0;i<list.size();i++){
            HashMap temp=(HashMap) list.get(i);
            {
                temp.put("userheadimage", j.jiami1(mapper.getuserheadimage((Integer) temp.get("userid"))));

            }
            HashMap  userinfo= new HashMap();
            userinfo = mapper.getheadborder((Integer)temp.get("userid"));
            String url1="";
            HashMap pianyi = new HashMap();
            if(userinfo!=null) {
                url1=userinfo.get("url").toString();
                pianyi=mapper.getpianyi(url1);
            }
            temp.put("headborder",url1);
            temp.put("pianyi",pianyi);
            temp.put("username",mapper.getusername((Integer)temp.get("userid")));
            List list1 = getmyshenfen3((Integer)temp.get("userid"));
            String text=(String)temp.get("text");
            String image="";
            List imagelistt = getImgSrc(text);
            for(int a=0;a<imagelistt.size();a++){
                int x=text.indexOf((String)imagelistt.get(a));
                int y=((String)imagelistt.get(a)).length();
                text=text.substring(0,x+y)+j.jiami1((String)imagelistt.get(a))+text.substring(x+y+1,text.length());
                image=image+j.jiami1((String)imagelistt.get(a))+";";
            }
            temp.remove("text");

            temp.put("image",image);
            String str="";
            for(int n=0;n<list1.size();n++){
                HashMap temp1=(HashMap)list1.get(n);
                str+=temp1.get("name");
                if(n<list1.size()-1)
                    str+="、";
            }
            temp.put("shenfen",str);
        }
        HashMap map2= new HashMap();
        map2.put("code",1001);
        map2.put("data",list);
        return map2;
    }
    @PostMapping("/api/getthemelist")
    @ResponseBody
    @CrossOrigin
    public HashMap getthemelist(@RequestBody HashMap map) throws IOException {
        List list = new ArrayList();
        Long time1 = System.currentTimeMillis();

        {
            //list = mapper.getthemelist();
            SearchSourceBuilder builder = new SearchSourceBuilder();
            //条件搜索
            BoolQueryBuilder boolQueryBuilder=new BoolQueryBuilder();
            boolQueryBuilder.must(QueryBuilders.termQuery("show1", 1));
            if((Integer)map.get("type")==1){
                boolQueryBuilder.must(QueryBuilders.termQuery("jing", 1));
            }
            builder.query(boolQueryBuilder);
            //结果集合分页
            builder.from(0).size(10);
            //排序
            builder.sort("replytime",SortOrder.DESC);
            //搜索
            SearchRequest searchRequest = new SearchRequest();
            searchRequest.indices("theme");
            searchRequest.source(builder);
            // 执行请求
            SearchResponse response = client1.search(searchRequest, RequestOptions.DEFAULT);

            SearchHits hits = response.getHits();
            SearchHit[] searchHits = hits.getHits();
            for(SearchHit hit : searchHits)
            {
                Map<String,Object> sourceAsMap = hit.getSourceAsMap();
                list.add(sourceAsMap);
            }
        }
        for(int i=0;i<list.size();i++){
            HashMap temp=(HashMap) list.get(i);
            HashMap userinfo = mapper.getuserinfo((Integer)temp.get("userid"));
            if(null==userinfo)continue;
             {
                 if(null!=userinfo&&null!=userinfo.get("userheadimage"))
                 temp.put("userheadimage", j.jiami1(userinfo.get("userheadimage").toString()));
            }

            temp.put("username",userinfo.get("username"));
            HashMap str1= new HashMap();
            str1 = mapper.getheadborder((Integer)temp.get("userid"));
            String url1="";
            HashMap pianyi = new HashMap();
            if(str1!=null) {
                url1=str1.get("url").toString();
                pianyi=mapper.getpianyi(url1);
            }
            temp.put("headborder",url1);
            temp.put("pianyi",pianyi);
            List list1 = getmyshenfen3((Integer)temp.get("userid"));
            String text=(String)temp.get("text");
            String image="";
            List imagelistt = getImgSrc(text);
            for(int a=0;a<imagelistt.size();a++){
                int x=text.indexOf((String)imagelistt.get(a));
                int y=((String)imagelistt.get(a)).length();
                text=text.substring(0,x+y)+j.jiami1((String)imagelistt.get(a))+text.substring(x+y+1,text.length());
                image=image+j.jiami1((String)imagelistt.get(a))+";";
            }
            temp.put("text",text);
            temp.put("image",image);
            String str="";
            for(int n=0;n<list1.size();n++){
                HashMap temp1=(HashMap)list1.get(n);
                str+=temp1.get("name");
                if(n<list1.size()-1)
                    str+="、";
            }
            temp.put("shenfen",str);
        }
        HashMap map2= new HashMap();
        map2.put("code",1001);
        map2.put("data",list);
        map.put("fenlei","全部分类");
        map2.put("activity",getactivity1(map));
        Long time2 = System.currentTimeMillis();

        return map2;
    }
    @PostMapping("/api/getthemelistby_fenlei")
    @ResponseBody
    @CrossOrigin
    public HashMap getthemelistby_fenlei(@RequestBody HashMap map) throws IOException {
        List list = new ArrayList();
        {
            //list = mapper.getthemelist();
            SearchSourceBuilder builder = new SearchSourceBuilder();
            //条件搜索
            BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
            boolQueryBuilder.must(QueryBuilders.termQuery("show1", 1));
            if((Integer) map.get("type")==1)
            boolQueryBuilder.must(QueryBuilders.termQuery("jing",1));
            else if((Integer)map.get("type")==2){
                List userid = mapper.getattentionidlist(map);
                boolQueryBuilder.must(QueryBuilders.termsQuery("userid", userid));
            }
            if(!map.get("fenlei").equals("全部分类")) {
                boolQueryBuilder.must(QueryBuilders.termQuery("fenlei", map.get("fenlei").toString()));

            }
            builder.query(boolQueryBuilder);
            //结果集合分页
            builder.from((Integer) map.get("limit")*10).size(10);
            //排序
            builder.sort("replytime", SortOrder.DESC);
            //搜索
            SearchRequest searchRequest = new SearchRequest();
            searchRequest.indices("theme");
            searchRequest.source(builder);
            // 执行请求
            SearchResponse response = client1.search(searchRequest, RequestOptions.DEFAULT);
            SearchHits hits = response.getHits();
            SearchHit[] searchHits = hits.getHits();

            for (SearchHit hit : searchHits) {

                Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                sourceAsMap.put("userheadimage", j.jiami1(mapper.getuserheadimage((Integer) sourceAsMap.get("userid"))));
                sourceAsMap.put("username", mapper.getusername((Integer) sourceAsMap.get("userid")));
                String text = (String) sourceAsMap.get("text");
                String image = "";
                HashMap  userinfo= new HashMap();
                userinfo = mapper.getheadborder((Integer)sourceAsMap.get("userid"));
                String url1="";
                HashMap pianyi = new HashMap();
                if(userinfo!=null) {
                    url1=userinfo.get("url").toString();
                    pianyi=mapper.getpianyi(url1);
                }
                sourceAsMap.put("headborder",url1);
                sourceAsMap.put("pianyi",pianyi);
                List imagelistt = getImgSrc(text);
                for (int a = 0; a < imagelistt.size(); a++) {
                    int x = text.indexOf((String) imagelistt.get(a));
                    int y = ((String) imagelistt.get(a)).length();
                    text = text.substring(0, x + y) + j.jiami1((String) imagelistt.get(a)) + text.substring(x + y + 1, text.length());
                    if(a<3)
                    image = image + j.jiami1((String) imagelistt.get(a)) + ";";

                }
                if(map.get("app")==null){
                    sourceAsMap.put("text", text);
                    sourceAsMap.put("data","");
                }
                else{
                    sourceAsMap.put("text","");
                    if(sourceAsMap.get("data").toString().length()>200){
                        sourceAsMap.put("data",sourceAsMap.get("data").toString().substring(0,200));
                    }
                }
                sourceAsMap.put("image", image);
                List list1 = getmyshenfen3((Integer) sourceAsMap.get("userid"));
                String str = "";
                for (int n = 0; n < list1.size(); n++) {
                    HashMap temp1 = (HashMap) list1.get(n);
                    str += temp1.get("name");
                    if (n < list1.size() - 1)
                        str += "、";
                }
                sourceAsMap.put("shenfen", str);
                list.add(sourceAsMap);
            }
        }
        HashMap map2= new HashMap();
        map2.put("code",1001);
        map2.put("data",list);
        if((Integer) map.get("limit")==0&&!map.get("fenlei").equals("全部分类")){
            map2.put("activity",getactivity(map));
        }
        else if((Integer) map.get("limit")==0&&map.get("fenlei").equals("全部分类")){
            map2.put("activity",getactivity1(map));
        }
        return map2;
    }
    @PostMapping("/api/getthemelist_top")
    @ResponseBody
    @CrossOrigin
    public HashMap getthemelist_top(@RequestBody HashMap map) throws IOException {
        List list = new ArrayList();
        {
            //list = mapper.getthemelist();
            SearchSourceBuilder builder = new SearchSourceBuilder();
            //条件搜索
            BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
            boolQueryBuilder.must(QueryBuilders.termQuery("show1", 1));
            boolQueryBuilder.must(QueryBuilders.termQuery("top",1));
            builder.query(boolQueryBuilder);
            //结果集合分页
            builder.from(0).size(100);
            //排序
            builder.sort("replytime", SortOrder.DESC);
            //搜索
            SearchRequest searchRequest = new SearchRequest();
            searchRequest.indices("theme");
            searchRequest.source(builder);
            // 执行请求
            SearchResponse response = client1.search(searchRequest, RequestOptions.DEFAULT);
            SearchHits hits = response.getHits();
            SearchHit[] searchHits = hits.getHits();
            for (SearchHit hit : searchHits) {
                Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                sourceAsMap.put("userheadimage", j.jiami1(mapper.getuserheadimage((Integer) sourceAsMap.get("userid"))));
                sourceAsMap.put("username", mapper.getusername((Integer) sourceAsMap.get("userid")));
                String text = (String) sourceAsMap.get("text");
                String image = "";
                HashMap  userinfo= new HashMap();
                userinfo = mapper.getheadborder((Integer)sourceAsMap.get("userid"));
                String url1="";
                HashMap pianyi = new HashMap();
                if(userinfo!=null) {
                    url1=userinfo.get("url").toString();
                    pianyi=mapper.getpianyi(url1);
                }
                sourceAsMap.put("headborder",url1);
                sourceAsMap.put("pianyi",pianyi);
                List imagelistt = getImgSrc(text);
                for (int a = 0; a < imagelistt.size(); a++) {
                    int x = text.indexOf((String) imagelistt.get(a));
                    int y = ((String) imagelistt.get(a)).length();
                    text = text.substring(0, x + y) + j.jiami1((String) imagelistt.get(a)) + text.substring(x + y + 1, text.length());
                    if(a<3)
                    image = image + j.jiami1((String) imagelistt.get(a)) + ";";

                }
                sourceAsMap.put("text", text);
                sourceAsMap.put("image", image);
                List list1 = getmyshenfen3((Integer) sourceAsMap.get("userid"));
                String str = "";
                for (int n = 0; n < list1.size(); n++) {
                    HashMap temp1 = (HashMap) list1.get(n);
                    str += temp1.get("name");
                    if (n < list1.size() - 1)
                        str += "、";
                }
                sourceAsMap.put("shenfen", str);
                list.add(sourceAsMap);
            }
        }
        HashMap map2= new HashMap();
        map2.put("code",1001);
        map2.put("data",list);
        return map2;
    }
    @PostMapping("/api/getthemelist_jing")
    @ResponseBody
    @CrossOrigin
    public HashMap getthemelist_jing(@RequestBody HashMap map) throws IOException {
        List list= mapper.getthemelist_jing();
        {
            //list = mapper.getthemelist();
            SearchSourceBuilder builder = new SearchSourceBuilder();
            //条件搜索
            BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
            boolQueryBuilder.must(QueryBuilders.termQuery("show1", 1));
            boolQueryBuilder.must(QueryBuilders.termQuery("jing",1));
            builder.query(boolQueryBuilder);
            //结果集合分页
            builder.from(0).size(100);
            //排序
            builder.sort("replytime", SortOrder.DESC);
            //搜索
            SearchRequest searchRequest = new SearchRequest();
            searchRequest.indices("theme");
            searchRequest.source(builder);
            // 执行请求
            SearchResponse response = client1.search(searchRequest, RequestOptions.DEFAULT);
            SearchHits hits = response.getHits();
            SearchHit[] searchHits = hits.getHits();
            for (SearchHit hit : searchHits) {
                Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                sourceAsMap.put("userheadimage", j.jiami1(mapper.getuserheadimage((Integer) sourceAsMap.get("userid"))));
                sourceAsMap.put("username", mapper.getusername((Integer) sourceAsMap.get("userid")));
                String text = (String) sourceAsMap.get("text");
                String image = "";
                List imagelistt = getImgSrc(text);
                for (int a = 0; a < imagelistt.size(); a++) {
                    int x = text.indexOf((String) imagelistt.get(a));
                    int y = ((String) imagelistt.get(a)).length();
                    text = text.substring(0, x + y) + j.jiami1((String) imagelistt.get(a)) + text.substring(x + y + 1, text.length());
                    if(a<3)
                    image = image + j.jiami1((String) imagelistt.get(a)) + ";";

                }
                HashMap  userinfo= new HashMap();
                userinfo = mapper.getheadborder((Integer)sourceAsMap.get("userid"));
                String url1="";
                HashMap pianyi = new HashMap();
                if(userinfo!=null) {
                    url1=userinfo.get("url").toString();
                    pianyi=mapper.getpianyi(url1);
                }
                sourceAsMap.put("headborder",url1);
                sourceAsMap.put("pianyi",pianyi);
                sourceAsMap.put("text", text);
                sourceAsMap.put("image", image);
                List list1 = getmyshenfen3((Integer) sourceAsMap.get("userid"));
                System.out.println(list1);
                String str = "";
                for (int n = 0; n < list1.size(); n++) {
                    HashMap temp1 = (HashMap) list1.get(n);
                    str += temp1.get("name");
                    if (n < list1.size() - 1)
                        str += "、";
                }
                sourceAsMap.put("shenfen", str);
                list.add(sourceAsMap);
            }
        }
        HashMap map2= new HashMap();
        map2.put("code",1001);
        map2.put("data",list);
        return map2;
    }
    @PostMapping("/api/deletetheme")
    @ResponseBody
    @CrossOrigin
    public HashMap deletetheme(@RequestBody HashMap map) throws IOException {
        mapper.deletetheme((Integer)map.get("themeid"));
        mapper.themenumcut((Integer)map.get("themeid"));
        UpdateRequest request1 = new UpdateRequest();
        {
            request1.index("theme").id(map.get("themeid").toString());
            // 拓展：局部更新也可以这样写：
            request1.doc(XContentType.JSON, "show1",0);
            // 3、发送请求到ES
            UpdateResponse response1 = client1.update(request1, RequestOptions.DEFAULT);
        }
        HashMap map2= new HashMap();
        map2.put("code",1001);
        map2.put("data","success");
        return map2;
    }


    public void email( HashMap map){
        // 构建一个邮件对象
        System.out.println(map);
        SimpleMailMessage message = new SimpleMailMessage();
        // 设置邮件主题
        message.setSubject("测试博客激活");
        // 设置邮件发送者，这个跟application.yml中设置的要一致
        message.setFrom("2483242070@qq.com");
        // 设置邮件接收者，可以有多个接收者，中间用逗号隔开，以下类似
        // message.setTo("10*****16@qq.com","12****32*qq.com");
        message.setTo((String)map.get("email"));
        // 设置邮件抄送人，可以有多个抄送人
        //message.setCc("12****32*qq.com");
        // 设置隐秘抄送人，可以有多个
        //message.setBcc("7******9@qq.com");
        // 设置邮件发送日期
        message.setSentDate(new Date());
        // 设置邮件的正文
        System.out.println((String) map.get("emailtest"));
        message.setText("请访问此链接以激活账户:"+"https://www.begonia.cafe/pages/index/jihuo?cookie="+(String)map.get("emailtest"));
        // 发送邮件
        javaMailSender.send(message);
    }
    @Test
    public void email1( HashMap map){
        // 构建一个邮件对象
        SimpleMailMessage message = new SimpleMailMessage();
        // 设置邮件主题
        message.setSubject("密码找回");
        // 设置邮件发送者，这个跟application.yml中设置的要一致
        message.setFrom("2483242070@qq.com");
        // 设置邮件接收者，可以有多个接收者，中间用逗号隔开，以下类似
        // message.setTo("10*****16@qq.com","12****32*qq.com");
        message.setTo((String)map.get("email"));
        // 设置邮件抄送人，可以有多个抄送人
        //message.setCc("12****32*qq.com");
        // 设置隐秘抄送人，可以有多个
        //message.setBcc("7******9@qq.com");
        // 设置邮件发送日期
        message.setSentDate(new Date());
        // 设置邮件的正文
        message.setText("你的验证码是:"+(String)map.get("findpassword")+"请在十五分钟内修改");
        // 发送邮件
        javaMailSender.send(message);
    }
    @PostMapping("/api/login")
    @ResponseBody
    @CrossOrigin
    public HashMap login(@RequestBody HashMap map,HttpServletRequest request){
        HashMap map1=new HashMap();
        HashMap temp=mapper.login((String)map.get("email"),(String)map.get("password"));

        temp.put("userheadimage",j.jiami1(temp.get("userheadimage").toString()));
        if(temp.get("userbackgroundimage")!=null)
        temp.put("userbackgroundimage",j.jiami1(temp.get("userbackgroundimage").toString()));
        temp.put("toutu",j.jiami1(temp.get("toutu").toString()));

        if(null!=temp) {
            if(temp.get("emailtest")!=null){
                map1.put("code",1003);
                map1.put("data","邮箱未激活");
                return map1;
            }
            map1.put("code", 1001);
            StringBuffer cookie = new StringBuffer();
            Random random = new Random();
            String str="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
            for(int j=0;j<=18;j++) {
                int sub = random.nextInt(61);
                cookie.append(str.substring(sub, sub + 1));
            }
            temp.put("cookie",cookie.toString());
            temp.remove("password");
            mapper.setcookie(cookie.toString(),(Integer)temp.get("userid"));
            map1.put("data",temp);
            {
                String ip = request.getRemoteAddr();
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String time = (formatter.format(System.currentTimeMillis()));
                String address = ip2regionSearcher.getAddress(ip);
                String newlogin =
                        "学伴 WMU"+
                        "\n发现新设备尝试登录"+
                        "\n登录IP: "+ ip+
                        "\n登录地址: "+ address+
                        "\n登录时间: "+ time;
                HashMap map2 = new HashMap();
                map2.put("email",mapper.getmyself(cookie.toString()).get("email"));
                map2.put("userid",mapper.getmyself(cookie.toString()).get("userid"));
                map2.put("msg",newlogin);
                map2.put("title","登录通知");
                send.loginmessage(JSON.toJSONString(map2));
            }
        }
        else {
            map1.put("code",1002);
            map1.put("data","邮箱或密码错误");
        }
        return map1;
    }
    @PostMapping("/api/login_app")
    @ResponseBody
    @CrossOrigin
    public HashMap login_app(@RequestBody HashMap map,HttpServletRequest request){
        HashMap map1=new HashMap();
        HashMap temp=mapper.login((String)map.get("email"),(String)map.get("password"));

        if(null!=temp) {
            if(temp.get("emailtest")!=null){
                map1.put("code",1003);
                map1.put("data","邮箱未激活");
                return map1;
            }
            map1.put("code", 1001);
            StringBuffer cookie = new StringBuffer();
            Random random = new Random();
            String str="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
            for(int j=0;j<=18;j++) {
                int sub = random.nextInt(61);
                cookie.append(str.substring(sub, sub + 1));
            }
            temp.put("cookie",cookie.toString());
            temp.remove("password");
            mapper.set_app_cookie(cookie.toString(),(Integer)temp.get("userid"));
            {
                String ip = request.getRemoteAddr();
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String time = (formatter.format(System.currentTimeMillis()));
                String address = ip2regionSearcher.getAddress(ip);
                String newlogin =
                        "学伴 WMU APP"+
                                "\n发现新设备尝试登录"+
                                "\n登录IP: "+ ip+
                                "\n登录地址: "+ address+
                                "\n登录时间: "+ time;
                HashMap map2 = new HashMap();
                map2.put("email",mapper.getmyself(cookie.toString()).get("email"));
                map2.put("userid",mapper.getmyself(cookie.toString()).get("userid"));
                map2.put("msg",newlogin);
                map2.put("title","登录通知");
                send.loginmessage(JSON.toJSONString(map2));
            }
            map1.put("data",temp);
        }
        else {
            map1.put("code",1002);
            map1.put("data","邮箱或密码错误");
        }
        return map1;
    }
    @PostMapping("/api/register")
    @ResponseBody
    @CrossOrigin
    public HashMap register(@RequestBody HashMap map){
        HashMap map1 = new HashMap();
        HashMap map2=mapper.getuserbyemail((String)map.get("email"));
        if(null!=map2){
            if(null!=map2.get("emailtest")){
                StringBuffer cookie = new StringBuffer();
                Random random = new Random();
                String str="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789%";
                for(int j=0;j<=58;j++) {
                    int sub = random.nextInt(61);
                    cookie.append(str.substring(sub, sub + 1));
                }
                mapper.reregister((String)map.get("email"),(String)map.get("password"),cookie.toString(), (int)(new Date().getTime()/1000));
                map1.put("code",1001);
                map1.put("data","注册成功");
                map.put("emailtest",cookie.toString());

                email(map);
            }
            else{
                map1.put("code",1002);
                map1.put("data","邮箱已被注册");
            }
        }
        else{
            StringBuffer cookie = new StringBuffer();
            Random random = new Random();
            String str="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789%";
            for(int j=0;j<=58;j++) {
                int sub = random.nextInt(61);
                cookie.append(str.substring(sub, sub + 1));
            }
            try{
                map.put("emailtest",cookie.toString());
                mapper.register((String)map.get("email"),(String)map.get("password"),cookie.toString() ,(int)(new Date().getTime()/1000));
                email(map);

            }
            catch(Exception e){
                map1.put("code",1003);
                map1.put("data","注册失败");
                return map1;
            }

            map1.put("code",1001);
            map1.put("data","注册成功");
            map.put("emailtest",cookie.toString());

        }
        return map1;
    }
    @PostMapping("/api/jihuo")
    @ResponseBody
    @CrossOrigin
    public HashMap jihuo(@RequestBody HashMap map){
        HashMap map1 = new HashMap();
        if(null==mapper.findemailtest((String)map.get("cookie"))){
            map1.put("code",1002);
            map1.put("data","error");
        }
        else{
            if(null!=mapper.getuserbyusername((String)map.get("username"))){
                map1.put("code",1003);
                map1.put("data","用户名已被使用");
            }
            else{
                mapper.setusernameby_emailtest((String)map.get("cookie"),(String)map.get("username"));
                map1.put("code",1001);
                map1.put("data","邮箱已激活");
            }
        }
        return map1;
    }
    @PostMapping("/api/setuserbackgroundimage")
    @ResponseBody
    @CrossOrigin
    public HashMap setuserbackgroundimage(@RequestBody HashMap map){
        HashMap map1 = new HashMap();
        mapper.setuserbackgroundimage((Integer)map.get("userid"),(String)map.get("userbackgroundimage"));
        map1.put("code",1001);
        map1.put("data","设置成功");
        return map1;
    }
    @PostMapping("/api/setusertoutu")
    @ResponseBody
    @CrossOrigin
    public HashMap setusertoutu(@RequestBody HashMap map){
        HashMap map1 = new HashMap();
        mapper.setusertoutu((Integer)map.get("userid"),(String)map.get("toutu"));
        map1.put("code",1001);
        map1.put("data","设置成功");
        return map1;
    }
    @PostMapping("/api/setuserheadimage")
    @ResponseBody
    @CrossOrigin
    public HashMap setuserheadimage(@RequestBody HashMap map){
        HashMap map1 = new HashMap();
        mapper.setuserheadimage((Integer)map.get("userid"),(String)map.get("userheadimage"));
        map1.put("code",1001);
        map1.put("data","设置成功");
        return map1;
    }
    @PostMapping("/api/setuserheadimage1")
    @ResponseBody
    @CrossOrigin
    public HashMap setuserheadimage1(@RequestBody HashMap map){
        HashMap map1 = new HashMap();
        mapper.setuserheadimage((Integer)map.get("userid"),(String)map.get("userheadimage"));
        map1.put("code",1001);
        map1.put("data","设置成功");
        return map1;
    }

    @PostMapping("/api/setjing")
    @ResponseBody
    @CrossOrigin
    public HashMap setjing(@RequestBody HashMap map) throws IOException {
        HashMap map1 = new HashMap();
        if(mapper.jugeadmin((String)map.get("cookie"))!=1){
            map1.put("code",1003);
            map1.put("data","没有权限");
            return map1;
        }
        else{
            mapper.setjing((Integer)map.get("themeid"));
            UpdateRequest request1 = new UpdateRequest();
            {
                request1.index("theme").id(map.get("themeid").toString());
                // 拓展：局部更新也可以这样写：
                request1.doc(XContentType.JSON, "jing",1);
                // 3、发送请求到ES
                UpdateResponse response1 = client1.update(request1, RequestOptions.DEFAULT);
            }
            map1.put("code",1001);
            map1.put("data","设置成功");
        }
        return map1;
    }
    @PostMapping("/api/deletejing")
    @ResponseBody
    @CrossOrigin
    public HashMap deletejing(@RequestBody HashMap map) throws IOException {
        HashMap map1 = new HashMap();
        if(mapper.jugeadmin((String)map.get("cookie"))!=1){
            map1.put("code",1003);
            map1.put("data","没有权限");
            return map1;
        }
        else{
            mapper.deletejing((Integer)map.get("themeid"));
            UpdateRequest request1 = new UpdateRequest();
            {
                request1.index("theme").id(map.get("themeid").toString());
                // 拓展：局部更新也可以这样写：
                request1.doc(XContentType.JSON, "jing",0);
                // 3、发送请求到ES
                UpdateResponse response1 = client1.update(request1, RequestOptions.DEFAULT);
            }
            map1.put("code",1001);
            map1.put("data","设置成功");
        }
        return map1;
    }
    @PostMapping("/api/settop")
    @ResponseBody
    @CrossOrigin
    public HashMap settop(@RequestBody HashMap map) throws IOException {
        HashMap map1 = new HashMap();
       // System.out.println(mapper.jugeadmin((String)map.get("cookie")));
        if(mapper.jugeadmin((String)map.get("cookie"))!=1){
            map1.put("code",1003);
            map1.put("data","没有权限");
            return map1;
        }
        else{
            mapper.settop((Integer)map.get("themeid"));
            UpdateRequest request1 = new UpdateRequest();
            {
                request1.index("theme").id(map.get("themeid").toString());
                // 拓展：局部更新也可以这样写：
                request1.doc(XContentType.JSON, "top",1);
                // 3、发送请求到ES
                UpdateResponse response1 = client1.update(request1, RequestOptions.DEFAULT);
            }
            map1.put("code",1001);
            map1.put("data","取消成功");
        }
        return map1;
    }
    @PostMapping("/api/deletetop")
    @ResponseBody
    @CrossOrigin
    public HashMap deletetop(@RequestBody HashMap map) throws IOException {
        HashMap map1 = new HashMap();
        if(mapper.jugeadmin((String)map.get("cookie"))!=1){
            map1.put("code",1003);
            map1.put("data","没有权限");
            return map1;
        }
        else{
            mapper.deletetop((Integer)map.get("themeid"));
            UpdateRequest request1 = new UpdateRequest();
            {
                request1.index("theme").id(map.get("themeid").toString());
                // 拓展：局部更新也可以这样写：
                request1.doc(XContentType.JSON, "top",0);
                // 3、发送请求到ES
                UpdateResponse response1 = client1.update(request1, RequestOptions.DEFAULT);
            }
            map1.put("code",1001);
            map1.put("data","设置");
        }
        return map1;
    }
    @PostMapping("/api/lookadd")
    @ResponseBody
    @CrossOrigin
    public void lookadd(@RequestBody HashMap map){
        mapper.lookadd((Integer)map.get("themeid"));
    }
    @PostMapping("/api/jugeuser")
    @ResponseBody
    @CrossOrigin
    public HashMap jugeuser(@RequestBody HashMap map){
        HashMap map1=new HashMap();
        if(mapper.getuserbycookie((String)map.get("cookie"))!=null)
            map1.put("code",1001);
        else
            map1.put("code",1002);
        return map1;
    }
    
    @PostMapping("/api/getfenlei")
    @ResponseBody
    @CrossOrigin
    public List getfenlei(){
        List list = mapper.getfenlei();
        List list1=new ArrayList();List list2=new ArrayList();List list3=new ArrayList();
        Integer sum=0;
        for(int i=0;i<list.size();i++){
            HashMap temp = (HashMap) list.get(i);
           // System.out.println(temp.get("fenlei"));
            sum+=((Long)temp.get("count")).intValue();
            list1.add((String)temp.get("fenlei"));
            list2.add((Long)temp.get("count"));
        }
        list1.add("全部分类");list2.add(sum);
        list3.add(list1);list3.add(list2);
        return list3;
    }
    @PostMapping("/api/getfenleilist")
    @ResponseBody
    @CrossOrigin
    public List getfenleilist(){
        List list = mapper.getfenleilist();
        return list;
    }
    @PostMapping("/api/setmylike")
    @ResponseBody
    @CrossOrigin
    public HashMap setmylike(@RequestBody HashMap map) throws IOException {
        HashMap map1=new HashMap();
        if(mapper.getuserbycookie((String)map.get("cookie"))!=null){
            List temp = mapper.getmylike((Integer)map.get("userid"));
            for(int i=0;i<temp.size();i++){
                if(temp.get(i)==(Integer)map.get("themeid")){
                    map1.put("code",1002);
                    return map1;
                }
            }
            map1.put("code",1001);
            mapper.setmylike((Integer) map.get("userid"), (Integer) map.get("themeid"));
            mapper.likeadd((Integer)map.get("themeid"));
            UpdateRequest request = new UpdateRequest();
            {
                request.index("theme").id(map.get("themeid").toString());
                // 拓展：局部更新也可以这样写：
                request.doc(XContentType.JSON, "likes",mapper.gettheme((Integer)map.get("themeid")).get("likes"));
                // 3、发送请求到ES
                UpdateResponse response = client1.update(request, RequestOptions.DEFAULT);
            }
            mapper.userlikeadd((Integer)map.get("themeid"));
        }
        else map1.put("code",1002);
        return map1;
    }
    @PostMapping("/api/deletemylike")
    @ResponseBody
    @CrossOrigin
    public HashMap deletemylike(@RequestBody HashMap map) throws IOException {
        HashMap map1=new HashMap();
        if(mapper.getuserbycookie((String)map.get("cookie"))!=null){
            List temp = mapper.getmylike((Integer)map.get("userid"));
            Integer themeid = (Integer)map.get("themeid");
            for(int i=0;i<temp.size();i++){
                Integer tempid = (Integer) temp.get(i);
                if(tempid.toString().equals(themeid.toString())){
                    map1.put("code",1001);
                    mapper.deletemylike((Integer) map.get("userid"), (Integer) map.get("themeid"));
                    mapper.likecut((Integer) map.get("themeid"));
                    mapper.userlikecut((Integer)map.get("themeid"));
                    UpdateRequest request = new UpdateRequest();
                    {
                        request.index("theme").id(map.get("themeid").toString());
                        // 拓展：局部更新也可以这样写：
                        request.doc(XContentType.JSON, "likes",mapper.gettheme((Integer)map.get("themeid")).get("likes"));
                        // 3、发送请求到ES
                        UpdateResponse response = client1.update(request, RequestOptions.DEFAULT);
                    }
                    return map1;
                }
            }

            map1.put("code",1001);
        }
        else map1.put("code",1002);
        return map1;
    }
    @PostMapping("/api/getmylike")
    @ResponseBody
    @CrossOrigin
    public HashMap getmylike(@RequestBody HashMap map){
        HashMap map1=new HashMap();
        if(mapper.getuserbycookie((String)map.get("cookie"))!=null){
            map1.put("code",1001);
            map1.put("mylike",mapper.getmylike((Integer)map.get("userid")));
        }
        else map1.put("code",1002);
        return map1;
    }
    @PostMapping("/api/getmyself")
    @ResponseBody
    @CrossOrigin
    public HashMap getmyself(@RequestBody HashMap map){
        HashMap map1=new HashMap();
        map1.put("code",1001);
        HashMap map2 = mapper.getmyself((String)map.get("cookie"));
        map1.put("shenfen",getmyshenfen3((Integer)map2.get("userid")));
        map2.put("userheadimage",map2.get("userheadimage").toString());
        if(map2.get("userbackgroundimage")!=null)
        map2.put("userbackgroundimage",map2.get("userbackgroundimage").toString());
        map2.put("toutu",map2.get("toutu").toString());
        HashMap  userinfo= new HashMap();
        userinfo = mapper.getheadborder((Integer)map2.get("userid"));
        String url1="";
        HashMap pianyi = new HashMap();
        if(userinfo!=null) {
            url1=userinfo.get("url").toString();
            pianyi=mapper.getpianyi(url1);
        }

        map2.put("headborder",url1);
        map2.put("pianyi",pianyi);
        map1.put("data",map2);
        return map1;
    }
    @PostMapping("/api/getheself")
    @ResponseBody
    @CrossOrigin
    public HashMap getheself(@RequestBody HashMap map){
        HashMap map1=new HashMap();
        map1.put("code",1001);
        HashMap map2=mapper.getheself((Integer)map.get("userid"));
        map1.put("shenfen",getmyshenfen3((Integer)map.get("userid")));
        HashMap  userinfo= new HashMap();
        userinfo = mapper.getheadborder((Integer)map.get("userid"));
        String url1="";
        HashMap pianyi = new HashMap();
        if(userinfo!=null) {
            url1=userinfo.get("url").toString();
            pianyi=mapper.getpianyi(url1);
        }
        map2.put("headborder",url1);
        map2.put("pianyi",pianyi);
        map2.put("userheadimage", j.jiami1(map2.get("userheadimage").toString()));
        if(null!=map2.get("userbackgroundimage"))
        map2.put("userbackgroundimage",j.jiami1(map2.get("userbackgroundimage").toString()));
        map2.put("toutu",j.jiami1(map2.get("toutu").toString()));
        map1.put("data",map2);
        return map1;
    }
    @PostMapping("/api/getthemelistby_user")
    @ResponseBody
    @CrossOrigin
    public HashMap getthemelistby_user(@RequestBody HashMap map) throws IOException {
        List list = new ArrayList();
        {
            //list = mapper.getthemelist();
            SearchSourceBuilder builder = new SearchSourceBuilder();
            //条件搜索
            BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
            boolQueryBuilder.must(QueryBuilders.termQuery("userid", (Integer)map.get("userid")));
            boolQueryBuilder.must(QueryBuilders.termQuery("show1", 1));
            builder.query(boolQueryBuilder);
            //结果集合分页
            builder.from((Integer)map.get("limit")*10).size(10);
            //排序
            builder.sort("createtime", SortOrder.DESC);
            //搜索
            SearchRequest searchRequest = new SearchRequest();
            searchRequest.indices("theme");
            searchRequest.source(builder);
            // 执行请求
            SearchResponse response = client1.search(searchRequest, RequestOptions.DEFAULT);
            SearchHits hits = response.getHits();
            SearchHit[] searchHits = hits.getHits();
            for (SearchHit hit : searchHits) {
                Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                sourceAsMap.put("userheadimage", j.jiami1(mapper.getuserheadimage((Integer) sourceAsMap.get("userid"))));
                sourceAsMap.put("username", mapper.getusername((Integer) sourceAsMap.get("userid")));
                String text = (String) sourceAsMap.get("text");
                String image = "";
                HashMap  userinfo= new HashMap();
                userinfo = mapper.getheadborder((Integer)sourceAsMap.get("userid"));
                String url1="";
                HashMap pianyi = new HashMap();
                if(userinfo!=null) {
                    url1=userinfo.get("url").toString();
                    pianyi=mapper.getpianyi(url1);
                }
                sourceAsMap.put("headborder",url1);
                sourceAsMap.put("pianyi",pianyi);
                List imagelistt = getImgSrc(text);
                for (int a = 0; a < imagelistt.size(); a++) {
                    int x = text.indexOf((String) imagelistt.get(a));
                    int y = ((String) imagelistt.get(a)).length();
                    text = text.substring(0, x + y) + j.jiami1((String) imagelistt.get(a)) + text.substring(x + y + 1, text.length());
                    if(a<3)
                    image = image + j.jiami1((String) imagelistt.get(a)) + ";";

                }
                if(map.get("app")==null){
                    sourceAsMap.put("text", text);
                    sourceAsMap.put("data","");
                }
                else{
                    sourceAsMap.put("text","");
                    if(sourceAsMap.get("data").toString().length()>200){
                        sourceAsMap.put("data",sourceAsMap.get("data").toString().substring(0,200));
                    }
                }
                sourceAsMap.put("image", image);
                List list1 = getmyshenfen3((Integer) sourceAsMap.get("userid"));
                String str = "";
                for (int n = 0; n < list1.size(); n++) {
                    HashMap temp1 = (HashMap) list1.get(n);
                    str += temp1.get("name");
                    if (n < list1.size() - 1)
                        str += "、";
                }
                sourceAsMap.put("shenfen", str);
                list.add(sourceAsMap);
            }
        }
        HashMap map2= new HashMap();
        map2.put("code",1001);
        map2.put("data",list);
        map2.put("themelistnum",mapper.getthemelistnum((Integer)map.get("userid")));
        return map2;
    }
    @PostMapping("/api/setsign")
    @ResponseBody
    @CrossOrigin
    public HashMap setsign(@RequestBody HashMap map){
        HashMap map1=new HashMap();
        mapper.setsign((String)map.get("cookie"),(String)map.get("sign"));
        map1.put("code",1001);
        return map1;
    }
    @PostMapping("/api/reply")
    @ResponseBody
    @CrossOrigin
    public HashMap reply(@RequestBody HashMap map) throws IOException {
        if((Integer)map.get("userid")==(Integer)mapper.getuserbycookie((String)map.get("cookie")).get("userid")){
            if(mapper.jugeuserban(map)!=0){
                HashMap map1 = new HashMap();
                map1.put("code",1002);
                return  map1;
            }
            String time=(new Date().getTime() / 1000)+"";
            if((Integer)map.get("type")==1) {
                mapper.newreply((Integer) map.get("themeid"), (Integer) map.get("userid"), (String) map.get("data"), (String) map.get("image"),
                        mapper.getlouceng((Integer) map.get("themeid")) + 1, time
                );
                mapper.loucengadd((Integer) map.get("themeid"));
                mapper.numadd((Integer)map.get("themeid"));
                mapper.setthemereplytime((Integer)map.get("themeid"),Integer.valueOf(time));
                UpdateRequest request1 = new UpdateRequest();
                {
                    HashMap temp = mapper.gettheme((Integer)map.get("themeid"));
                    request1.index("theme").id(map.get("themeid").toString());
                    request1.doc(XContentType.JSON, "num",temp.get("num"),"replytime",time,"louceng",temp.get("louceng"));
                    UpdateResponse response2 = client1.update(request1, RequestOptions.DEFAULT);

                }
            }

        }
        HashMap map1 = new HashMap();
        map1.put("code",1001);
        return map1;
    }
    @PostMapping("/api/getreply")
    @ResponseBody
    @CrossOrigin
    public HashMap getreply(@RequestBody HashMap map){
        List list = new ArrayList();
        if((Integer)map.get("replytype")==1) {
             list = mapper.getreply(map);
        }
        else if((Integer)map.get("replytype")==2){
             list = mapper.getreply_author(map);
        }else{
            list = mapper.getreply1(map);
        }
        for(int i=0;i<list.size();i++){
            HashMap map2=(HashMap) list.get(i);
            List list1 = getmyshenfen3((Integer)map2.get("userid"));
            String str="";
            for(int j=0;j<list1.size();j++){
                HashMap temp=(HashMap)list1.get(j);
                str+=temp.get("name");
                if(j<list1.size()-1)
                    str+="、";
            }
            List list2 = mapper.getloucengreply((Integer)map.get("themeid"),(Integer)map2.get("louceng"));
            for(int t=0;t<list2.size();t++){
                HashMap temp = (HashMap) list2.get(t);
                List list3 = getmyshenfen3((Integer)temp.get("userid"));
                String str1="";
                for(int x=0;x<list3.size();x++){
                    HashMap temp1=(HashMap)list3.get(x);
                    str1+=temp1.get("name");
                    if(x<list3.size()-1)
                        str1+="、";
                }
                temp.put("shenfen",str1);
                HashMap  userinfo= new HashMap();
                userinfo = mapper.getheadborder((Integer)temp.get("userid"));
                String url1="";
                HashMap pianyi = new HashMap();
                if(userinfo!=null) {
                    url1=userinfo.get("url").toString();
                    pianyi=mapper.getpianyi(url1);
                }
                temp.put("headborder",url1);
                temp.put("pianyi",pianyi);
                temp.put("userheadimage",j.jiami1(temp.get("userheadimage").toString()));
                list2.set(t,temp);
            }
            map2.put("loucengreply",list2);
            map2.put("shenfen",str);
            HashMap str1 = mapper.getheadborder((Integer)map2.get("userid"));
            String url1="";
            HashMap pianyi = new HashMap();
            if(str1!=null) {
                url1=str1.get("url").toString();
                pianyi=mapper.getpianyi(url1);
            }
            map2.put("headborder",url1);
            map2.put("pianyi",pianyi);
            map2.put("userheadimage",j.jiami1(map2.get("userheadimage").toString()));
            String text=map2.get("data").toString();
            String[] image=map2.get("image").toString().split(";");
            String images="";
            if(image.length!=0)
            for(int t=0;t<image.length;t++){
                images=images+j.jiami1(image[t])+";";
            }
            List imagelistt = getImgSrc(text);
            for(int a=0;a<imagelistt.size();a++){
                int x=text.indexOf((String)imagelistt.get(a));
                int y=((String)imagelistt.get(a)).length();
                text=text.substring(0,x+y)+j.jiami1((String)imagelistt.get(a))+text.substring(x+y+1,text.length());
            }
            map2.put("text",text);
            map2.put("image",images);
            list.set(i,map2);
        }
        HashMap map1 = new HashMap();
        map1.put("code",1001);
        map1.put("data",list);
        return map1;
    }
    @PostMapping("/api/deletereply")
    @ResponseBody
    @CrossOrigin
    public HashMap deletereply(@RequestBody HashMap map){
        HashMap map1 = new HashMap();

        if(mapper.getuseridbylouceng((Integer)map.get("themeid"),(Integer)map.get("louceng"))==(Integer)map.get("userid")
        ||mapper.getadmin((String)map.get("cookie"))==1){
            mapper.deletelouceng((Integer)map.get("themeid"),(Integer)map.get("louceng"));
            map1.put("code",1001);
            return map1;
        }
        map1.put("code",1002);
        return map1;
    }
    @PostMapping("/api/getmysave")
    @ResponseBody
    @CrossOrigin
    public List getmysave(@RequestBody HashMap map){
        return mapper.getmysave((Integer)map.get("userid"));
    }
    @PostMapping("/api/addsave")
    @ResponseBody
    @CrossOrigin
    public HashMap addsave(@RequestBody HashMap map){
        HashMap map1=new HashMap();
        List<Integer> mysave = mapper.getmysave((Integer)map.get("userid"));
        if(!mysave.contains((Integer)map.get("themeid"))){
            mapper.saveadd((Integer)map.get("userid"),(Integer)map.get("themeid"));
            map1.put("code",1001);
            return map1;
        }
        else {
            map1.put("code", 1002);
            return map1;
        }
    }
    @PostMapping("/api/savedelete")
    @ResponseBody
    @CrossOrigin
    public HashMap savedelete(@RequestBody HashMap map){
        HashMap map1=new HashMap();
        List<Integer> mysave = mapper.getmysave((Integer)map.get("userid"));
        if(mysave.contains((Integer)map.get("themeid"))){
            mapper.savedelete((Integer)map.get("userid"),(Integer)map.get("themeid"));
            map1.put("code",1001);
            return map1;
        }
        else {
            map1.put("code", 1002);
            return map1;
        }
    }
    @PostMapping("/api/getmyattention")
    @ResponseBody
    @CrossOrigin
    public HashMap getmyattention(@RequestBody HashMap map){
        HashMap map1=new HashMap();
        if((Integer)map.get("userid")==mapper.getuserbycookie((String)map.get("cookie")).get("userid")){
            List list = mapper.getmyattention((Integer)map.get("userid"));
            map1.put("data",list);
            map1.put("code",1001);
            return map1;
        }
        else {
            map1.put("code", 1002);
            return map1;
        }
    }
    @PostMapping("/api/addattention")
    @ResponseBody
    @CrossOrigin
    public HashMap addattention(@RequestBody HashMap map){
        HashMap map1=new HashMap();
        if((Integer)map.get("a")==mapper.getuserbycookie((String)map.get("cookie")).get("userid")){
            List list = mapper.getmyattention((Integer)map.get("a"));
            if(!list.contains((Integer)map.get("b"))) {
                map1.put("code", 1001);
                mapper.addattention((Integer) map.get("a"), (Integer) map.get("b"));
                mapper.addattentionnum((Integer)map.get("a"));
                mapper.addfansnum((Integer)map.get("b"));
            }
            else map1.put("code",1003);
            return map1;
        }
        else {
            map1.put("code", 1002);
            return map1;
        }
    }
    @PostMapping("/api/deleteattention")
    @ResponseBody
    @CrossOrigin
    public HashMap deleteattention(@RequestBody HashMap map){
        HashMap map1=new HashMap();
        if((Integer)map.get("a")==mapper.getuserbycookie((String)map.get("cookie")).get("userid")){
            List list = mapper.getmyattention((Integer)map.get("a"));
            if(list.contains((Integer)map.get("b"))) {
                map1.put("code", 1001);
                mapper.deleteattention((Integer) map.get("a"), (Integer) map.get("b"));
                mapper.deleteattentionnum((Integer)map.get("a"));
                mapper.deletefansnum((Integer)map.get("b"));
            }
            else map1.put("code",1003);
            return map1;
        }
        else {
            map1.put("code", 1002);
            return map1;
        }
    }
    @PostMapping("/api/getmyattentionlist")
    @ResponseBody
    @CrossOrigin
    public HashMap getmyattentionlist(@RequestBody HashMap map){
        HashMap map1=new HashMap();
        if((Integer)map.get("a")==mapper.getuserbycookie((String)map.get("cookie")).get("userid")){
            map1.put("code", 1001);
            map.put("page",((int)map.get("attentionpage"))*5);
            List list = mapper.getmyattentionlist(map);
            for(int i=0;i<list.size();i++){
                HashMap map4 =(HashMap)list.get(i);

                map4.put("userheadimage",j.jiami1(map4.get("userheadimage").toString()));
                list.set(i,map4);
            }
            map1.put("data",list);
            return map1;
        }
        else {
            map1.put("code", 1002);
            return map1;
        }
    }
    @PostMapping("/api/getmyfanslist")
    @ResponseBody
    @CrossOrigin
    public HashMap getmyfanslist(@RequestBody HashMap map){
        HashMap map1=new HashMap();
        if((Integer)map.get("b")==mapper.getuserbycookie((String)map.get("cookie")).get("userid")){
            map1.put("code", 1001);
            map.put("page",((int)map.get("fanspage"))*5);
            List list = mapper.getmyfanslist(map);
            for(int i=0;i<list.size();i++){
                HashMap map4 =(HashMap)list.get(i);

                    map4.put("userheadimage",j.jiami1(map4.get("userheadimage").toString()));
                list.set(i,map4);
            }
            map1.put("data",list);
            return map1;
        }
        else {
            map1.put("code", 1002);
            return map1;
        }
    }
    @PostMapping("/api/updatehot")
    @ResponseBody
    @CrossOrigin
    @Scheduled(cron = "0 */5 * * * ?")
    public void updatehot() throws IOException {
        Integer time = (int)(new Date().getTime()/1000);
        mapper.updatehot(time);
        List list = mapper.hotthemelist();
        for(int i=0;i<list.size();i++){
            UpdateRequest request1 = new UpdateRequest();
            HashMap map = (HashMap) list.get(i);
            {
                request1.index("theme").id(map.get("themeid").toString());
                // 拓展：局部更新也可以这样写：
                request1.doc(XContentType.JSON, "hot",(double)map.get("hot"));
                // 3、发送请求到ES
                UpdateResponse response1 = client1.update(request1, RequestOptions.DEFAULT);
            }
        }
    }
    @PostMapping("/api/resetlogintime")
    @ResponseBody
    @CrossOrigin
    @Scheduled(cron = "0 0 0  * * ?")
    public void resetlogintime(){
        Long current=System.currentTimeMillis();
        Long zero=current/(1000*3600*24)*(1000*3600*24)-TimeZone.getDefault().getRawOffset();
        Integer time=(int)(zero/1000)-(7*24*60*60);
        mapper.resetlogintime(time);
    }
    @PostMapping("/api/userhot")
    @ResponseBody
    @CrossOrigin
    @Scheduled(cron = "0 0 */1 * * ?")
    public void userhot(){
        Integer time = (int)(new Date().getTime()/1000);
        mapper.userhot(time);
    }
    @PostMapping("/api/getthemelistby_hot")
    @ResponseBody
    @CrossOrigin
    public HashMap getthemelistby_hot(@RequestBody HashMap map) throws IOException {
        List list = new ArrayList();
        {
            //list = mapper.getthemelist();
            SearchSourceBuilder builder = new SearchSourceBuilder();
            //条件搜索
            BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
            boolQueryBuilder.must(QueryBuilders.termQuery("show1", 1));
            builder.query(boolQueryBuilder);
            //结果集合分页
            Integer limit = 0;
            if(map.get("limit")!=null){
                limit=(int)map.get("limit");
            }
            builder.from(limit*10).size(10);
            //排序
            builder.sort("hot", SortOrder.DESC);
            //搜索
            SearchRequest searchRequest = new SearchRequest();
            searchRequest.indices("theme");
            searchRequest.source(builder);
            // 执行请求
            SearchResponse response = client1.search(searchRequest, RequestOptions.DEFAULT);
            SearchHits hits = response.getHits();
            SearchHit[] searchHits = hits.getHits();
            for (SearchHit hit : searchHits) {
                Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                sourceAsMap.put("userheadimage", j.jiami1(mapper.getuserheadimage((Integer) sourceAsMap.get("userid"))));
                sourceAsMap.put("username", mapper.getusername((Integer) sourceAsMap.get("userid")));
                String text = (String) sourceAsMap.get("text");
                String image = "";
                HashMap  userinfo= new HashMap();
                userinfo = mapper.getheadborder((Integer)sourceAsMap.get("userid"));
                String url1="";
                HashMap pianyi = new HashMap();
                if(userinfo!=null) {
                    url1=userinfo.get("url").toString();
                    pianyi=mapper.getpianyi(url1);
                }
                sourceAsMap.put("headborder",url1);

                sourceAsMap.put("pianyi",pianyi);
                List imagelistt = getImgSrc(text);
                for (int a = 0; a < imagelistt.size(); a++) {
                    int x = text.indexOf((String) imagelistt.get(a));
                    int y = ((String) imagelistt.get(a)).length();
                    text = text.substring(0, x + y) + j.jiami1((String) imagelistt.get(a)) + text.substring(x + y + 1, text.length());
                    if(a<3)
                    image = image + j.jiami1((String) imagelistt.get(a)) + ";";

                }
                if(map.get("app")==null){
                    sourceAsMap.put("text", text);
                    sourceAsMap.put("data","");
                }
                else{
                    sourceAsMap.put("text","");
                    if(sourceAsMap.get("data").toString().length()>200){
                        sourceAsMap.put("data",sourceAsMap.get("data").toString().substring(0,200));
                    }
                }
                sourceAsMap.put("image", image);
                List list1 = getmyshenfen3((Integer) sourceAsMap.get("userid"));
                String str = "";
                for (int n = 0; n < list1.size(); n++) {
                    HashMap temp1 = (HashMap) list1.get(n);
                    str += temp1.get("name");
                    if (n < list1.size() - 1)
                        str += "、";
                }
                sourceAsMap.put("shenfen", str);
                list.add(sourceAsMap);
            }
        }
        HashMap map2= new HashMap();
        map2.put("code",1001);
        map2.put("data",list);
        return map2;
    }
    @PostMapping("/api/user_visit")
    @ResponseBody
    @CrossOrigin
    public void user_visit(@RequestBody HashMap map){
        mapper.user_visit((Integer)map.get("userid"),(int)(new Date().getTime()/1000));
        Long current=System.currentTimeMillis();
        Long zero=current/(1000*3600*24)*(1000*3600*24)-TimeZone.getDefault().getRawOffset();
        SimpleDateFormat formatter = new SimpleDateFormat("MM-dd");
        String millisecondStrings = formatter.format(current);
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        if(mapper.juge_user_visit((Integer)map.get("userid"),millisecondStrings)==0)
            mapper.user_visit1((Integer)map.get("userid"),millisecondStrings,(int)( calendar.getTimeInMillis()/1000));
        int now = (int)(new Date().getTime()/1000);

        mapper.jugebanout((Integer)map.get("userid"),now);

    }
    @PostMapping("/api/userhotlist")
    @ResponseBody
    @CrossOrigin
    public List userhotlist(@RequestBody HashMap map){
        if((Integer)map.get("userid")!=0&&(Integer)map.get("userid")!=-1) {
            List list = mapper.tuijianuser((Integer) map.get("userid"));
            List list1 = new ArrayList();
            if(list.size()!=0) {
                 list1 = mapper.tuijianuser1(list);
            }
            else{
                List list3 = mapper.userhotlist();
                for(int i=0;i<list3.size();i++){
                    HashMap map1= (HashMap) list3.get(i);
                    map1.put("shenfen",getmyshenfen3((Integer)map1.get("userid")));
                    HashMap  userinfo= new HashMap();
                    userinfo = mapper.getheadborder((Integer)map1.get("userid"));
                    String url1="";
                    HashMap pianyi = new HashMap();
                    if(userinfo!=null) {
                        url1=userinfo.get("url").toString();
                        pianyi=mapper.getpianyi(url1);
                    }
                    map1.put("headborder",url1);
                    map1.put("pianyi",pianyi);
                    list3.set(i,map1);
                }
                return list3;
            }
            for (int i = 0; i < list1.size(); i++) {
                HashMap map2 = (HashMap) list1.get(i);
                if (map2.get("userid") == map.get("userid")) {
                    list1.remove(i);
                    break;
                }
            }
            int sub = 5 - list1.size();
            if (sub != 0) {
                List list2 = mapper.tuijianuser2(list);
                for (int i = 0; i < sub; i++) {
                    list1.add(list2.get(i));
                }
            }
            for (int i = 0; i < list1.size(); i++) {
                HashMap map2 = (HashMap) list1.get(i);
                if (map2.get("userid") == map.get("userid")) {
                    list1.remove(i);
                    break;
                }
            }
            for(int i=0;i<list1.size();i++){
                HashMap map1= (HashMap) list1.get(i);
                map1.put("shenfen",getmyshenfen3((Integer)map1.get("userid")));
                HashMap  userinfo= new HashMap();
                userinfo = mapper.getheadborder((Integer)map1.get("userid"));
                String url1="";
                HashMap pianyi = new HashMap();
                if(userinfo!=null) {
                    url1=userinfo.get("url").toString();
                    pianyi=mapper.getpianyi(url1);
                }
                map1.put("headborder",url1);
                map1.put("pianyi",pianyi);
                list1.set(i,map1);
            }
            return list1;
        }else{
            List list1 = mapper.userhotlist();
            for(int i=0;i<list1.size();i++){
                HashMap map1= (HashMap) list1.get(i);
                map1.put("shenfen",getmyshenfen3((Integer)map1.get("userid")));

                HashMap userinfo = mapper.getheadborder((Integer)map1.get("userid"));
                String url1="";
                HashMap pianyi = new HashMap();
                if(userinfo!=null) {
                    url1=userinfo.get("url").toString();
                    pianyi=mapper.getpianyi(url1);
                }
                map1.put("headborder",url1);
                map1.put("pianyi",pianyi);
                list1.set(i,map1);
            }
            return list1;
        }
    }
    @PostMapping("/api/getmysavelist")
    @ResponseBody
    @CrossOrigin
    public HashMap getmysavelist(@RequestBody HashMap map){
        HashMap map1 = new HashMap();
        if((Integer)map.get("userid")==mapper.getuserbycookie((String)map.get("cookie")).get("userid")){
            map1.put("code",1001);
            map.put("page",((int)map.get("savepage"))*5);
            List list = mapper.getmysavelist(map);
            for(int i=0;i<list.size();i++){
                HashMap temp=(HashMap) list.get(i);
                HashMap  userinfo= new HashMap();
                userinfo = mapper.getheadborder((Integer)temp.get("userid"));
                String url1="";
                HashMap pianyi = new HashMap();
                if(userinfo!=null) {
                    url1=userinfo.get("url").toString();
                    pianyi=mapper.getpianyi(url1);
                }
                temp.put("headborder",url1);
                temp.put("pianyi",pianyi);
                temp.put("userheadimage",j.jiami1(mapper.getuserheadimage((Integer)temp.get("userid"))));
                temp.put("username",mapper.getusername((Integer)temp.get("userid")));

                temp.put("admin",mapper.getadminbyuserid((Integer)temp.get("userid")));
                String text=(String)temp.get("text");
                String image="";
                List imagelistt = getImgSrc(text);
                for(int a=0;a<imagelistt.size();a++){
                    int x=text.indexOf((String)imagelistt.get(a));
                    int y=((String)imagelistt.get(a)).length();
                    text=text.substring(0,x+y)+j.jiami1((String)imagelistt.get(a))+text.substring(x+y+1,text.length());
                    image=image+j.jiami1((String)imagelistt.get(a))+";";
                }
                temp.put("text",text);
                temp.put("image",image);
                List list1 = getmyshenfen3((Integer)temp.get("userid"));
                String str="";
                for(int n=0;n<list1.size();n++){
                    HashMap temp1=(HashMap)list1.get(n);
                    str+=temp1.get("name");
                    if(n<list1.size()-1)
                        str+="、";
                }
                temp.put("shenfen",str);
            }
            map1.put("data",list);
        }
        else
        map1.put("code",1003);
        return map1;
    }
    @ResponseBody
    @PostMapping("/api/getmylikelist")
    @CrossOrigin
    public HashMap getmylikelist(@RequestBody HashMap map){
        HashMap map1 = new HashMap();
        if((Integer)map.get("userid")==mapper.getuserbycookie((String)map.get("cookie")).get("userid")){
            map1.put("code",1001);
            map.put("page",((int)map.get("likepage"))*5);
            List list = mapper.getmylikelist(map);
            for(int i=0;i<list.size();i++){
                HashMap temp=(HashMap) list.get(i);
                HashMap  userinfo= new HashMap();
                userinfo = mapper.getheadborder((Integer)temp.get("userid"));
                String url1="";
                HashMap pianyi = new HashMap();
                if(userinfo!=null) {
                    url1=userinfo.get("url").toString();
                    pianyi=mapper.getpianyi(url1);
                }
                temp.put("headborder",url1);
                temp.put("pianyi",pianyi);
                temp.put("userheadimage",j.jiami1(mapper.getuserheadimage((Integer)temp.get("userid"))));
                temp.put("username",mapper.getusername((Integer)temp.get("userid")));
                temp.put("admin",mapper.getadminbyuserid((Integer)temp.get("userid")));
                String text=(String)temp.get("text");
                String image="";
                List imagelistt = getImgSrc(text);
                for(int a=0;a<imagelistt.size();a++){
                    int x=text.indexOf((String)imagelistt.get(a));
                    int y=((String)imagelistt.get(a)).length();
                    text=text.substring(0,x+y)+j.jiami1((String)imagelistt.get(a))+text.substring(x+y+1,text.length());
                    image=image+j.jiami1((String)imagelistt.get(a))+";";
                }
                temp.put("text",text);
                temp.put("image",image);
                List list1 = getmyshenfen3((Integer)temp.get("userid"));
                String str="";
                for(int n=0;n<list1.size();n++){
                    HashMap temp1=(HashMap)list1.get(n);
                    str+=temp1.get("name");
                    if(n<list1.size()-1)
                        str+="、";
                }
                temp.put("shenfen",str);
            }
            map1.put("data",list);
        }
        else
            map1.put("code",1003);
        return map1;
    }
    @PostMapping("/api/gethottag")
    @ResponseBody
    @CrossOrigin
    public HashMap gethottag(@RequestBody HashMap map){
        HashMap map1=new HashMap();
        map1.put("code",1001);
        List list = mapper.hottag();
        List returnlist=new ArrayList();
        for(int i=0;i<list.size();i++){
            List list1 = mapper.getthemelistby_tag1((String)list.get(i));

            Integer num=0;
            Integer num1=0;
            String imagesrc="";
            String title="";
            HashMap map2= new HashMap();
            for(int j=0;j<list1.size();j++){
                HashMap temp =(HashMap)list1.get(j);
                if(temp.get("image")!=null&&temp.get("image").toString().length()>10&&temp.get("image")!=""&&imagesrc==""){
                    String imagesrc1=(String)temp.get("image");
                    imagesrc=imagesrc1.split(";")[0];
                }
                if(temp.get("title")!=""&&title=="")
                    title=(String)temp.get("title");
                num+=(Integer)temp.get("look");
                num1+=(Integer)temp.get("num");
            }
            map2.put("tagname",list.get(i));
            map2.put("imagesrc",imagesrc);
            map2.put("look",num);
            map2.put("num",num1);
            map2.put("title",title);
            returnlist.add(map2);
        }
        map1.put("data",returnlist);
        return map1;
    }
    @PostMapping("/api/gethemelistby_tagname")
    @ResponseBody
    @CrossOrigin
    public HashMap getthemelistby_tag(@RequestBody HashMap map) throws IOException {
        HashMap map1 = new HashMap();
        HashMap map2 = new HashMap();
        HashMap map3 = new HashMap();
        map1.put("code",1001);
        List list1 = new ArrayList();
        {
            //list = mapper.getthemelist();
            SearchSourceBuilder builder = new SearchSourceBuilder();
            //条件搜索
            BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
            boolQueryBuilder.must(QueryBuilders.termQuery("show1", 1));
            boolQueryBuilder.must(QueryBuilders.matchQuery("tags", map.get("tagname").toString()));

            builder.query(boolQueryBuilder);
            //结果集合分页
            builder.from((Integer) map.get("page")*5).size(5);
            //排序
            builder.sort("hot", SortOrder.DESC);
            //搜索
            SearchRequest searchRequest = new SearchRequest();
            searchRequest.indices("theme");
            searchRequest.source(builder);
            // 执行请求
            SearchResponse response = client1.search(searchRequest, RequestOptions.DEFAULT);
            SearchHits hits = response.getHits();
            SearchHit[] searchHits = hits.getHits();
            for (SearchHit hit : searchHits) {
                Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                sourceAsMap.put("userheadimage", j.jiami1(mapper.getuserheadimage((Integer) sourceAsMap.get("userid"))));
                sourceAsMap.put("username", mapper.getusername((Integer) sourceAsMap.get("userid")));
                HashMap  userinfo= new HashMap();
                userinfo = mapper.getheadborder((Integer)sourceAsMap.get("userid"));
                String url1="";
                HashMap pianyi = new HashMap();
                if(userinfo!=null) {
                    url1=userinfo.get("url").toString();
                    pianyi=mapper.getpianyi(url1);
                }

                sourceAsMap.put("headborder",url1);
                sourceAsMap.put("pianyi",pianyi);
                String text = (String) sourceAsMap.get("text");
                String image = "";
                List imagelistt = getImgSrc(text);
                for (int a = 0; a < imagelistt.size(); a++) {
                    int x = text.indexOf((String) imagelistt.get(a));
                    int y = ((String) imagelistt.get(a)).length();
                    text = text.substring(0, x + y) + j.jiami1((String) imagelistt.get(a)) + text.substring(x + y + 1, text.length());
                    if(a<3)
                    image = image + j.jiami1((String) imagelistt.get(a)) + ";";

                }
                if(map.get("app")==null){
                    sourceAsMap.put("text", text);
                    sourceAsMap.put("data","");
                }
                else{
                    sourceAsMap.put("text","");
                    if(sourceAsMap.get("data").toString().length()>200){
                        sourceAsMap.put("data",sourceAsMap.get("data").toString().substring(0,200));
                    }
                }
                sourceAsMap.put("image", image);
                List list2 = getmyshenfen3((Integer) sourceAsMap.get("userid"));
                String str = "";
                for (int n = 0; n < list2.size(); n++) {
                    HashMap temp1 = (HashMap) list2.get(n);
                    str += temp1.get("name");
                    if (n < list2.size() - 1)
                        str += "、";
                }
                sourceAsMap.put("shenfen", str);
                list1.add(sourceAsMap);
            }
        }

        map2.put("themelist",list1);
        Integer num=0;
        Integer num1=0;
        String imagesrc="";
        String title="";
        for(int j=0;j<list1.size();j++){
            HashMap temp =(HashMap)list1.get(j);
            if(temp.get("image")!=""&&imagesrc==""){
                String imagesrc1=(String)temp.get("image");
                imagesrc=imagesrc1.split(";")[0];
            }
            if(temp.get("title")!=""&&title=="")
                title=(String)temp.get("title");
            num+=(Integer)temp.get("look");
            num1+=(Integer)temp.get("num");
        }
        map3.put("tagname",map.get("tagname"));
        map3.put("imagesrc",imagesrc);
        map3.put("look",num);
        map3.put("num",num1);
        map3.put("title",title);
        map2.put("info",map3);
        map1.put("data",map2);
        return map1;
    }
    @Scheduled(cron = "0 0/10 * ? * ?")
    public void updateredis(){
        Set set = redisTemplate.opsForSet().members("searchlist");
        List list = Arrays.asList(set.toArray());
        for(int i=0;i<list.size();i++){
            String key=list.get(i).toString();
            Long searchtime=Long.valueOf(redisTemplate.opsForValue().get(list.get(i).toString()).toString());
            Long time=System.currentTimeMillis();
            if(searchtime<time){
                Long score=redisTemplate.opsForZSet().score("search",key).longValue()-1;
                if(score<=0){
                    redisTemplate.opsForZSet().remove("search",key);
                    redisTemplate.delete(key);
                    redisTemplate.opsForSet().remove("searchlist",key);
                }else
                redisTemplate.opsForZSet().add("search",key,score);
            }
        }
    }
    @PostMapping("/api/hotsearch")
    @ResponseBody
    @CrossOrigin
    public HashMap hotsearch(@RequestBody HashMap map){
        HashMap returnmap = new HashMap();
        Set zSet = redisTemplate.opsForZSet().reverseRange("search",0,9);
        List list = Arrays.asList(zSet.toArray());
        returnmap.put("data",list);
        returnmap.put("code",1001);
        return returnmap;
    }
    @PostMapping("/api/search")
    @ResponseBody
    @CrossOrigin
    public HashMap search(@RequestBody HashMap map) throws IOException {

        HashMap map1 = new HashMap();
        HashMap map2 = new HashMap();
        map1.put("code",1001);
        if(map.get("key").equals(""))return map1;
        List list = mapper.searchuser((String)map.get("key"));
        String key=(String)map.get("key");
        {
            ZSetOperations<String,String> zset= redisTemplate.opsForZSet();
            Long time = System.currentTimeMillis();
            if(!redisTemplate.opsForSet().isMember("searchlist",key)){
                redisTemplate.opsForSet().add("searchlist",key);
                redisTemplate.opsForValue().set(key,time+1000000000);
                redisTemplate.opsForZSet().add("search",key,1);
            }else{
                Long score=zset.score("search",key).longValue()+1;
                redisTemplate.opsForZSet().add("search",key,score);
                redisTemplate.opsForValue().set(key,time+1000000000);
            }
        }
        for(int i=0;i<list.size();i++){
            HashMap map3 =(HashMap) list.get(i);

            map3.put("userheadimage",j.jiami1(map3.get("userheadimage").toString()));
            list.set(i,map3);
        }
        List list1 = new ArrayList();
        {
            //list = mapper.getthemelist();
            SearchSourceBuilder builder = new SearchSourceBuilder();
            //设置高亮渲染
            String preTag = "<span style='color:red;font-weight:bold'>";
            String postTag = "</span>";
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.preTags(preTag);//设置前缀
            highlightBuilder.postTags(postTag);//设置后缀
            highlightBuilder.field("*");//设置高亮字段
            highlightBuilder.requireFieldMatch(false);
            highlightBuilder.numOfFragments(0);
            //highlightBuilder.field("text");//设置高亮字段
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

            //must 查询条件

            TermQueryBuilder mustBool = QueryBuilders.termQuery("show1",1);

            //must_not 查询条件
            //should 查询条件
            MultiMatchQueryBuilder shouldBool = QueryBuilders.multiMatchQuery(
                    map.get("key").toString(),"text","title","tags"
            );

            boolQueryBuilder.must(mustBool).must(shouldBool);

            builder.query(boolQueryBuilder);
            //结果集合分页
            builder.from((Integer) map.get("limit")*10).size(10);
            builder.highlighter(highlightBuilder);

            //排序
            //builder.sort("replytime", SortOrder.DESC);
            builder.explain(false);
            //搜索
            SearchRequest searchRequest = new SearchRequest();
            searchRequest.indices("theme");
            searchRequest.source(builder);
            // 执行请求
            SearchResponse response = client1.search(searchRequest, RequestOptions.DEFAULT);
            SearchHits hits = response.getHits();
            SearchHit[] searchHits = hits.getHits();
            for (SearchHit hit : searchHits) {
                Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                //System.out.println(hit.getHighlightFields());
                if(hit.getHighlightFields().get("text")!=null){
                    String text=Arrays.toString(hit.getHighlightFields().get("text").getFragments());
                    text=text.substring(1,text.length()-1);
                    sourceAsMap.put("text",text);

                    //System.out.println(Arrays.toString(hit.getHighlightFields().get("text").getFragments()));
                }
                if(hit.getHighlightFields().get("title")!=null){
                    String title=Arrays.toString(hit.getHighlightFields().get("title").getFragments());
                    title=title.substring(1,title.length()-1);
                    sourceAsMap.put("title",title);

                }
                /*if(hit.getHighlightFields().get("data")!=null){
                    String data=Arrays.toString(hit.getHighlightFields().get("data").getFragments());
                    data=data.substring(1,data.length()-1);
                    if(data.length()>100){
                        data=data.substring(0,100)+"...";
                    }
                    sourceAsMap.put("data",data);

                }*/
                sourceAsMap.put("userheadimage", j.jiami1(mapper.getuserheadimage((Integer) sourceAsMap.get("userid"))));
                sourceAsMap.put("username", mapper.getusername((Integer) sourceAsMap.get("userid")));
                String text = (String) sourceAsMap.get("text");
                String image = "";
                List imagelistt = getImgSrc(text);
                for (int a = 0; a < imagelistt.size(); a++) {
                    int x = text.indexOf((String) imagelistt.get(a));
                    int y = ((String) imagelistt.get(a)).length();
                    text = text.substring(0, x + y) + j.jiami1((String) imagelistt.get(a)) + text.substring(x + y + 1, text.length());
                    if(a<3)
                    image = image + j.jiami1((String) imagelistt.get(a)) + ";";

                }
                if(map.get("app")==null){
                    sourceAsMap.put("text", text);
                    sourceAsMap.put("data","");
                }
                else{
                    sourceAsMap.put("text","");
                    if(sourceAsMap.get("data").toString().length()>200){
                        sourceAsMap.put("data",sourceAsMap.get("data").toString().substring(0,200));
                    }
                }
                sourceAsMap.put("image", image);
                HashMap  userinfo= new HashMap();
                userinfo = mapper.getheadborder((Integer)sourceAsMap.get("userid"));
                String url1="";
                HashMap pianyi = new HashMap();
                if(userinfo!=null) {
                    url1=userinfo.get("url").toString();
                    pianyi=mapper.getpianyi(url1);
                }
                sourceAsMap.put("headborder",url1);
                sourceAsMap.put("pianyi",pianyi);
                List list2 = getmyshenfen3((Integer) sourceAsMap.get("userid"));
                String str = "";
                for (int n = 0; n < list2.size(); n++) {
                    HashMap temp1 = (HashMap) list2.get(n);
                    str += temp1.get("name");
                    if (n < list2.size() - 1)
                        str += "、";
                }
                sourceAsMap.put("shenfen", str);

                list1.add(sourceAsMap);
            }
        }

        map2.put("user",list);map2.put("theme",list1);
        map1.put("data",map2);
        return map1;
    }
    @PostMapping("/api/searchmoretheme")
    @ResponseBody
    @CrossOrigin
    public HashMap searchmoretheme(@RequestBody HashMap map) throws IOException {
        List list1 = new ArrayList();
        {
            //list = mapper.getthemelist();
            SearchSourceBuilder builder = new SearchSourceBuilder();
            //设置高亮渲染
            String preTag = "<span style='color:red;font-weight:bold'>";
            String postTag = "</span>";
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.preTags(preTag);//设置前缀
            highlightBuilder.postTags(postTag);//设置后缀
            highlightBuilder.field("*");//设置高亮字段
            highlightBuilder.requireFieldMatch(false);
            highlightBuilder.numOfFragments(0);
            //highlightBuilder.field("text");//设置高亮字段
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

            //must 查询条件

            TermQueryBuilder mustBool = QueryBuilders.termQuery("show1",1);

            //must_not 查询条件
            //should 查询条件
            MultiMatchQueryBuilder shouldBool = QueryBuilders.multiMatchQuery(
                    map.get("key").toString(),"text","title","tags"
            );

            boolQueryBuilder.must(mustBool).must(shouldBool);

            builder.query(boolQueryBuilder);
            //结果集合分页
            builder.from((Integer) map.get("limit")*10).size(10);
            builder.highlighter(highlightBuilder);

            //排序
            //builder.sort("replytime", SortOrder.DESC);
            builder.explain(false);
            //搜索
            SearchRequest searchRequest = new SearchRequest();
            searchRequest.indices("theme");
            searchRequest.source(builder);
            // 执行请求
            SearchResponse response = client1.search(searchRequest, RequestOptions.DEFAULT);
            SearchHits hits = response.getHits();
            SearchHit[] searchHits = hits.getHits();
            for (SearchHit hit : searchHits) {
                Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                //System.out.println(hit.getHighlightFields());
                if(hit.getHighlightFields().get("text")!=null){
                    String text=Arrays.toString(hit.getHighlightFields().get("text").getFragments());
                    text=text.substring(1,text.length()-1);
                    sourceAsMap.put("text",text);

                    //System.out.println(Arrays.toString(hit.getHighlightFields().get("text").getFragments()));
                }
                if(hit.getHighlightFields().get("title")!=null){
                    String title=Arrays.toString(hit.getHighlightFields().get("title").getFragments());
                    title=title.substring(1,title.length()-1);
                    sourceAsMap.put("title",title);

                }

                sourceAsMap.put("userheadimage", j.jiami1(mapper.getuserheadimage((Integer) sourceAsMap.get("userid"))));
                sourceAsMap.put("username", mapper.getusername((Integer) sourceAsMap.get("userid")));
                String text = (String) sourceAsMap.get("text");
                String image = "";
                List imagelistt = getImgSrc(text);
                for (int a = 0; a < imagelistt.size(); a++) {
                    int x = text.indexOf((String) imagelistt.get(a));
                    int y = ((String) imagelistt.get(a)).length();
                    text = text.substring(0, x + y) + j.jiami1((String) imagelistt.get(a)) + text.substring(x + y + 1, text.length());
                    if(a<3)
                    image = image + j.jiami1((String) imagelistt.get(a)) + ";";

                }
                sourceAsMap.put("text", text);
                sourceAsMap.put("image", image);
                HashMap  userinfo= new HashMap();
                userinfo = mapper.getheadborder((Integer)sourceAsMap.get("userid"));
                String url1="";
                HashMap pianyi = new HashMap();
                if(userinfo!=null) {
                    url1=userinfo.get("url").toString();
                    pianyi=mapper.getpianyi(url1);
                }
                sourceAsMap.put("headborder",url1);
                sourceAsMap.put("pianyi",pianyi);
                List list2 = getmyshenfen3((Integer) sourceAsMap.get("userid"));
                String str = "";
                for (int n = 0; n < list2.size(); n++) {
                    HashMap temp1 = (HashMap) list2.get(n);
                    str += temp1.get("name");
                    if (n < list2.size() - 1)
                        str += "、";
                }
                sourceAsMap.put("shenfen", str);

                list1.add(sourceAsMap);
            }
        }
        HashMap map1 = new HashMap();
        map1.put("code",1001);
        map1.put("data",list1);
        return map1;
    }
    @PostMapping("/api/getusermessage")
    @ResponseBody
    @CrossOrigin
    public HashMap getusermessage(@RequestBody HashMap map){
        HashMap map1 = new HashMap();
        List temp =mapper.getusermessage((String)map.get("userid"),(String)map.get("username"),(String)map.get("email"));
        for(int i=0;i<temp.size();i++){
            HashMap temp1 = (HashMap) temp.get(i);
            List list1 = mapper.getuserheadborder1((Integer)temp1.get("userid"));
            List list2 = mapper.getuserheadborder2((Integer)temp1.get("userid"));
            HashMap userheadborder=new HashMap();
            userheadborder.put("list1",list1);
            userheadborder.put("list2",list2);
            temp1.put("userheadborderlist",userheadborder);
            temp.set(i,temp1);
        }
        map1.put("data",temp);
        return map1;
    }
    @PostMapping("/api/deleteuserheadborder")
    @ResponseBody
    @CrossOrigin
    public HashMap deleteuserheadborder(@RequestBody HashMap map){
        mapper.deleteuserheadborder(map);
        HashMap map1 = new HashMap();
        map1.put("code",1001);
        return map1;
    }
    @PostMapping("/api/adduserheadborder")
    @ResponseBody
    @CrossOrigin
    public HashMap adduserheadborder(@RequestBody HashMap map){
        mapper.adduserheadborder(map);
        HashMap map1 = new HashMap();
        map1.put("code",1001);
        return map1;
    }
    @PostMapping("/api/changeusermessage")
    @ResponseBody
    @CrossOrigin
    public HashMap changeusermessage(@RequestBody HashMap map){
        if(!"".equals((String)map.get("password"))){
            mapper.changepassword1(map);
        }
        mapper.changemessage(map);
        HashMap map1 = new HashMap();
        map1.put("code",1001);
        return map1;
    }
    @PostMapping("/api/ban")
    @ResponseBody
    @CrossOrigin
    public HashMap ban(@RequestBody HashMap map){
        mapper.ban(map);
        HashMap map1 = new HashMap();
        map1.put("code",1001);
        return map1;
    }
    @PostMapping("/api/getfenleis")
    @ResponseBody
    @CrossOrigin
    public List getfenleis(@RequestBody HashMap map){
        return mapper.getfenleis();
    }
    @PostMapping("/api/newfenlei")
    @ResponseBody
    @CrossOrigin
    public HashMap newfenlei(@RequestBody HashMap map){
        mapper.newfenlei(map);
        HashMap map1 = new HashMap();
        map1.put("code",1001);
        return map1;
    }
    @PostMapping("/api/deletefenlei")
    @ResponseBody
    @CrossOrigin
    public HashMap deletefenlei(@RequestBody HashMap map){
        mapper.setfenleimoren(map);
        mapper.deletefenlei(map);
        HashMap map1 = new HashMap();
        map1.put("code",1001);
        return map1;
    }
    @PostMapping("/api/searchtheme_admin")
    @ResponseBody
    @CrossOrigin
    public HashMap searchtheme_admin(@RequestBody HashMap map){
        List list = mapper.searchtheme_admin(map);
        HashMap map1 = new HashMap();
        map1.put("data",list);
        map1.put("code",1001);
        map1.put("num",mapper.searchtheme_admin_num(map));
        return map1;
    }
    @PostMapping("/api/deletetheme_admin")
    @ResponseBody
    @CrossOrigin
    public HashMap deletetheme_admin(@RequestBody HashMap map) throws IOException {
        mapper.deletetheme_admin(map);
        UpdateRequest request1 = new UpdateRequest();
        {
            request1.index("theme").id(map.get("themeid").toString());
            // 拓展：局部更新也可以这样写：
            request1.doc(XContentType.JSON, "show1",0);
            // 3、发送请求到ES
            UpdateResponse response1 = client1.update(request1, RequestOptions.DEFAULT);
        }
        HashMap map1 = new HashMap();
        map1.put("code",1001);
        return map1;
    }
    @PostMapping("/api/huifutheme_admin")
    @ResponseBody
    @CrossOrigin
    public HashMap huifutheme_admin(@RequestBody HashMap map) throws IOException {

        mapper.huifutheme_admin(map);
        UpdateRequest request1 = new UpdateRequest();
        {
            request1.index("theme").id(map.get("themeid").toString());
            // 拓展：局部更新也可以这样写：
            request1.doc(XContentType.JSON, "show1",1);
            // 3、发送请求到ES
            UpdateResponse response1 = client1.update(request1, RequestOptions.DEFAULT);
        }
        HashMap map1 = new HashMap();
        map1.put("code",1001);
        return map1;
    }
    @PostMapping("/api/deletethemeall_admin")
    @ResponseBody
    @CrossOrigin
    public HashMap deletethemeall_admin(@RequestBody HashMap map) throws IOException {
        List list = (List)map.get("themelist");
        for(int i=0;i<list.size();i++) {
            HashMap temp = new HashMap();
            temp.put("themeid",list.get(i));
            mapper.deletetheme_admin(temp);
            UpdateRequest request1 = new UpdateRequest();
            {
                request1.index("theme").id(temp.get("themeid").toString());
                // 拓展：局部更新也可以这样写：
                request1.doc(XContentType.JSON, "show1",0);
                // 3、发送请求到ES
                UpdateResponse response1 = client1.update(request1, RequestOptions.DEFAULT);
            }
        }
        HashMap map1 = new HashMap();
        map1.put("code",1001);
        return map1;
    }
    @PostMapping("/api/huifuthemeall_admin")
    @ResponseBody
    @CrossOrigin
    public HashMap huifutheme_adminall(@RequestBody HashMap map) throws IOException {
        List list = (List)map.get("themelist");
        for(int i=0;i<list.size();i++) {
            HashMap temp = new HashMap();
            temp.put("themeid",list.get(i));
            mapper.huifutheme_admin(temp);
            UpdateRequest request1 = new UpdateRequest();
            {
                request1.index("theme").id(temp.get("themeid").toString());
                // 拓展：局部更新也可以这样写：
                request1.doc(XContentType.JSON, "show1",1);
                // 3、发送请求到ES
                UpdateResponse response1 = client1.update(request1, RequestOptions.DEFAULT);
            }
        }
        HashMap map1 = new HashMap();
        map1.put("code",1001);
        return map1;
    }
    @PostMapping("/api/deletetop_admin")
    @ResponseBody
    @CrossOrigin
    public HashMap deletetop_admin(@RequestBody HashMap map) throws IOException {
        List list = (List)map.get("themelist");
        for(int i=0;i<list.size();i++) {
            HashMap temp = new HashMap();
            temp.put("themeid",list.get(i));
            mapper.deletetop_admin(temp);
            UpdateRequest request1 = new UpdateRequest();
            {
                request1.index("theme").id(temp.get("themeid").toString());
                // 拓展：局部更新也可以这样写：
                request1.doc(XContentType.JSON, "top",0);
                // 3、发送请求到ES
                UpdateResponse response1 = client1.update(request1, RequestOptions.DEFAULT);
            }
        }
        HashMap map1 = new HashMap();
        map1.put("code",1001);
        return map1;
    }
    @PostMapping("/api/settop_admin")
    @ResponseBody
    @CrossOrigin
    public HashMap settop_admin(@RequestBody HashMap map) throws IOException {
        List list = (List)map.get("themelist");
        for(int i=0;i<list.size();i++) {
            HashMap temp = new HashMap();
            temp.put("themeid",list.get(i));
            UpdateRequest request1 = new UpdateRequest();
            {
                request1.index("theme").id(temp.get("themeid").toString());
                // 拓展：局部更新也可以这样写：
                request1.doc(XContentType.JSON, "top",1);
                // 3、发送请求到ES
                UpdateResponse response1 = client1.update(request1, RequestOptions.DEFAULT);
            }
            mapper.settop_admin(temp);
        }
        HashMap map1 = new HashMap();
        map1.put("code",1001);
        return map1;
    }
    @PostMapping("/api/deletejing_admin")
    @ResponseBody
    @CrossOrigin
    public HashMap deletejing_admin(@RequestBody HashMap map) throws IOException {
        List list = (List)map.get("themelist");
        for(int i=0;i<list.size();i++) {
            HashMap temp = new HashMap();
            temp.put("themeid",list.get(i));
            mapper.deletejing_admin(temp);
            UpdateRequest request1 = new UpdateRequest();
            {
                request1.index("theme").id(temp.get("themeid").toString());
                // 拓展：局部更新也可以这样写：
                request1.doc(XContentType.JSON, "jing",0);
                // 3、发送请求到ES
                UpdateResponse response1 = client1.update(request1, RequestOptions.DEFAULT);
            }
        }
        HashMap map1 = new HashMap();
        map1.put("code",1001);
        return map1;
    }
    @PostMapping("/api/setjing_admin")
    @ResponseBody
    @CrossOrigin
    public HashMap setjing_admin(@RequestBody HashMap map) throws IOException {
        List list = (List)map.get("themelist");
        for(int i=0;i<list.size();i++) {
            HashMap temp = new HashMap();
            temp.put("themeid",list.get(i));

            mapper.setjing_admin(temp);
            UpdateRequest request1 = new UpdateRequest();
            {
                request1.index("theme").id(temp.get("themeid").toString());
                // 拓展：局部更新也可以这样写：
                request1.doc(XContentType.JSON, "jing",1);
                // 3、发送请求到ES
                UpdateResponse response1 = client1.update(request1, RequestOptions.DEFAULT);
            }
        }
        HashMap map1 = new HashMap();
        map1.put("code",1001);
        return map1;
    }
    @PostMapping("/api/getmessage")
    @ResponseBody
    @CrossOrigin
    public List getmessage(@RequestBody HashMap map){
        List list=mapper.getmessage();
        return list;
    }
    @PostMapping("/api/getreply_admin")
    @ResponseBody
    @CrossOrigin
    public HashMap getreply_admin(@RequestBody HashMap map){
       HashMap map1 = new HashMap();
       map1.put("code",1001);
       map1.put("data",mapper.getreply_admin(map));
       map1.put("num",mapper.getreply_admin_num(map));
       return map1;
    }
    @PostMapping("/api/getloucengreply_admin")
    @ResponseBody
    @CrossOrigin
    public HashMap getloucengreply_admin(@RequestBody HashMap map){
        HashMap map1 = new HashMap();
        map1.put("code",1001);
        map1.put("data",mapper.getreplylouceng_admin(map));
        map1.put("num",mapper.getreplylouceng_admin_num(map));
        return map1;
    }
    @PostMapping("/api/deletereply_admin")
    @ResponseBody
    @CrossOrigin
    public HashMap deletereply_admin(@RequestBody HashMap map){
        mapper.deletereply_admin(map);
        HashMap map1 = new HashMap();
        map1.put("code",1001);
        return map1;
    }
    @PostMapping("/api/huifureply_admin")
    @ResponseBody
    @CrossOrigin
    public HashMap huifureply_admin(@RequestBody HashMap map){
        mapper.huifureply_admin(map);
        HashMap map1 = new HashMap();
        map1.put("code",1001);
        return map1;
    }
    @PostMapping("/api/deletloucengereply_admin")
    @ResponseBody
    @CrossOrigin
    public HashMap deletloucengereply_admin(@RequestBody HashMap map){
        mapper.deletloucengereply_admin(map);
        HashMap map1 = new HashMap();
        map1.put("code",1001);
        return map1;
    }
    @PostMapping("/api/huifuloucengreply_admin")
    @ResponseBody
    @CrossOrigin
    public HashMap huifuloucengreply_admin(@RequestBody HashMap map){
        mapper.huifuloucengreply_admin(map);
        HashMap map1 = new HashMap();
        map1.put("code",1001);
        return map1;
    }
    @PostMapping("/api/deletereplyall_admin")
    @ResponseBody
    @CrossOrigin
    public HashMap deletereplyall_admin(@RequestBody HashMap map){
        List list = (List)map.get("themelist");
        for(int i=0;i<list.size();i++) {
            HashMap temp = new HashMap();
            temp.put("id",list.get(i));
            mapper.deletereply_admin(temp);
        }
        HashMap map1 = new HashMap();
        map1.put("code",1001);
        return map1;
    }
    @PostMapping("/api/huifureplyall_admin")
    @ResponseBody
    @CrossOrigin
    public HashMap huifureplyall_admin(@RequestBody HashMap map){
        List list = (List)map.get("themelist");
        for(int i=0;i<list.size();i++) {
            HashMap temp = new HashMap();
            temp.put("id",list.get(i));
            mapper.huifureply_admin(temp);
        }
        HashMap map1 = new HashMap();
        map1.put("code",1001);
        return map1;
    }
    @PostMapping("/api/jugeadmin")
    @ResponseBody
    @CrossOrigin
    public HashMap jugeadmin(@RequestBody HashMap map){
        HashMap map1 = new HashMap();
        if(mapper.jugeadmin((String)map.get("cookie"))==1)
            map1.put("code",1001);
        else map1.put("code",1002);
        return map1;
    }
    @PostMapping("/api/getStatistics")
    @ResponseBody
    @CrossOrigin
    public HashMap getStatistics(@RequestBody HashMap map){
        HashMap map1 = new HashMap();
        Long current=System.currentTimeMillis();
        SimpleDateFormat formatter = new SimpleDateFormat("MM-dd");
        String millisecondStrings = formatter.format(current);
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        Integer time=(int)(calendar.getTimeInMillis()/1000);
        Long current2=System.currentTimeMillis();
        String millisecondStrings2 = formatter.format(current2);
        map1.put("newregister",mapper.gettodayregister(time));
        map1.put("todayhotuser",mapper.gettodayhotuser(millisecondStrings2));
        map1.put("todaynewtheme",mapper.gettodaynewtheme(time));
        map1.put("todaynewreply",mapper.gettodaynewreply(time));
        map1.put("totleusernum",mapper.gettotleusernum());
        map1.put("totlethemenum",mapper.gettotlethemenum());
        map1.put("totlereplynum",mapper.gettotlereplynum());
        map1.put("jingnum",mapper.getjingnum());
        HashMap map2 = new HashMap();
        List newregister = new ArrayList();
        List hotuser = new ArrayList();
        List newtheme = new ArrayList();
        List newreply = new ArrayList();
        for(int i=6;i>=0;i--){
            newregister.add(mapper.gettodayregister(time-i*(24*60*60)));
            Long current1=System.currentTimeMillis();
            String millisecondStrings1 = formatter.format(current1-i*(24*60*60*1000));
            hotuser.add(mapper.gettodayhotuser(millisecondStrings1));
            newtheme.add(mapper.gettodaynewtheme(time-i*(24*60*60)));
            newreply.add(mapper.gettodaynewreply(time-i*(24*60*60)));
        }
        map2.put("weeknewregister",newregister);
        map2.put("weektodayhotuser",hotuser);
        map2.put("weektodaynewtheme",newtheme);
        map2.put("weektodaynewreply",newreply);
        List list = mapper.getfenleichart();
        List list1 = mapper.gethotagschart();
        HashMap map3 = new HashMap();
        map3.put("code",1001);
        map3.put("today",map1);
        map3.put("week",map2);
        map3.put("todaytime",time);
        map3.put("fenleichart",list);
        map3.put("hottagschart",list1);
        LocalDate date = LocalDate.now();
        Calendar calendar1 = Calendar.getInstance();
        calendar1.set(Calendar.HOUR_OF_DAY, 0);
        calendar1.set(Calendar.MINUTE, 0);
        calendar1.set(Calendar.SECOND, 0);
        calendar1.set(Calendar.MILLISECOND, 0);
        long timeInMillis = calendar1.getTimeInMillis();
        HashMap fangke = (HashMap) redisTemplate.opsForHash().entries(String.valueOf(timeInMillis));
        map3.put("todayfangke",fangke.size());
        return map3;
    }
    @PostMapping("/api/gethottagschart")
    @ResponseBody
    @CrossOrigin
    public HashMap gethottagschart(@RequestBody HashMap map){
        HashMap map3 = new HashMap();
        List list1 = mapper.gethotagschart();
        map3.put("code",1001);
        map3.put("hottagschart",list1);
        return map3;
    }
    @PostMapping("/api/getreplymessage")
    @ResponseBody
    @CrossOrigin
    public HashMap getreplymessage(@RequestBody HashMap map){
        map.put("limit",(Integer)map.get("limit")*10);
        List list = mapper.getreplymessage(map);
        for(int i=0;i<list.size();i++){
            HashMap temp =(HashMap) list.get(i);
            HashMap  userinfo= new HashMap();
            userinfo = mapper.getheadborder((Integer)temp.get("userid"));
            String url1="";
            HashMap pianyi = new HashMap();
            if(userinfo!=null) {
                url1=userinfo.get("url").toString();
                pianyi=mapper.getpianyi(url1);
            }
            temp.put("headborder",url1);
            temp.put("pianyi",pianyi);
        }
        HashMap map3 = new HashMap();
        map3.put("code",1001);
        map3.put("reply",list);
        return map3;
    }
    @PostMapping("/api/getreplymessage1")
    @ResponseBody
    @CrossOrigin
    public HashMap getreplymessage1(@RequestBody HashMap map){
        List list = mapper.getreplymessage(map);
        HashMap map3 = new HashMap();
        map3.put("code",1001);
        map3.put("reply",list);
        return map3;
    }
    @PostMapping("/api/getreplymessagenum")
    @ResponseBody
    @CrossOrigin
    public HashMap getreplymessagenum(@RequestBody HashMap map){
        Integer num = mapper.getreplymessagenum(map);
        HashMap map3 = new HashMap();
        map3.put("code",1001);
        map3.put("num",num);
        return map3;
    }
    @PostMapping("/api/changepassword")
    @ResponseBody
    @CrossOrigin
    public HashMap changepassword(@RequestBody HashMap map){
        HashMap map3 = new HashMap();
        List temp = mapper.searchpassword(map);
        if(temp.size()==0){
            map3.put("code",1002);
            map3.put("data","fail");
            return map3;
        }
        mapper.changepassword(map);
        map3.put("code",1001);
        map3.put("data","success");
        send.removeuserchat(JSONObject.toJSONString(map));
        return map3;
    }
    @PostMapping("/api/sendemail")
    @ResponseBody
    @CrossOrigin
    public HashMap sendemail(@RequestBody HashMap map){
        HashMap map3 = new HashMap();
        HashMap temp = mapper.getuserbyemail((String)map.get("email"));
        if(temp==null){
            map3.put("code",1002);
            map3.put("data","fail");
            return map3;
        }
        StringBuffer themecookie = new StringBuffer();
        Random random = new Random();
        String str1="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        for(int j=0;j<=5;j++) {
            int sub = random.nextInt(61);
            themecookie.append(str1.substring(sub, sub + 1));
        }
        HashMap temp1 = new HashMap();
        temp1.put("email",map.get("email"));
        temp1.put("findpassword",themecookie.toString());
        mapper.setfindpassword(temp1);
        email1(temp1);
        map3.put("code",1001);
        map3.put("data","success");
        return map3;
    }
    @PostMapping("/api/changepassword_byemail")
    @ResponseBody
    @CrossOrigin
    public HashMap changepassword_byemail(@RequestBody HashMap map){
        List temp = mapper.finduserby_findpassword(map);

        if(temp.size()==0){
            HashMap map1 = new HashMap();
            map1.put("code",1002);
            return map1;
        }
        HashMap map1 = new HashMap();
        map1.put("code",1001);
        mapper.changepassword_byemail(map);
        HashMap temp1 = mapper.getuseridbyemail(map);
        send.removeuserchat(JSONObject.toJSONString(temp1));
        return map1;
    }
    @PostMapping("/api/getall_systemmessage")
    @ResponseBody
    @CrossOrigin
    public List getall_systemmessage(@RequestBody HashMap map){
        List list = new ArrayList();
        if(mapper.jugeadmin((String)map.get("cookie"))==1){
            list = mapper.getall_systemmessage();
            return list;
        }
        else
            return list;
    }
    @PostMapping("/api/getmy_systemmessage")
    @ResponseBody
    @CrossOrigin
    public List getmy_systemmessage(@RequestBody HashMap map){
        List list = new ArrayList();
        map.put("page",Integer.valueOf(map.get("page").toString())*5);
        list = mapper.getmy_systemmessage(map);
        for(int i=0;i<list.size();i++){
            HashMap temp = new HashMap();
            temp=(HashMap) list.get(i);
            temp.put("systemname","学伴 WMU");
            list.set(i,temp);
        }
        return list;
    }
    @PostMapping("/api/replysystemmessage")
    @ResponseBody
    @CrossOrigin
    public HashMap replysystemmessage(@RequestBody HashMap map){
        HashMap map1 = new HashMap();
        if(mapper.jugeadmin((String)map.get("cookie"))==1){
            mapper.replysystemmessage(map);
            map1.put("code",1001);
            return map1;
        }
        else {
            map1.put("code",1002);
            return map1;
        }
    }
    @PostMapping("/api/deletesystemmessage")
    @ResponseBody
    @CrossOrigin
    public HashMap deletesystemmessage(@RequestBody HashMap map){
        HashMap map1 = new HashMap();
        if(mapper.jugeadmin((String)map.get("cookie"))==1){
            mapper.deletesystemmessage(map);
            map1.put("code",1001);
            return map1;
        }
        else {
            map1.put("code",1002);
            return map1;
        }
    }
    @PostMapping("/api/newsystemmessage")
    @ResponseBody
    @CrossOrigin
    public HashMap newsystemmessage(@RequestBody HashMap map){
        HashMap map1 = new HashMap();
        if(mapper.jugeadmin((String)map.get("cookie"))==1){
            map.put("createtime",(int)(new Date().getTime()/1000));
            map.put("systemname","学伴 WMU");
            map.put("systemheadimage","https://txtzz-1301452902.file.myqcloud.com/RqH32RQWblpBddrwujizvGJLy.jpeg");
            map.put("show1",1);
            map1.put("code",1001);
            mapper.newsystemmessage(map);
            return map1;
        }
        else {
            map1.put("code",1002);
            return map1;
        }
    }
    @PostMapping("/api/getmychat")
    @ResponseBody
    @CrossOrigin
    public List getmychat(@RequestBody HashMap map){
        List list = mapper.getmychatlist(map);
        List list1 = new ArrayList();
        List list2 = new ArrayList();
        for(int i=0;i<list.size();i++){
            HashMap map1 = (HashMap) list.get(i);
            Integer userid1 =(Integer)map1.get("a");
            Integer userid2 =(Integer)map1.get("b");
            if(userid1==map.get("userid")){
                if(!list1.contains(userid2)){
                    list1.add(userid2);
                }
            }
            else{
                if(!list1.contains(userid1)){
                    list1.add(userid1);
                }
            }
        }
        for(int i=0;i<list1.size();i++){
            HashMap map2 = mapper.getchatinfo((Integer)list1.get(i));
            HashMap map3 = mapper.getmychat((Integer)map.get("userid"),(Integer)list1.get(i));
            map2.put("time",map3.get("time"));
            map2.put("message",map3.get("message"));
            map2.put("image",map3.get("image"));
            list2.add(map2);
        }

        return list2;
    }
    @PostMapping("/api/getmychatlist")
    @ResponseBody
    @CrossOrigin
    public List getmychatlist(@RequestBody HashMap map){
        List list = mapper.getmychatlist(map);
        return list;
    }
    @PostMapping("/api/getusername")
    @ResponseBody
    @CrossOrigin
    public HashMap getusername(@RequestBody HashMap map){
        return mapper.getusername1((Integer)map.get("userid"));

    }
    @PostMapping("/api/getidentity")
    @ResponseBody
    @CrossOrigin
    public HashMap getidentity(@RequestBody HashMap map){
        List list = mapper.getfenleilistid();
        HashMap map1 = new HashMap();
        for(int i=0;i<list.size();i++){
            HashMap temp = (HashMap)list.get(i);
            List list1 = new ArrayList();
            list1=mapper.getidentity((Integer)temp.get("id"));
            map1.put(temp.get("id"),list1);
        }
        return map1;
    }
    @PostMapping("/api/newidentity")
    @ResponseBody
    @CrossOrigin
    public HashMap newidentity(@RequestBody HashMap map){
        HashMap map1 = new HashMap();
        mapper.newidentity(map);
        map1.put("code",1001);
        return map1;
    }
    @PostMapping("/api/deleteidentity")
    @ResponseBody
    @CrossOrigin
    public HashMap deleteidentity(@RequestBody HashMap map){
        HashMap map1 = new HashMap();
        mapper.deleteidentity(map);
        map1.put("code",1001);
        return map1;
    }
    @PostMapping("/api/getinterest")
    @ResponseBody
    @CrossOrigin
    public HashMap getinterest(@RequestBody HashMap map){
        List list = mapper.getfenleilistid();
        HashMap map1 = new HashMap();
        for(int i=0;i<list.size();i++){
            HashMap temp = (HashMap)list.get(i);
            List list1 = new ArrayList();
            list1=mapper.getinterest((Integer)temp.get("id"));
            map1.put(temp.get("id"),list1);
        }
        return map1;
    }
    @PostMapping("/api/newinterest")
    @ResponseBody
    @CrossOrigin
    public HashMap newinterest(@RequestBody HashMap map){
        HashMap map1 = new HashMap();
        mapper.newinterest(map);
        map1.put("code",1001);
        return map1;
    }
    @PostMapping("/api/deleteinterest")
    @ResponseBody
    @CrossOrigin
    public HashMap deleteinterest(@RequestBody HashMap map){
        HashMap map1 = new HashMap();
        mapper.deleteinterest(map);
        map1.put("code",1001);
        return map1;
    }
    @PostMapping("/api/getshenfenlist")
    @ResponseBody
    @CrossOrigin
    public List getshenfenlist(@RequestBody HashMap map){
        List list = new ArrayList();
        List list1 = mapper.getidentitylist();
        List list2 = mapper.getinterestlist();
        list.add(list1);list.add(list2);
        return list;
    }
    @PostMapping("/api/setmyshenfen")
    @ResponseBody
    @CrossOrigin
    public HashMap setmyshenfen(@RequestBody HashMap map){
        List list = (List)map.get("data");
        mapper.deletemyshenfen((Integer)map.get("userid"));
        for(int i=0;i<list.size();i++){
            HashMap map1 = (HashMap) list.get(i);
            map1.put("userid",(Integer)map.get("userid"));
            mapper.setmyshenfen(map1);
        }
        HashMap map1 = new HashMap();
        map1.put("code",1001);
        return map1;
    }
    @PostMapping("/api/getmyshenfen")
    @ResponseBody
    @CrossOrigin
    public List getmyshenfen(@RequestBody HashMap map){
        List list = mapper.getmyshenfen(map);
        return list;
    }
    @PostMapping("/api/getmyshenfen1")
    @ResponseBody
    @CrossOrigin
    public List getmyshenfen1(@RequestBody HashMap map){
       return getmyshenfen3((Integer)map.get("userid"));
    }

    public List getmyshenfen3(Integer userid){
        HashMap map = new HashMap();
        map.put("userid",userid);
        List list = mapper.getmyshenfen(map);
        List list1 = new ArrayList();
        for(int i=0;i<list.size();i++){
            HashMap map1=(HashMap) list.get(i);
            HashMap map2=new HashMap();
            if((Integer)map1.get("type")==1){
                list1.add(mapper.getmyshenfen1((Integer)map1.get("id"),(Integer)map.get("userid")));
            }
            else{
                list1.add(mapper.getmyshenfen2((Integer)map1.get("id"),(Integer)map.get("userid")));
            }
        }
        return list1;
    }
    @PostMapping("/api/setbirthday")
    @ResponseBody
    @CrossOrigin
    public HashMap setbirthday(@RequestBody HashMap map){
        HashMap map1 = new HashMap();
        mapper.setbirthday(map);
        map1.put("code",1001);
        return map1;
    }
    @PostMapping("/api/getmypattention")
    @ResponseBody
    @CrossOrigin
    public HashMap getmypattention(@RequestBody HashMap map){
        int time = (int)((new Date().getTime())/1000);
        Integer lasttime = mapper.gettuijian_cd(map);
        /*if(lasttime+(7+3600*24)>time){
            HashMap temp = mapper.getlastpattention(map);
            System.out.println("上周最佳推荐匹配用户："+temp.get("username"));
            return temp;
        }*/
        mapper.settuijian_cd((Integer)map.get("userid"),time);
        HashMap map1 = new HashMap();
        List list = mapper.tuijianuser((Integer) map.get("userid"));
        List list1 = new ArrayList();
        if(list.size()!=0) {
            list1 = mapper.tuijianuser1(list);
        }
        List myshenfen = mapper.getmyshenfenid(map);
        /***/
        List shenfen = shenfenid(map);
        List attention = mapper.getattentionidlist(map);
        /***/
        //List mypattentionlist = mapper.getmypattention(map);
        List mypattentionlist = mapper.getmypattention1(map);
        HashMap pattention = new HashMap();
        double d=0;
        HashMap temp3 = new HashMap();
        pattention.put("user",temp3);
        pattention.put("value",d);
        System.out.println(list1);
        list1=mypattentionlist;
        System.out.println(list1);
        for(int i=0;i<list1.size();i++){
            HashMap temp=(HashMap) list1.get(i);
            if((Integer)temp.get("userid")==(Integer)map.get("userid")||mypattentionlist.contains((Integer)temp.get("userid"))){
                continue;
            }
            /*分母*/
            List shenfenvalue = shenfenid(temp);
            /*待判用户分子*/
            List heshenfen = shenfenvalue;

            shenfenvalue.addAll(myshenfen);

            double value=0;
            double fenmu=Math.sqrt(shenfenvalue.size());
            int fenzi=0;
            for(int x=0;x<myshenfen.size();x++){
                for(int y=0;y<heshenfen.size();y++){
                    if(myshenfen.get(x)==heshenfen.get(y)){
                        fenzi++;
                        break;
                    }
                }
            }
            value = fenzi/fenmu;
            /**/

            /*分母*/
            List attentionvalue = mapper.getattentionidlist(temp);
            if(attentionvalue.size()>=0) {
                /*待判用户分子*/
                if(attentionvalue.size()==0){
                    ;
                }
                else {
                    List heattention = attentionvalue;

                    attentionvalue.addAll(mypattentionlist);
                    double value1 = 0;
                    double fenmu1 = Math.sqrt(shenfenvalue.size());
                    int fenzi1 = 0;
                    for (int x = 0; x < attention.size(); x++) {
                        for (int y = 0; y < heattention.size(); y++) {
                            if (attention.get(x) == heattention.get(y)) {
                                fenzi1++;
                                break;
                            }
                        }
                    }
                    value1 = fenzi1 / fenmu1;
                    value=(value+value1)/2;
                    //value=value/value1;
                }
            }
            System.out.println("UID："+(Integer)temp.get("userid")+"匹配度："+value);
            if((double)pattention.get("value")<value){
                pattention.put("value",value);
                pattention.put("user",temp);
            }
        }
        HashMap temp = (HashMap) pattention.get("user");
        d = (double) pattention.get("value");
        System.out.println("本周最佳推荐匹配用户："+temp.get("username")+"\n匹配度："+d);
        mapper.setlastpattention((Integer)map.get("userid"),(Integer)temp.get("userid"));
       return temp;
    }
    public List shenfenid (HashMap map){
        List myshenfen = mapper.getmyshenfenid(map);
        List shenfen = new ArrayList();
        for(int i=0;i<myshenfen.size();i++){
            HashMap temp = (HashMap) myshenfen.get(i);
            if((Integer)temp.get("type")==1){
                shenfen.add("a"+(Integer)temp.get("id"));
            }
            else{
                shenfen.add("b"+(Integer)temp.get("id"));
            }
        }
        return shenfen;
    }
    @PostMapping("/api/newloucengreply")
    @ResponseBody
    @CrossOrigin
    public HashMap newloucengreply(@RequestBody HashMap map) throws IOException {
        Integer time = (int)(System.currentTimeMillis()/1000);
        map.put("time",time);
        StringBuffer cookie = new StringBuffer();
        Random random = new Random();
        String str1="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        for(int j=0;j<=18;j++) {
            int sub = random.nextInt(61);
            cookie.append(str1.substring(sub, sub + 1));
        }
        map.put("cookie",cookie.toString());
        mapper.newloucengreply(map);
        HashMap map1 = new HashMap();
        map1.put("code",1001);
        map1.put("time",time);
        List list = getmyshenfen3((Integer)map.get("userid"));
        String str="";
        for(int i=0;i<list.size();i++){
            HashMap temp=(HashMap)list.get(i);
            str+=temp.get("name");
            if(i<list.size()-1)
                str+="、";
        }
        map1.put("shenfen",str);
        map1.put("id",mapper.getreplylouceng_id(cookie.toString()));
        mapper.setthemereplytime((Integer)map.get("themeid"),Integer.valueOf(time));
        mapper.numadd((Integer)map.get("themeid"));
        UpdateRequest request1 = new UpdateRequest();
        {
            HashMap temp = mapper.gettheme((Integer)map.get("themeid"));
            request1.index("theme").id(map.get("themeid").toString());
            // 拓展：局部更新也可以这样写：
            request1.doc(XContentType.JSON, "num",temp.get("num"),"replytime",time);
            // 3、发送请求到ES
            UpdateResponse response2 = client1.update(request1, RequestOptions.DEFAULT);

        }
        return map1;
    }
    @PostMapping("/api/deleteloucengreply")
    @ResponseBody
    @CrossOrigin
    public HashMap deleteloucengreply(@RequestBody HashMap map){
        mapper.deleteloucengreply(map);
        HashMap map1 = new HashMap();
        map1.put("code",1001);
        return map1;
    }
    @PostMapping("/api/pixiv")
    @ResponseBody
    @CrossOrigin
    //@Scheduled(cron = "0 0 0 * * ?")
    public void pixiv() throws IOException {
        RestTemplate restTemplate = new RestTemplate();
        String url ="https://cloud.mokeyjay.com/pixiv/?r=api/source-json";
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        List<MediaType> mediaTypeList = new ArrayList<>();
        mediaTypeList.add(MediaType.APPLICATION_JSON);
        httpHeaders.setAccept(mediaTypeList);
        HttpEntity<String> request = new HttpEntity<>(httpHeaders);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
        JSONObject json = JSONObject.parseObject(response.getBody());
        List list = (List)json.get("data");
        if(pixiv_time==null){
            pixiv_time=(String)json.get("date");
        }

        {
            pixiv_time = (String) json.get("date");
            for (Object i : list) {
                Map map1 =(Map)i;

                map1.put("date",pixiv_time);
                url = (String)map1.get("url");
                url ="https://i.pixiv.re"+url.substring(29,url.length());
                String []urllist = url.split("/");
                String str="";
                for(int j=0;j<((List)map1.get("tags")).size();j++)str+=((List)map1.get("tags")).get(j);
                map1.put("tags",str);
                String name=urllist[5]+urllist[6]+urllist[7]+urllist[8]+urllist[9]+urllist[10]+urllist[11];
                map1.put("url","https://txtzz-1301452902.file.myqcloud.com/"+name);
                mapper.newpixiv(map1);
                {
                    URL url1 = new URL(url);
                    URLConnection conn = url1.openConnection();
                    conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
                    InputStream inputStream = conn.getInputStream();
                    FileOutputStream fileOutputStream = new FileOutputStream(
                            //"C:\\Users\\xihang\\Desktop\\新建文件夹 (2)\\"
                            "/springboot_temp/"
                            +name);
                    int bytesum = 0;
                    int byteread;
                    byte[] buffer = new byte[1024];

                    while ((byteread = inputStream.read(buffer)) != -1) {
                        bytesum += byteread;
                        fileOutputStream.write(buffer, 0, byteread);
                    }
                    fileOutputStream.close();
                }

            }
            newpixivtheme(pixiv_time);
        }
    }
    @PostMapping("/api/getpixiv")
    @ResponseBody
    @CrossOrigin
    public List getpixiv(@RequestBody HashMap map){
        if((Integer)map.get("type")==1) {
            Random r = new Random();
            List list = mapper.getpixiv_pc(map);
            List temp = new ArrayList();
            temp.add(list.get(r.nextInt(list.size()-1)));
            HashMap temp1=(HashMap) temp.get(0);
            temp1.put("url",temp1.get("url"));
            list.set(0,temp1);
            return temp;
        }
        else {
            Random r = new Random();
            List list = mapper.getpixiv_pe(map);
            List temp = new ArrayList();
            temp.add(list.get(0));
            HashMap temp1=(HashMap) temp.get(0);
            temp1.put("url",temp1.get("url"));
            list.set(0,temp1);
            return temp;
        }
    }

    public void newpixivtheme(String date){
        if(mapper.getpixivcount("Pixiv "+date+"日榜")==1)return;
        StringBuffer themecookie = new StringBuffer();
        HashMap map = new HashMap();
        String text="<!DOCTYPE html>\n" + "<html>\n" + "<head>\n" + "</head>\n" + "<body>\n" + "<p>";
        String data="";
        Integer time = (int)(System.currentTimeMillis()/1000);
        String image="";
        Random random = new Random();
        String str1="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        for(int j=0;j<=18;j++) {
            int sub = random.nextInt(61);
            themecookie.append(str1.substring(sub, sub + 1));
        }
        List list = mapper.getpixiv(date);{
            for(int i=0;i<list.size();i++){
                HashMap temp = (HashMap) list.get(i);
                image=image+temp.get("url")+";";
                text=text+"<p style='text-align:center'><img src='"+temp.get("url")+"?imageMogr2/format/webp' alt='' width='800' style='margin-bottom:10px;'/></p>" +
                        "<br>" +
                        "<p style='font-size:13px;color:#909399;text-align:center'>"+temp.get("title")+"|pid:"+temp.get("pid")+"|画师:"+temp.get("username")+"</p><br>";
                //data=data+" "+temp.get("title")+"|pid:"+temp.get("pid")+"|画师:"+temp.get("username");
            }
            text=text+"</p>\n" + "</body>\n" + "</html>";
        }
        HashMap map1 = new HashMap();
        map1.put("title","Pixiv "+date+"日榜");map1.put("userid",1);map1.put("text",text);map1.put("data",data);map1.put("image",image);
        map1.put("tags","Pixiv;");map1.put("fenlei","二次元");map1.put("createtime",time);map1.put("themecookie",themecookie.toString());map1.put("replytime",time);
        map1.put("type",0);map1.put("ycsrc","");
        mapper.newpixivtheme(map1);
        mapper.themenumadd((Integer)map1.get("userid"));
        String str=(String)map1.get("tags");
        if(null!=str&&str.length()>0) {
            String[] arr=(str.split(";"));
            for(int i=0;i<arr.length;i++)
                mapper.addtag(themecookie.toString(),arr[i]);
        }
    }

    public static List<String> getImgSrc(String content){

        List<String> list = new ArrayList<String>();
        //目前img标签标示有3种表达式
        //<img alt="" src="1.jpg"/>   <img alt="" src="1.jpg"></img>     <img alt="" src="1.jpg">
        //开始匹配content中的<img />标签
        Pattern p_img = Pattern.compile("<(img|IMG)(.*?)(/>|></img>|>)");
        Matcher m_img = p_img.matcher(content);
        boolean result_img = m_img.find();
        if (result_img) {
            while (result_img) {
                //获取到匹配的<img />标签中的内容
                String str_img = m_img.group(2);

                //开始匹配<img />标签中的src
                Pattern p_src = Pattern.compile("(src|SRC)=(\"|\')(.*?)(\"|\')");
                Matcher m_src = p_src.matcher(str_img);
                if (m_src.find()) {
                    String str_src = m_src.group(3);
                    list.add(str_src);
                }
                //结束匹配<img />标签中的src

                //匹配content中是否存在下一个<img />标签，有则继续以上步骤匹配<img />标签中的src
                result_img = m_img.find();
            }
        }
        return list;
    }
        @Autowired
        RestHighLevelClient client1;

    @GetMapping("/test")
    @ResponseBody
    @CrossOrigin
    public String test() throws IOException {
        List list = mapper.test1();
        for(int i=0;i<list.size();i++) {
            HashMap map = (HashMap)list.get(i);
            DeleteRequest request1 = new DeleteRequest("theme", map.get("themeid").toString());
            client1.delete(request1, RequestOptions.DEFAULT);
        }
        for(int i=0;i<list.size();i++) {
            HashMap map = (HashMap)list.get(i);
            IndexRequest request = new IndexRequest("theme").id(map.get("themeid").toString());
            request.source(JSONObject.toJSONString(map), XContentType.JSON);
            IndexResponse response = client1.index(request, RequestOptions.DEFAULT);
        }
        return "ok";
    }
    @PostMapping("/api/getheadborderlist")
    @ResponseBody
    @CrossOrigin
    public List getheadborderlist(@RequestBody HashMap map){
        List list =mapper.getheadborderlist();
        return list;
    }
    @PostMapping("/api/myheadborderlist")
    @ResponseBody
    @CrossOrigin
    public HashMap myheadborderlist(@RequestBody HashMap map){
        List list =mapper.getuserheadborder1((Integer)map.get("userid"));
        HashMap returnmap = new HashMap();
        returnmap.put("list",list);
        returnmap.put("headborderid",mapper.getheadborderid((Integer)map.get("userid")));
        return returnmap;
    }
    @PostMapping("/api/newheadborder")
    @ResponseBody
    @CrossOrigin
    public HashMap newheadborder(@RequestBody HashMap map){
      mapper.newheadborder(map);
      HashMap temp = new HashMap();
      temp.put("code",1001);
      return temp;
    }
    @PostMapping("/api/setuserheadborder")
    @ResponseBody
    @CrossOrigin
    public HashMap setuserheadborder(@RequestBody HashMap map){
        mapper.setuserheadborder(map);
        HashMap temp = new HashMap();
        temp.put("code",1001);
        return temp;
    }
    @PostMapping("/api/updateheadborder")
    @ResponseBody
    @CrossOrigin
    public HashMap updateheadborder(@RequestBody HashMap map){
        mapper.updateheadborder(map);
        HashMap temp = new HashMap();
        temp.put("code",1001);
        return temp;
    }

    public List getactivity(HashMap map){
        List list = mapper.getactivity(map);
        return list;
    }
    public List getactivity1(HashMap map){
        List list = mapper.getactivity1(map);
        return list;
    }

    @PostMapping("/api/setactivity")
    @ResponseBody
    @CrossOrigin
    public HashMap setactivity(@RequestBody HashMap map){
        HashMap returnmap= new HashMap();
        mapper.setactivity(map);
        returnmap.put("code",1001);
        return returnmap;
    }
    @PostMapping("/api/deleteactivity")
    @ResponseBody
    @CrossOrigin
    public HashMap deleteactivity(@RequestBody HashMap map){
        HashMap returnmap= new HashMap();
        returnmap.put("code",1001);
        mapper.deleteactivity(map);
        return returnmap;
    }

    @PostMapping("/api/getskinlist")
    @ResponseBody
    @CrossOrigin
    public List getskinlist(@RequestBody HashMap map){
        List list =mapper.getskinlist();
        return list;
    }

    @PostMapping("/api/gethottagnum")
    @ResponseBody
    @CrossOrigin
    public HashMap gethottagnum(@RequestBody HashMap map){
        HashMap map1 = new HashMap();
        Integer num =mapper.gethottagnum(map);
        map1.put("num",num);
        map1.put("code",1001);
        return map1;
    }
    @PostMapping("/api/getipaddress")
    @ResponseBody
    @CrossOrigin
    public HashMap getipaddress(@RequestBody HashMap map, HttpServletRequest request){
        HashMap map1 = new HashMap();
        String ip = request.getRemoteAddr();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String time = (formatter.format(System.currentTimeMillis()));
        String address = ip2regionSearcher.getAddress(ip);
        HashMap temp = new HashMap();
        temp.put("ip",ip);
        temp.put("address",address);
        map1.put("data",temp);
        map1.put("code",1001);
        return map1;
    }

    @PostMapping("/api/test1")
    @ResponseBody
    @CrossOrigin
    public void test1(){
        List table1 = mapper.gettable1();
        List table2 = mapper.gettable2();
        List temp = new ArrayList();
        for(int i=0;i< table1.size();i++){
            for(int j=0;j<table2.size();j++){
                if(jugesame(table1.get(i).toString(),table2.get(j).toString())){
                    String str=table2.get(i).toString();
                    table2.set(i,table2.get(j));
                    table2.set(j,str);
                }
            }
        }
        for(int i=0;i<table1.size();i++){
            System.out.println(table1.get(i)+" "+table2.get(i));
        }
    }
    public Boolean jugesame(String str1,String str2){
        String[] arr1 =str1.split(" ");
        String[] arr2 =str2.split(" ");
        if(!arr1[0].equals(arr2[0])) return false;

        if(arr1[1].indexOf(arr2[1])==-1&&arr2[1].indexOf(arr1[1])==-1)return false;

        if(!arr1[2].equals(arr2[2]))return false;

        String temp1="";String temp2="";
        for(int i=3;i<arr1.length;i++){
            temp1+=arr1[i];
        }
        for(int i=3;i<arr2.length;i++){
            temp2+=arr2[i];
        }
        if(temp2.length()>=temp1.length()){

            return jugechar(temp2,temp1);
        }
        else return jugechar(temp1,temp2);

    }
    public Boolean jugechar(String str1,String str2){
        String[] arr2=str2.split("");
        double length=0;
        for(int i=0;i<arr2.length;i++){
            if(str1.indexOf(arr2[i])!=-1){
                length++;
            }
        }
        if(length/str2.length()>0.8)
            return true;
        else return false;
    }

    @PostMapping("/api/musiclist")
    @ResponseBody
    @CrossOrigin
    public HashMap musiclist(@RequestBody HashMap map, HttpServletRequest request){
        HashMap map1 = new HashMap();
        map1.put("musiclist",mapper.getmusiclist());
        map1.put("code",1001);
        return map1;
    }
    @PostMapping("/api/fangke")
    @ResponseBody
    @CrossOrigin
    public void fangke(@RequestBody HashMap map,HttpServletRequest request){
        String ip = request.getRemoteAddr();
        String address = ip2regionSearcher.getAddress(ip);
        LocalDate date = LocalDate.now();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long timeInMillis = calendar.getTimeInMillis();
        redisTemplate.opsForHash().put(String.valueOf(timeInMillis),ip,address);
        Date date1 = new Date(timeInMillis+7*24*3600*1000);
        redisTemplate.expireAt(String.valueOf(timeInMillis),date1);
    }
    @PostMapping("/api/updatenotes")
    @ResponseBody
    @CrossOrigin
    public HashMap updatenotes(@RequestBody HashMap map){
        HashMap map1 = new HashMap();
        map1.put("data",mapper.getupdatenotes());
        return map1;
    }
    @PostMapping("/api/fangkejilu")
    @ResponseBody
    @CrossOrigin
    public HashMap fangkejilu(@RequestBody HashMap map){
        HashMap map1 = new HashMap();
        LocalDate date = LocalDate.now();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long timeInMillis = calendar.getTimeInMillis();
        Object object1 = (redisTemplate.opsForHash().values(String.valueOf(timeInMillis)).toString());
        Object object2 = (redisTemplate.opsForHash().values(String.valueOf(timeInMillis-3600000*24*1)).toString());
        Object object3 = (redisTemplate.opsForHash().values(String.valueOf(timeInMillis-3600000*24*2)).toString());
        Object object4 = (redisTemplate.opsForHash().values(String.valueOf(timeInMillis-3600000*24*3)).toString());
        Object object5 = (redisTemplate.opsForHash().values(String.valueOf(timeInMillis-3600000*24*4)).toString());
        Object object6 = (redisTemplate.opsForHash().values(String.valueOf(timeInMillis-3600000*24*5)).toString());
        Object object7 = (redisTemplate.opsForHash().values(String.valueOf(timeInMillis-3600000*24*6)).toString());
        HashMap temp = new HashMap();
        List list = new ArrayList();
        if(object1!=null){
            temp.put('a'+String.valueOf(timeInMillis),object1);
            list.add(String.valueOf(timeInMillis));
        }
        if(object2!=null){
            temp.put('a'+String.valueOf(timeInMillis-3600000*24*1),object2);
            list.add(String.valueOf(timeInMillis-3600000*24*1));
        }
        if(object3!=null){
            temp.put("a"+String.valueOf(timeInMillis-3600000*24*2),object3);
            list.add(String.valueOf(timeInMillis-3600000*24*2));
        }
        if(object4!=null){
            temp.put('a'+String.valueOf(timeInMillis-3600000*24*3),object4);
            list.add(String.valueOf(timeInMillis-3600000*24*3));
        }
        if(object5!=null){
            temp.put('a'+String.valueOf(timeInMillis-3600000*24*4),object5);
            list.add(String.valueOf(timeInMillis-3600000*24*4));
        }
        if(object6!=null){
            temp.put('a'+String.valueOf(timeInMillis-3600000*24*5),object6);
            list.add(String.valueOf(timeInMillis-3600000*24*5));
        }
        if(object7!=null){
            temp.put('a'+String.valueOf(timeInMillis-3600000*24*6),object7);
            list.add(String.valueOf(timeInMillis-3600000*24*6));
        }
        map1.put("time",list);
        map1.put("data",temp);
        return map1;
    }
}
