package eu.tango.scamscreener.profiler.web;

import java.net.URI;

public record ProfilerWebOpenResult(Status status, URI uri, String detail) {
    public static ProfilerWebOpenResult success(URI uri) {
        return new ProfilerWebOpenResult(Status.SUCCESS, uri, "");
    }

    public static ProfilerWebOpenResult missingDependency() {
        return new ProfilerWebOpenResult(Status.MISSING_DEPENDENCY, null, "");
    }

    public static ProfilerWebOpenResult unavailable(String detail) {
        return new ProfilerWebOpenResult(Status.UNAVAILABLE, null, detail == null ? "" : detail.trim());
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS && uri != null;
    }

    public boolean isMissingDependency() {
        return status == Status.MISSING_DEPENDENCY;
    }

    public enum Status {
        SUCCESS,
        MISSING_DEPENDENCY,
        UNAVAILABLE
    }
}
