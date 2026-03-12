package eu.tango.scamscreener.profiler.web;

import eu.tango.scamscreener.ScamScreenerMod;
import eu.tango.scamscreener.profiler.ScamScreenerProfiler;
import net.fabricmc.loader.api.FabricLoader;

public final class ProfilerWebService {
    private static final String PRIMARY_TANGO_WEBAPI_MOD_ID = "tango_webapi";
    private static final String LEGACY_TANGO_WEBAPI_MOD_ID = "tango-webapi";
    public static final String DOWNLOAD_URL = "https://modrinth.com/mod/tango-webapi";
    private static final ProfilerWebService INSTANCE = new ProfilerWebService();

    private volatile boolean initialized;
    private volatile String initializationFailure = "";
    private volatile ProfilerWebBackend backend;

    private ProfilerWebService() {
    }

    public static ProfilerWebService getInstance() {
        return INSTANCE;
    }

    public synchronized void initialize() {
        if (initialized) {
            return;
        }

        initialized = true;
        if (!isDependencyLoaded()) {
            return;
        }

        try {
            backend = instantiateBackend();
            backend.initialize();
        } catch (Throwable throwable) {
            initializationFailure = throwable.getMessage() == null ? throwable.getClass().getSimpleName() : throwable.getMessage().trim();
            ScamScreenerMod.LOGGER.warn("Could not initialize TangoWeb profiler integration.", throwable);
            backend = null;
        }
    }

    public ProfilerWebOpenResult open() {
        initialize();
        if (!isDependencyLoaded()) {
            return ProfilerWebOpenResult.missingDependency();
        }

        ProfilerWebBackend activeBackend = backend;
        if (activeBackend == null) {
            return ProfilerWebOpenResult.unavailable(
                initializationFailure == null || initializationFailure.isBlank()
                    ? "Tango Web API routes are unavailable. Check the log for details."
                    : initializationFailure
            );
        }

        return ProfilerWebOpenResult.success(activeBackend.profilerUri());
    }

    public static boolean isDependencyLoaded() {
        FabricLoader loader = FabricLoader.getInstance();
        return loader.isModLoaded(PRIMARY_TANGO_WEBAPI_MOD_ID) || loader.isModLoaded(LEGACY_TANGO_WEBAPI_MOD_ID);
    }

    private static ProfilerWebBackend instantiateBackend() {
        try {
            Class<?> backendClass = Class.forName("eu.tango.scamscreener.profiler.web.TangoWebApiBackend");
            return (ProfilerWebBackend) backendClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not load Tango Web API backend.", exception);
        }
    }
}
