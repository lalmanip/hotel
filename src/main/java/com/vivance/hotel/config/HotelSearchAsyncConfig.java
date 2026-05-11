package com.vivance.hotel.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class HotelSearchAsyncConfig {

    /**
     * Parallel TBO affiliate searches — one in-flight request per hotel code (up to 50 at a time) and background drain.
     */
    @Bean(name = "hotelSearchChunkExecutor")
    public Executor hotelSearchChunkExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Bounded pool for TBO static Hoteldetails (supplier limit: 10 parallel calls).
     */
    @Bean(destroyMethod = "shutdown")
    @Qualifier("tboStaticHotelDetailsExecutor")
    public ExecutorService tboStaticHotelDetailsExecutor(AggregatorProperties aggregatorProperties) {
        int n = Math.max(1, aggregatorProperties.getTbo().getStaticHotelDetailsMaxConcurrentRequests());
        ThreadFactory tf = new ThreadFactory() {
            private final AtomicInteger i = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "tbo-static-hoteldetails-" + i.incrementAndGet());
                t.setDaemon(true);
                return t;
            }
        };
        return Executors.newFixedThreadPool(n, tf);
    }
}
