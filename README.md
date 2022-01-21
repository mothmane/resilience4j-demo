# Resiliency patterns with resilience4j and Spring boot

In this article we will introduce resilience4j, we will learn about different resiliency patterns and we will implement them in a Spring boot application,

### What You Need

Any text editor or IDE

- JDK 13 or later

- Maven 3+

- apache benchmarck

### Why should we think about resiliency

There are many reasons why resiliency is an important subject in our daily jobs, 
mainly in microservices architectures.

In real microservices world :  
 - error happens
 - communication between services is no more a simple method call, it will go through many infrastructure layers.
 - we do not have control on the producer microservices

### what we should do 

- plan for the worst
- go asynchronous whenever we can 


### Introduction to resilience4j

Resilience4j is a **modular**, **lightweight**, **easy to use**, fault tolerance library, **build with and for java 8**.

- **modular** : you can use only the dependencies that you need, there is no all or nothing
- **lightweight** : have only one external dependency  (Vavr)
- **easy to use** : based on decorators, higher order functions, to enhance any function (functional interface, lambda expression,method reference) 


Resilience4j implements multiple resiliency patterns : 

- Circuit Breaker
- RateLimiter
- TimeLimiter
- Retry
- Bulkhead
- Cache

Now that Hystrix is dead resilience4j is the first choice fault tolerance library for java developers.

#### Little bit more about resilience4j modularity

Resilience4j provides different modules, core, addons, frameworks, reactive and metrics.

For example in your application you can pick only what you really need :

- resilience4j-retry
- resilience4j-feign

or 

- resilience4j-circuitbreanker 
- resilience4j-bulkhead
- resilience4j-spring-boot2


NB : you can use a _resilience4j-all_ that envelopes all core modules 

- resilience4j-retry
- resilience4j-circuitbreaker
- resilience4j-ratelimiter
- resilience4j-bulkhead
- resilience4j-cache
- resilience4j-timelimiter

### Retry 

### Our unstable service

We will start by creating a spring boot 2 application that mimics an unstable producer, 
We will skip the details of the configuration since Sprint boot is outside this article scope

Here is the unstable endpoint.

```java

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
  
  private final Logger logger = LoggerFactory.getLogger("UNSTABLE PRODUCER");
}
```
This endpoint will fail 1/5 times,
This behavior mimics one specific kind of errors, **transient** errors.

##### What is a transient error 

Transient error is an  error that occurs once or at unpredictable intervals. 

##### Examples of real scenarios transient errors :

- loss of Network connectivity
- timeouts requests
- temporarily unavailable services
- unavailable shared resources , limited resources (db connections,  threads pools ...)
- **a more explicit scenario** : an instance of a microservices is not responding well, 
  but the round robin loadbalancer is still forwarding requests to it, 
  whenever a call is processed by this failing instance it will generate errors, the other request will be correctly handled by other working instances.
until this instance is removed, or the loadbalancer stop forwarding request to it, your consumer will face random errors. it will face 1 error each N call, N being the producer number of instances.


####  Retry

The retry pattern, let your consumer retry calls whenever they fail.
This solution can solve cascading failure caused by  transient errors, 
The basic deal is that if the error cause will resolve itself,
we can be pretty sure one of the next retry calls will succeed, 
and this will prevent our consumer from cascading failure.

###### Simple Resilience4j Retry configuration

| Attribute  | default value   |  comment  |  
|---|---|---|
| maxAttenpts  | 3  | miximum number of attempts. The retry should not retry indefinitely, if the producer is struggling. sending a lot of requests could prevent it from recovering.  |
| waitDuration  | 500 [ms]  |  fixed wait duration between retry attempts. Giving time to the producer can provide more chances to retry successfully a request.|
| retryExceptions  | empty  | list of excpetion on wich the retry happens  |
| ignoreExceptions  | empty  | list of exception on wich the retry will not be triggered  |

NB: resilience4j offers much more configuration options, 
we can configure a retry based on some result or exception predicate
we can configure a waiting interval function

## Demo code 

you can download the code using this url : https://github.com/mothmane/resilience4j-demo/archive/refs/heads/main.zip

or clone  the projet using the below command 

```bash
git clone https://github.com/mothmane/resilience4j-demo.git
```

The demo project is composed of two maven modules, _producer_ and _retry-consumer_, eash one is a ready to use spring boot application

## build the project 

you can use below command to build the project

```bash
./mvnw clean package
```
## producer code

the producer app is a simple spring boot webflux project exposing  **_/unstable_** endpoint,
this endpoint has an  average failure of 20%

```java

@RestController
public class UnstableController {

  public static final int BOUND = 5;

  @GetMapping("/unstable")
  public Mono<Product> unstable() throws Exception {
    logger.info("unstable endpoint called");
    if(new Random().nextInt(BOUND)==1){
      logger.error("unstable endpoint called returning Exception");
      throw new Exception("oups something bad has happend");
    }
    return Mono.just(new Product("TV",350.00));
  }

  private final Logger logger = LoggerFactory.getLogger("PRODUCER");
}
```
## retry consumer code

