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
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScamScreenerMod implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("scamscreener");
    public static final String VERSION = "2.2.0";
    public static final String MINECRAFT = "26.1";

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
        DisabledJoinNotifier.initialize();
        UpdateJoinNotifier.initialize();
    }

    public static Identifier id(String namespace, String path) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }
}
