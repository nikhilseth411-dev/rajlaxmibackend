package com.rajlaxmi.jewellers;

import com.rajlaxmi.jewellers.service.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class RajlaxmiApplicationTests {

    @Autowired
    private EmailService emailService;

    @Test
    void contextLoads() {
        // Verifies the Spring application context starts without errors
    }

    @Test
    void emailDeliveryIsAsyncProxied() {
        assertTrue(AopUtils.isAopProxy(emailService));
    }
}
