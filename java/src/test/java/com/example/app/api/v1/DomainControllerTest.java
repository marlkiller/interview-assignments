package com.example.app.api.v1;

import com.alibaba.fastjson.JSONObject;
import com.example.app.AppApplicationTests;
import com.example.app.common.dto.GenerateReq;
import com.example.app.service.impl.DomainServiceImpl;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.concurrent.CountDownLatch;

/**
 * @author voidm
 * @date 2021/9/19
 */
@AutoConfigureMockMvc
public class DomainControllerTest extends AppApplicationTests {


    @Autowired
    private MockMvc mvc;
    String fullUrl = "http://voidm.com";
    String errorUrl = "voidm.error";


    @Test
    public void generate() throws Exception {
        generateShortUrl(fullUrl);
    }

    private String generateShortUrl(String url) throws Exception {
        GenerateReq generateReq = new GenerateReq();
        generateReq.setFullUrl(url);

        ResultActions resultActions = mvc.perform(MockMvcRequestBuilders.post("/short_domain/generate")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        //传json参数
                        .content(JSONObject.toJSONString(generateReq)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print());

        return JSONObject.parseObject(resultActions.andReturn().getResponse().getContentAsString()).getJSONObject(
                "result").getString("shortUrl");

    }

    @Test
    public void parse() throws Exception {
        String shortUrl = generateShortUrl(fullUrl);

        mvc.perform(MockMvcRequestBuilders.get(String.format("/short_domain/parse?shortUrl=%s", shortUrl))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print());
    }

    @Test
    public void generateError() throws Exception {
        generateShortUrl(errorUrl);
    }

    @Test
    public void parseError() throws Exception {

        mvc.perform(MockMvcRequestBuilders.get(String.format("/short_domain/parse?shortUrl=%s", errorUrl))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print());
    }



    @Autowired
    private DomainServiceImpl domainService;


    /**
     * 压力测试
     *
     * @throws Exception
     */
    @Test
    public void pressureTest() throws Exception {


        int totalThreadCount = 40;
        log.info("开始压力测试 >>> ");
        CountDownLatch countDownLatch = new CountDownLatch(totalThreadCount);

        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < 20; i++) {
                int finalI = i;
                new Thread(() -> {
                    countDownLatch.countDown();
                    try {
                        countDownLatch.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    for (int j = 0; j < 100; j++) {
                        try {
                            generateShortUrl("http://voidm.com/" + finalI + "/" + j);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                }).start();
            }
        });

        Thread thread2 = new Thread(() -> {
            for (int i = 0; i < 20; i++) {
                int finalI = i;
                new Thread(() -> {
                    countDownLatch.countDown();
                    try {
                        countDownLatch.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    for (int j = 0; j < 100; j++) {
                        try {
                            mvc.perform(MockMvcRequestBuilders.get(String.format("/short_domain/parse?shortUrl=%s",
                                                    "http://voidm.com/" + finalI + j))
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .accept(MediaType.APPLICATION_JSON))
                                    .andExpect(MockMvcResultMatchers.status().isOk())
                                    .andDo(MockMvcResultHandlers.print());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });

        thread1.start();
        thread2.start();

        Thread.sleep(5000);
        while (!thread1.isAlive() && !thread2.isAlive()){
            log.info(String.format("结束压力测试 <<< CacheSize: %d/%d , 并发数: %d",
                    domainService.getCacheByFullUrlSize(),domainService.getCacheByShortUrlSize(),totalThreadCount));
            break;
        }
    }
}