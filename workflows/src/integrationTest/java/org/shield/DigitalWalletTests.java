package org.shield;

import org.junit.Before;

import java.util.concurrent.ExecutionException;

public class DigitalWalletTests {
    @Before
    public void setUp() throws ExecutionException, InterruptedException {
        TestHelper.setupNetwork();
        MembershipTests membershipTests = new MembershipTests();
        membershipTests.configAllRolesTest();
    }
}
