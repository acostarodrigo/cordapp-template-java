package org.shield.trade;

import net.corda.core.serialization.CordaSerializable;

@CordaSerializable
public enum State {
    SENT,
    AKNOWLEDGE,
    ACCEPTED,
    CANCELLED
}
