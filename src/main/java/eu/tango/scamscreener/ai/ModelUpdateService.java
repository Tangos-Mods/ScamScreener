package eu.tango.scamscreener.ai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import eu.tango.scamscreener.config.LocalAiModelConfig;
import eu.tango.scamscreener.rules.ScamRules;
import eu.tango.scamscreener.ui.Messages;
import eu.tango.scamscreener.ui.DebugMessages;
import eu.tango.scamscreener.util.AsyncDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class ModelUpdateService {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final HttpClient HTTP = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(6))
		.build();
	private static final String VERSION_URL = "https://raw.githubusercontent.com/Tangos-Mods/ScamScreener/main/scripts/model-version.json";

	private final Map<String, PendingModel> pending = new ConcurrentHashMap<>();
	private volatile String latestPendingId;
	private boolean debugEnabled;

	public void checkForUpdateAsync(Consumer<Component> reply) {
		debug(reply, "check started");
		AsyncDispatcher.runIo(() -> checkForUpdate(reply, false, false, ScamRules.notifyAiUpToDateOnJoin()));
	}

	public void checkForUpdateAndDownloadAsync(Consumer<Component> reply, boolean force) {
		debug(reply, "check started (command)");
		AsyncDispatcher.runIo(() -> checkForUpdate(reply, force, true, true));
	}

	public void setDebugEnabled(boolean enabled) {
		debugEnabled = enabled;
	}

	public boolean isDebugEnabled() {
		return debugEnabled;
	}

	public PendingUpdateSnapshot latestPendingSnapshot() {
		String id = latestPendingId;
		if (id == null || id.isBlank()) {
			return null;
		}
		PendingModel pendingModel = pending.get(id);
		if (pendingModel == null || pendingModel.info() == null) {
			return null;
		}
		boolean downloaded = pendingModel.content() != null && !pendingModel.content().isBlank();
		return new PendingUpdateSnapshot(id, pendingModel.info().version(), downloaded);
	}

	public int download(String id, Consumer<Component> reply) {
		PendingModel pendingModel = pending.get(id);
		if (pendingModel == null || pendingModel.info() == null) {
			// Code: MU-LOOKUP-001
			reply.accept(Messages.modelUpdateNotFound());
			return 0;
		}
		debug(reply, "download requested id=" + id);
		AsyncDispatcher.runIo(() -> downloadModel(id, pendingModel, reply));
		return 1;
	}

	public int accept(String id, Consumer<Component> reply) {
		PendingModel pendingModel = pending.get(id);
		if (pendingModel == null || pendingModel.content() == null) {
			// Code: MU-DOWNLOAD-001
			reply.accept(Messages.modelUpdateNotReady());
			return 0;
		}
		try {
			backupLocalModel();
			LocalAiModelConfig incoming = parseIncomingModel(pendingModel.content());
			if (incoming == null) {
				reply.accept(Messages.modelUpdateFailed("unsupported model schema"));
				return 0;
			}
			LocalAiModelConfig.save(incoming);
			ScamRules.reloadConfig();
			reply.accept(Messages.modelUpdateApplied("accepted"));
			debug(reply, "accepted id=" + id);
			removePending(id);
			return 1;
		} catch (IOException e) {
			debug(reply, "accept failed: " + e.getMessage());
			// Code: MU-UPDATE-001
			reply.accept(Messages.modelUpdateFailed(modelUpdateErrorDetail(e)));
			return 0;
		}
	}

	public int merge(String id, Consumer<Component> reply) {
		PendingModel pendingModel = pending.get(id);
		if (pendingModel == null || pendingModel.content() == null) {
			// Code: MU-DOWNLOAD-001
			reply.accept(Messages.modelUpdateNotReady());
			return 0;
		}
		try {
			backupLocalModel();
			LocalAiModelConfig local = LocalAiModelConfig.loadOrCreate();
			LocalAiModelConfig incoming = parseIncomingModel(pendingModel.content());
			if (incoming == null) {
				// Code: MU-UPDATE-001
				reply.accept(Messages.modelUpdateFailed("unsupported model schema"));
				return 0;
			}
			ensureModelShape(local);
			ensureModelShape(incoming);
			incoming.version = Math.max(LocalAiModelConfig.MODEL_SCHEMA_VERSION, Math.max(local.version, incoming.version));
			if (local.denseFeatureWeights != null) {
				for (Map.Entry<String, Double> entry : local.denseFeatureWeights.entrySet()) {
					incoming.denseFeatureWeights.putIfAbsent(entry.getKey(), entry.getValue());
				}
			}
			if (local.tokenWeights != null) {
				for (Map.Entry<String, Double> entry : local.tokenWeights.entrySet()) {
					incoming.tokenWeights.putIfAbsent(entry.getKey(), entry.getValue());
				}
			}
			if (local.funnelHead != null && local.funnelHead.denseFeatureWeights != null) {
				for (Map.Entry<String, Double> entry : local.funnelHead.denseFeatureWeights.entrySet()) {
					incoming.funnelHead.denseFeatureWeights.putIfAbsent(entry.getKey(), entry.getValue());
				}
			}
			if (local.funnelHead != null && !Double.isFinite(incoming.funnelHead.intercept)
				&& Double.isFinite(local.funnelHead.intercept)) {
				incoming.funnelHead.intercept = local.funnelHead.intercept;
			}
			incoming.funnelHead.normalizeFromMain(incoming.intercept, incoming.denseFeatureWeights);
			LocalAiModelConfig.save(incoming);
			ScamRules.reloadConfig();
			reply.accept(Messages.modelUpdateApplied("merged"));
			debug(reply, "merged id=" + id);
			removePending(id);
			return 1;
		} catch (Exception e) {
			debug(reply, "merge failed: " + e.getMessage());
			// Code: MU-UPDATE-001
			reply.accept(Messages.modelUpdateFailed(modelUpdateErrorDetail(e)));
			return 0;
		}
	}

	public int ignore(String id, Consumer<Component> reply) {
		if (removePending(id) != null) {
			reply.accept(Messages.modelUpdateIgnored());
			debug(reply, "ignored id=" + id);
			return 1;
		}
		// Code: MU-LOOKUP-001
		reply.accept(Messages.modelUpdateNotFound());
		return 0;
	}

	private void checkForUpdate(Consumer<Component> reply, boolean force, boolean autoDownload, boolean notifyWhenUpToDate) {
		FetchResult fetch = fetchVersionInfo();
		ModelVersionInfo info = fetch.info();
		if (info == null) {
			debug(reply, "version info not found");
			// Code: MU-CHECK-001
			reply.accept(Messages.modelUpdateCheckFailed(fetch.error()));
			return;
		}
		if (info.url == null || info.url.isBlank()) {
			// Code: MU-CHECK-001
			reply.accept(Messages.modelUpdateCheckFailed("missing update url"));
			return;
		}
		byte[] localModelBytes = readLocalModelBytes();
		String localHash = sha256(localModelBytes);
		debug(reply, "local hash=" + (localHash == null ? "none" : localHash));
		debug(reply, "remote sha256=" + (info.sha256 == null ? "none" : info.sha256));
		VersionComparison versionComparison = compareModelVersions(info.version, localModelBytes);
		if (versionComparison.comparable()) {
			debug(reply, "local version=" + versionComparison.localVersion() + ", remote version=" + versionComparison.remoteVersion());
		}
		if (!force && versionComparison.upToDate()) {
			debug(reply, "no update (version compare)");
			if (notifyWhenUpToDate) {
				reply.accept(Messages.modelUpdateUpToDate());
			}
			return;
		}
		if (!force && hashMatchesExpected(localModelBytes, info.sha256)) {
			debug(reply, "no update (hash match)");
			if (notifyWhenUpToDate) {
				reply.accept(Messages.modelUpdateUpToDate());
			}
			return;
		}

		String id = UUID.randomUUID().toString().replace("-", "");
		pending.put(id, new PendingModel(info, null));
		latestPendingId = id;
		if (autoDownload) {
			debug(reply, "update available id=" + id + " (auto-download)");
			downloadModel(id, pending.get(id), reply);
			return;
		}
		reply.accept(Messages.modelUpdateAvailable(Messages.modelUpdateDownloadLink(
			"/scamscreener ai model download " + id
		)));
		debug(reply, "update available id=" + id);
	}

	private void downloadModel(String id, PendingModel pendingModel, Consumer<Component> reply) {
		String payload = fetchText(pendingModel.info().url);
		if (payload == null || payload.isBlank()) {
			debug(reply, "download failed (empty)");
			// Code: MU-UPDATE-001
			reply.accept(Messages.modelUpdateFailed("download failed"));
			return;
		}
		if (pendingModel.info().sha256 != null && !pendingModel.info().sha256.isBlank()) {
			if (!hashMatchesExpected(payload.getBytes(StandardCharsets.UTF_8), pendingModel.info().sha256)) {
				debug(reply, "sha256 mismatch");
				// Code: MU-UPDATE-001
				reply.accept(Messages.modelUpdateFailed("sha256 mismatch"));
				return;
			}
		}
		if (parseIncomingModel(payload) == null) {
			debug(reply, "unsupported model schema in payload");
			reply.accept(Messages.modelUpdateFailed("unsupported model schema"));
			return;
		}

		pending.put(id, new PendingModel(pendingModel.info(), payload));
		latestPendingId = id;
		reply.accept(Messages.modelUpdateReady(buildActionComponent(id)));
		debug(reply, "downloaded id=" + id);
	}

	private static LocalAiModelConfig parseIncomingModel(String payload) {
		if (payload == null || payload.isBlank()) {
			return null;
		}
		try {
			LocalAiModelConfig parsed = GSON.fromJson(payload, LocalAiModelConfig.class);
			if (parsed == null) {
				return null;
			}
			if (parsed.version < 9) {
				return null;
			}
			if (parsed.denseFeatureWeights == null || parsed.denseFeatureWeights.isEmpty()) {
				return null;
			}
			ensureModelShape(parsed);
			return parsed;
		} catch (Exception ignored) {
			return null;
		}
	}

	private static void ensureModelShape(LocalAiModelConfig model) {
		if (model == null) {
			return;
		}
		model.version = Math.max(LocalAiModelConfig.MODEL_SCHEMA_VERSION, model.version);
		if (model.denseFeatureWeights == null || model.denseFeatureWeights.isEmpty()) {
			model.denseFeatureWeights = new LinkedHashMap<>(AiFeatureSpace.defaultDenseWeights());
		} else {
			for (Map.Entry<String, Double> entry : AiFeatureSpace.defaultDenseWeights().entrySet()) {
				model.denseFeatureWeights.putIfAbsent(entry.getKey(), entry.getValue());
			}
		}
		model.maxTokenWeights = LocalAiModelConfig.normalizeMaxTokenWeights(model.maxTokenWeights);
		if (model.tokenWeights == null) {
			model.tokenWeights = new LinkedHashMap<>();
		}
		model.tokenWeights = LocalAiModelConfig.pruneTokenWeights(model.tokenWeights, model.maxTokenWeights);
		if (model.funnelHead == null) {
			model.funnelHead = LocalAiModelConfig.DenseHeadConfig.fromMainHead(model.intercept, model.denseFeatureWeights);
		} else {
			model.funnelHead.normalizeFromMain(model.intercept, model.denseFeatureWeights);
		}
	}

	private static void backupLocalModel() throws IOException {
		Path modelPath = LocalAiModelConfig.filePath();
		if (!Files.exists(modelPath)) {
			return;
		}
		Path archiveDir = modelPath.resolveSibling("old").resolve("models");
		Files.createDirectories(archiveDir);
		Path target = nextArchiveTarget(modelPath, archiveDir);
		Files.copy(modelPath, target, StandardCopyOption.COPY_ATTRIBUTES);
	}

	private static Path nextArchiveTarget(Path baseFile, Path archiveDir) throws IOException {
		int index = 1;
		Path target = archiveDir.resolve(baseFile.getFileName() + ".old." + index);
		while (Files.exists(target)) {
			index++;
			target = archiveDir.resolve(baseFile.getFileName() + ".old." + index);
		}
		return target;
	}

	private static String modelUpdateErrorDetail(Exception error) {
		if (error == null) {
			return "unknown error";
		}
		if (error instanceof NoSuchFileException missing) {
			String file = missing.getFile();
			return "Model file not found: " + (file == null ? "unknown" : file);
		}
		if (error instanceof AccessDeniedException denied) {
			String file = denied.getFile();
			return "Access denied while updating model: " + (file == null ? "unknown" : file);
		}
		String message = error.getMessage();
		if (message == null || message.isBlank()) {
			return error.getClass().getSimpleName();
		}
		String trimmed = message.trim();
		Path modelPath = LocalAiModelConfig.filePath();
		if (modelPath != null && trimmed.equals(modelPath.toString())) {
			return "Model file not found: " + trimmed;
		}
		return trimmed;
	}

	private static byte[] readLocalModelBytes() {
		try {
			Path modelPath = LocalAiModelConfig.filePath();
			if (!Files.exists(modelPath)) {
				return null;
			}
			return Files.readAllBytes(modelPath);
		} catch (IOException ignored) {
			return null;
		}
	}

	private static VersionComparison compareModelVersions(String remoteVersionRaw, byte[] localModelBytes) {
		Integer remoteVersion = parseVersionNumber(remoteVersionRaw);
		Integer localVersion = extractLocalModelVersion(localModelBytes);
		return new VersionComparison(localVersion, remoteVersion);
	}

	private static Integer extractLocalModelVersion(byte[] localModelBytes) {
		if (localModelBytes == null || localModelBytes.length == 0) {
			return null;
		}
		try {
			String text = stripUtf8Bom(new String(localModelBytes, StandardCharsets.UTF_8));
			ModelVersionProbe parsed = GSON.fromJson(text, ModelVersionProbe.class);
			if (parsed == null || parsed.version() == null || parsed.version() < 1) {
				return null;
			}
			return parsed.version();
		} catch (Exception ignored) {
			return null;
		}
	}

	private static Integer parseVersionNumber(String rawVersion) {
		if (rawVersion == null || rawVersion.isBlank()) {
			return null;
		}
		try {
			int parsed = Integer.parseInt(rawVersion.trim());
			return parsed < 1 ? null : parsed;
		} catch (Exception ignored) {
			return null;
		}
	}

	private static boolean hashMatchesExpected(byte[] bytes, String expectedSha) {
		if (bytes == null || expectedSha == null || expectedSha.isBlank()) {
			return false;
		}
		String expected = expectedSha.trim();
		String directHash = sha256(bytes);
		if (directHash != null && directHash.equalsIgnoreCase(expected)) {
			return true;
		}
		String text = new String(bytes, StandardCharsets.UTF_8);
		Set<String> variants = new LinkedHashSet<>();
		addHashTextVariants(variants, text);
		addHashTextVariants(variants, normalizeLineEndingsToLf(text));
		addHashTextVariants(variants, normalizeLineEndingsToCrlf(text));
		for (String variant : variants) {
			String variantHash = sha256(variant.getBytes(StandardCharsets.UTF_8));
			if (variantHash != null && variantHash.equalsIgnoreCase(expected)) {
				return true;
			}
		}
		return false;
	}

	private static void addHashTextVariants(Set<String> variants, String value) {
		if (value == null) {
			return;
		}
		String withoutBom = stripUtf8Bom(value);
		variants.add(withoutBom);
		variants.add("\uFEFF" + withoutBom);
	}

	private static String normalizeLineEndingsToLf(String value) {
		if (value == null || value.isEmpty()) {
			return value;
		}
		return value.replace("\r\n", "\n").replace("\r", "\n");
	}

	private static String normalizeLineEndingsToCrlf(String value) {
		String lf = normalizeLineEndingsToLf(value);
		if (lf == null || lf.isEmpty()) {
			return lf;
		}
		return lf.replace("\n", "\r\n");
	}

	private static String stripUtf8Bom(String value) {
		if (value != null && !value.isEmpty() && value.charAt(0) == '\uFEFF') {
			return value.substring(1);
		}
		return value;
	}

	private static String sha256(byte[] bytes) {
		if (bytes == null) {
			return null;
		}
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(bytes);
			StringBuilder out = new StringBuilder();
			for (byte b : hash) {
				out.append(String.format("%02x", b));
			}
			return out.toString();
		} catch (Exception ignored) {
			return null;
		}
	}

	private static FetchResult fetchVersionInfo() {
		String payload = fetchText(VERSION_URL);
		if (payload == null || payload.isBlank()) {
			return new FetchResult(null, "empty response");
		}
		try {
			return new FetchResult(GSON.fromJson(payload, ModelVersionInfo.class), null);
		} catch (Exception e) {
			return new FetchResult(null, e.getMessage());
		}
	}

	private static String fetchText(String url) {
		if (url == null || url.isBlank()) {
			return null;
		}
		try {
			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.GET()
				.timeout(Duration.ofSeconds(8))
				.header("User-Agent", "ScamScreener")
				.build();
			HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				return null;
			}
			return response.body();
		} catch (Exception ignored) {
			return null;
		}
	}

	private void debug(Consumer<Component> reply, String message) {
		if (!debugEnabled || reply == null) {
			return;
		}
		reply.accept(DebugMessages.updater(message));
	}

	private PendingModel removePending(String id) {
		PendingModel removed = pending.remove(id);
		if (id != null && id.equals(latestPendingId)) {
			latestPendingId = null;
		}
		return removed;
	}

	private static MutableComponent buildActionComponent(String id) {
		MutableComponent line = Component.empty()
			.append(Component.literal("[Accept]").withStyle(Style.EMPTY
				.withColor(ChatFormatting.GREEN)
				.withClickEvent(new ClickEvent.RunCommand("/scamscreener ai model accept " + id))
				.withHoverEvent(new HoverEvent.ShowText(Component.literal("Replace with new model")))))
			.append(Component.literal(" "))
			.append(Component.literal("[Merge]").withStyle(Style.EMPTY
				.withColor(ChatFormatting.GOLD)
				.withClickEvent(new ClickEvent.RunCommand("/scamscreener ai model merge " + id))
				.withHoverEvent(new HoverEvent.ShowText(Component.literal("Merge with local model")))))
			.append(Component.literal(" "))
			.append(Component.literal("[Ignore]").withStyle(Style.EMPTY
				.withColor(ChatFormatting.RED)
				.withClickEvent(new ClickEvent.RunCommand("/scamscreener ai model ignore " + id))
				.withHoverEvent(new HoverEvent.ShowText(Component.literal("Ignore this update")))));
		return line;
	}

	private record ModelVersionInfo(String version, String sha256, String url) {
	}

	private record ModelVersionProbe(Integer version) {
	}

	private record VersionComparison(Integer localVersion, Integer remoteVersion) {
		private boolean comparable() {
			return localVersion != null && remoteVersion != null;
		}

		private boolean upToDate() {
			return comparable() && remoteVersion <= localVersion;
		}
	}

	private record FetchResult(ModelVersionInfo info, String error) {
	}

	private record PendingModel(ModelVersionInfo info, String content) {
	}

	public record PendingUpdateSnapshot(String id, String version, boolean downloaded) {
	}
}
