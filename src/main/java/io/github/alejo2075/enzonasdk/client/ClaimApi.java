package io.github.alejo2075.enzonasdk.client;

import io.github.alejo2075.enzonasdk.exception.EnzonaException;
import io.github.alejo2075.enzonasdk.model.request.CreateClaimsRequest;
import io.github.alejo2075.enzonasdk.model.response.CreateClaimsResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

/**
 * Handles claim-related operations for the Enzona API.
 * Provides functionality to create new claims using the details provided in a {@link CreateClaimsRequest}.
 *
 * This class interacts with the Enzona API's claims endpoint by sending HTTP requests to manage claims.
 * Each request is authenticated using an access token obtained via {@link AuthClient}.
 *
 * <p>Instances of this class should be created by providing the consumer key and consumer secret which are used to
 * instantiate an {@link AuthClient}. This client is then used to authenticate requests made by this class.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * ClaimApi claimApi = new ClaimApi("consumerKey", "consumerSecret");
 * CreateClaimsRequest request = new CreateClaimsRequest(...);
 * CreateClaimsResponse response = claimApi.createClaims(request);
 * </pre>
 */
@Data
public class ClaimApi {

    private final String baseUrl = "https://api.enzona.net/payment/";
    private final AuthClient authClient;
    private CloseableHttpClient httpClient = HttpClients.createDefault();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Constructs a ClaimApi instance using provided consumer key and consumer secret.
     * This constructor initializes an {@link AuthClient} for handling authentication.
     *
     * @param consumerKey The consumer key issued by Enzona for API authentication.
     * @param consumerSecret The consumer secret issued by Enzona for API authentication.
     */
    public ClaimApi(String consumerKey, String consumerSecret) {
        this.authClient = new AuthClient(consumerKey, consumerSecret);
    }

    /**
     * Creates a new claim based on the provided {@link CreateClaimsRequest}.
     * This method sends a POST request to the claims endpoint and returns the results wrapped in a {@link CreateClaimsResponse}.
     *
     * @param request The request containing all necessary information for creating the claim, must not be null.
     * @return A {@link CreateClaimsResponse} object containing the response from the API.
     * @throws EnzonaException if there is an issue with network communication, or if the API response indicates an error.
     */
    public CreateClaimsResponse createClaims(CreateClaimsRequest request) throws EnzonaException {
        try (CloseableHttpResponse response = executeClaimRequest(request)) {
            String jsonResponse = EntityUtils.toString(response.getEntity());
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode != 200) {
                throw new EnzonaException("Failed to create claim: HTTP Status " + statusCode + " - " + jsonResponse);
            }

            return objectMapper.readValue(jsonResponse, CreateClaimsResponse.class);
        } catch (Exception e) {
            throw new EnzonaException("Exception occurred while creating claim: " + e.getMessage(), e);
        }
    }

    /**
     * Executes a POST request to the claims endpoint.
     *
     * @param request The {@link CreateClaimsRequest} to be serialized and sent as part of the POST request.
     * @return {@link CloseableHttpResponse} representing the response from the server.
     * @throws Exception if an error occurs during the HTTP request execution.
     */
    private CloseableHttpResponse executeClaimRequest(CreateClaimsRequest request) throws Exception {
        String url = baseUrl + "createClaims";
        HttpPost httpPost = new HttpPost(url);
        configureRequestHeaders(httpPost);
        String json = objectMapper.writeValueAsString(request);
        httpPost.setEntity(new StringEntity(json));
        return httpClient.execute(httpPost);
    }

    /**
     * Configures headers required for the HTTP POST request.
     *
     * @param httpPost The {@link HttpPost} object whose headers need to be set.
     */
    private void configureRequestHeaders(HttpPost httpPost) throws EnzonaException {
        httpPost.setHeader("Authorization", "Bearer " + authClient.getAccessToken());
        httpPost.setHeader("Content-Type", "application/json");
    }
}
