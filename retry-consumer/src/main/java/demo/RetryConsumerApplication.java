package demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
public class RetryConsumerApplication {


  public static void main(String[] args) {
    SpringApplication.run(RetryConsumerApplication.class, args);
  }

  @Bean
  public WebClient webClient(@Value("${producer.url}") String url) {
    return WebClient.builder().baseUrl(url).build();
  }

}