### retry consumer pom.xml
to add resilience4j  to our consumer app we will need the following maven configuration 
```xml
 <properties>
  <resilience4j-spring-boot2.version>1.7.1</resilience4j-spring-boot2.version>
</properties> 
...
<dependency>
      <groupId>io.github.resilience4j</groupId>
      <artifactId>resilience4j-spring-boot2</artifactId>
      <version>${resilience4j-spring-boot2.version}</version>
    </dependency>
    <dependency>
      <groupId>io.github.resilience4j</groupId>
      <artifactId>resilience4j-annotations</artifactId>
      <version>${resilience4j-spring-boot2.version}</version>
      <scope>compile</scope>
    </dependency>
    <dependency>
      <groupId>io.github.resilience4j</groupId>
      <artifactId>resilience4j-retry</artifactId>
      <version>${resilience4j-spring-boot2.version}</version>
    </dependency>
    <dependency>
      <groupId>io.github.resilience4j</groupId>
      <artifactId>resilience4j-reactor</artifactId>
      <version>${resilience4j-spring-boot2.version}</version>
    </dependency>
```
### retry consumer application.yaml config

resilience4j is configured in spring boot application properties files, below is the configuration used in this demo

```yml
resilience4j.retry:
  instances:
    unstableService:
      maxAttempts: 5
      waitDuration: 100
      enableExponentialBackoff: true
      exponentialBackoffMultiplier: 2
```
### retry consumer client code

in below code we have a simple client that do not implement retry, 
and the other annotated with **_Retry_** annotation, the resilience4j retry annotation, have two properties, 
name that is valued with unstableService  the instance name in application yaml file.
and fallbackMethod wish take a method name that will be used as fall back in case the retry pattern do not work and the service after all retries still return errors, 
the value of the fall back method returned.

```java

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
```

below a simple controller using the two clients

```java

@RestController
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
    return unstableClient.unstableWithRetry();
  }
}

```
## Run Demo

we will start the producer  

using your favorite IDE you can import the project and start it,

the producer app will run on port 8081 and the retry-consumer on 8082

Or use the following commands

```bash
java -jar producer/target/producer-0.0.1-SNAPSHOT.jar
```

the logs should look like this 

```
.   ____          _            __ _ _
/\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
\\/  ___)| |_)| | | | | || (_| |  ) ) ) )
'  |____| .__|_| |_|_| |_\__, | / / / /
=========|_|==============|___/=/_/_/_/
:: Spring Boot ::                (v2.4.3)

2021-12-15 00:23:05.441  INFO 17401 --- [           main] com.producer.demo.ProducerApplication    : Starting ProducerApplication using Java 13.0.1 on MBP-de-Othmane with PID 17401 (/Users/toto/Downloads/resilience4j-demo/producer/target/classes started by toto in /Users/toto/Downloads/resilience4j-demo)
2021-12-15 00:23:05.443  INFO 17401 --- [           main] com.producer.demo.ProducerApplication    : No active profile set, falling back to default profiles: default
2021-12-15 00:23:06.346  INFO 17401 --- [           main] o.s.b.web.embedded.netty.NettyWebServer  : Netty started on port 8081
2021-12-15 00:23:06.356  INFO 17401 --- [           main] com.producer.demo.ProducerApplication    : Started ProducerApplication in 1.261 seconds (JVM running for 1.787)

```
and the consumer app 
```
.   ____          _            __ _ _
/\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
\\/  ___)| |_)| | | | | || (_| |  ) ) ) )
'  |____| .__|_| |_|_| |_\__, | / / / /
=========|_|==============|___/=/_/_/_/
:: Spring Boot ::                (v2.5.2)

2021-12-15 00:25:46.570  INFO 19026 --- [           main] demo.RetryConsumerApplication            : Starting RetryConsumerApplication using Java 13.0.1 on MBP-de-Othmane with PID 19026 (/Users/toto/Downloads/resilience4j-demo/retry-consumer/target/classes started by toto in /Users/toto/Downloads/resilience4j-demo)
2021-12-15 00:25:46.572  INFO 19026 --- [           main] demo.RetryConsumerApplication            : No active profile set, falling back to default profiles: default
2021-12-15 00:25:48.615  INFO 19026 --- [           main] o.s.b.a.e.web.EndpointLinksResolver      : Exposing 25 endpoint(s) beneath base path '/actuator'
2021-12-15 00:25:48.984  INFO 19026 --- [           main] o.s.b.web.embedded.netty.NettyWebServer  : Netty started on port 8082
2021-12-15 00:25:48.997  INFO 19026 --- [           main] demo.RetryConsumerApplication            : Started RetryConsumerApplication in 2.714 seconds (JVM running for 3.234)
```


et now let use apache bench to get some stats about the producer unstable endpoint, for this you need to run this command 

```bash
ab -n 100 -c 1 http://localhost:8082/unstable-client
```

result 

```bash

Complete requests:      100
Failed requests:        36
   (Connect: 0, Receive: 0, Length: 36, Exceptions: 0)
Non-2xx responses:      36

```
the apache bench shows that 36 requestS has failed, the error propagated from producer api to non protected client causing it to fail each time the produced has failed.

let's now use the protected endpoint 

```bash
ab -n 100 -c 1 http://localhost:8082/unstable-with-retry-client
```

result 

```bash

Complete requests:      100
Failed requests:        0

```

It's clear that the error did not propagate to our consumer, the retry pattern protected our system from cascading failures.


## Conclusion : 

in this article we learned about transient failure, we learned basic configuration options for retry pattern and we demonstrated how this pattern prevent from cascading failure.
in the next article we will lezrn about another type of resiliency pattern wish is the Bulkhead.

<!-- CONTACT -->
## Contact

Maniar Othmane

[![LinkedIn][linkedin-shield]][linkedin-url]


<!-- MARKDOWN LINKS & IMAGES -->
<!-- https://www.markdownguide.org/basic-syntax/#reference-style-links -->
[linkedin-shield]: https://img.shields.io/badge/-LinkedIn-black.svg?style=for-the-badge&logo=linkedin&colorB=555
[linkedin-url]: https://linkedin.com/in/othmane-maniar-2364b518/
