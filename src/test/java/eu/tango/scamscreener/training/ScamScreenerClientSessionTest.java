package eu.tango.scamscreener.training;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScamScreenerClientSessionTest {
    @Test
    void parsesLoginSuccessResponse() {
        ScamScreenerClientSession session = ScamScreenerClientSession.parseLoginResponse(
            200,
            """
                {
                  "status": "ok",
                  "sessionToken": "TOKEN_VALUE",
                  "expiresAt": "2099-03-28T20:15:00Z",
                  "user": {
                    "id": 1,
                    "username": "alice",
                    "isAdmin": false
                  }
                }
                """,
            "fallback-user"
        );

        assertEquals("alice", session.username());
        assertEquals(Instant.parse("2099-03-28T20:15:00Z"), session.expiresAt());
    }

    @Test
    void fallsBackToSubmittedUsernameWhenResponseUserIsMissing() {
        ScamScreenerClientSession session = ScamScreenerClientSession.parseLoginResponse(
            200,
            """
                {
                  "status": "ok",
                  "sessionToken": "TOKEN_VALUE",
                  "expiresAt": "2099-03-28T20:15:00Z"
                }
                """,
            "alice@example.com"
        );

        assertEquals("alice@example.com", session.username());
    }

    @Test
    void parsesAcceptedUploadResponse() {
        ScamScreenerClientSession.UploadResult result = ScamScreenerClientSession.parseUploadResponse(
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
        ScamScreenerClientSession.UploadResult result = ScamScreenerClientSession.parseUploadResponse(
            429,
            """
                {
                  "status": "quota-exceeded",
                  "detail": "Daily upload count limit reached for your account.",
                  "caseCount": 34,
                  "sha256": "abc"
                }
                """
        );

        assertEquals("quota-exceeded", result.status());
        assertEquals(34, result.caseCount());
        assertEquals("Daily upload count limit reached for your account.", result.detail());
    }

    @Test
    void throwsSessionExpiredForUnauthorizedUploadResponse() {
        assertThrows(
            ScamScreenerClientSession.SessionExpiredException.class,
            () -> ScamScreenerClientSession.parseUploadResponse(
                401,
                """
                    {
                      "detail": "Expired Bearer token."
                    }
                    """
            )
        );
    }

    @Test
    void throwsUploadRejectedForBadRequestUploadResponse() {
        assertThrows(
            ScamScreenerClientSession.UploadRejectedException.class,
            () -> ScamScreenerClientSession.parseUploadResponse(
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
    void includesRetryAfterForLockedLoginResponse() {
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> ScamScreenerClientSession.parseLoginResponse(
                429,
                """
                    {
                      "status": "locked",
                      "retryAfter": 120
                    }
                    """,
                "alice"
            )
        );

        assertEquals("locked Retry after 120s.", exception.getMessage());
    }
}
