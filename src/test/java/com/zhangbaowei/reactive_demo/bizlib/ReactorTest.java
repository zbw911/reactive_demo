package com.zhangbaowei.reactive_demo.bizlib;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Iterator;

/**
 * 版权声明：本文为CSDN博主「链上研发」的原创文章，遵循CC4.0BY-SA版权协议，转载请附上原文出处链接及本声明。
 * 原文链接：https://blog.csdn.net/fang_sh_lianjia/article/details/53436977
 */
public class ReactorTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReactorTest.class);


    /**
     * 随便测试下
     */
    @Test
    public void concurrentTest() {

        //这里没有什么用,纯粹是Schedulers.elastic()可以复用这里的线程池,不想写多的代码了
        Flux.range(1, 100).map(a -> a * 1)
                .subscribeOn(Schedulers.elastic())
                .subscribe();

        //开始测试了
        long start = System.currentTimeMillis();


        //第一个参数20 20个并发
        //后面表示N个请求,最长的一个请求可能要2000ms
        list(20, 1000l, 2000l, 100l, 200l, 300l, 400l, 500l, 600l, 700l, 800l, 900l)
                .forEachRemaining(show -> LOGGER.info(show));

        LOGGER.info("总时间 : {} ms", System.currentTimeMillis() - start);

    }

    /**
     * 并行执行
     *
     * @param concurrent 并行数量
     * @param sleeps     模拟停顿时间
     * @return 随便返回了
     */
    public Iterator<String> list(int concurrent, Long... sleeps) {
        return Flux.fromArray(sleeps)
                .log()
                .flatMap(sleep -> Mono.fromCallable(() -> mockHttp(sleep)).subscribeOn(Schedulers.elastic()), concurrent)
                .toIterable().iterator();
    }

    /**
     * 实际上是一个http请求
     *
     * @param sleep 请求耗时
     * @return
     */
    public String mockHttp(long sleep) {
        try {
            Thread.sleep(sleep);
            LOGGER.info("停顿{}ms真的执行了", sleep);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return String.format("停顿了%sms", sleep);
    }

}

