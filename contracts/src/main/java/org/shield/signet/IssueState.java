package org.shield.signet;

import net.corda.core.serialization.CordaSerializable;

import java.io.Serializable;

@CordaSerializable
public enum IssueState implements Serializable {
    ERROR,
    TRANSFER_COMPLETE,
    ISSUE_COMPLETE,
    CANCELLED
}
