package com.cjrequena.sample.query.handler;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
  "com.cjrequena.sample.query.handler" // The main package
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
