package com.javaoperatorsdk;

import java.util.logging.Logger;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

import com.javaoperatorsdk.controller.PodSetController;

@SpringBootApplication
public class JavakubecontrollerApplication {
	public static final Logger logger = Logger.getLogger(JavakubecontrollerApplication.class.getName());

	public static void main(String[] args) {		
		 SpringApplication.run(JavakubecontrollerApplication.class, args);
		//context.getBean(PodSetController.class).init();
	}
	
	@Bean
	public TaskExecutor taskExecutor() {
	    return new SimpleAsyncTaskExecutor(); // Or use another one of your liking
	}
	
	@Bean
	public CommandLineRunner schedulingRunner(TaskExecutor executor) {
	    return new CommandLineRunner() {
	        public void run(String... args) throws Exception {
	            executor.execute(new PodSetController());
	        }
	    };
	}

}
