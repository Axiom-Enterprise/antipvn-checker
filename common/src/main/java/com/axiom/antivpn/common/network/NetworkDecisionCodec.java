package com.axiom.antivpn.common.network;

import com.axiom.antivpn.common.policy.EnforcementAction;
import org.jetbrains.annotations.NotNull;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

public final class NetworkDecisionCodec {
    private static final int SIGNATURE_LENGTH = 32;
    private static final long MAX_AGE_SECONDS = 30;
    private static final long MAX_FUTURE_SKEW_SECONDS = 5;

    public byte @NotNull [] encode(@NotNull NetworkDecision decision, byte @NotNull [] secret) {
        requireSecret(secret);
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream out = new DataOutputStream(bytes)) {
                out.writeLong(decision.playerUuid().getMostSignificantBits());
                out.writeLong(decision.playerUuid().getLeastSignificantBits());
                writeString(out, decision.ip());
                out.writeByte(decision.action().ordinal());
                writeString(out, decision.reason());
                out.writeInt(decision.riskScore());
                out.writeLong(decision.issuedAtEpochSecond());
            }
            byte[] unsigned = bytes.toByteArray();
            byte[] signature = sign(unsigned, secret);
            byte[] payload = Arrays.copyOf(unsigned, unsigned.length + signature.length);
            System.arraycopy(signature, 0, payload, unsigned.length, signature.length);
            return payload;
        } catch (Exception e) {
            throw new IllegalStateException("Could not encode network decision", e);
        }
    }

    public @NotNull Optional<NetworkDecision> decode(byte @NotNull [] payload, byte @NotNull [] secret,
                                                      @NotNull Instant now) {
        requireSecret(secret);
        if (payload.length <= SIGNATURE_LENGTH || payload.length > 4096) return Optional.empty();
        try {
            byte[] unsigned = Arrays.copyOf(payload, payload.length - SIGNATURE_LENGTH);
            byte[] supplied = Arrays.copyOfRange(payload, payload.length - SIGNATURE_LENGTH, payload.length);
            if (!MessageDigest.isEqual(sign(unsigned, secret), supplied)) return Optional.empty();
            try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(unsigned))) {
                UUID uuid = new UUID(in.readLong(), in.readLong());
                String ip = readString(in);
                int ordinal = in.readUnsignedByte();
                if (ordinal >= EnforcementAction.values().length) return Optional.empty();
                String reason = readString(in);
                int score = in.readInt();
                long issuedAt = in.readLong();
                long age = now.getEpochSecond() - issuedAt;
                if (age > MAX_AGE_SECONDS || age < -MAX_FUTURE_SKEW_SECONDS || in.available() != 0) {
                    return Optional.empty();
                }
                return Optional.of(new NetworkDecision(uuid, ip, EnforcementAction.values()[ordinal], reason, score, issuedAt));
            }
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private static void requireSecret(byte[] secret) {
        if (secret.length < 32) throw new IllegalArgumentException("Network secret must be at least 32 bytes");
    }

    private static byte[] sign(byte[] value, byte[] secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return mac.doFinal(value);
    }

    private static void writeString(DataOutputStream out, String value) throws Exception {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > 512) throw new IllegalArgumentException("Network field is too long");
        out.writeShort(bytes.length);
        out.write(bytes);
    }

    private static String readString(DataInputStream in) throws Exception {
        int length = in.readUnsignedShort();
        if (length > 512) throw new IllegalArgumentException("Network field is too long");
        return new String(in.readNBytes(length), StandardCharsets.UTF_8);
    }
}
