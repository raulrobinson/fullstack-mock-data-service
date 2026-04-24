package com.raulbolivar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.raulbolivar")
public class FsApplication {

	public static void main(String[] args) {
		SpringApplication.run(FsApplication.class, args);
	}

}
