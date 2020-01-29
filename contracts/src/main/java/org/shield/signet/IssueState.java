package org.shield.signet;

import net.corda.core.serialization.CordaSerializable;

import java.io.Serializable;

@CordaSerializable
public enum IssueState implements Serializable {
    PENDING,
    COMPLETED,
    CANCELLED
}
