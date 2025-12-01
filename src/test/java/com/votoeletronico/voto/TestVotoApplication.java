package com.votoeletronico.voto;

import org.springframework.boot.SpringApplication;

public class TestVotoApplication {

	public static void main(String[] args) {
		SpringApplication.from(VotoApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
