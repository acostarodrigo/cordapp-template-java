package org.shield.webserver;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.springframework.boot.WebApplicationType.SERVLET;

/**
 * Our Spring Boot application.
 */
@SpringBootApplication
public class Starter {
    private static final Logger logger = LoggerFactory.getLogger(Starter.class);
    /**
     * Starts our Spring Boot application.
     */
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Starter.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setWebApplicationType(SERVLET);
        app.run(args);

        // we initialize the connection pool, with a threshold of 1 minute for the connection recycler.
        // new ConnectionPool((60 * 60) * 60 ); // todo, this needs to be migrated to Service
    }
}
