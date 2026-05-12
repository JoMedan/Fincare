package Fincare.FincareAppProject;

import Fincare.FincareAppProject.Service.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class FincareAppProjectApplicationTests {

    @MockitoBean
    TokenService tokenService;

    @Test
    void contextLoads() {
    }
}
