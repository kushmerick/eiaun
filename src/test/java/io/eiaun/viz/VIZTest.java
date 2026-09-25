package io.eiaun.viz;

import org.junit.jupiter.api.Test;

class VIZTest {

    @Test
    void canRunMain() {
        // TODO: Honestly I am not sure why this tests terminates. Doesn't
        // TODO: `main` start a web server which presumably runs forever?
        VIZ.main(new String[] {
                "--spring.profiles.active=viz,test"
        });
    }

}