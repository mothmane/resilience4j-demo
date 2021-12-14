package com.producer.demo;


import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class UnstableController {

  public static final int BOUND = 5;

  @GetMapping("/unstable")
  public Mono<Product> unstable() throws Exception {
    logger.info("unstable endpoint called");
    if(new Random().nextInt(BOUND)==1){
      logger.error("unstable endpoint called returning Exception");
      throw new Exception("oups ");
    }
    return Mono.just(new Product("TV",350.00));
  }

  private Logger logger = LoggerFactory.getLogger("PRODUCER");
}

