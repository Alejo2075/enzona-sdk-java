package desoft.enzonasdk.client;

import com.desoft.enzonasdk.client.ClaimApi;
import com.desoft.enzonasdk.exception.EnzonaException;
import com.desoft.enzonasdk.model.request.CreateClaimsRequest;
import com.desoft.enzonasdk.model.response.CreateClaimsResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.junit.Before;

public class ClaimApiTest {

    @Mock
    private CloseableHttpClient mockHttpClient;
    @Mock
    private CloseableHttpResponse mockResponse;
    @Mock
    private StatusLine mockStatusLine;

    private ClaimApi claimApi;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        claimApi = new ClaimApi("consumerKey", "consumerSecret");
        claimApi.setHttpClient(mockHttpClient);
    }

    @Test(expected = EnzonaException.class)
    public void testCreateClaimsFailure() throws Exception {
        when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(400);
        when(mockResponse.getEntity()).thenReturn(new StringEntity("{\"error\":\"Bad Request\"}"));

        CreateClaimsRequest request = new CreateClaimsRequest(); // Populate request object as needed
        claimApi.createClaims(request);
    }

    @Test
    public void testCreateClaimsSuccess() throws Exception {
        String jsonResponse = "{\"message\":\"Claim created successfully\", \"status\":\"success\"}";
        when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(200);
        when(mockResponse.getEntity()).thenReturn(new StringEntity(jsonResponse));

        CreateClaimsRequest request = new CreateClaimsRequest(); // Populate request object as needed
        CreateClaimsResponse response = claimApi.createClaims(request);

        assertNotNull(response);
        assertEquals("success", response.getStatusCode());
        assertEquals("Claim created successfully", response.getMessage());
    }
}
