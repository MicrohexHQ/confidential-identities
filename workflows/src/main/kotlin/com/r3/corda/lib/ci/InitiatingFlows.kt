package com.r3.corda.lib.ci

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.workflows.internal.flows.confidential.RequestConfidentialIdentityFlow
import com.r3.corda.lib.tokens.workflows.internal.flows.confidential.RequestConfidentialIdentityFlowHandler
import net.corda.confidential.identities.ShareKeyFlow
import net.corda.confidential.identities.ShareKeyFlowHandler
import net.corda.core.crypto.SignedData
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.identity.PartyAndCertificate
import net.corda.core.transactions.WireTransaction
import java.security.PublicKey
import java.util.*

/**
 * Initiating version of [RequestConfidentialIdentityFlow].
 */
@InitiatingFlow
@StartableByRPC
class ConfidentialIdentityInitiator(private val party: Party) : FlowLogic<PartyAndCertificate>() {
    @Suspendable
    override fun call(): PartyAndCertificate {
        return subFlow(RequestConfidentialIdentityFlow(initiateFlow(party)))
    }
}

/**
 * Responder flow to [ConfidentialIdentityInitiator].
 */
@InitiatedBy(ConfidentialIdentityInitiator::class)
class ConfidentialIdentityResponder(private val otherSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(RequestConfidentialIdentityFlowHandler(otherSession))
    }

}

/**
 * Initiating version of [RequestKeyFlow].
 */
@InitiatingFlow
class RequestKeyInitiator
private constructor(
        private val otherParty: Party,
        private val uuid: UUID?,
        private val key: PublicKey?
) : FlowLogic<SerializedSignedOwnershipClaim<SignedOwnershipClaim>>() {

    constructor(otherParty: Party, uuid: UUID) : this(otherParty, uuid, null)
    constructor(otherParty: Party, key: PublicKey) : this(otherParty, null, key)

    @Suspendable
    override fun call(): SerializedSignedOwnershipClaim<SignedOwnershipClaim> {
        return if (uuid != null) {
            subFlow(RequestKeyFlow(initiateFlow(otherParty), uuid))
        } else {
            subFlow(RequestKeyFlow(initiateFlow(otherParty), key ?: throw IllegalArgumentException("You must specify" +
                    "the identifier or a known public key")))
        }
    }
}

/**
 * Responder flow to [RequestKeyInitiator].
 */
@InitiatedBy(RequestKeyInitiator::class)
class RequestKeyResponder(private val otherSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(RequestKeyFlowHandler(otherSession))
    }
}

/**
 * Initiating version of [ShareKeyFlow].
 */
@InitiatingFlow
class ShareKeyInitiator(private val otherParty: Party, private val uuid: UUID) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(ShareKeyFlow(initiateFlow(otherParty), uuid))
    }
}

/**
 * Responder flow to [ShareKeyInitiator].
 */
@InitiatedBy(ShareKeyInitiator::class)
class ShareKeyResponder(private val otherSession: FlowSession) : FlowLogic<SerializedSignedOwnershipClaim<SignedOwnershipClaim>>() {
    @Suspendable
    override fun call(): SerializedSignedOwnershipClaim<SignedOwnershipClaim> {
        return subFlow(ShareKeyFlowHandler(otherSession))
    }
}

/**
 * Initiating version of [SyncKeyMappingFlow].
 */
@InitiatingFlow
class SyncKeyMappingInitiator
private constructor(
        private val otherParty: Party,
        private val tx: WireTransaction?,
        private val identitiesToSync: List<AbstractParty>?) : FlowLogic<Unit>() {
    constructor(otherParty: Party, tx: WireTransaction) : this(otherParty, tx, null)
    constructor(otherParty: Party, identitiesToSync: List<AbstractParty>) : this(otherParty, null, identitiesToSync)

    @Suspendable
    override fun call() {
        if (tx != null) {
            subFlow(SyncKeyMappingFlow(initiateFlow(otherParty), tx))
        } else {
            subFlow(SyncKeyMappingFlow(initiateFlow(otherParty), identitiesToSync
                    ?: throw IllegalArgumentException("A list of anonymous parties must be provided to this flow.")))
        }
    }
}

/**
 * Responder flow to [SyncKeyMappingInitiator].
 */
@InitiatedBy(SyncKeyMappingInitiator::class)
class SyncKeyMappingResponder(private val otherSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(SyncKeyMappingFlowHandler(otherSession))
    }
}