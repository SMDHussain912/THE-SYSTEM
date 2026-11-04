package com.thesystem.backend.auth;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncMailConfiguration {
	@Bean(name = "otpMailExecutor")
	ThreadPoolTaskExecutor otpMailExecutor() {
		return executor("otp-mail-");
	}

	@Bean(name = "otpRequestExecutor")
	ThreadPoolTaskExecutor otpRequestExecutor() {
		return executor("otp-request-");
	}

	private ThreadPoolTaskExecutor executor(String threadNamePrefix) {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(8);
		executor.setQueueCapacity(100);
		executor.setThreadNamePrefix(threadNamePrefix);
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
		return executor;
	}
}
