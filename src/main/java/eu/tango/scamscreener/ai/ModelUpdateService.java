package eu.tango.scamscreener.ai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import eu.tango.scamscreener.config.LocalAiModelConfig;
import eu.tango.scamscreener.rules.ScamRules;
import eu.tango.scamscreener.ui.Messages;
import eu.tango.scamscreener.ui.DebugMessages;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public final class ModelUpdateService {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final HttpClient HTTP = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(6))
		.build();
	private static final String VERSION_URL = "https://raw.githubusercontent.com/Tangos-Mods/ScamScreener/main/scripts/model-version.json";

	private final Map<String, PendingModel> pending = new LinkedHashMap<>();
	private boolean debugEnabled;

	public void checkForUpdateAsync(Consumer<Component> reply) {
		debug(reply, "check started");
		Thread thread = new Thread(() -> checkForUpdate(reply), "scamscreener-model-check");
		thread.setDaemon(true);
		thread.start();
	}

	public void setDebugEnabled(boolean enabled) {
		debugEnabled = enabled;
	}

	public boolean isDebugEnabled() {
		return debugEnabled;
	}

	public int download(String id, Consumer<Component> reply) {
		PendingModel pendingModel = pending.get(id);
		if (pendingModel == null || pendingModel.info() == null) {
			reply.accept(Messages.modelUpdateNotFound());
			return 0;
		}
		debug(reply, "download requested id=" + id);
		Thread thread = new Thread(() -> downloadModel(id, pendingModel, reply), "scamscreener-model-download");
		thread.setDaemon(true);
		thread.start();
		return 1;
	}

	public int accept(String id, Consumer<Component> reply) {
		PendingModel pendingModel = pending.get(id);
		if (pendingModel == null || pendingModel.content() == null) {
			reply.accept(Messages.modelUpdateNotReady());
			return 0;
		}
		try {
			backupLocalModel();
			writeModel(pendingModel.content());
			ScamRules.reloadConfig();
			reply.accept(Messages.modelUpdateApplied("accepted"));
			debug(reply, "accepted id=" + id);
			pending.remove(id);
			return 1;
		} catch (IOException e) {
			debug(reply, "accept failed: " + e.getMessage());
			reply.accept(Messages.modelUpdateFailed(e.getMessage()));
			return 0;
		}
	}

	public int merge(String id, Consumer<Component> reply) {
		PendingModel pendingModel = pending.get(id);
		if (pendingModel == null || pendingModel.content() == null) {
			reply.accept(Messages.modelUpdateNotReady());
			return 0;
		}
		try {
			backupLocalModel();
			LocalAiModelConfig local = LocalAiModelConfig.loadOrCreate();
			LocalAiModelConfig incoming = GSON.fromJson(pendingModel.content(), LocalAiModelConfig.class);
			if (incoming == null) {
				reply.accept(Messages.modelUpdateFailed("invalid model payload"));
				return 0;
			}
			if (incoming.tokenWeights == null) {
				incoming.tokenWeights = new LinkedHashMap<>();
			}
			if (local.tokenWeights != null) {
				for (Map.Entry<String, Double> entry : local.tokenWeights.entrySet()) {
					incoming.tokenWeights.putIfAbsent(entry.getKey(), entry.getValue());
				}
			}
			LocalAiModelConfig.save(incoming);
			ScamRules.reloadConfig();
			reply.accept(Messages.modelUpdateApplied("merged"));
			debug(reply, "merged id=" + id);
			pending.remove(id);
			return 1;
		} catch (Exception e) {
			debug(reply, "merge failed: " + e.getMessage());
			reply.accept(Messages.modelUpdateFailed(e.getMessage()));
			return 0;
		}
	}

	public int ignore(String id, Consumer<Component> reply) {
		if (pending.remove(id) != null) {
			reply.accept(Messages.modelUpdateIgnored());
			debug(reply, "ignored id=" + id);
			return 1;
		}
		reply.accept(Messages.modelUpdateNotFound());
		return 0;
	}

	private void checkForUpdate(Consumer<Component> reply) {
		ModelVersionInfo info = fetchVersionInfo();
		if (info == null) {
			debug(reply, "version info not found");
		}
		if (info == null || info.url == null || info.url.isBlank()) {
			return;
		}
		String localHash = hashLocalModel();
		debug(reply, "local hash=" + (localHash == null ? "none" : localHash));
		debug(reply, "remote sha256=" + (info.sha256 == null ? "none" : info.sha256));
		if (localHash != null && info.sha256 != null && !info.sha256.isBlank()
			&& localHash.equalsIgnoreCase(info.sha256.trim())) {
			debug(reply, "no update (hash match)");
			return;
		}

		String id = UUID.randomUUID().toString().replace("-", "");
		pending.put(id, new PendingModel(info, null));
		String localVersion = String.valueOf(LocalAiModelConfig.loadOrCreate().version);
		reply.accept(Messages.modelUpdateAvailable(Messages.modelUpdateDownloadLink(
			"/scamscreener ai model download " + id,
			localVersion,
			info.version
		)));
		debug(reply, "update available id=" + id);
	}

	private void downloadModel(String id, PendingModel pendingModel, Consumer<Component> reply) {
		String payload = fetchText(pendingModel.info().url);
		if (payload == null || payload.isBlank()) {
			debug(reply, "download failed (empty)");
			reply.accept(Messages.modelUpdateFailed("download failed"));
			return;
		}
		if (pendingModel.info().sha256 != null && !pendingModel.info().sha256.isBlank()) {
			String hash = sha256(payload.getBytes(StandardCharsets.UTF_8));
			if (hash != null && !hash.equalsIgnoreCase(pendingModel.info().sha256.trim())) {
				debug(reply, "sha256 mismatch");
				reply.accept(Messages.modelUpdateFailed("sha256 mismatch"));
				return;
			}
		}

		pending.put(id, new PendingModel(pendingModel.info(), payload));
		reply.accept(Messages.modelUpdateReady(buildActionComponent(id)));
		debug(reply, "downloaded id=" + id);
	}

	private static void writeModel(String payload) throws IOException {
		Files.createDirectories(LocalAiModelConfig.filePath().getParent());
		Files.writeString(LocalAiModelConfig.filePath(), payload, StandardCharsets.UTF_8);
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

	private static String hashLocalModel() {
		try {
			if (!Files.exists(LocalAiModelConfig.filePath())) {
				return null;
			}
			byte[] bytes = Files.readAllBytes(LocalAiModelConfig.filePath());
			return sha256(bytes);
		} catch (IOException ignored) {
			return null;
		}
	}

	private static String sha256(byte[] bytes) {
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

	private static ModelVersionInfo fetchVersionInfo() {
		String payload = fetchText(VERSION_URL);
		if (payload == null || payload.isBlank()) {
			return null;
		}
		try {
			return GSON.fromJson(payload, ModelVersionInfo.class);
		} catch (Exception ignored) {
			return null;
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

	private record PendingModel(ModelVersionInfo info, String content) {
	}
}
