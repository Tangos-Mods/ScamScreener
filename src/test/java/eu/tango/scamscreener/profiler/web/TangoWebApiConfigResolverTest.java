package eu.tango.scamscreener.profiler.web;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TangoWebApiConfigResolverTest {
    @Test
    void normalizeBrowserHostMapsWildcardHostsToLoopback() {
        assertEquals("127.0.0.1", TangoWebApiConfigResolver.normalizeBrowserHost("0.0.0.0"));
        assertEquals("127.0.0.1", TangoWebApiConfigResolver.normalizeBrowserHost("::"));
        assertEquals("127.0.0.1", TangoWebApiConfigResolver.normalizeBrowserHost("*"));
    }

    @Test
    void buildProfilerUriUsesNormalizedHostAndStaticPath() {
        URI uri = TangoWebApiConfigResolver.buildProfilerUri("0.0.0.0", 9090, "scamscreener", "profiler", "index.html");

        assertEquals("http://127.0.0.1:9090/scamscreener/profiler/index.html", uri.toString());
    }
}
