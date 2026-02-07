package eu.tango.scamscreener.ui;

import eu.tango.scamscreener.ui.messages.BlacklistMessages;
import eu.tango.scamscreener.ui.messages.CommandMessages;
import eu.tango.scamscreener.ui.messages.DebugMessages;
import eu.tango.scamscreener.ui.messages.ModelUpdateMessages;
import eu.tango.scamscreener.ui.messages.RiskMessages;
import eu.tango.scamscreener.ui.messages.ScreenMessages;
import eu.tango.scamscreener.ui.messages.TrainingMessages;

import eu.tango.scamscreener.blacklist.BlacklistManager;
import eu.tango.scamscreener.pipeline.model.DetectionLevel;
import eu.tango.scamscreener.pipeline.model.DetectionResult;
import eu.tango.scamscreener.pipeline.model.Signal;
import eu.tango.scamscreener.pipeline.model.SignalSource;
import eu.tango.scamscreener.rules.ScamRules;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class UiPreview {
	private static final List<Class<?>> PREVIEW_CLASSES = List.of(
		CommandMessages.class,
		BlacklistMessages.class,
		ModelUpdateMessages.class,
		TrainingMessages.class,
		RiskMessages.class,
		ScreenMessages.class,
		DebugMessages.class
	);

	private UiPreview() {
	}

	public static List<Component> buildAll(Supplier<String> lastCapturedChatSupplier) {
		PreviewContext ctx = new PreviewContext(lastCapturedChatSupplier);
		List<Component> out = new ArrayList<>();
		if (ctx.lastCapturedLine != null && !ctx.lastCapturedLine.isBlank()) {
			out.add(Component.literal("[ScamScreener] Preview last captured: " + ctx.lastCapturedLine));
		}

		List<Method> methods = new ArrayList<>();
		for (Class<?> type : PREVIEW_CLASSES) {
			for (Method method : type.getDeclaredMethods()) {
				if (!Modifier.isStatic(method.getModifiers()) || !Modifier.isPublic(method.getModifiers())) {
					continue;
				}
				if (!Component.class.isAssignableFrom(method.getReturnType())) {
					continue;
				}
				methods.add(method);
			}
		}

		methods.sort(Comparator
			.comparingInt((Method method) -> classOrder(method.getDeclaringClass()))
			.thenComparing(Method::getName)
			.thenComparingInt(Method::getParameterCount));

		for (Method method : methods) {
			Object[] args = buildArgs(method.getParameterTypes(), ctx);
			if (args == null) {
				continue;
			}
			try {
				Object result = method.invoke(null, args);
				if (result instanceof Component component) {
					out.add(component);
				}
			} catch (Exception ignored) {
			}
		}

		return out;
	}

	private static int classOrder(Class<?> type) {
		for (int i = 0; i < PREVIEW_CLASSES.size(); i++) {
			if (PREVIEW_CLASSES.get(i).equals(type)) {
				return i;
			}
		}
		return PREVIEW_CLASSES.size();
	}

	private static Object[] buildArgs(Class<?>[] paramTypes, PreviewContext ctx) {
		Object[] args = new Object[paramTypes.length];
		for (int i = 0; i < paramTypes.length; i++) {
			Object value = defaultFor(paramTypes[i], ctx);
			if (value == null && paramTypes[i].isPrimitive()) {
				return null;
			}
			if (value == null) {
				return null;
			}
			args[i] = value;
		}
		return args;
	}

	private static Object defaultFor(Class<?> type, PreviewContext ctx) {
		if (type == String.class) {
			return "Demo";
		}
		if (type == int.class || type == Integer.class) {
			return 42;
		}
		if (type == long.class || type == Long.class) {
			return 30L;
		}
		if (type == double.class || type == Double.class) {
			return 0.5d;
		}
		if (type == boolean.class || type == Boolean.class) {
			return true;
		}
		if (type == UUID.class) {
			return ctx.demoUuid;
		}
		if (type == List.class) {
			return List.of("demo line one", "demo line two");
		}
		if (type == java.util.Set.class) {
			return EnumSet.of(ScamRules.ScamRule.PRESSURE_AND_URGENCY);
		}
		if (type == Map.class) {
			return Map.of(ScamRules.ScamRule.PRESSURE_AND_URGENCY, "demo detail");
		}
		if (type == EnumSet.class) {
			return EnumSet.of(ScamRules.ScamRule.PRESSURE_AND_URGENCY);
		}
		if (type == ScamRules.ScamRule.class) {
			return ScamRules.ScamRule.PRESSURE_AND_URGENCY;
		}
		if (type == ScamRules.ScamRiskLevel.class) {
			return ScamRules.ScamRiskLevel.HIGH;
		}
		if (type == DetectionLevel.class) {
			return DetectionLevel.HIGH;
		}
		if (type == DetectionResult.class) {
			return ctx.demoResult;
		}
		if (type == BlacklistManager.ScamEntry.class) {
			return ctx.demoEntry;
		}
		if (type == Component.class) {
			return Component.literal("Demo");
		}
		if (type == net.minecraft.network.chat.MutableComponent.class) {
			return Component.literal("Demo");
		}
		return null;
	}

	private static final class PreviewContext {
		private final UUID demoUuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
		private final BlacklistManager.ScamEntry demoEntry = null;
		private final String lastCapturedLine;
		private final DetectionResult demoResult = new DetectionResult(
			72.0,
			DetectionLevel.CRITICAL,
			List.of(new Signal(
				"demo",
				SignalSource.RULE,
				15,
				"demo evidence",
				ScamRules.ScamRule.TRUST_MANIPULATION,
				List.of("demo message")
			)),
			Map.of(ScamRules.ScamRule.TRUST_MANIPULATION, "demo detail"),
			true,
			List.of("demo message")
		);

		private PreviewContext(Supplier<String> lastCapturedChatSupplier) {
			String captured = null;
			if (lastCapturedChatSupplier != null) {
				String last = lastCapturedChatSupplier.get();
				if (last != null && !last.isBlank()) {
					captured = last;
				}
			}
			lastCapturedLine = captured;
		}
	}
}

