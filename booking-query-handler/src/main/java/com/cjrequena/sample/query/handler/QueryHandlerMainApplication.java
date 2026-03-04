package com.cjrequena.sample.query.handler;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@ComponentScan(basePackages = {
  "com.cjrequena.sample.query.handler",
  "com.cjrequena.sample.es.core"  // Eventstore
})
@RequiredArgsConstructor
public class QueryHandlerMainApplication implements CommandLineRunner {

  public static void main(String... args) {
    SpringApplication.run(QueryHandlerMainApplication.class, args);
  }

  @SneakyThrows
  @Override
  public void run(String... args) throws Exception {

  }
}
