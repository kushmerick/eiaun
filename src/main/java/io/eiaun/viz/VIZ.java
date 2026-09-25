package io.eiaun.viz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication(scanBasePackages="io.eiaun.shared")
@RestController
public class VIZ {

    public static void main(String[] args) {
        String[] newArgs = new String[args.length + 1];
        newArgs[0] = "--spring.profiles.active=viz";
        System.arraycopy(args, 0, newArgs, 1, args.length);
        SpringApplication.run(VIZ.class, newArgs);
    }

    @RequestMapping("/")
    String home() {
        return "Hello World!";
    }

}
