package eu.tango.scamscreener.message;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Small client-side message bridge for local ScamScreener output.
 */
public final class MessageDispatcher {
    private static final long LOCAL_ECHO_TTL_MS = 2_000L;
    private static final int MAX_PENDING_LOCAL_ECHOES = 32;
    private static final List<PendingLocalEcho> PENDING_LOCAL_ECHOES = new ArrayList<>();

    private MessageDispatcher() {
    }

    /**
     * Displays a local client-side chat message.
     *
     * @param text the message to show
     */
    public static void reply(Text text) {
        if (text == null) {
            return;
        }

        rememberLocalEchoText(text.getString());

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }

        client.execute(() -> {
            if (client.player != null) {
                client.player.sendMessage(text, false);
            }
        });
    }

    /**
     * Consumes one pending local echo match, when present.
     *
     * @param rawMessage the inbound raw message
     * @return {@code true} when the message was produced locally by ScamScreener
     */
    public static synchronized boolean consumeLocalEcho(String rawMessage) {
        String normalizedMessage = normalizeEchoText(rawMessage);
        if (normalizedMessage.isBlank()) {
            purgeExpiredEchoes(System.currentTimeMillis());
            return false;
        }

        long now = System.currentTimeMillis();
        purgeExpiredEchoes(now);
        for (int index = 0; index < PENDING_LOCAL_ECHOES.size(); index++) {
            PendingLocalEcho entry = PENDING_LOCAL_ECHOES.get(index);
            if (entry.normalizedText().equals(normalizedMessage)) {
                PENDING_LOCAL_ECHOES.remove(index);
                return true;
            }
        }

        return false;
    }

    static synchronized void rememberLocalEchoText(String rawText) {
        long now = System.currentTimeMillis();
        purgeExpiredEchoes(now);

        for (String signature : collectEchoSignatures(rawText)) {
            PENDING_LOCAL_ECHOES.add(new PendingLocalEcho(signature, now));
        }

        trimPendingEchoes();
    }

    static synchronized void clearLocalEchoes() {
        PENDING_LOCAL_ECHOES.clear();
    }

    private static List<String> collectEchoSignatures(String rawText) {
        String normalizedText = normalizeEchoText(rawText);
        if (normalizedText.isBlank()) {
            return List.of();
        }

        List<String> signatures = new ArrayList<>();
        signatures.add(normalizedText);

        String[] lines = normalizedText.split("\n");
        for (String line : lines) {
            String normalizedLine = normalizeEchoText(line);
            if (!normalizedLine.isBlank() && !signatures.contains(normalizedLine)) {
                signatures.add(normalizedLine);
            }
        }

        return signatures;
    }

    private static String normalizeEchoText(String rawText) {
        if (rawText == null) {
            return "";
        }

        return rawText
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .trim();
    }

    private static void purgeExpiredEchoes(long now) {
        PENDING_LOCAL_ECHOES.removeIf(entry -> now - entry.createdAtMs() > LOCAL_ECHO_TTL_MS);
    }

    private static void trimPendingEchoes() {
        while (PENDING_LOCAL_ECHOES.size() > MAX_PENDING_LOCAL_ECHOES) {
            PENDING_LOCAL_ECHOES.remove(0);
        }
    }

    private record PendingLocalEcho(String normalizedText, long createdAtMs) {
    }
}
