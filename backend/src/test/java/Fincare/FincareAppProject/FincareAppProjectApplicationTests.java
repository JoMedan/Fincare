package Fincare.FincareAppProject;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
		exclude = {
				org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
				org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
		}

)
class FincareAppProjectApplicationTests {

	@Test
	void contextLoads() {
	}

}
