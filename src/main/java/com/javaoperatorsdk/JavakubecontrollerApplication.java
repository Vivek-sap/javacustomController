package com.javaoperatorsdk;

import java.util.logging.Logger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.javaoperatorsdk.controller.PodSetController;

@Configuration
@EnableAutoConfiguration
@ComponentScan
public class JavakubecontrollerApplication extends SpringBootServletInitializer {
	public static final Logger logger = Logger.getLogger(JavakubecontrollerApplication.class.getName());

	public static void main(String[] args) {		
		ConfigurableApplicationContext context = SpringApplication.run(JavakubecontrollerApplication.class, args);
		context.getBean(PodSetController.class).init();
	}

}
