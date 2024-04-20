package io.github.alejo2075.enzonasdk.client;

import io.github.alejo2075.enzonasdk.exception.EnzonaException;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class AuthClientTest {

    @Mock
    private HttpClient mockHttpClient;
    @Mock
    private HttpResponse mockHttpResponse;
    @Mock
    private StatusLine mockStatusLine;

    private AuthClient authClient;

    @BeforeEach
    public void setUp() {
        authClient = new AuthClient("consumerKey", "consumerSecret");
        authClient.setHttpClient(mockHttpClient);
    }

    @Test
    @DisplayName("getAccessToken returns valid token when response is successful")
    public void getAccessToken_Success() throws Exception {
        // Setup mock response
        when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(mockHttpResponse);
        when(mockHttpResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(200);
        when(mockHttpResponse.getEntity()).thenReturn(new StringEntity("{\"access_token\":\"validToken123\"}"));

        // Execute
        String accessToken = authClient.getAccessToken();

        // Verify
        assertAll("Valid access token retrieval",
                () -> assertNotNull(accessToken, "Access token should not be null"),
                () -> assertEquals("validToken123", accessToken, "Access token should match expected value")
        );
    }

    @Test
    @DisplayName("getAccessToken throws EnzonaException when response is unauthorized")
    public void getAccessToken_Failure() throws IOException {
        // Setup mock response
        when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(mockHttpResponse);
        when(mockHttpResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(401);
        when(mockHttpResponse.getEntity()).thenReturn(new StringEntity("{\"error\":\"Unauthorized\"}"));

        // This should throw an exception
        EnzonaException thrown = assertThrows(EnzonaException.class, () -> authClient.getAccessToken(),
                "Should throw an EnzonaException for unauthorized access");

        assertTrue(thrown.getMessage().contains("Unauthorized"), "Exception message should include 'Unauthorized'");
    }
}
