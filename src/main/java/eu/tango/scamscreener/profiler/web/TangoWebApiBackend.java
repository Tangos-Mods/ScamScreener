package eu.tango.scamscreener.profiler.web;

import eu.pankraz01.tangowebapi.RequestContext;
import eu.pankraz01.tangowebapi.TangoWebAPI;
import eu.tango.scamscreener.ScamScreenerMod;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;

final class TangoWebApiBackend implements ProfilerWebBackend {
    private static final String MOD_ID = "scamscreener";
    private static final String STATIC_PREFIX = "profiler";
    private static final String STATIC_INDEX = "index.html";
    private static final String SNAPSHOT_ROUTE = "api/profiler";
    private static final String RESET_ROUTE = "api/profiler/reset";
    private static final String EXPORT_ROUTE = "api/profiler/export";

    private boolean initialized;
    private URI profilerUri;

    @Override
    public synchronized void initialize() {
        if (initialized) {
            return;
        }

        registerStaticSite();
        TangoWebAPI.get(MOD_ID, SNAPSHOT_ROUTE, TangoWebApiBackend::handleSnapshot);
        TangoWebAPI.get(MOD_ID, RESET_ROUTE, TangoWebApiBackend::handleReset);
        TangoWebAPI.get(MOD_ID, EXPORT_ROUTE, TangoWebApiBackend::handleExport);
        profilerUri = TangoWebApiConfigResolver.resolveProfilerUri(MOD_ID, STATIC_PREFIX, STATIC_INDEX);
        initialized = true;
    }

    @Override
    public synchronized URI profilerUri() {
        if (!initialized) {
            initialize();
        }

        return profilerUri;
    }

    private static void handleSnapshot(RequestContext context) throws IOException {
        context.sendJson(200, ProfilerWebPayload.snapshotJson());
    }

    private static void handleReset(RequestContext context) throws IOException {
        eu.tango.scamscreener.profiler.ScamScreenerProfiler.getInstance().reset();
        context.sendJson(200, ProfilerWebPayload.snapshotJson());
    }

    private static void handleExport(RequestContext context) throws IOException {
        ProfilerExportPayload.ExportFile export = ProfilerExportPayload.exportFile();
        context.sendBytes(200, export.mimeType(), export.bytes());
    }

    private static void registerStaticSite() {
        Optional<ModContainer> container = FabricLoader.getInstance().getModContainer(MOD_ID);
        if (container.isEmpty()) {
            throw new IllegalStateException("Could not resolve ScamScreener mod container for web assets.");
        }

        Path assetRoot = container.get()
            .findPath("assets/scamscreener/web")
            .orElseThrow(() -> new IllegalStateException("Profiler web assets are missing under assets/scamscreener/web."));
        TangoWebAPI.serveStatic(MOD_ID, STATIC_PREFIX, assetRoot);
        ScamScreenerMod.LOGGER.info("Registered TangoWeb profiler routes.");
    }
}
