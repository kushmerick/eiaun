package io.eiaun.viz;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.concurrent.ExecutorService;

@SpringBootApplication(scanBasePackages = "io.eiaun.shared")
@Push
@StyleSheet("styles.css")
@Slf4j
public class VIZ implements AppShellConfigurator {

    @Autowired private ExecutorService executor;

    static void main(String[] args) {
        SpringApplication app = new SpringApplication(VIZ.class);
        app.setAdditionalProfiles("viz");
        app.run(args);
    }

    @PreDestroy
    public void stop() {
        log.info("Stopping");
        if (this.executor != null) {
            this.executor.shutdownNow();
        }
    }

}
