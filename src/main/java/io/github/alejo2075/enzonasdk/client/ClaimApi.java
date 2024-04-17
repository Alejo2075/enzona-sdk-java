package io.github.alejo2075.enzonasdk.client;

import io.github.alejo2075.enzonasdk.exception.EnzonaException;
import io.github.alejo2075.enzonasdk.model.request.CreateClaimsRequest;
import io.github.alejo2075.enzonasdk.model.response.CreateClaimsResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

@Data
public class ClaimApi {

    private final String baseUrl = "https://api.enzona.net/payment/";
    private final AuthClient authClient;

    private CloseableHttpClient httpClient = HttpClients.createDefault();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ClaimApi(String consumerKey, String consumerSecret) {
        this.authClient = new AuthClient(consumerKey, consumerSecret);
    }

    /**
     * Creates a new claim based on the provided details.
     * <p>
     * This method sends a POST request to the claims endpoint, submitting a variety of details pertinent to the claim.
     * These details encompass the type of claim, reasons for the claim, and specific identifiers related to the customer
     * and the transaction in question, among others.
     *
     * @param request The {@link CreateClaimsRequest} object containing all necessary information for creating the claim.
     * @return A {@link CreateClaimsResponse} object, potentially containing a message or other details indicating the outcome of the claim creation process.
     * @throws EnzonaException If there is an issue with network communication or processing the request/response.
     */
    public CreateClaimsResponse createClaims(CreateClaimsRequest request) throws EnzonaException {
        try  {
            String url = baseUrl + "createClaims";
            HttpPost httpPost = new HttpPost(url);

            httpPost.setHeader("Authorization", "Bearer " + authClient.getAccessToken());
            httpPost.setHeader("Content-Type", "application/json");

            // Serialize request to JSON using ObjectMapper instead of JsonUtil for consistency with your environment setup
            String json = objectMapper.writeValueAsString(request);
            StringEntity entity = new StringEntity(json);
            httpPost.setEntity(entity);

            // Execute HTTP request
            var response = httpClient.execute(httpPost);
            String jsonResponse = EntityUtils.toString(response.getEntity());

            if (response.getStatusLine().getStatusCode() != 200) {
                throw new EnzonaException("Failed to create claim: " + response.getStatusLine().getStatusCode() + " " + jsonResponse);
            }

            // Deserialize response from JSON
            return objectMapper.readValue(jsonResponse, CreateClaimsResponse.class);
        } catch (Exception e) {
            throw new EnzonaException("Exception occurred while creating claim: " + e.getMessage(), e);
        }
    }

}
