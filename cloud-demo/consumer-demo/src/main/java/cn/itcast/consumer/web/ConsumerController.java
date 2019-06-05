package cn.itcast.consumer.web;

import cn.itcast.consumer.client.UserClient;
import cn.itcast.consumer.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("consumer")
//@DefaultProperties(defaultFallback = "defaultFallback")
public class ConsumerController {

//    @Autowired
//    private RestTemplate restTemplate;

    //@Autowired
    //private RibbonLoadBalancerClient client;

    @Autowired
    private UserClient userClient;

    @GetMapping("/{id}")
    public User queryById(@PathVariable("id") Long id){
       return userClient.queryByid(id);
    }

//    @GetMapping("/{id}")
//    //@HystrixCommand(fallbackMethod = "queryByIdFallback")
//    /*@HystrixCommand(commandProperties = {
//            @HystrixProperty(name="execution.isolation.thread.timeoutInMilliseconds",value="4000")
//    })*/ //单个配
//    @HystrixCommand(commandProperties = {
//            @HystrixProperty(name="circuitBreaker.requestVolumeThreshold",value="10"),
//            @HystrixProperty(name="circuitBreaker.sleepWindowInMilliseconds",value="10000"),
//            @HystrixProperty(name="circuitBreaker.errorThresholdPercentage",value="60")
//    })    //内部实现了线程隔离
//    public String queryById(@PathVariable("id") Long id){
//        if(id % 2 == 0){
//            throw new RuntimeException("");
//        }
//        String url = "http://user-service/user/"+id;
//        String user = restTemplate.getForObject(url,String.class);
//        return user;
//    }

    public String queryByIdFallback(Long id){
        return "不好意思，服务器太拥挤了";
    }

    public String defaultFallback(){
        return "不好意思，服务器太拥挤了";
    }

//    @GetMapping("/{id}")
//    public User queryById(@PathVariable("id") Long id){
//        //根据服务ID获取实例
//        //List<ServiceInstance> instances = discoveryClient.getInstances("user-service");
//        //ServiceInstance instance = instances.get(0);
//        //client.choose("user-service");
//
//        //String url = "http://"+instance.getHost()+":"+instance.getPort()+"/user/"+id;
//        String url = "http://user-service/user/"+id;
//        User user = restTemplate.getForObject(url,User.class);
//        return user;
//    }
}
