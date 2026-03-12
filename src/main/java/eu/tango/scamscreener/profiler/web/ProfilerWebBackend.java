package eu.tango.scamscreener.profiler.web;

import java.net.URI;

interface ProfilerWebBackend {
    void initialize();

    URI profilerUri();
}
