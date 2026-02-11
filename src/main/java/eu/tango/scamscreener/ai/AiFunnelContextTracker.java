package eu.tango.scamscreener.ai;

import eu.tango.scamscreener.pipeline.model.IntentTag;
import eu.tango.scamscreener.rules.ScamRules;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class AiFunnelContextTracker {
	private final Map<String, Deque<IntentRecord>> historyByPlayer = new HashMap<>();

	public Snapshot update(String playerName, long timestampMs, Set<IntentTag> tags, boolean negativeOffer) {
		if (playerName == null || playerName.isBlank()) {
			return Snapshot.empty();
		}

		ScamRules.FunnelConfig cfg = ScamRules.funnelConfig();
		long now = timestampMs > 0 ? timestampMs : System.currentTimeMillis();
		String key = playerName.trim().toLowerCase(Locale.ROOT);
		Deque<IntentRecord> history = historyByPlayer.computeIfAbsent(key, ignored -> new ArrayDeque<>());

		while (!history.isEmpty() && now - history.peekFirst().timestampMs() > cfg.windowMillis()) {
			history.removeFirst();
		}

		history.addLast(new IntentRecord(now, tags == null ? Set.of() : Set.copyOf(tags), negativeOffer));
		while (history.size() > cfg.windowSize()) {
			history.removeFirst();
		}

		List<IntentRecord> snapshot = new ArrayList<>(history);
		boolean hasOffer = find(snapshot, 0, IntentTag.SERVICE_OFFER, IntentTag.FREE_OFFER) >= 0;
		int offerIdx = find(snapshot, 0, IntentTag.SERVICE_OFFER, IntentTag.FREE_OFFER);
		int payIdx = offerIdx < 0 ? -1 : find(snapshot, offerIdx + 1, IntentTag.PAYMENT_UPFRONT);
		int repIdx = offerIdx < 0 ? -1 : find(snapshot, offerIdx + 1, IntentTag.REP_REQUEST);
		int redIdx = repIdx < 0 ? -1 : find(snapshot, repIdx + 1, IntentTag.PLATFORM_REDIRECT);
		int instIdx = redIdx < 0 ? -1 : find(snapshot, redIdx + 1, IntentTag.INSTRUCTION_INJECTION);

		int simpleRepRed = findSequence(snapshot, List.of(IntentTag.REP_REQUEST, IntentTag.PLATFORM_REDIRECT));
		int simpleRedInst = findSequence(snapshot, List.of(IntentTag.PLATFORM_REDIRECT, IntentTag.INSTRUCTION_INJECTION));
		int simpleOfferPay = findOfferPaymentSequence(snapshot);

		int stepIndex = 0;
		if (hasOffer) {
			stepIndex = 1;
		}
		if (repIdx >= 0 || payIdx >= 0) {
			stepIndex = 2;
		}
		if (redIdx >= 0) {
			stepIndex = 3;
		}
		if (instIdx >= 0) {
			stepIndex = 4;
		}

		boolean fullChain = instIdx >= 0;
		boolean partialChain = !fullChain && (simpleRepRed >= 0 || simpleRedInst >= 0 || simpleOfferPay >= 0);

		double score = 0.0;
		if (fullChain) {
			score = cfg.fullSequenceWeight();
		} else if (partialChain) {
			score = cfg.partialSequenceWeight();
			if (simpleRepRed >= 0 && simpleRedInst >= 0) {
				score += 6.0;
			}
		}

		return new Snapshot(stepIndex, score, fullChain, partialChain);
	}

	public void reset() {
		historyByPlayer.clear();
	}

	private static int find(List<IntentRecord> history, int fromIndex, IntentTag... tags) {
		if (history == null || history.isEmpty()) {
			return -1;
		}
		for (int i = Math.max(0, fromIndex); i < history.size(); i++) {
			IntentRecord record = history.get(i);
			if (record == null || record.tags().isEmpty()) {
				continue;
			}
			for (IntentTag tag : tags) {
				if ((tag == IntentTag.SERVICE_OFFER || tag == IntentTag.FREE_OFFER) && record.negativeOffer()) {
					continue;
				}
				if (record.tags().contains(tag)) {
					return i;
				}
			}
		}
		return -1;
	}

	private static int findSequence(List<IntentRecord> history, List<IntentTag> seq) {
		if (history == null || history.isEmpty() || seq == null || seq.isEmpty()) {
			return -1;
		}
		int from = 0;
		int last = -1;
		for (IntentTag tag : seq) {
			last = find(history, from, tag);
			if (last < 0) {
				return -1;
			}
			from = last + 1;
		}
		return last;
	}

	private static int findOfferPaymentSequence(List<IntentRecord> history) {
		int offer = find(history, 0, IntentTag.SERVICE_OFFER, IntentTag.FREE_OFFER);
		if (offer < 0) {
			return -1;
		}
		return find(history, offer + 1, IntentTag.PAYMENT_UPFRONT);
	}

	public record Snapshot(int stepIndex, double score, boolean fullChain, boolean partialChain) {
		public static Snapshot empty() {
			return new Snapshot(0, 0.0, false, false);
		}
	}

	private record IntentRecord(long timestampMs, Set<IntentTag> tags, boolean negativeOffer) {
		private IntentRecord {
			tags = tags == null ? Set.of() : Set.copyOf(tags);
		}
	}
}
