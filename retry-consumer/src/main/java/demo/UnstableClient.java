package demo;

import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class UnstableClient {

  private static final String UNSTABLE_SERVICE = "unstableService";

  private final WebClient webClient;

  public UnstableClient(WebClient webClient) {
    this.webClient = webClient;
  }

  public Mono<Product> unstable() {
    return webClient.get().uri("/unstable").retrieve().bodyToMono(Product.class);
  }

  @Retry(name = UNSTABLE_SERVICE,fallbackMethod = "defaultProduct")
  public Mono<Product> unstableWithRetry() {
    return webClient.get().uri("/unstable").retrieve().bodyToMono(Product.class);
  }

  private Mono<Product> defaultProduct(Exception ex) {
    return Mono.just(new Product("DVD",12));
  }

}
