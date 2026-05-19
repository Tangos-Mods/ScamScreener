package eu.tango.scamscreener;

import eu.tango.scamscreener.chat.ChatPipelineListener;
import eu.tango.scamscreener.chat.mute.ChatMuteFilter;
import eu.tango.scamscreener.command.ScamScreenerCommandHandler;
import eu.tango.scamscreener.message.DisabledJoinNotifier;
import eu.tango.scamscreener.message.DecisionMessageHandler;
import eu.tango.scamscreener.message.UpdateJoinNotifier;
import eu.tango.scamscreener.profiler.ScamScreenerProfiler;
import eu.tango.scamscreener.profiler.web.ProfilerWebService;
import eu.tango.scamscreener.review.ReviewCaptureHandler;
import eu.tango.scamscreener.training.TrainingUploadReminder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScamScreenerMod implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("scamscreener");
    private static final String MOD_ID = "scamscreener";
    private static final String DEFAULT_MOD_VERSION = /*$ mod_version*/ "0.1.0";
    private static final String DEFAULT_MINECRAFT_VERSION = /*$ minecraft*/ "1.21.11";
    public static final String VERSION = resolveModVersion();
    public static final String MINECRAFT = resolveMinecraftVersion();

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing ScamScreener");
        ScamScreenerRuntime.getInstance();
        ScamScreenerProfiler.getInstance().initialize();
        ProfilerWebService.getInstance().initialize();
        ScamScreenerCommandHandler.initialize();
        ChatMuteFilter.initialize();
        ReviewCaptureHandler.initialize();
        DecisionMessageHandler.initialize();
        ChatPipelineListener.initialize();
        TrainingUploadReminder.initialize();
        DisabledJoinNotifier.initialize();
        UpdateJoinNotifier.initialize();

        //? if !release
        LOGGER.warn("Running a non-release ScamScreener build.");

        //? if fapi: <0.100
        /*LOGGER.info("Fabric API is old on this version");*/
    }

    /**
     * Adapts to cross-version {@link Identifier} creation.
     */
    public static Identifier id(String namespace, String path) {
        //? if <1.21 {
        /*return new Identifier(namespace, path);
        *///?} else
        return Identifier.of(namespace, path);
    }

    private static String resolveModVersion() {
        String fullVersion = FabricLoader.getInstance()
            .getModContainer(MOD_ID)
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse(DEFAULT_MOD_VERSION);

        int separatorIndex = fullVersion.indexOf('+');
        if (separatorIndex <= 0) {
            return fullVersion;
        }

        return fullVersion.substring(0, separatorIndex);
    }

    private static String resolveMinecraftVersion() {
        return FabricLoader.getInstance()
            .getModContainer("minecraft")
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse(DEFAULT_MINECRAFT_VERSION);
    }
}
