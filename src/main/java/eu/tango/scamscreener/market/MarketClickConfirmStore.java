package eu.tango.scamscreener.market;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class MarketClickConfirmStore {
	private static final int MAX_TRACKED_ACTIONS = 128;

	private final Map<String, ClickState> states = new LinkedHashMap<>();

	public Decision registerAttempt(String fingerprint, long nowMs, int requiredClicks, long windowMs) {
		if (fingerprint == null || fingerprint.isBlank()) {
			return Decision.allowDecision();
		}
		int safeRequired = Math.max(1, requiredClicks);
		long safeWindowMs = Math.max(500L, windowMs);
		pruneExpired(nowMs, safeWindowMs);

		ClickState state = states.get(fingerprint);
		if (state == null || (nowMs - state.lastAttemptMs()) > safeWindowMs) {
			state = new ClickState(1, nowMs);
		} else {
			state = new ClickState(state.count() + 1, nowMs);
		}

		if (state.count() >= safeRequired) {
			states.remove(fingerprint);
			return Decision.allowDecision();
		}

		states.put(fingerprint, state);
		trim();
		return Decision.blockDecision(state.count(), safeRequired);
	}

	public void clear() {
		states.clear();
	}

	private void pruneExpired(long nowMs, long windowMs) {
		Iterator<Map.Entry<String, ClickState>> iterator = states.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<String, ClickState> entry = iterator.next();
			if ((nowMs - entry.getValue().lastAttemptMs()) > windowMs) {
				iterator.remove();
			}
		}
	}

	private void trim() {
		while (states.size() > MAX_TRACKED_ACTIONS) {
			String oldest = states.keySet().iterator().next();
			states.remove(oldest);
		}
	}

	private record ClickState(int count, long lastAttemptMs) {
		private ClickState {
			count = Math.max(0, count);
		}
	}

	public record Decision(boolean allow, int currentCount, int requiredCount) {
		public Decision {
			currentCount = Math.max(0, currentCount);
			requiredCount = Math.max(1, requiredCount);
		}

		public int remaining() {
			return Math.max(0, requiredCount - currentCount);
		}

		private static Decision allowDecision() {
			return new Decision(true, 0, 1);
		}

		private static Decision blockDecision(int currentCount, int requiredCount) {
			return new Decision(false, currentCount, requiredCount);
		}
	}
}
