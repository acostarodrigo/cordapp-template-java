package org.shield.webserver;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.shield.webserver.bond.BondController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest(classes=Starter.class)
public class SpringTest {
    @Autowired
    private BondController bondController;

    @Test
    public void testSomething() throws Exception {
        assertNotNull(bondController);
    }
}
