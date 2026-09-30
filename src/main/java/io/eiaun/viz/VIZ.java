package io.eiaun.viz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication(scanBasePackages="io.eiaun.shared")
@RestController
public class VIZ {

    static void main(String[] args) {
        SpringApplication app = new SpringApplication(VIZ.class);
        app.setAdditionalProfiles("viz");
        app.run(args);
    }

    @RequestMapping("/")
    String home() {
        return "Hello World!";
    }

}
