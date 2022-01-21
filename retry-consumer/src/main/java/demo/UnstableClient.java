package demo;

import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class UnstableClient {

  private static final String UNSTABLE_SERVICE = "unstableService";

  private final WebClient webClient;

  public UnstableClient(WebClient webClient) {
    this.webClient = webClient;
  }

  public Mono<Product> unstable() {
    return webClient.get().uri("/unstable").retrieve().bodyToMono(Product.class);
  }

  @Retry(name = UNSTABLE_SERVICE)
  public Mono<Product> unstableWithRetry() {
    System.out.println("the unstable with retry called");
    return webClient.get().uri("/unstable").retrieve().bodyToMono(Product.class);
  }

  private Mono<Product> defaultProduct(Exception ex) {
    log.info("the fallback method is called");
    return
        Mono.just(new Product("DVD",12));
  }

}
