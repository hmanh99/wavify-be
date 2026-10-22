package com.example.wavifybe;

import org.springframework.boot.SpringApplication;

public class TestWavifyBeApplication {

    public static void main(String[] args) {
        SpringApplication.from(WavifyBeApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
