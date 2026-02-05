package eu.tango.scamscreener.ui;

import net.minecraft.network.chat.ClickEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class MessageFlagging {
	private static final String CLICK_PREFIX = "scamscreener:msg:";
	private static final int MAX_ENTRIES = 200;
	private static final Map<String, String> RECENT = new LinkedHashMap<>(MAX_ENTRIES, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
			return size() > MAX_ENTRIES;
		}
	};
	private static String hoveredId;

	private MessageFlagging() {
	}

	public static String registerMessage(String message) {
		if (message == null || message.isBlank()) {
			return "";
		}
		String id = UUID.randomUUID().toString().replace("-", "");
		RECENT.put(id, message);
		return id;
	}

	public static String clickValue(String id) {
		if (id == null || id.isBlank()) {
			return "";
		}
		return CLICK_PREFIX + id;
	}

	public static String extractId(ClickEvent clickEvent) {
		if (clickEvent == null || clickEvent.action() != ClickEvent.Action.COPY_TO_CLIPBOARD) {
			return null;
		}
		String value = extractValue(clickEvent);
		if (value == null || !value.startsWith(CLICK_PREFIX)) {
			return null;
		}
		String id = value.substring(CLICK_PREFIX.length());
		return id.isBlank() ? null : id;
	}

	private static String extractValue(ClickEvent clickEvent) {
		try {
			return (String) clickEvent.getClass().getMethod("getValue").invoke(clickEvent);
		} catch (ReflectiveOperationException ignored) {
		}
		try {
			return (String) clickEvent.getClass().getMethod("value").invoke(clickEvent);
		} catch (ReflectiveOperationException ignored) {
		}
		try {
			java.lang.reflect.Field field = clickEvent.getClass().getDeclaredField("value");
			field.setAccessible(true);
			Object value = field.get(clickEvent);
			return value == null ? null : value.toString();
		} catch (ReflectiveOperationException ignored) {
			return null;
		}
	}

	public static void setHoveredId(String id) {
		hoveredId = id;
	}

	public static void clearHovered() {
		hoveredId = null;
	}

	public static String hoveredMessage() {
		if (hoveredId == null) {
			return null;
		}
		return RECENT.get(hoveredId);
	}

	public static String messageById(String id) {
		if (id == null || id.isBlank()) {
			return null;
		}
		return RECENT.get(id);
	}
}
