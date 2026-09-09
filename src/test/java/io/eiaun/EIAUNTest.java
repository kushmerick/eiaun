package io.eiaun;

import org.junit.jupiter.api.Test;

class EIAUNTest {

    @Test
    void canRunMain() {
        EIAUN.main(new String[]{"--spring.profiles.active=test"});
    }

}