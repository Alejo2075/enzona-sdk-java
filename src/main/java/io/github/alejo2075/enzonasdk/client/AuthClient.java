package io.github.alejo2075.enzonasdk.client;

import io.github.alejo2075.enzonasdk.util.JsonUtil;
import io.github.alejo2075.enzonasdk.exception.EnzonaException;
import lombok.Data;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Client for authenticating with the Enzona API and retrieving an access token.
 */
@Data
public class AuthClient {

    private HttpClient httpClient;
    private final String consumerKey;
    private final String consumerSecret;
    private final String tokenEndpoint = "https://api.enzona.net/token";

    /**
     * Creates an instance of AuthClient using the provided consumer key and secret.
     *
     * @param consumerKey    The consumer key issued by Enzona for API authentication.
     * @param consumerSecret The consumer secret issued by Enzona for API authentication.
     */
    public AuthClient(String consumerKey, String consumerSecret) {
        this.consumerKey = consumerKey;
        this.consumerSecret = consumerSecret;
    }

    /**
     * Retrieves an access token from Enzona's authentication server.
     *
     * @return The access token used for subsequent API requests.
     * @throws EnzonaException if there is a failure in retrieving the access token.
     */
    public String getAccessToken() throws EnzonaException {
        try {
            httpClient = HttpClients.createDefault();
            HttpPost request = new HttpPost(tokenEndpoint);

            // Encode Consumer Key and Secret
            String encoding = Base64.getEncoder().encodeToString((consumerKey + ":" + consumerSecret).getBytes());

            // Set headers
            request.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoding);
            request.setHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");

            // Set request parameters
            List<NameValuePair> urlParameters = new ArrayList<>();
            urlParameters.add(new BasicNameValuePair("grant_type", "client_credentials"));
            urlParameters.add(new BasicNameValuePair("scope", "enzona_business_payment"));
            request.setEntity(new UrlEncodedFormEntity(urlParameters));

            var response = httpClient.execute(request);
            var responseString = EntityUtils.toString(response.getEntity());

            // Handle response
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new EnzonaException("Failed to retrieve access token: " + responseString);
            }

            var jsonResponse = JsonUtil.fromJson(responseString, TokenResponse.class);
            return jsonResponse.getAccessToken();
        } catch (Exception e) {
            throw new EnzonaException("Exception occurred while fetching access token: " + e.getMessage(), e);
        }
    }

    /**
     * Inner class to handle the JSON response for token request.
     */
    private static class TokenResponse {
        private String access_token;

        public String getAccessToken() {
            return access_token;
        }
    }
}
