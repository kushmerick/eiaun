package io.eiaun.viz.fakes;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import lombok.Getter;

@Route("")
public class HelloWorldView extends HorizontalLayout {

    @Getter TextField name;
    @Getter Button sayHello;

    public HelloWorldView() {
        name = new TextField("Your name");
        sayHello = new Button("Say hello");
        sayHello.addClickListener(_ ->
                Notification.show("Hello " + name.getValue()));
        add(name, sayHello);
    }

}