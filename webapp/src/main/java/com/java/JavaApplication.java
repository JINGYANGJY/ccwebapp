package com.java;

import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication
public class JavaApplication {
    public static final StatsDClient statsDClient = new NonBlockingStatsDClient("", "localhost", 8125);

    public static void main(String[] args) {
        SpringApplication.run(JavaApplication.class, args);
    }

}
