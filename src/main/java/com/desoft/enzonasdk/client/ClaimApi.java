package com.desoft.enzonasdk.client;

import com.desoft.enzonasdk.model.request.CreateClaimsRequest;
import com.desoft.enzonasdk.model.response.CreateClaimsResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class ClaimApi {

    private final String baseUrl = "https://api.enzona.net/payment/";
    private final AuthClient authClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ClaimApi(String consumerKey, String consumerSecret) {
        this.authClient = new AuthClient(consumerKey, consumerSecret);
    }

    /**
     * Creates a new claim based on the provided details.
     *
     * This method sends a POST request to the claims endpoint, submitting a variety of details pertinent to the claim.
     * These details encompass the type of claim, reasons for the claim, and specific identifiers related to the customer
     * and the transaction in question, among others.
     *
     * @param request The {@link CreateClaimsRequest} object containing all necessary information for creating the claim.
     * @return A {@link CreateClaimsResponse} object, potentially containing a message or other details indicating the outcome of the claim creation process.
     * @throws IOException If there is an issue with network communication or processing the request/response.
     */
    public CreateClaimsResponse createClaims(CreateClaimsRequest request) throws IOException {
        // Implementation goes here.
    }

}
