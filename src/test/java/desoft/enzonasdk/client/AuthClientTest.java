package desoft.enzonasdk.client;

import com.desoft.enzonasdk.client.AuthClient;
import com.desoft.enzonasdk.exception.EnzonaException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;
import org.apache.http.client.HttpClient;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import static org.junit.Assert.*;

public class AuthClientTest {

    @Mock
    private HttpClient mockHttpClient;
    @Mock
    private HttpResponse mockHttpResponse;
    @Mock
    private StatusLine mockStatusLine;

    private AuthClient authClient;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        authClient = new AuthClient("consumerKey", "consumerSecret");
    }

    @Test
    public void getAccessToken_Success() throws Exception {
        // Setup mock response
        when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(mockHttpResponse);
        when(mockHttpResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(200);
        when(mockHttpResponse.getEntity()).thenReturn(new StringEntity("{\"access_token\":\"validToken123\"}"));

        // Execute
        String accessToken = authClient.getAccessToken();

        // Verify
        assertNotNull("Access token should not be null", accessToken);
        assertEquals("Access token should match expected value", "validToken123", accessToken);
    }

    @Test(expected = EnzonaException.class)
    public void getAccessToken_Failure() throws Exception {
        // Setup mock response
        when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(mockHttpResponse);
        when(mockHttpResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(401);
        when(mockHttpResponse.getEntity()).thenReturn(new StringEntity("{\"error\":\"Unauthorized\"}"));

        // This should throw an exception
        authClient.getAccessToken();
    }
}
