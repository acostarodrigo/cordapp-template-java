package org.shield.trade;

import net.corda.core.serialization.CordaSerializable;

@CordaSerializable
public enum State {
    SENT,
    ACCEPTED_NOTPAYED,
    ACCEPTED_PAYED,
    CANCELLED
}
