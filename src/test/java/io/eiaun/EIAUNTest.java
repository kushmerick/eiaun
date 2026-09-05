package io.eiaun;

import org.junit.jupiter.api.Test;

class EIAUNTest {

    @Test
    void canRunMain() throws InterruptedException {
        EIAUN.main(new String[]{"--spring.profiles.active=test"});
    }

}