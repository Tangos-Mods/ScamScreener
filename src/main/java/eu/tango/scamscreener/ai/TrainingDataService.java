package eu.tango.scamscreener.ai;

import eu.tango.scamscreener.chat.parser.ChatLineParser;
import eu.tango.scamscreener.config.ScamRulesConfig;
import eu.tango.scamscreener.config.ScamScreenerPaths;
import eu.tango.scamscreener.pipeline.core.DefaultRuleConfig;
import eu.tango.scamscreener.pipeline.core.IntentTagger;
import eu.tango.scamscreener.pipeline.core.MessageEventParser;
import eu.tango.scamscreener.pipeline.model.DetectionResult;
import eu.tango.scamscreener.pipeline.model.IntentTag;
import eu.tango.scamscreener.pipeline.model.MessageContext;
import eu.tango.scamscreener.pipeline.model.MessageEvent;
import eu.tango.scamscreener.pipeline.model.Signal;
import eu.tango.scamscreener.pipeline.model.SignalSource;
import eu.tango.scamscreener.rules.ScamRules;
import eu.tango.scamscreener.util.TextUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class TrainingDataService {
	public static final int DEFAULT_CAPTURED_CHAT_CACHE_SIZE = ScamRulesConfig.DEFAULT_CAPTURED_CHAT_CACHE_SIZE;

	private static final List<String> TRAINING_COLUMNS = List.of(
		"message",
		"label",
		"window_id",
		"window_label",
		"channel",
		"delta_ms",
		"intent_offer",
		"intent_rep",
		"intent_redirect",
		"intent_instruction",
		"intent_payment",
		"intent_anchor",
		"funnel_step_index",
		"funnel_sequence_score",
		"funnel_full_chain",
		"funnel_partial_chain",
		"rule_hits",
		"similarity_hits",
		"behavior_hits",
		"trend_hits",
		"funnel_hits",
		"ai_hits",
		"pushes_external_platform",
		"demands_upfront_payment",
		"requests_sensitive_data",
		"claims_middleman_without_proof",
		"too_good_to_be_true",
		"repeated_contact_attempts",
		"is_spam",
		"asks_for_stuff",
		"advertising",
		"hard_negative",
		"sample_weight"
	);
	private static final String TRAINING_HEADER = String.join(",", TRAINING_COLUMNS);
	private static final Path TRAINING_DATA_PATH = ScamScreenerPaths.inModConfigDir("scam-screener-training-data.csv");

	private final IntentTagger intentTagger = new IntentTagger(new DefaultRuleConfig());
	private final AiFunnelContextTracker funnelTracker = new AiFunnelContextTracker();
	private final Deque<CapturedChat> recentChat = new ArrayDeque<>();
	private final Deque<CapturedChat> pendingReviewChat = new ArrayDeque<>();
	private final Map<String, Long> lastTimestampByPlayer = new HashMap<>();
	private final Map<String, Integer> repeatedContactByPlayer = new HashMap<>();
	private final ChatEchoDeduplicator outgoingEchoDeduplicator = new ChatEchoDeduplicator(30_000L);
	private final Object captureLock = new Object();
	private final Object trainingFileLock = new Object();
	private final int maxCapturedChatLines;
	private String lastCapturedChatLine = "";

	public TrainingDataService() {
		this(DEFAULT_CAPTURED_CHAT_CACHE_SIZE);
	}

	public TrainingDataService(int maxCapturedChatLines) {
		this.maxCapturedChatLines = maxCapturedChatLines <= 0 ? DEFAULT_CAPTURED_CHAT_CACHE_SIZE : maxCapturedChatLines;
	}

	public int maxCapturedChatLines() {
		return maxCapturedChatLines;
	}

	public void recordChatLine(String plain) {
		recordChatLine(plain, 0);
	}

	public void recordChatLine(String plain, int modScore) {
		if (plain == null || plain.isBlank()) {
			return;
		}
		long now = System.currentTimeMillis();
		MessageEvent parsed = MessageEventParser.parse(plain, now);
		if (parsed == null) {
			return;
		}
		recordChatEvent(parsed, modScore);
	}

	public void recordChatEvent(MessageEvent event, int modScore) {
		if (event == null || event.rawMessage() == null || event.rawMessage().isBlank()) {
			return;
		}
		if (outgoingEchoDeduplicator.consumeIncomingEcho(event.playerName(), event.rawMessage(), event.channel(), event.timestampMs())) {
			return;
		}

		pushCapture(new CapturedChat(event.playerName(), event.rawMessage(), event.channel(), event.timestampMs(), clampReviewScore(modScore)));
	}

	public void recordOutgoingChatLine(String playerName, String message, String channel) {
		if (message == null || message.isBlank()) {
			return;
		}
		CapturedChat capture = new CapturedChat(playerName, message.trim(), channel, System.currentTimeMillis());
		pushCapture(capture);
		outgoingEchoDeduplicator.rememberOutgoing(capture.speakerKey(), capture.rawMessage(), capture.channel(), capture.timestampMs());
	}

	public String lastCapturedLine() {
		synchronized (captureLock) {
			return lastCapturedChatLine == null ? "" : lastCapturedChatLine.trim();
		}
	}

	public List<String> recentLines(int count) {
		List<CapturedChat> captures = recentCaptured(count);
		List<String> lines = new ArrayList<>(captures.size());
		for (CapturedChat capture : captures) {
			lines.add(capture.rawMessage());
		}
		return lines;
	}

	public List<String> recentLinesForPlayer(String playerName, int count) {
		List<CapturedChat> captures = recentCapturedForPlayer(playerName, count);
		List<String> lines = new ArrayList<>(captures.size());
		for (CapturedChat capture : captures) {
			lines.add(capture.rawMessage());
		}
		return lines;
	}

	public List<CapturedChat> recentCaptured(int count) {
		synchronized (captureLock) {
			if (count <= 0 || recentChat.isEmpty()) {
				return List.of();
			}
			List<CapturedChat> snapshot = new ArrayList<>(recentChat);
			int take = Math.min(count, snapshot.size());
			return new ArrayList<>(snapshot.subList(snapshot.size() - take, snapshot.size()));
		}
	}

	public List<CapturedChat> recentPendingCaptured(int count) {
		synchronized (captureLock) {
			if (count <= 0 || pendingReviewChat.isEmpty()) {
				return List.of();
			}
			List<CapturedChat> snapshot = new ArrayList<>(pendingReviewChat);
			int take = Math.min(count, snapshot.size());
			return new ArrayList<>(snapshot.subList(snapshot.size() - take, snapshot.size()));
		}
	}

	public List<CapturedChat> recentCapturedForPlayer(String playerName, int count) {
		synchronized (captureLock) {
			if (playerName == null || playerName.isBlank() || count <= 0 || recentChat.isEmpty()) {
				return List.of();
			}
			String target = toSpeakerKey(playerName);
			List<CapturedChat> matching = new ArrayList<>();
			for (CapturedChat capture : recentChat) {
				if (capture.speakerKey().equals(target)) {
					matching.add(capture);
				}
			}
			if (matching.isEmpty()) {
				return List.of();
			}
			int take = Math.min(count, matching.size());
			return new ArrayList<>(matching.subList(matching.size() - take, matching.size()));
		}
	}

	public boolean hasRecentCaptureForPlayer(String playerName) {
		synchronized (captureLock) {
			if (playerName == null || playerName.isBlank() || recentChat.isEmpty()) {
				return false;
			}
			String target = toSpeakerKey(playerName);
			for (CapturedChat capture : recentChat) {
				if (capture != null && capture.speakerKey().equals(target)) {
					return true;
				}
			}
			return false;
		}
	}

	public Path trainingDataPath() {
		return TRAINING_DATA_PATH;
	}

	public void appendRows(List<String> messages, int label) throws IOException {
		if (messages == null || messages.isEmpty()) {
			return;
		}
		List<CapturedChat> captured = new ArrayList<>(messages.size());
		long now = System.currentTimeMillis();
		for (int i = 0; i < messages.size(); i++) {
			String message = messages.get(i);
			if (message == null || message.isBlank()) {
				continue;
			}
			MessageEvent parsed = MessageEventParser.parse(message, now + i);
			if (parsed == null) {
				captured.add(new CapturedChat("unknown", message.trim(), "unknown", now + i));
			} else {
				captured.add(new CapturedChat(parsed.playerName(), parsed.rawMessage(), parsed.channel(), parsed.timestampMs()));
			}
		}
		appendCapturedRows(captured, label);
	}

	public void appendCapturedRows(List<CapturedChat> captures, int label) throws IOException {
		if (captures == null || captures.isEmpty()) {
			return;
		}
		List<String> savedMessages = new ArrayList<>();
		synchronized (trainingFileLock) {
			ensureFileInitialized();
			ensureLatestHeader();

			StringBuilder rows = new StringBuilder();
			for (CapturedChat capture : captures) {
				if (capture == null || shouldFilterMessage(capture.rawMessage())) {
					continue;
				}
				String row = buildTrainingCsvRow(capture, label, null);
				if (row != null && !row.isBlank()) {
					rows.append(row).append(System.lineSeparator());
					savedMessages.add(capture.rawMessage());
				}
			}
			if (rows.length() == 0) {
				return;
			}

			Files.writeString(
				TRAINING_DATA_PATH,
				rows.toString(),
				StandardCharsets.UTF_8,
				StandardOpenOption.APPEND
			);
		}
		markMessagesAsSaved(savedMessages);
	}

	public void appendDetectedEvent(MessageEvent event, DetectionResult result, int label) throws IOException {
		if (event == null || event.rawMessage() == null || event.rawMessage().isBlank()) {
			return;
		}
		CapturedChat capture = new CapturedChat(
			event.playerName(),
			event.rawMessage(),
			event.channel() == null ? "unknown" : event.channel(),
			event.timestampMs() > 0 ? event.timestampMs() : System.currentTimeMillis()
		);
		synchronized (trainingFileLock) {
			ensureFileInitialized();
			ensureLatestHeader();
			String row = buildTrainingCsvRow(capture, label, result);
			if (row == null || row.isBlank()) {
				return;
			}
			Files.writeString(
				TRAINING_DATA_PATH,
				row + System.lineSeparator(),
				StandardCharsets.UTF_8,
				StandardOpenOption.APPEND
			);
		}
		markMessagesAsSaved(List.of(capture.rawMessage()));
	}

	private static void ensureFileInitialized() throws IOException {
		Files.createDirectories(TRAINING_DATA_PATH.getParent());
		if (!Files.exists(TRAINING_DATA_PATH)) {
			Files.writeString(
				TRAINING_DATA_PATH,
				TRAINING_HEADER + System.lineSeparator(),
				StandardCharsets.UTF_8,
				StandardOpenOption.CREATE_NEW
			);
		}
	}

	private static int ensureLatestHeader() throws IOException {
		if (!Files.exists(TRAINING_DATA_PATH)) {
			return 0;
		}
		List<String> lines = Files.readAllLines(TRAINING_DATA_PATH, StandardCharsets.UTF_8);
		if (lines.isEmpty()) {
			Files.writeString(TRAINING_DATA_PATH, TRAINING_HEADER + System.lineSeparator(), StandardCharsets.UTF_8);
			return 0;
		}
		String first = lines.get(0).trim();
		if (TRAINING_HEADER.equals(first)) {
			return 0;
		}

		Map<String, Integer> oldIndex = headerIndex(first);
		if (!oldIndex.containsKey("message") || !oldIndex.containsKey("label")) {
			lines.set(0, TRAINING_HEADER);
			Files.write(TRAINING_DATA_PATH, lines, StandardCharsets.UTF_8);
			return 0;
		}

		List<String> migrated = new ArrayList<>(lines.size());
		migrated.add(TRAINING_HEADER);
		int updated = 0;
		for (int i = 1; i < lines.size(); i++) {
			String line = lines.get(i).trim();
			if (line.isEmpty()) {
				continue;
			}
			List<String> cols = parseCsvLine(line);
			String message = normalizeTrainingMessage(value(cols, oldIndex.get("message"), ""));
			int label = parseInt(value(cols, oldIndex.get("label"), "0"), 0);
			if (message.isBlank()) {
				continue;
			}
			Map<String, String> values = defaultColumnValues(message, label, "unknown", 0L);
			values.put("pushes_external_platform", value(cols, oldIndex.get("pushes_external_platform"), "0"));
			values.put("demands_upfront_payment", value(cols, oldIndex.get("demands_upfront_payment"), "0"));
			values.put("requests_sensitive_data", value(cols, oldIndex.get("requests_sensitive_data"), "0"));
			values.put("claims_middleman_without_proof", value(cols, oldIndex.get("claims_middleman_without_proof"), "0"));
			values.put("too_good_to_be_true", value(cols, oldIndex.get("too_good_to_be_true"), "0"));
			values.put("repeated_contact_attempts", value(cols, oldIndex.get("repeated_contact_attempts"), "0"));
			values.put("is_spam", value(cols, oldIndex.get("is_spam"), "0"));
			values.put("asks_for_stuff", value(cols, oldIndex.get("asks_for_stuff"), "0"));
			values.put("advertising", value(cols, oldIndex.get("advertising"), "0"));
			migrated.add(joinRow(values));
			updated++;
		}
		Files.write(TRAINING_DATA_PATH, migrated, StandardCharsets.UTF_8);
		return updated;
	}

	public int migrateTrainingData() throws IOException {
		synchronized (trainingFileLock) {
			ensureFileInitialized();
			return ensureLatestHeader();
		}
	}

	public List<String> readTrainingCsvLines() throws IOException {
		synchronized (trainingFileLock) {
			if (!Files.isRegularFile(TRAINING_DATA_PATH)) {
				return List.of();
			}
			ensureLatestHeader();
			return Files.readAllLines(TRAINING_DATA_PATH, StandardCharsets.UTF_8);
		}
	}

	private String buildTrainingCsvRow(CapturedChat capture, int label, DetectionResult detection) {
		String normalizedMessage = normalizeTrainingMessage(capture.rawMessage());
		if (normalizedMessage.isBlank()) {
			return null;
		}

		long timestamp = capture.timestampMs() > 0 ? capture.timestampMs() : System.currentTimeMillis();
		String speakerKey = capture.speakerKey();
		long deltaMs = computeDelta(speakerKey, timestamp);
		int repeatedContact = updateRepeatedContact(speakerKey);

		MessageEvent messageEvent = MessageEvent.from(speakerKey, capture.rawMessage(), timestamp, MessageContext.UNKNOWN, capture.channel());
		List<Signal> existingSignals = detection == null || detection.signals() == null ? List.of() : detection.signals();
		IntentTagger.TaggingResult tagging = intentTagger.tag(messageEvent, existingSignals);
		Set<IntentTag> tags = tagging.tags();
		AiFunnelContextTracker.Snapshot funnel = funnelTracker.update(speakerKey, timestamp, tags, tagging.negativeContext());

		boolean pushesExternalPlatform = tags.contains(IntentTag.PLATFORM_REDIRECT);
		boolean demandsUpfrontPayment = tags.contains(IntentTag.PAYMENT_UPFRONT) || containsAny(normalizedMessage, "pay first", "send first", "vorkasse", "upfront");
		boolean requestsSensitiveData = containsAny(normalizedMessage, "password", "2fa", "otp", "verification code", "email login");
		boolean claimsMiddleman = containsAny(normalizedMessage, "middleman", "trusted middleman", "legit middleman");
		boolean tooGood = tags.contains(IntentTag.FREE_OFFER) || containsAny(normalizedMessage, "free", "guaranteed", "100 safe", "dupe");
		boolean isSpam = containsAny(normalizedMessage, "spam", "last chance", "buy now", "cheap", "limited") || normalizedMessage.contains("!!!");
		boolean asksForStuff = containsAny(normalizedMessage, "borrow", "lend me", "give me", "can i have");
		boolean advertising = containsAny(normalizedMessage, "selling", "service", "carry", "shop", "/visit");

		SignalHistogram hits = SignalHistogram.from(existingSignals);
		boolean hardNegative = label == 0 && (funnel.score() > 0 || pushesExternalPlatform || requestsSensitiveData || hits.ruleHits() > 0);
		double sampleWeight = computeSampleWeight(label, hardNegative, funnel, repeatedContact);

		long windowBucket = timestamp / Math.max(1L, ScamRules.funnelConfig().windowMillis());
		String windowId = speakerKey + ":" + windowBucket;
		Map<String, String> values = defaultColumnValues(normalizedMessage, label, capture.channel(), deltaMs);
		values.put("window_id", windowId);
		values.put("window_label", String.valueOf(label));
		values.put("intent_offer", bool(tags.contains(IntentTag.SERVICE_OFFER) || tags.contains(IntentTag.FREE_OFFER)));
		values.put("intent_rep", bool(tags.contains(IntentTag.REP_REQUEST)));
		values.put("intent_redirect", bool(tags.contains(IntentTag.PLATFORM_REDIRECT)));
		values.put("intent_instruction", bool(tags.contains(IntentTag.INSTRUCTION_INJECTION)));
		values.put("intent_payment", bool(tags.contains(IntentTag.PAYMENT_UPFRONT)));
		values.put("intent_anchor", bool(tags.contains(IntentTag.COMMUNITY_ANCHOR)));
		values.put("funnel_step_index", String.valueOf(funnel.stepIndex()));
		values.put("funnel_sequence_score", formatDouble(funnel.score()));
		values.put("funnel_full_chain", bool(funnel.fullChain()));
		values.put("funnel_partial_chain", bool(funnel.partialChain()));
		values.put("rule_hits", String.valueOf(hits.ruleHits()));
		values.put("similarity_hits", String.valueOf(hits.similarityHits()));
		values.put("behavior_hits", String.valueOf(hits.behaviorHits()));
		values.put("trend_hits", String.valueOf(hits.trendHits()));
		values.put("funnel_hits", String.valueOf(hits.funnelHits()));
		values.put("ai_hits", String.valueOf(hits.aiHits()));
		values.put("pushes_external_platform", bool(pushesExternalPlatform));
		values.put("demands_upfront_payment", bool(demandsUpfrontPayment));
		values.put("requests_sensitive_data", bool(requestsSensitiveData));
		values.put("claims_middleman_without_proof", bool(claimsMiddleman));
		values.put("too_good_to_be_true", bool(tooGood));
		values.put("repeated_contact_attempts", String.valueOf(repeatedContact));
		values.put("is_spam", bool(isSpam));
		values.put("asks_for_stuff", bool(asksForStuff));
		values.put("advertising", bool(advertising));
		values.put("hard_negative", bool(hardNegative));
		values.put("sample_weight", formatDouble(sampleWeight));
		return joinRow(values);
	}

	private long computeDelta(String speakerKey, long timestamp) {
		synchronized (captureLock) {
			Long previous = lastTimestampByPlayer.put(speakerKey, timestamp);
			if (previous == null || previous <= 0L || timestamp <= previous) {
				return 0L;
			}
			return timestamp - previous;
		}
	}

	private int updateRepeatedContact(String speakerKey) {
		synchronized (captureLock) {
			int next = repeatedContactByPlayer.getOrDefault(speakerKey, 0) + 1;
			if (next > 9) {
				next = 9;
			}
			repeatedContactByPlayer.put(speakerKey, next);
			return next;
		}
	}

	private static Map<String, String> defaultColumnValues(String message, int label, String channel, long deltaMs) {
		Map<String, String> values = new LinkedHashMap<>();
		for (String column : TRAINING_COLUMNS) {
			values.put(column, "0");
		}
		values.put("message", message == null ? "" : message);
		values.put("label", String.valueOf(label));
		values.put("window_id", "unknown");
		values.put("window_label", String.valueOf(label));
		values.put("channel", channel == null || channel.isBlank() ? "unknown" : channel.toLowerCase(Locale.ROOT));
		values.put("delta_ms", String.valueOf(Math.max(0L, deltaMs)));
		values.put("sample_weight", "1.0");
		return values;
	}

	private static String joinRow(Map<String, String> values) {
		StringBuilder row = new StringBuilder();
		for (int i = 0; i < TRAINING_COLUMNS.size(); i++) {
			String column = TRAINING_COLUMNS.get(i);
			String value = values.getOrDefault(column, "0");
			if ("message".equals(column) || "window_id".equals(column) || "channel".equals(column)) {
				row.append(escapeCsv(value));
			} else {
				row.append(value == null || value.isBlank() ? "0" : value);
			}
			if (i + 1 < TRAINING_COLUMNS.size()) {
				row.append(",");
			}
		}
		return row.toString();
	}

	private static double computeSampleWeight(int label, boolean hardNegative, AiFunnelContextTracker.Snapshot funnel, int repeatedContact) {
		double weight = 1.0;
		if (hardNegative) {
			weight += 0.35;
		}
		if (label == 1 && funnel != null && funnel.fullChain()) {
			weight += 0.25;
		}
		if (label == 1 && funnel != null && funnel.partialChain()) {
			weight += 0.12;
		}
		if (repeatedContact >= 4) {
			weight += 0.08;
		}
		return weight;
	}

	private static Map<String, Integer> headerIndex(String headerLine) {
		Map<String, Integer> index = new HashMap<>();
		List<String> columns = parseCsvLine(headerLine);
		for (int i = 0; i < columns.size(); i++) {
			String column = columns.get(i);
			if (column == null || column.isBlank()) {
				continue;
			}
			index.put(column.trim().toLowerCase(Locale.ROOT), i);
		}
		return index;
	}

	private static String value(List<String> cols, Integer index, String fallback) {
		if (index == null || index < 0 || index >= cols.size()) {
			return fallback;
		}
		String value = cols.get(index);
		return value == null ? fallback : value;
	}

	private static String normalizeTrainingMessage(String raw) {
		if (raw == null || raw.isBlank()) {
			return "";
		}
		if (ChatLineParser.isSystemLine(raw)) {
			return "";
		}
		ChatLineParser.ParsedPlayerLine parsed = ChatLineParser.parsePlayerLine(raw);
		String base = parsed != null && parsed.message() != null ? parsed.message() : raw;
		String lowered = base.toLowerCase(Locale.ROOT);
		return lowered.replaceAll("[^a-z0-9]+", " ").trim();
	}

	private static boolean shouldFilterMessage(String raw) {
		if (raw == null || raw.isBlank()) {
			return true;
		}
		if (ChatLineParser.isSystemLine(raw)) {
			return true;
		}
		String normalized = normalizeTrainingMessage(raw);
		if (normalized.isBlank()) {
			return true;
		}
		int tokens = 0;
		for (String token : normalized.split("\\s+")) {
			if (token != null && !token.isBlank()) {
				tokens++;
				if (tokens > 1) {
					return false;
				}
			}
		}
		return true;
	}

	private static boolean containsAny(String text, String first, String... others) {
		if (text == null || text.isBlank()) {
			return false;
		}
		String normalized = text.toLowerCase(Locale.ROOT);
		if (normalized.contains(first)) {
			return true;
		}
		for (String other : others) {
			if (normalized.contains(other)) {
				return true;
			}
		}
		return false;
	}

	private static int parseInt(String value, int fallback) {
		try {
			return Integer.parseInt(value.trim());
		} catch (Exception ignored) {
			return fallback;
		}
	}

	private static List<String> parseCsvLine(String line) {
		List<String> values = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		boolean inQuotes = false;
		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if (c == '"') {
				if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
					current.append('"');
					i++;
				} else {
					inQuotes = !inQuotes;
				}
				continue;
			}
			if (c == ',' && !inQuotes) {
				values.add(current.toString());
				current.setLength(0);
				continue;
			}
			current.append(c);
		}
		values.add(current.toString());
		return values;
	}

	private static String toSpeakerKey(String playerName) {
		return TextUtil.anonymizedSpeakerKey(playerName);
	}

	private static String bool(boolean value) {
		return value ? "1" : "0";
	}

	private static String formatDouble(double value) {
		if (Double.isNaN(value) || Double.isInfinite(value)) {
			return "0";
		}
		return String.format(Locale.ROOT, "%.4f", value);
	}

	private static String escapeCsv(String value) {
		String escaped = (value == null ? "" : value)
			.replace("\r", " ")
			.replace("\n", " ")
			.replace("\"", "\"\"");
		return "\"" + escaped + "\"";
	}

	private record SignalHistogram(int ruleHits, int similarityHits, int behaviorHits, int trendHits, int funnelHits, int aiHits) {
		private static SignalHistogram from(List<Signal> signals) {
			int rule = 0;
			int similarity = 0;
			int behavior = 0;
			int trend = 0;
			int funnel = 0;
			int ai = 0;
			if (signals != null) {
				for (Signal signal : signals) {
					if (signal == null) {
						continue;
					}
					if (signal.source() == SignalSource.RULE) {
						rule++;
					}
					if (signal.ruleId() == ScamRules.ScamRule.SIMILARITY_MATCH) {
						similarity++;
					}
					if (signal.source() == SignalSource.BEHAVIOR) {
						behavior++;
					}
					if (signal.source() == SignalSource.TREND) {
						trend++;
					}
					if (signal.source() == SignalSource.FUNNEL) {
						funnel++;
					}
					if (signal.source() == SignalSource.AI) {
						ai++;
					}
				}
			}
			return new SignalHistogram(rule, similarity, behavior, trend, funnel, ai);
		}
	}

	public record CapturedChat(String speakerKey, String rawMessage, String channel, long timestampMs, int modScore) {
		public CapturedChat(String speakerKey, String rawMessage, String channel, long timestampMs) {
			this(speakerKey, rawMessage, channel, timestampMs, 0);
		}

		public CapturedChat {
			speakerKey = toSpeakerKey(speakerKey);
			rawMessage = rawMessage == null ? "" : rawMessage;
			channel = channel == null || channel.isBlank() ? "unknown" : channel.trim().toLowerCase(Locale.ROOT);
			modScore = clampReviewScore(modScore);
		}
	}

	private void pushCapture(CapturedChat capture) {
		if (capture == null || capture.rawMessage() == null || capture.rawMessage().isBlank()) {
			return;
		}
		synchronized (captureLock) {
			lastCapturedChatLine = capture.rawMessage().trim();
			recentChat.addLast(capture);
			while (recentChat.size() > maxCapturedChatLines) {
				recentChat.removeFirst();
			}
			pendingReviewChat.addLast(capture);
			while (pendingReviewChat.size() > maxCapturedChatLines) {
				pendingReviewChat.removeFirst();
			}
		}
	}

	private void markMessagesAsSaved(List<String> messages) {
		synchronized (captureLock) {
			if (messages == null || messages.isEmpty() || pendingReviewChat.isEmpty()) {
				return;
			}
			for (String message : messages) {
				String key = pendingKey(message);
				if (key.isBlank()) {
					continue;
				}
				Iterator<CapturedChat> iterator = pendingReviewChat.iterator();
				while (iterator.hasNext()) {
					CapturedChat capture = iterator.next();
					if (capture == null) {
						continue;
					}
					if (pendingKey(capture.rawMessage()).equals(key)) {
						iterator.remove();
						break;
					}
				}
			}
		}
	}

	private static String pendingKey(String message) {
		if (message == null) {
			return "";
		}
		return message.replace('\n', ' ').replace('\r', ' ').trim();
	}

	private static int clampReviewScore(int score) {
		return Math.max(0, Math.min(100, score));
	}
}
