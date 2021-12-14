package demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class ConsumerController {

  private UnstableClient unstableClient;

  public ConsumerController(UnstableClient unstableClient) {
    this.unstableClient = unstableClient;
  }

  @GetMapping("/unstable-client")
  public Mono<Product> unstable() {
    return unstableClient.unstable();
  }

  @GetMapping("/unstable-with-retry-client")
  public Mono<Product> unstableWithRetry() {
    return unstableClient.unstableWithRetry();
  }
}
