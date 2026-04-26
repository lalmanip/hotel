package com.vivance.hotel.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
public class HotelSearchAsyncConfig {

    /**
     * Parallel TBO affiliate searches — one in-flight request per hotel code (up to 50 at a time) and background drain.
     */
    @Bean(name = "hotelSearchChunkExecutor")
    public Executor hotelSearchChunkExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
