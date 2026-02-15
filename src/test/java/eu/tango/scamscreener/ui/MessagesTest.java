package eu.tango.scamscreener.ui;

import eu.tango.scamscreener.rules.ScamRules;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MessagesTest {
	@Test
	void messageFactoriesReturnNonNullForSafeMethods() throws Exception {
		List<Method> methods = java.util.Arrays.stream(Messages.class.getDeclaredMethods())
			.filter(method -> java.lang.reflect.Modifier.isPublic(method.getModifiers()))
			.filter(method -> java.lang.reflect.Modifier.isStatic(method.getModifiers()))
			.filter(method -> Component.class.isAssignableFrom(method.getReturnType()))
			.filter(method -> !Set.of("behaviorRiskWarning", "blacklistWarning", "blacklistEntry").contains(method.getName()))
			.sorted(Comparator.comparing(Method::getName))
			.toList();

		for (Method method : methods) {
			Object[] args = java.util.Arrays.stream(method.getParameterTypes())
				.map(MessagesTest::defaultArg)
				.toArray(Object[]::new);
			Object result = method.invoke(null, args);
			assertNotNull(result, "Expected non-null message from " + method.getName());
		}
	}

	@Test
	void errorMessagesIncludeStableErrorCodes() {
		assertTrue(Messages.modelUpdateCheckFailed("boom").getString().contains("[MU-CHECK-001]"));
		assertTrue(Messages.trainingSaveFailed("boom").getString().contains("[TR-SAVE-001]"));
		assertTrue(Messages.trainingUploadWebhookFailed("boom").getString().contains("[TR-UPLOAD-001]"));
		assertTrue(Messages.trainingUploadUnavailable("boom").getString().contains("[TR-UPLOAD-002]"));
		assertTrue(Messages.mutePatternInvalid("(bad").getString().contains("[MUTE-REGEX-001]"));
	}

	@Test
	void bypassMessagesContainRunCommand() {
		MutableComponent email = Messages.emailSafetyBlocked("abc123");
		MutableComponent discord = Messages.discordSafetyBlocked("def456");
		MutableComponent coop = Messages.coopAddSafetyBlocked("SomePlayer", "ghi789", true);

		assertTrue(email.getString().contains("[BYPASS]"));
		assertTrue(discord.getString().contains("[BYPASS]"));
		assertTrue(coop.getString().contains("[BYPASS]"));
		assertTrue(hasRunCommand(email, "/scamscreener bypass abc123"));
		assertTrue(hasRunCommand(discord, "/scamscreener bypass def456"));
		assertTrue(hasRunCommand(coop, "/scamscreener bypass ghi789"));
	}

	@Test
	void modelUpdateDownloadLinkContainsRunCommand() {
		MutableComponent link = Messages.modelUpdateDownloadLink("/scamscreener ai model download 123");

		assertTrue(link.getString().contains("A new AI Model is available."));
		assertTrue(hasRunCommand(link, "/scamscreener ai model download 123"));
	}

	@Test
	void trainingWebhookMessagesUseSimpleStatusText() {
		MutableComponent started = Messages.trainingUploadWebhookStarted("ignored-path");
		MutableComponent success = Messages.trainingUploadWebhookSucceeded("ignored-path", "ignored-detail");

		assertTrue(started.getString().contains("Uploading..."));
		assertTrue(success.getString().contains("Training data uploaded sucessfully."));
	}

	@Test
	void helpMessagesListUploadCommandInsteadOfAiTrain() {
		String commandHelp = Messages.commandHelp().getString();
		String aiHelp = Messages.aiCommandHelp().getString();

		assertTrue(commandHelp.contains("/scamscreener upload"));
		assertFalse(commandHelp.contains("/scamscreener ai train"));
		assertTrue(commandHelp.contains("/scamscreener ai metrics"));
		assertTrue(aiHelp.contains("/scamscreener upload"));
		assertFalse(aiHelp.contains("/scamscreener ai train"));
		assertTrue(aiHelp.contains("/scamscreener ai metrics"));
	}

	@Test
	void nullInputsUseSafeFallbackText() {
		String updated = Messages.updatedBlacklistEntry(null, -5, null).getString();
		String autoLeave = Messages.autoLeaveExecuted(null).getString();
		String flagged = Messages.trainingSampleFlagged(null).getString();

		assertTrue(updated.contains("unknown"));
		assertTrue(updated.contains("0"));
		assertTrue(updated.contains("n/a"));
		assertTrue(autoLeave.contains("unknown"));
		assertTrue(flagged.contains("unknown"));
	}

	private static Object defaultArg(Class<?> type) {
		if (type == String.class) {
			return "value";
		}
		if (type == int.class || type == Integer.class) {
			return 1;
		}
		if (type == boolean.class || type == Boolean.class) {
			return true;
		}
		if (type == UUID.class) {
			return UUID.randomUUID();
		}
		if (type == ScamRules.ScamRule.class) {
			return ScamRules.ScamRule.UPFRONT_PAYMENT;
		}
		if (type == ScamRules.ScamRiskLevel.class) {
			return ScamRules.ScamRiskLevel.HIGH;
		}
		if (type == MutableComponent.class || type == Component.class) {
			return Component.literal("value");
		}
		if (List.class.isAssignableFrom(type)) {
			return List.of("one", "two");
		}
		if (Set.class.isAssignableFrom(type)) {
			return Set.of(ScamRules.ScamRule.UPFRONT_PAYMENT);
		}
		if (Map.class.isAssignableFrom(type)) {
			return Map.of(ScamRules.ScamRule.UPFRONT_PAYMENT, "detail");
		}
		return null;
	}

	private static boolean hasRunCommand(Component root, String expected) {
		return hasClickValue(root, expected);
	}

	private static boolean hasClickValue(Component root, String expected) {
		for (Component component : flatten(root)) {
			ClickEvent event = component.getStyle().getClickEvent();
			if (event == null) {
				continue;
			}
			String value = extractClickValue(event);
			if (expected.equals(value)) {
				return true;
			}
		}
		return false;
	}

	private static List<Component> flatten(Component root) {
		List<Component> out = new ArrayList<>();
		collect(root, out);
		return out;
	}

	private static void collect(Component component, List<Component> out) {
		if (component == null) {
			return;
		}
		out.add(component);
		for (Component child : component.getSiblings()) {
			collect(child, out);
		}
	}

	private static String extractClickValue(ClickEvent event) {
		for (String accessor : List.of("command", "value", "getValue", "uri", "url", "file", "path")) {
			try {
				Method method = event.getClass().getMethod(accessor);
				Object out = method.invoke(event);
				if (out != null) {
					return out.toString();
				}
			} catch (Exception ignored) {
			}
		}
		return null;
	}
}
