package com.zhangbaowei.reactive_demo.bizlib;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import reactor.core.Exceptions;
import reactor.core.publisher.*;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import reactor.test.StepVerifierOptions;
import reactor.util.context.Context;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author zhangbaowei
 * @description:
 * @date 2021/7/20 9:19
 */
class ProcessManagerTest {

    static final String HTTP_CORRELATION_ID = "reactive.http.library.correlationId";

    public static <T> Consumer<Signal<T>> logOnNext(Consumer<T> logStatement) {
        return signal -> {
            if (!signal.isOnNext()) return;
            Optional<String> toPutInMdc = signal.getContext().getOrEmpty("CONTEXT_KEY");

            toPutInMdc.ifPresentOrElse(tpim -> {
                        try (MDC.MDCCloseable cMdc = MDC.putCloseable("MDC_KEY", tpim)) {
                            logStatement.accept(signal.get());
                        }
                    },
                    () -> logStatement.accept(signal.get()));
        };
    }

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

        Thread.sleep(1000);
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

    @Test
    void testFromWiki() {
        long start = System.currentTimeMillis();
        Flux<String> ids = ifhrIds();

        Flux<String> combinations =
                ids.flatMap(id -> {
                    Mono<String> nameTask = ifhrName(id);
                    Mono<Integer> statTask = ifhrStat(id);

                    return nameTask.zipWith(statTask,
                            (name, stat) -> "Name " + name + " has stats " + stat);
                });

        Mono<List<String>> result = combinations.collectList();

//        List<String> results = result.block();
//        System.out.println(System.currentTimeMillis() - start);
//        for (String s : results) {
//            System.out.println(s);
//        }

        result.subscribe(x -> x.forEach(y -> System.out.println(y)));
    }

