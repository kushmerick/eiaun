package io.eiaun.viz;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "io.eiaun.shared")
@StyleSheet("styles.css")
public class VIZ implements AppShellConfigurator {

    static void main(String[] args) {
        SpringApplication app = new SpringApplication(VIZ.class);
        app.setAdditionalProfiles("viz");
        app.run(args);
    }

}
