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
		for (Component component : flatten(root)) {
			ClickEvent event = component.getStyle().getClickEvent();
			if (event == null) {
				continue;
			}
			String command = extractClickCommand(event);
			if (expected.equals(command)) {
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

	private static String extractClickCommand(ClickEvent event) {
		try {
			Method command = event.getClass().getMethod("command");
			Object value = command.invoke(event);
			return value == null ? null : value.toString();
		} catch (Exception ignored) {
		}
		try {
			Method value = event.getClass().getMethod("value");
			Object out = value.invoke(event);
			return out == null ? null : out.toString();
		} catch (Exception ignored) {
		}
		try {
			Method getValue = event.getClass().getMethod("getValue");
			Object out = getValue.invoke(event);
			return out == null ? null : out.toString();
		} catch (Exception ignored) {
		}
		return null;
	}
}
