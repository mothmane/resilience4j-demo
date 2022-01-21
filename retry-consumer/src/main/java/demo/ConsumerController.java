package demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
public class ConsumerController {

  private final UnstableClient unstableClient;

  public ConsumerController(UnstableClient unstableClient) {
    this.unstableClient = unstableClient;
  }

  @GetMapping("/unstable-client")
  public Mono<Product> unstable() {
    return unstableClient.unstable();
  }

  @GetMapping("/unstable-with-retry-client")
  public Mono<Product> unstableWithRetry() {
    log.info("the unstable with retry controller called");
    return unstableClient.unstableWithRetry();
  }
}
