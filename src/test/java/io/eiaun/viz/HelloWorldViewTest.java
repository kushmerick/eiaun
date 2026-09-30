package io.eiaun.viz;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.notification.Notification;
import io.eiaun.viz.fakes.HelloWorldView;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "spring.main.web-application-type=servlet"
)
class HelloWorldViewTest extends SpringBrowserlessTest {

    @Test
    public void setText_clickButton_notificationIsShown() {
        final HelloWorldView helloView = navigate(HelloWorldView.class);
        test(helloView.getName()).setValue("Test");
        test(helloView.getSayHello()).click();
        Notification notification = find(Notification.class).single();
        Assertions.assertEquals("Hello Test", test(notification).getText());
    }

}
