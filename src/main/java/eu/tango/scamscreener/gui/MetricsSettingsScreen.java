package eu.tango.scamscreener.gui;

import eu.tango.scamscreener.ai.FunnelMetricsService;
import eu.tango.scamscreener.ui.MessageDispatcher;
import eu.tango.scamscreener.ui.Messages;
import eu.tango.scamscreener.util.VersionInfo;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Supplier;

final class MetricsSettingsScreen extends GUI {
	private static final FunnelMetricsService.Snapshot EMPTY_SNAPSHOT =
		new FunnelMetricsService.Snapshot(0, 0, 0, 0, 0, 0, 0.0, 0.0, 0.0, 5.0);

	private final Supplier<FunnelMetricsService.Snapshot> snapshotSupplier;

	private Button overviewButton;
	private Button detectionButton;
	private Button feedbackButton;
	private Button boundaryButton;
	private Button copyButton;

	MetricsSettingsScreen(Screen parent, Supplier<FunnelMetricsService.Snapshot> snapshotSupplier) {
		super(Component.literal("ScamScreener Metrics"), parent);
		this.snapshotSupplier = snapshotSupplier;
	}

	@Override
	protected void init() {
		ColumnState column = defaultColumnState();
		int buttonWidth = column.buttonWidth();
		int x = column.x();
		int y = column.y();

		overviewButton = addInfoButton(x, y, buttonWidth);
		y += ROW_HEIGHT;

		detectionButton = addInfoButton(x, y, buttonWidth);
		y += ROW_HEIGHT;

		feedbackButton = addInfoButton(x, y, buttonWidth);
		y += ROW_HEIGHT;

		boundaryButton = addInfoButton(x, y, buttonWidth);
		y += ROW_HEIGHT;

		copyButton = this.addRenderableWidget(Button.builder(Component.literal("Copy for Discord"), button -> copyDiscordPayload())
			.bounds(x, y, buttonWidth, 20)
			.build());
		copyButton.active = snapshotSupplier != null;

		addBackButton(buttonWidth);
		refreshState();
	}

	@Override
	public void tick() {
		super.tick();
		if (isPeriodicTick(10)) {
			refreshState();
		}
	}

	private void refreshState() {
		FunnelMetricsService.Snapshot snapshot = readSnapshot();
		if (overviewButton != null) {
			overviewButton.setMessage(Component.literal(
				"Evaluated: " + snapshot.evaluatedMessages() + " | Funnel: " + snapshot.funnelDetections()
			));
		}
		if (detectionButton != null) {
			detectionButton.setMessage(Component.literal("Detection rate: " + snapshot.detectionRatePercent()));
		}
		if (feedbackButton != null) {
			feedbackButton.setMessage(Component.literal(
				"User marks: " + snapshot.userMarkedSamples()
					+ " | FP rate: " + snapshot.falsePositivePercent()
			));
		}
		if (boundaryButton != null) {
			boundaryButton.setMessage(Component.literal(
				"Boundary cases: " + snapshot.uncertainBoundaryCases()
					+ " (+/-" + whole(snapshot.uncertainMargin())
					+ " around " + thresholdText(snapshot.alertThreshold()) + ")"
			));
		}
		if (copyButton != null) {
			copyButton.active = snapshotSupplier != null;
		}
	}

	private FunnelMetricsService.Snapshot readSnapshot() {
		if (snapshotSupplier == null) {
			return EMPTY_SNAPSHOT;
		}
		try {
			FunnelMetricsService.Snapshot snapshot = snapshotSupplier.get();
			return snapshot == null ? EMPTY_SNAPSHOT : snapshot;
		} catch (Exception ignored) {
			return EMPTY_SNAPSHOT;
		}
	}

	private void copyDiscordPayload() {
		if (this.minecraft == null || this.minecraft.keyboardHandler == null) {
			MessageDispatcher.reply(Messages.funnelMetricsClipboardUnavailable());
			return;
		}
		FunnelMetricsService.Snapshot snapshot = readSnapshot();
		String payload = discordPayload(snapshot);
		this.minecraft.keyboardHandler.setClipboard(payload);
		MessageDispatcher.reply(Messages.funnelMetricsCopiedToClipboard());
	}

	private static String discordPayload(FunnelMetricsService.Snapshot snapshot) {
		StringBuilder out = new StringBuilder();
		out.append("**ScamScreener Funnel Metrics**").append('\n');
		out.append("```yaml").append('\n');
		out.append("captured_at_utc: ").append(DateTimeFormatter.ISO_INSTANT.format(Instant.now())).append('\n');
		out.append("mod_version: ").append(oneLine(VersionInfo.modVersion())).append('\n');
		out.append("ai_model_version: ").append(VersionInfo.aiModelVersion()).append('\n');
		out.append("evaluated_messages: ").append(snapshot.evaluatedMessages()).append('\n');
		out.append("funnel_detections: ").append(snapshot.funnelDetections()).append('\n');
		out.append("funnel_detection_rate_percent: ").append(percentRaw(snapshot.funnelDetectionRate())).append('\n');
		out.append("user_marked_samples: ").append(snapshot.userMarkedSamples()).append('\n');
		out.append("user_marked_legit: ").append(snapshot.userMarkedLegit()).append('\n');
		out.append("user_marked_scam: ").append(snapshot.userMarkedScam()).append('\n');
		out.append("false_positive_rate_percent: ").append(percentRaw(snapshot.falsePositiveRate())).append('\n');
		out.append("uncertain_boundary_cases: ").append(snapshot.uncertainBoundaryCases()).append('\n');
		out.append("alert_threshold: ").append(thresholdText(snapshot.alertThreshold())).append('\n');
		out.append("uncertain_margin: ").append(whole(snapshot.uncertainMargin())).append('\n');
		out.append("```");
		return out.toString();
	}

	private static String oneLine(String value) {
		if (value == null) {
			return "unknown";
		}
		String normalized = value.replace('\n', ' ').replace('\r', ' ').trim();
		return normalized.isBlank() ? "unknown" : normalized;
	}

	private static String thresholdText(double threshold) {
		if (!Double.isFinite(threshold) || threshold <= 0.0) {
			return "n/a";
		}
		return whole(threshold);
	}

	private static String whole(double value) {
		double safe = Double.isFinite(value) ? Math.max(0.0, value) : 0.0;
		return String.format(Locale.ROOT, "%.0f", safe);
	}

	private static String percentRaw(double ratio) {
		double safe = Double.isFinite(ratio) ? Math.max(0.0, ratio) * 100.0 : 0.0;
		return String.format(Locale.ROOT, "%.2f", safe);
	}
}
