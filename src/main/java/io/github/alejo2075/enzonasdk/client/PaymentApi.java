package io.github.alejo2075.enzonasdk.client;

import io.github.alejo2075.enzonasdk.model.request.*;
import io.github.alejo2075.enzonasdk.model.response.*;
import io.github.alejo2075.enzonasdk.model.request.*;
import io.github.alejo2075.enzonasdk.model.response.*;
import io.github.alejo2075.enzonasdk.util.JsonUtil;
import io.github.alejo2075.enzonasdk.exception.EnzonaException;
import io.github.alejo2075.enzonasdk.model.request.*;
import io.github.alejo2075.enzonasdk.model.response.*;
import lombok.Data;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles payment-related operations for the Enzona API.
 * Provides functionality to manage payments, including creating, confirming, and completing transactions.
 *
 * This class interacts with the Enzona API's payment endpoints by sending HTTP requests to manage payment transactions.
 * Each request is authenticated using an access token obtained via {@link AuthClient}.
 *
 * <p>Instances of this class should be created by providing the consumer key and consumer secret which are used to
 * instantiate an {@link AuthClient}. This client is then used to authenticate requests made by this class.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * PaymentApi paymentApi = new PaymentApi("consumerKey", "consumerSecret");
 * ConfirmPaymentRequest confirmRequest = new ConfirmPaymentRequest(...);
 * ConfirmPaymentResponse confirmResponse = paymentApi.confirmPayment("transactionUuid", confirmRequest);
 * </pre>
 */
@Data
public class PaymentApi {

    private final String baseUrl = "https://api.enzona.net/";
    private final AuthClient authClient;
    private CloseableHttpClient httpClient = HttpClients.createDefault();

    /**
     * Constructs a PaymentApi instance using provided consumer key and consumer secret.
     * This constructor initializes an {@link AuthClient} for handling authentication.
     *
     * @param consumerKey The consumer key issued by Enzona for API authentication.
     * @param consumerSecret The consumer secret issued by Enzona for API authentication.
     */
    public PaymentApi(String consumerKey, String consumerSecret) {
        this.authClient = new AuthClient(consumerKey, consumerSecret);
    }

