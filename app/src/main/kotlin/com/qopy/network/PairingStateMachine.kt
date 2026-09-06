package com.qopy.network

import com.qopy.crypto.NodeIdentity
import com.qopy.crypto.PairingCrypto
import com.qopy.data.PeerEntity
import com.qopy.protocol.FrameCodec
import com.qopy.proto.Qopy.*
import com.google.protobuf.ByteString
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

object PairingStateMachine {
    fun runInitiatorPairing(
        input: InputStream,
        out: OutputStream,
        identity: NodeIdentity,
        code: String
    ): PeerEntity {
        val sessionId = UUID.randomUUID().toString()
        val normCode = PairingCrypto.normalizePairingCode(code)

        // 1. Send PairRequest
        val req = PairRequest.newBuilder()
            .setPairingSessionId(sessionId)
            .setDeviceId(identity.deviceId)
            .setDeviceName(identity.deviceName)
            .setOs("android")
            .setPublicKey(ByteString.copyFrom(identity.publicKey))
            .setTlsCertSha256(ByteString.copyFrom(identity.tlsCertSha256))
            .setProtocolVersion(1)
            .build()

        val env = Envelope.newBuilder()
            .setProtocolVersion(1)
            .setMessageId(UUID.randomUUID().toString())
            .setTimestampMs(System.currentTimeMillis())
            .setPairRequest(req)
            .build()

        FrameCodec.writeFrame(out, env)

        // 2. Read PairChallenge
        val challengeEnv = FrameCodec.readFrame(input) ?: throw IllegalStateException("Connection closed by responder")
        if (!challengeEnv.hasPairChallenge()) {
            throw IllegalStateException("Expected PairChallenge")
        }
        val challenge = challengeEnv.pairChallenge

        // 3. Compute Transcript Hash & Proof
        val transcriptHash = PairingCrypto.computeTranscriptHash(
            sessionId = sessionId,
            initiatorDeviceId = identity.deviceId,
            responderDeviceId = challenge.deviceId,
            initiatorPublicKey = identity.publicKey,
            responderPublicKey = challenge.publicKey.toByteArray(),
            initiatorTlsCertSha256 = identity.tlsCertSha256,
            responderTlsCertSha256 = challenge.tlsCertSha256.toByteArray()
        )

        val proof = PairingCrypto.computePairingProof(normCode, transcriptHash)

        // 4. Send PairProof
        val proofMsg = PairProof.newBuilder()
            .setPairingSessionId(sessionId)
            .setProof(ByteString.copyFrom(proof))
            .build()

        val envProof = Envelope.newBuilder()
            .setProtocolVersion(1)
            .setMessageId(UUID.randomUUID().toString())
            .setTimestampMs(System.currentTimeMillis())
            .setPairProof(proofMsg)
            .build()

        FrameCodec.writeFrame(out, envProof)

        // 5. Read PairAck
        val ackEnv = FrameCodec.readFrame(input) ?: throw IllegalStateException("Connection closed before PairAck")
        if (!ackEnv.hasPairAck()) {
            throw IllegalStateException("Expected PairAck")
        }
        val ack = ackEnv.pairAck
        if (!ack.accepted) {
            throw SecurityException("Pairing rejected: ${ack.message}")
        }

        val now = System.currentTimeMillis()
        return PeerEntity(
            deviceId = challenge.deviceId,
            deviceName = challenge.deviceName,
            osType = challenge.os,
            publicKey = challenge.publicKey.toByteArray(),
            tlsCertSha256 = challenge.tlsCertSha256.toByteArray(),
            lastKnownIp = null,
            lastKnownPort = null,
            isTrusted = true,
            createdAt = now,
            updatedAt = now
        )
    }

    fun runResponderPairing(
        input: InputStream,
        out: OutputStream,
        identity: NodeIdentity,
        code: String
    ): PeerEntity {
        val normCode = PairingCrypto.normalizePairingCode(code)

        // 1. Read PairRequest
        val reqEnv = FrameCodec.readFrame(input) ?: throw IllegalStateException("Connection closed")
        if (!reqEnv.hasPairRequest()) {
            throw IllegalStateException("Expected PairRequest")
        }
        val req = reqEnv.pairRequest
        val sessionId = req.pairingSessionId

        // 2. Send PairChallenge
        val challenge = PairChallenge.newBuilder()
            .setPairingSessionId(sessionId)
            .setDeviceId(identity.deviceId)
            .setDeviceName(identity.deviceName)
            .setOs("android")
            .setPublicKey(ByteString.copyFrom(identity.publicKey))
            .setTlsCertSha256(ByteString.copyFrom(identity.tlsCertSha256))
            .build()

        val envChallenge = Envelope.newBuilder()
            .setProtocolVersion(1)
            .setMessageId(UUID.randomUUID().toString())
            .setTimestampMs(System.currentTimeMillis())
            .setPairChallenge(challenge)
            .build()

        FrameCodec.writeFrame(out, envChallenge)

        // 3. Read PairProof
        val proofEnv = FrameCodec.readFrame(input) ?: throw IllegalStateException("Connection closed")
        if (!proofEnv.hasPairProof()) {
            throw IllegalStateException("Expected PairProof")
        }
        val proof = proofEnv.pairProof

        // 4. Verify Proof
        val transcriptHash = PairingCrypto.computeTranscriptHash(
            sessionId = sessionId,
            initiatorDeviceId = req.deviceId,
            responderDeviceId = identity.deviceId,
            initiatorPublicKey = req.publicKey.toByteArray(),
            responderPublicKey = identity.publicKey,
            initiatorTlsCertSha256 = req.tlsCertSha256.toByteArray(),
            responderTlsCertSha256 = identity.tlsCertSha256
        )

        val isValid = PairingCrypto.verifyPairingProof(normCode, transcriptHash, proof.proof.toByteArray())

        // 5. Send PairAck
        val ack = PairAck.newBuilder()
            .setPairingSessionId(sessionId)
            .setAccepted(isValid)
            .setMessage(if (isValid) "Success" else "Invalid code")
            .build()

        val envAck = Envelope.newBuilder()
            .setProtocolVersion(1)
            .setMessageId(UUID.randomUUID().toString())
            .setTimestampMs(System.currentTimeMillis())
            .setPairAck(ack)
            .build()

        FrameCodec.writeFrame(out, envAck)

        if (!isValid) {
            throw SecurityException("Invalid pairing proof")
        }

        val now = System.currentTimeMillis()
        return PeerEntity(
            deviceId = req.deviceId,
            deviceName = req.deviceName,
            osType = req.os,
            publicKey = req.publicKey.toByteArray(),
            tlsCertSha256 = req.tlsCertSha256.toByteArray(),
            lastKnownIp = null,
            lastKnownPort = null,
            isTrusted = true,
            createdAt = now,
            updatedAt = now
        )
    }
}
