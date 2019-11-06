package com.java;

import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@SpringBootApplication
public class JavaApplication {
    public static final StatsDClient statsDClient = new NonBlockingStatsDClient("", "localhost", 8125);
    public static final Logger LOGGER = LoggerFactory.getLogger(JavaApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(JavaApplication.class, args);
    }

}
