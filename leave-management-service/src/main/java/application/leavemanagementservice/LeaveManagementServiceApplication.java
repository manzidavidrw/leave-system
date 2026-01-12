package application.leavemanagementservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableFeignClients(basePackages = "application.leavemanagementservice.config")

public class LeaveManagementServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LeaveManagementServiceApplication.class, args);
    }

}