    /**
     * Confirms a payment for a specified transaction using its unique identifier.
     * This method sends a POST request to the payment service to confirm a payment transaction.
     *
     * @param transactionUuid The unique identifier of the transaction to be confirmed.
     * @param request The {@link ConfirmPaymentRequest} object containing the necessary data for payment confirmation.
     * @return A {@link ConfirmPaymentResponse} object containing the status of the confirmed payment.
     * @throws EnzonaException If there is an issue with network communication or processing the request/response.
     */
    public ConfirmPaymentResponse confirmPayment(String transactionUuid, ConfirmPaymentRequest request) throws EnzonaException {
        try {
            String url = baseUrl + "payments/" + transactionUuid + "/confirm";
            HttpPost httpPost = new HttpPost(url);
            configureHttpHeaders(httpPost);
            String json = JsonUtil.toJson(request);
            httpPost.setEntity(new StringEntity(json));
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                return processHttpResponse(response, ConfirmPaymentResponse.class);
            }
        } catch (IOException e) {
            throw new EnzonaException("Exception occurred while confirming payment: " + e.getMessage(), e);
        }
    }

    /**
     * Completes a payment for the given transaction UUID.
     * This method sends a POST request to the payment service to mark a transaction as completed.
     *
     * @param transactionUuid The unique identifier of the transaction to be completed.
     * @return A {@link CompletePaymentResponse} object containing details about the completed payment transaction.
     * @throws EnzonaException If there is a problem with the network communication.
     */
    public CompletePaymentResponse completePayment(String transactionUuid) throws EnzonaException {
        try {
            String url = baseUrl + "payments/" + transactionUuid + "/complete";
            HttpPost httpPost = new HttpPost(url);
            configureHttpHeaders(httpPost);
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                return processHttpResponse(response, CompletePaymentResponse.class);
            }
        } catch (IOException e) {
            throw new EnzonaException("Exception occurred while completing payment: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves the details of a refund for a specific transaction.
     * This method makes a GET request to the payment service, querying for details about a particular refund.
     *
     * @param transactionUuid The unique identifier of the refund transaction to retrieve details for.
     * @return A {@link RefundDetailsResponse} object containing detailed information about the requested refund.
     * @throws EnzonaException If there is an issue with network communication or processing the request/response.
     */
    public RefundDetailsResponse getRefundDetails(String transactionUuid) throws EnzonaException {
        try {
            String url = baseUrl + "payments/refund/" + transactionUuid;
            HttpGet httpGet = new HttpGet(url);
            configureHttpHeaders(httpGet);
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                return processHttpResponse(response, RefundDetailsResponse.class);
            }
        } catch (IOException e) {
            throw new EnzonaException("Exception occurred while retrieving refund details: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves a list of refunds based on provided criteria encapsulated within a request object.
     * This method sends a GET request to the payment service, querying for a list of refunds.
     *
     * @param request The {@link RefundsListRequest} object containing all the necessary data to filter the refunds list.
     * @return A {@link RefundsListResponse} object containing a list of refunds that match the query criteria specified in the request object.
     * @throws EnzonaException If there is an issue with network communication or processing the request/response.
     */
    public RefundsListResponse getRefundsList(RefundsListRequest request) throws EnzonaException {
        try {
            List<String> queryParams = new ArrayList<>();
            if (request.getMerchantUuid() != null) {
                queryParams.add("merchant_uuid=" + URLEncoder.encode(request.getMerchantUuid(), StandardCharsets.UTF_8));
            }
            if (request.getTransactionUuid() != null) {
                queryParams.add("transaction_uuid=" + URLEncoder.encode(request.getTransactionUuid(), StandardCharsets.UTF_8));
            }
            if (request.getCommerceRefundId() != null) {
                queryParams.add("commerce_refund_id=" + URLEncoder.encode(request.getCommerceRefundId(), StandardCharsets.UTF_8));
            }
            String queryStr = String.join("&", queryParams);
            String url = baseUrl + "payments/refunds" + (queryStr.isEmpty() ? "" : "?" + queryStr);
            HttpGet httpGet = new HttpGet(url);
            configureHttpHeaders(httpGet);
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                return processHttpResponse(response, RefundsListResponse.class);
            }
        } catch (IOException e) {
            throw new EnzonaException("Exception occurred while retrieving refunds list: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a new payment with the specified details.
     * This method sends a POST request to the payment service and expects a response containing the status of the payment.
     *
     * @param request The {@link CreatePaymentRequest} object containing all the necessary data to create a new payment.
     * @return A {@link CreatePaymentResponse} object containing the detailed result of the payment creation process.
     * @throws EnzonaException If there is an issue with network communication or processing the request/response.
     */
    public CreatePaymentResponse createPayment(CreatePaymentRequest request) throws EnzonaException {
        try {
            String url = baseUrl + "payments";
            HttpPost httpPost = new HttpPost(url);
            configureHttpHeaders(httpPost);
            String json = JsonUtil.toJson(request);
            httpPost.setEntity(new StringEntity(json));
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                return processHttpResponse(response, CreatePaymentResponse.class);
            }
        } catch (IOException e) {
            throw new EnzonaException("Exception occurred while creating payment: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves a list of payments based on specified criteria.
     * This method sends a GET request to the payment service, querying for a list of payments.
     *
     * @param request The {@link PaymentsListRequest} object containing all the necessary data to filter the payments list.
     * @return A {@link PaymentsListResponse} object containing a list of payments that match the query criteria specified in the request.
     * @throws EnzonaException If there is an issue with network communication or processing the request/response.
     */
    public PaymentsListResponse getPaymentsList(PaymentsListRequest request) throws EnzonaException {
        try {
            List<String> queryParams = new ArrayList<>();
            if (request.getMerchantUuid() != null) {
                queryParams.add("merchant_uuid=" + URLEncoder.encode(request.getMerchantUuid(), StandardCharsets.UTF_8));
            }
            String queryStr = queryParams.isEmpty() ? "" : "?" + String.join("&", queryParams);
            String url = baseUrl + "payments" + queryStr;
            HttpGet httpGet = new HttpGet(url);
            configureHttpHeaders(httpGet);
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                return processHttpResponse(response, PaymentsListResponse.class);
            }
        } catch (IOException e) {
            throw new EnzonaException("Exception occurred while retrieving payments list: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves the details of a specific payment transaction.
     * This method sends a GET request to the payment service, querying for detailed information about a payment.
     *
     * @param transactionUuid The unique identifier of the payment transaction for which details are being requested.
     * @return A {@link PaymentDetailsResponse} object containing detailed information about the specified payment transaction.
     * @throws EnzonaException If there is an issue with network communication or processing the request/response.
     */
    public PaymentDetailsResponse getPaymentDetails(String transactionUuid) throws EnzonaException {
        try {
            String url = baseUrl + "payments/" + transactionUuid;
            HttpGet httpGet = new HttpGet(url);
            configureHttpHeaders(httpGet);
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                return processHttpResponse(response, PaymentDetailsResponse.class);
            }
        } catch (IOException e) {
            throw new EnzonaException("Exception occurred while retrieving payment details: " + e.getMessage(), e);
        }
    }

    /**
     * Initiates the checkout process for a payment identified by the given UUID.
     * This method sends a GET request to the payment service to initiate the checkout process.
     *
     * @param uuid The unique identifier of the checkout process to be initiated.
     * @return A {@link CheckoutResponse} object containing a message about the outcome of the checkout initiation process.
     * @throws EnzonaException If there is an issue with network communication or processing the request/response.
     */
    public CheckoutResponse performCheckout(String uuid) throws EnzonaException {
        try {
            String url = baseUrl + "payments/checkout/" + uuid;
            HttpGet httpGet = new HttpGet(url);
            configureHttpHeaders(httpGet);
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                return processHttpResponse(response, CheckoutResponse.class);
            }
        } catch (IOException e) {
            throw new EnzonaException("Exception occurred while initiating checkout: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a receive code for a vendor payment with specified details.
     * This method sends a POST request to the payment service with details necessary for generating a receive code.
     *
     * @param request The {@link CreateReceiveCodeRequest} object containing all the necessary data to create a receive code.
     * @return A {@link CreateReceiveCodeResponse} object containing the response from the payment service.
     * @throws EnzonaException If there is an issue with network communication or processing the request/response.
     */
    public CreateReceiveCodeResponse createReceiveCode(CreateReceiveCodeRequest request) throws EnzonaException {
        try {
            String url = baseUrl + "payments/vendor/code";
            HttpPost httpPost = new HttpPost(url);
            configureHttpHeaders(httpPost);
            String json = JsonUtil.toJson(request);
            httpPost.setEntity(new StringEntity(json));
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                return processHttpResponse(response, CreateReceiveCodeResponse.class);
            }
        } catch (IOException e) {
            throw new EnzonaException("Exception occurred while creating receive code: " + e.getMessage(), e);
        }
    }

    /**
     * Fetches a list of refunds associated with a specific payment transaction.
     * This method queries the payment service for refunds related to a given transaction.
     *
     * @param request The {@link ListRefundsRequest} object containing the transaction UUID and optional filtering criteria.
     * @return A {@link ListRefundsResponse} containing a list of refunds matching the criteria specified in the request.
     * @throws EnzonaException If there is an issue with network communication or processing the request/response.
     */
    public ListRefundsResponse listRefunds(ListRefundsRequest request) throws EnzonaException {
        try {
            List<String> queryParams = new ArrayList<>();
            queryParams.add("transaction_uuid=" + URLEncoder.encode(request.getTransactionUuid(), StandardCharsets.UTF_8));
            if (request.getStatusFilter() != null) {
                queryParams.add("status_filter=" + URLEncoder.encode(request.getStatusFilter(), StandardCharsets.UTF_8));
            }
            String queryString = String.join("&", queryParams);
            String url = baseUrl + "payments/" + request.getTransactionUuid() + "/refunds" + "?" + queryString;
            HttpGet httpGet = new HttpGet(url);
            configureHttpHeaders(httpGet);
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                return processHttpResponse(response, ListRefundsResponse.class);
            }
        } catch (IOException e) {
            throw new EnzonaException("Exception occurred while retrieving refunds list: " + e.getMessage(), e);
        }
    }

    /**
     * Submits a payment for products added to a shopping cart.
     * This method sends a POST request to the shop endpoint with detailed information required for making a payment.
     *
     * @param request The {@link PayProductRequest} object containing all the necessary data for the payment transaction.
     * @return A {@link PayProductResponse} object containing the outcome of the payment operation, including a status message.
     * @throws EnzonaException If there is an issue with network communication or processing the request/response.
     */
    public PayProductResponse payProduct(PayProductRequest request) throws EnzonaException {
        try {
            String url = baseUrl + "shop";
            HttpPost httpPost = new HttpPost(url);
            configureHttpHeaders(httpPost);
            String json = JsonUtil.toJson(request);
            httpPost.setEntity(new StringEntity(json));
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                return processHttpResponse(response, PayProductResponse.class);
            }
        } catch (IOException e) {
            throw new EnzonaException("Exception occurred while paying for product: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a payment order with the specified details.
     * This method sends a POST request to the payment orders endpoint with the necessary information for creating a new payment order.
     *
     * @param request The {@link CreatePaymentOrderRequest} object containing all the necessary data for the payment order creation.
     * @return A {@link CreatePaymentOrderResponse} object containing the detailed information of the created payment order, including amount details,
     * status code, creation date, transaction UUID, and more.
     * @throws EnzonaException If there is an issue with network communication or processing the request/response.
     */
    public CreatePaymentOrderResponse createPaymentOrder(CreatePaymentOrderRequest request) throws EnzonaException {
        try {
            String url = baseUrl + "payment-orders";
            HttpPost httpPost = new HttpPost(url);
            configureHttpHeaders(httpPost);
            String json = JsonUtil.toJson(request);
            httpPost.setEntity(new StringEntity(json));
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                return processHttpResponse(response, CreatePaymentOrderResponse.class);
            }
        } catch (IOException e) {
            throw new EnzonaException("Exception occurred while creating payment order: " + e.getMessage(), e);
        }
    }

    /**
     * Cancels a payment transaction identified by the given UUID.
     * This method sends a POST request to the payment service to cancel a payment transaction.
     *
     * @param transactionUuid The unique identifier of the payment transaction to be canceled.
     * @return A {@link CancelPaymentResponse} object containing the outcome of the cancellation operation, including status codes and descriptions.
     * @throws EnzonaException If there is an issue with network communication or processing the request/response.
     */
    public CancelPaymentResponse cancelPayment(String transactionUuid) throws EnzonaException {
        try {
            String url = baseUrl + "payments/" + transactionUuid + "/cancel";
            HttpPost httpPost = new HttpPost(url);
            configureHttpHeaders(httpPost);
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                return processHttpResponse(response, CancelPaymentResponse.class);
            }
        } catch (IOException e) {
            throw new EnzonaException("Exception occurred while canceling payment: " + e.getMessage(), e);
        }
    }

    /**
     * Processes a refund for a specified payment transaction.
     * This method sends a POST request to the payment service to process a refund for a payment.
     *
     * @param transactionUuid The unique identifier of the payment transaction for which the refund is being processed.
     * @param request The {@link RefundPaymentRequest} object containing all the necessary data for processing the refund.
     * @return A {@link RefundPaymentResponse} object containing details about the processed refund, including the status code, parent payment UUID, and other information.
     * @throws EnzonaException If there is an issue with network communication or processing the request/response.
     */
    public RefundPaymentResponse refundPayment(String transactionUuid, RefundPaymentRequest request) throws EnzonaException {
        try {
            String url = baseUrl + "payments/" + transactionUuid + "/refund";
            HttpPost httpPost = new HttpPost(url);
            configureHttpHeaders(httpPost);
            String json = JsonUtil.toJson(request);
            httpPost.setEntity(new StringEntity(json));
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                return processHttpResponse(response, RefundPaymentResponse.class);
            }
        } catch (IOException e) {
            throw new EnzonaException("Exception occurred while processing refund: " + e.getMessage(), e);
        }
    }

    /**
     * Configures the common HTTP headers for every request sent to the Enzona API.
     * This method sets the authorization token and content type headers.
     *
     * @param request The HTTP request to which headers need to be added.
     * @throws EnzonaException If there is an issue obtaining the access token.
     */
    private void configureHttpHeaders(HttpRequest request) throws EnzonaException {
        request.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + authClient.getAccessToken());
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
    }

    /**
     * Processes the HTTP response from an API request.
     * This method checks the response status code and parses the JSON response body into a specified Java type.
     *
     * @param response The HTTP response to process.
     * @param responseType The class of the type into which the JSON response should be converted.
     * @return An instance of {@code T}, which contains the data converted from the JSON response.
     * @throws IOException If there is an issue with reading the response.
     * @throws EnzonaException If the response status is not 200 OK, indicating an error with the request.
     */
    private <T> T processHttpResponse(HttpResponse response, Class<T> responseType) throws IOException, EnzonaException {
        int statusCode = response.getStatusLine().getStatusCode();
        String jsonResponse = EntityUtils.toString(response.getEntity());

        if (statusCode != 200) {
            throw new EnzonaException("Request failed with HTTP Status " + statusCode + ": " + jsonResponse);
        }

        return JsonUtil.fromJson(jsonResponse, responseType);
    }

}
