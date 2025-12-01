package com.votoeletronico.voto;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class VotoApplicationTests {

	@Test
	void contextLoads() {
	}

}
