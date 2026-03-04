package eu.tango.scamscreener;

import eu.tango.scamscreener.chat.ChatPipelineListener;
import eu.tango.scamscreener.chat.mute.ChatMuteFilter;
import eu.tango.scamscreener.command.ScamScreenerCommandHandler;
import eu.tango.scamscreener.message.DecisionMessageHandler;
import eu.tango.scamscreener.review.ReviewCaptureHandler;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScamScreenerMod implements ClientModInitializer {
    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("scamscreener");
    public static final String VERSION = /*$ mod_version*/ "0.1.0";
    public static final String MINECRAFT = /*$ minecraft*/ "1.21.11";

    @Override
    public void onInitializeClient() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.
        LOGGER.info("Initializing ScamScreener");
        ScamScreenerRuntime.getInstance();
        ScamScreenerCommandHandler.initialize();
        ChatMuteFilter.initialize();
        ReviewCaptureHandler.initialize();
        DecisionMessageHandler.initialize();
        ChatPipelineListener.initialize();

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
}
