package com.zhangbaowei.reactive_demo.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * @author zhangbaowei
 */
@RestController
public class HomeController {

    @RequestMapping("/")
    public Mono<String> index() {
        return Mono.just("hello");
    }
}
