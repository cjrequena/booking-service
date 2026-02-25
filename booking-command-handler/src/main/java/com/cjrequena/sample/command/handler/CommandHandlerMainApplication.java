package com.cjrequena.sample.command.handler;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Log4j2
@SpringBootApplication
@RequiredArgsConstructor
@EnableScheduling
@EnableAsync
@ComponentScan(basePackages = {
  "com.cjrequena.sample.command.handler",  // The main package
  "com.cjrequena.sample.es.core"  // Eventstore
})
public class CommandHandlerMainApplication implements CommandLineRunner {

  public static void main(String... args) {
    SpringApplication.run(CommandHandlerMainApplication.class, args);
  }

  @SneakyThrows
  @Override
  public void run(String... args) {

  }
}