    private Mono<Integer> ifhrStat(String id) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return Mono.just(Integer.valueOf(id));
    }

    private Mono<String> ifhrName(String id) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return Mono.just(id);
    }

    private Flux<String> ifhrIds() {
        return Flux.just("1", "2", "3");
    }

    @Test
    void test2Y() {
        Flux<Integer> ints = Flux.range(1, 4)
                .map(i -> {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (i <= 3) return i;
                    throw new RuntimeException("Got to 4");
                });
        ints.subscribe(i -> System.out.println(i),
                error -> System.err.println("Error: " + error));
    }

    @Test
    void test2x() {
        CompletableFuture<List<String>> ids = ifhIds();

        CompletableFuture<List<String>> result = ids.thenComposeAsync(l -> {
            Stream<CompletableFuture<String>> zip =
                    l.stream().map(i -> {
                        CompletableFuture<String> nameTask = ifhName(i);
                        CompletableFuture<Integer> statTask = ifhStat(i);

                        return nameTask.thenCombineAsync(statTask, (name, stat) -> "Name " + name + " has stats " + stat);
                    });
            List<CompletableFuture<String>> combinationList = zip.collect(Collectors.toList());
            CompletableFuture<String>[] combinationArray = combinationList.toArray(new CompletableFuture[combinationList.size()]);

            CompletableFuture<Void> allDone = CompletableFuture.allOf(combinationArray);
            return allDone.thenApply(v -> combinationList.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList()));
        });

        List<String> results = result.join();

    }

    private CompletableFuture<Integer> ifhStat(String i) {
        return null;
    }

    private CompletableFuture<String> ifhName(String i) {
        return null;
    }

    private CompletableFuture<List<String>> ifhIds() {
        return null;
    }

    @Test
    void test5() {
        StepVerifier.create(Mono.just(1).log().map(i -> i + 10),
                StepVerifierOptions.create().withInitialContext(Context.of("foo", "bar")))
                .expectAccessibleContext()
                .contains("foo", "bar")
                .then()
                .expectNext(11)
                .verifyComplete();
    }

    @Test
    void test6() throws InterruptedException {
        Flux.just("hello")
                .doOnNext(v -> System.out.println("just " + Thread.currentThread().getName()))
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(v -> System.out.println("publish " + Thread.currentThread().getName()))
                .delayElements(Duration.ofMillis(500))
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(v -> System.out.println(v + " delayed " + Thread.currentThread().getName()));

        Thread.sleep(2000);

    }

    @Test
    void test7() throws InterruptedException {
        long start = System.currentTimeMillis();

        Flux.range(0, 100)
                .parallel()
                .runOn(Schedulers.parallel())
                .doOnNext(i -> {
                    System.out.println(i);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                })
                .sequential()
                .blockLast();
//                .subscribe();
        System.out.println(System.currentTimeMillis() - start);
    }

    @Test
    void test8() {
        Flux<String> flux = Flux.generate(
                () -> 0,
                (state, sink) -> {
                    sink.next("3 x " + state + " = " + 3 * state);
                    if (state == 10) sink.complete();
                    return state + 1;
                });
    }

    @Test
    void test9() {
        Flux<String> alphabet = Flux.just(-1, 30, 13, 9, 20)
                .handle((i, sink) -> {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    String letter = alphabet(i);
                    if (letter != null)
                        sink.next(letter);
                });


        alphabet.subscribe(System.out::println);
    }

    public String alphabet(int letterNumber) {
        if (letterNumber < 1 || letterNumber > 26) {
            return null;
        }
        int letterIndexAscii = 'A' + letterNumber - 1;
        return "" + (char) letterIndexAscii;
    }

    @Test
    void test10() {
        Flux.just("foo")
                .map(s -> {
                    throw new IllegalArgumentException(s);
                })
                .subscribe(v -> System.out.println("GOT VALUE"),
                        e -> System.out.println("ERROR: " + e));


        Flux<String> converted = Flux
                .range(1, 10)
                .map(i -> {
                    try {
                        return convert(i);
                    } catch (IOException e) {
                        throw Exceptions.propagate(e);
                    }
                });


        converted.subscribe(
                v -> System.out.println("RECEIVED: " + v),
                e -> {
                    if (Exceptions.unwrap(e) instanceof IOException) {
                        System.out.println("Something bad happened with I/O");
                    } else {
                        System.out.println("Something bad happened");
                    }
                }
        );
    }

    public String convert(int i) throws IOException {
        if (i > 3) {
            throw new IOException("boom " + i);
        }
        return "OK " + i;
    }

    @Test
    void test12() {
        AtomicInteger ai = new AtomicInteger();
        Function<Flux<String>, Flux<String>> filterAndMap = f -> {
            if (ai.incrementAndGet() == 1) {
                return f.filter(color -> !color.equals("orange"))
                        .map(String::toUpperCase);
            }
            return f.filter(color -> !color.equals("purple"))
                    .map(String::toUpperCase);
        };

        Flux<String> composedFlux =
                Flux.fromIterable(Arrays.asList("blue", "green", "orange", "purple"))
                        .doOnNext(System.out::println)
                        .transformDeferred(filterAndMap);

        composedFlux.subscribe(d -> System.out.println("Subscriber 1 to Composed MapAndFilter :" + d));
        composedFlux.subscribe(d -> System.out.println("Subscriber 2 to Composed MapAndFilter: " + d));
    }

    @Test
    void test13() throws InterruptedException {
        Flux<Integer> source = Flux.range(1, 3)
                .doOnSubscribe(s -> System.out.println("subscribed to source"));

        ConnectableFlux<Integer> co = source.publish();

        co.subscribe(System.out::println, e -> {
        }, () -> {
        });
        co.subscribe(System.out::println, e -> {
        }, () -> {
        });

        System.out.println("done subscribing");
        Thread.sleep(500);
        System.out.println("will now connect");

        co.connect();
    }

    @Test
    void test14() {
        Flux.range(1, 10)
                .parallel(2)
                .runOn(Schedulers.parallel())
                .subscribe(i -> System.out.println(Thread.currentThread().getName() + " -> " + i));
    }

    @Test
    void test15() {
        String key = "message";
        Mono<String> r = Mono.just("Hello")
                .contextWrite(ctx -> ctx.put(key, "World"))
                .flatMap(s -> Mono.subscriberContext()
                        .map(ctx -> s + " " + ctx.getOrDefault(key, "Stranger")));

        StepVerifier.create(r)
                .expectNext("Hello Stranger")
                .verifyComplete();
    }

    @Test
    void test16() {
        String key = "message";
        Mono<String> r = Mono.just("Hello")
                .flatMap(s -> Mono.subscriberContext()
                        .map(ctx -> s + " " + ctx.get(key)))
                .subscriberContext(ctx -> ctx.put(key, "Reactor"))
                .subscriberContext(ctx -> ctx.put(key, "World"));

        System.out.println(StepVerifier.create(r)
                .expectNext("Hello Reactor")
                .verifyComplete());

    }

    Mono<Tuple2<Integer, String>> doPut(String url, Mono<String> data) {
        Mono<Tuple2<String, Optional<Object>>> dataAndContext =
                data.zipWith(Mono.subscriberContext()
                        .map(c -> c.getOrEmpty(HTTP_CORRELATION_ID)));

        return dataAndContext
                .<String>handle((dac, sink) -> {
                    if (dac.getT2().isPresent()) {
                        sink.next("PUT <" + dac.getT1() + "> sent to " + url + " with header X-Correlation-ID = " + dac.getT2().get());
                    } else {
                        sink.next("PUT <" + dac.getT1() + "> sent to " + url);
                    }
                    sink.complete();
                })
                .map(msg -> Tuples.of(200, msg));
    }

    @Test
    public void contextForLibraryReactivePut() {
        Mono<String> put = doPut("www.example.com", Mono.just("Walter"))
                .subscriberContext(Context.of(HTTP_CORRELATION_ID, "2-j3r9afaf92j-afkaf"))
                .filter(t -> t.getT1() < 300)
                .map(Tuple2::getT2);

        StepVerifier.create(put)
                .expectNext("PUT <Walter> sent to www.example.com with header X-Correlation-ID = 2-j3r9afaf92j-afkaf")
                .verifyComplete();
    }

    @Test
    void test17() {
        Flux
                .just("foo", "chain")
                .map(secret -> secret.replaceAll(".", "*"))
                .subscribe(next -> System.out.println("Received: " + next));
    }

    @Test
    void test18() {
        AtomicInteger errorCount = new AtomicInteger();

        Flux.<String>error(new IllegalArgumentException())
                .doOnError(e -> errorCount.incrementAndGet())
                .retryWhen(Retry.from(companion ->
                        companion.map(rs -> {
                            if (rs.totalRetries() < 3) {
                                System.out.println("retry " + rs.totalRetries());
                                return rs.totalRetries();
                            } else {
                                System.out.println("out ");
                                throw Exceptions.propagate(rs.failure());
                            }
                        })
                )).blockLast();
    }

    @Test
    void test19() {
        Scheduler s = Schedulers.newParallel("parallel-scheduler", 4);

        final Flux<String> flux = Flux
                .range(1, 2)
                .map(i -> 10 + i)
                .subscribeOn(s)
                .map(i -> "value " + i);

        new Thread(() -> flux.subscribe(System.out::println));
    }

    @Test
    void test20() {

        long start = System.currentTimeMillis();
        Flux.range(1, 10)
                .map(x -> {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return x;
                })
                .parallel(2)
                .runOn(Schedulers.boundedElastic())
                .subscribe(i -> System.out.println(Thread.currentThread().getName() + " -> " + i));
        System.out.println(System.currentTimeMillis() - start);
    }

    @Test
    void test21() {
        Flux.range(1, 10)
                .parallel(2)
                .subscribe(i -> System.out.println(Thread.currentThread().getName() + " -> " + i));
    }

    @Test
    void test22() {
//        Flux.interval(Duration.ofSeconds(1), Schedulers.single())
//                .doOnSubscribe(s -> log.info("Started scheduler"))
//                .flatMap(time -> fetchEvents(fetchConfig))
//                .doOnNext(res -> log.info("Retrieved batch of events of size={}", res.getEvents().size()))
//                .filter(res -> !res.getEvents().isEmpty())
//                .publishOn(Schedulers.newBoundedElastic(10, 100, "event-workers"))
//                .flatMap(eventsResponse -> processAndAcknowledgeEvents(EventsResponse, config))
//                .subscribe();
    }
}