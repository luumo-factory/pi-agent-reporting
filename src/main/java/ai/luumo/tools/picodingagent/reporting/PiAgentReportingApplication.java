package ai.luumo.tools.picodingagent.reporting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PiAgentReportingApplication {

    public static void main(String[] args) {
        SpringApplication.run(PiAgentReportingApplication.class, args);
    }
}
