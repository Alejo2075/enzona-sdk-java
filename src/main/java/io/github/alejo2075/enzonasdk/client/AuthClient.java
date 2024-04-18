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
import java.util.Objects;

/**
 * This class provides functionality to authenticate with the Enzona API and retrieve an access token.
 * It encapsulates all necessary details like preparing HTTP requests, handling responses and managing errors.
 *
 * <p> Usage of this class involves providing consumer key and secret issued by Enzona and then calling
 * {@link #getAccessToken()} method to perform the authentication and receive an access token that can be used
 * for subsequent API requests. </p>
 *
 * <p> This class employs an {@link HttpClient} to send HTTP requests. It is initialized at the creation of the instance
 * and reused for all requests to improve performance and resource management. </p>
 */
@Data
public class AuthClient {

    private HttpClient httpClient;
    private final String consumerKey;
    private final String consumerSecret;
    private final String tokenEndpoint = "https://api.enzona.net/token";

    /**
     * Constructs an {@link AuthClient} instance with the specified consumer key and secret.
     *
     * @param consumerKey    The consumer key issued by Enzona for API authentication. Must not be null.
     * @param consumerSecret The consumer secret issued by Enzona for API authentication. Must not be null.
     * @throws NullPointerException if either consumerKey or consumerSecret is null.
     */
    public AuthClient(String consumerKey, String consumerSecret) {
        Objects.requireNonNull(consumerKey, "Consumer Key must not be null");
        Objects.requireNonNull(consumerSecret, "Consumer Secret must not be null");

        this.consumerKey = consumerKey;
        this.consumerSecret = consumerSecret;
        this.httpClient = HttpClients.createDefault();
    }

    /**
     * Retrieves an access token from Enzona's authentication server by sending a HTTP POST request.
     * This token is necessary for making authorized API calls to Enzona.
     *
     * @return A {@link String} representing the access token.
     * @throws EnzonaException if there is a failure in retrieving the access token, which may include
     * HTTP errors, network problems, or issues with JSON parsing.
     */
    public String getAccessToken() throws EnzonaException {
        try {
            HttpPost request = new HttpPost(tokenEndpoint);

            String encoding = Base64.getEncoder().encodeToString((consumerKey + ":" + consumerSecret).getBytes());
            request.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoding);
            request.setHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");

            List<NameValuePair> urlParameters = new ArrayList<>();
            urlParameters.add(new BasicNameValuePair("grant_type", "client_credentials"));
            urlParameters.add(new BasicNameValuePair("scope", "enzona_business_payment"));
            request.setEntity(new UrlEncodedFormEntity(urlParameters));

            var response = httpClient.execute(request);
            var responseString = EntityUtils.toString(response.getEntity());

            if (response.getStatusLine().getStatusCode() != 200) {
                throw new EnzonaException("Failed to retrieve access token: HTTP Status " + response.getStatusLine().getStatusCode() + " - " + responseString);
            }

            var jsonResponse = JsonUtil.fromJson(responseString, TokenResponse.class);
            return jsonResponse.getAccessToken();
        } catch (Exception e) {
            throw new EnzonaException("Exception occurred while fetching access token: " + e.getMessage(), e);
        }
    }

    /**
     * A private inner class to facilitate the parsing of JSON response containing the access token.
     */
    private static class TokenResponse {
        private String access_token;

        /**
         * Returns the access token from the JSON response.
         *
         * @return A {@link String} that is the access token used for authentication.
         */
        public String getAccessToken() {
            return access_token;
        }
    }
}
