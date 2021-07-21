package com.zhangbaowei.reactive_demo.bizlib;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ParallelFlux;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * @author zhangbaowei
 * @description:
 * @date 2021/7/20 9:19
 */
class ProcessManagerTest {

    @Test
    void name() {
        Flux<String> stringFlux = Flux.just("1", "2").flatMapSequential((x) -> {

            System.out.println(x);
            return Mono.just(x);

        });
    }

    @Test
    void test2() throws InterruptedException {
        Flux<String> stringFlux = Flux.just("1", "2").flatMap(x -> Flux.just(x));

        stringFlux.subscribe(x -> System.out.println(x));

        Thread.sleep(1000);
    }


    @Test
    void testParll() throws InterruptedException {
        long start = System.currentTimeMillis();
        ParallelFlux<String> map = Flux.range(1, 10)
                .log()
                .parallel(1)
                .flatMap(x -> Mono.fromCallable(() -> delayExecute(x.toString())).subscribeOn(Schedulers.boundedElastic()))
                .map(x -> x.block());

        map.subscribe(x -> System.out.println(x));

        System.out.println(System.currentTimeMillis() - start);
    }

    @Test
    void test3() {
        long start = System.currentTimeMillis();
        Flux.range(1, 255)
                .log()
                .flatMap(x -> Mono.fromCallable(() -> delayExecute(x.toString())).subscribeOn(Schedulers.elastic()))
                .toIterable()
                .iterator()
                .forEachRemaining(show -> System.out.println(show.block()));

        System.out.println(System.currentTimeMillis() - start);
    }

    public Mono<String> delayExecute(String input) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return Mono.just(input);
    }

    @Test
    void testA() {
        AtomicInteger i = new AtomicInteger();
        Flux.create((Consumer<FluxSink<Integer>>) fluxSink -> {
            // 无限次发送
            while (i.getAndIncrement() < 100) {
                fluxSink.next(i.get());
            }
        }, FluxSink.OverflowStrategy.BUFFER)
                .map(info -> String.valueOf(info))
                .subscribe(s -> {
                    // 下游处理
                    try {
                        TimeUnit.MILLISECONDS.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println(s);
                });
    }
}