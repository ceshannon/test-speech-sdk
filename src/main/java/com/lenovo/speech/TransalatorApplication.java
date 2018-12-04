package com.lenovo.speech;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TransalatorApplication implements CommandLineRunner {
	public static void main(String[] args) {
		SpringApplication.run(TransalatorApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		String key = args[0];
		String value = args[1];
		TranslatorWebsocketServer s = new TranslatorWebsocketServer();
		s.setApikey(key);
		s.setRegion(value);
		s.start();
	}
}
