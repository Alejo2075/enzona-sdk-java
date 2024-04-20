package io.github.alejo2075.enzonasdk.client;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.HttpClient;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;

import io.github.alejo2075.enzonasdk.exception.EnzonaException;
import io.github.alejo2075.enzonasdk.model.request.CreateClaimsRequest;
import io.github.alejo2075.enzonasdk.model.response.CreateClaimsResponse;

import java.io.IOException;

@ExtendWith(MockitoExtension.class)
public class ClaimApiTest {

    @Mock
    private CloseableHttpClient mockHttpClient;
    @Mock
    private CloseableHttpResponse mockResponse;
    @Mock
    private StatusLine mockStatusLine;

    private ClaimApi claimApi;

    @BeforeEach
    public void setUp() {
        claimApi = new ClaimApi("consumerKey", "consumerSecret");
        claimApi.setHttpClient(mockHttpClient);
    }

    @Test
    @DisplayName("Test createClaims fails with EnzonaException on 400 status")
    void testCreateClaimsFailure() throws IOException {
        when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(400);
        when(mockResponse.getEntity()).thenReturn(new StringEntity("{\"error\":\"Bad Request\"}", "UTF-8"));

        CreateClaimsRequest request = new CreateClaimsRequest(); // Populate request object as needed

        EnzonaException exception = assertThrows(EnzonaException.class, () -> claimApi.createClaims(request),
                "Expected createClaims to throw, but it did not");

        assertTrue(exception.getMessage().contains("Bad Request"), "Exception message should contain 'Bad Request'");
    }

    @Test
    @DisplayName("Test createClaims succeeds and returns expected result")
    void testCreateClaimsSuccess() throws Exception {
        String jsonResponse = "{\"message\":\"Claim created successfully\", \"status\":\"success\"}";
        when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(200);
        when(mockResponse.getEntity()).thenReturn(new StringEntity(jsonResponse, "UTF-8"));

        CreateClaimsRequest request = new CreateClaimsRequest(); // Populate request object as needed
        CreateClaimsResponse response = claimApi.createClaims(request);

        assertAll("Successful claim creation",
                () -> assertNotNull(response, "Response should not be null"),
                () -> assertEquals("success", response.getStatusCode(), "Status code should be 'success'"),
                () -> assertEquals("Claim created successfully", response.getMessage(), "Message should match expected value")
        );
    }
}
