package io.eiaun.viz;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "spring.main.web-application-type=servlet"
)
class SampleVaadinTest extends SpringBrowserlessTest {

    @Route("/sample")
    public static class Sample extends HorizontalLayout {

        @Getter TextField name;
        @Getter Button sayHello;

        public Sample() {
            name = new TextField("Your name");
            sayHello = new Button("Say hello");
            sayHello.addClickListener(_ ->
                    Notification.show("Hello " + name.getValue()));
            add(name, sayHello);
        }

    }

    @Test
    public void clickingButtonShowsNotification() {
        Sample view = navigate(Sample.class);
        test(view.getName()).setValue("Test");
        test(view.getSayHello()).click();
        Notification notification = find(Notification.class).single();
        assertEquals("Hello Test", test(notification).getText());
    }

}
