package com.hertt.moviesallieapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MoviesAllieApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(MoviesAllieApiApplication.class, args);
    }

}
