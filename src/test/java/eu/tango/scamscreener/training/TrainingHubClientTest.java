package eu.tango.scamscreener.training;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainingHubClientTest {
    private static final URI LOCAL_TEST_BASE_URI = URI.create("http://128.0.0.1/");
    private static final Duration LOCAL_TEST_TIMEOUT = Duration.ofSeconds(2);

    @Test
    void createsDeterministicUploadHandshake() {
        TrainingHubClient.UploadHandshake handshake = TrainingHubClient.createUploadHandshake("client-abc", "line1\nline2");

        assertEquals("683376e290829b482c2655745caffa7a1dccfa10afaa62dac2b42dd6c68d0f83", handshake.payloadSha256());
        assertEquals("0a5dea124999a5f92776028f4912e4f087653fbe23a4d652439f5d0acb27ce57", handshake.handshakeSha256());
    }

    @Test
    void parsesAcceptedUploadResponse() {
        TrainingHubClient.UploadResult result = TrainingHubClient.parseUploadResponse(
            201,
            """
                {
                  "status": "accepted",
                  "uploadId": 12,
                  "caseCount": 34,
                  "insertedCases": 34,
                  "updatedCases": 0,
                  "sha256": "abc"
                }
                """
        );

        assertEquals("accepted", result.status());
        assertEquals(12L, result.uploadId());
        assertEquals(34, result.caseCount());
        assertEquals(34, result.insertedCases());
        assertEquals(0, result.updatedCases());
        assertEquals("abc", result.sha256());
    }

    @Test
    void parsesQuotaExceededUploadResponseFromRateLimitStatus() {
        TrainingHubClient.UploadResult result = TrainingHubClient.parseUploadResponse(
            429,
            """
                {
                  "status": "quota-exceeded",
                  "detail": "Daily upload count limit reached for this client.",
                  "caseCount": 34,
                  "sha256": "abc"
                }
                """
        );

        assertEquals("quota-exceeded", result.status());
        assertEquals(34, result.caseCount());
        assertEquals("Daily upload count limit reached for this client.", result.detail());
    }

    @Test
    void throwsUploadRejectedForBadRequestUploadResponse() {
        assertThrows(
            TrainingHubClient.UploadRejectedException.class,
            () -> TrainingHubClient.parseUploadResponse(
                400,
                """
                    {
                      "detail": "Invalid schema."
                    }
                    """
            )
        );
    }

    @Test
    void throwsUploadRejectedForInvalidContentTypeUploadResponse() {
        assertThrows(
            TrainingHubClient.UploadRejectedException.class,
            () -> TrainingHubClient.parseUploadResponse(
                415,
                """
                    {
                      "detail": "Unsupported media type."
                    }
                    """
            )
        );
    }

    @Test
    void includesRetryAfterForRateLimitedUploadResponse() {
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> TrainingHubClient.parseUploadResponse(
                429,
                """
                    {
                      "status": "rate-limited",
                      "retryAfter": 120
                    }
                    """
            )
        );

        assertEquals("rate-limited Retry after 120s.", exception.getMessage());
    }

    @Test
    void uploadsAgainstLocalTestWebsiteWhenReachable(@TempDir Path tempDir) throws Exception {
        assumeLocalTestWebsiteReachable();

        Path trainingCasesFile = tempDir.resolve("training-cases-v2.jsonl");
        Files.writeString(
            trainingCasesFile,
            """
                {"format":"training_case_v2","schemaVersion":2,"caseId":"case.test-local.review-1"}
                """,
            StandardCharsets.UTF_8
        );

        try {
            TrainingHubClient.UploadResult result = TrainingHubClient.uploadTrainingDataAsync(
                    LOCAL_TEST_BASE_URI,
                    "test-local-client",
                    trainingCasesFile
                )
                .get(10, TimeUnit.SECONDS);

            assertNotNull(result);
            assertTrue(Set.of("accepted", "duplicate", "quota-exceeded").contains(result.status()));
        } catch (ExecutionException exception) {
            Throwable rootCause = rootCause(exception);
            assertTrue(
                rootCause instanceof TrainingHubClient.UploadRejectedException || rootCause instanceof IllegalStateException,
                "unexpected local test server failure: " + rootCause
            );
        } catch (TimeoutException exception) {
            throw new AssertionError("local test website did not answer within 10s", exception);
        }
    }

    private static void assumeLocalTestWebsiteReachable() {
        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(LOCAL_TEST_TIMEOUT)
            .build();
        HttpRequest request = HttpRequest.newBuilder(LOCAL_TEST_BASE_URI)
            .timeout(LOCAL_TEST_TIMEOUT)
            .GET()
            .build();

        try {
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            Assumptions.assumeTrue(response.statusCode() > 0, "local test website on 128.0.0.1 is not reachable");
        } catch (ConnectException exception) {
            Assumptions.assumeTrue(false, "local test website on 128.0.0.1 is not reachable");
        } catch (Exception exception) {
            Throwable rootCause = rootCause(exception);
            if (rootCause instanceof ConnectException) {
                Assumptions.assumeTrue(false, "local test website on 128.0.0.1 is not reachable");
                return;
            }

            throw new AssertionError("failed to probe local test website", exception);
        }
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable rootCause = throwable;
        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }

        return rootCause;
    }
}
