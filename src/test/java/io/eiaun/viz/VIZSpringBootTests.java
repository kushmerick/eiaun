package io.eiaun.viz;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest()
@ActiveProfiles({"viz", "test"})
class VIZSpringBootTests {

	@Test
	void contextLoads() {
	}

}
