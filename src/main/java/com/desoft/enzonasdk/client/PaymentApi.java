package com.desoft.enzonasdk.client;

import com.desoft.enzonasdk.exception.EnzonaException;
import com.desoft.enzonasdk.model.request.*;
import com.desoft.enzonasdk.model.response.*;
import com.desoft.enzonasdk.util.JsonUtil;
import lombok.Data;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Data
public class PaymentApi {

    private final String baseUrl = "https://api.enzona.net/";
    private final AuthClient authClient;

    public PaymentApi(String consumerKey, String consumerSecret) {
        this.authClient = new AuthClient(consumerKey, consumerSecret);
    }

    /**
     * Confirms a payment for a specified transaction.
     * <p>
     * This method sends a POST request to the payment service to confirm a payment transaction using its UUID. The request includes parameters such as
     * the UUID of the funding source, the payment password, and a fingerprint for authentication and authorization purposes.
     * <p>
     * Upon successful confirmation, the service responds with the status of the payment.
     *
     * @param transactionUuid The unique identifier of the transaction to be confirmed.
     * @param request The {@link ConfirmPaymentRequest} object containing the necessary data for payment confirmation.
     * @return A {@link ConfirmPaymentResponse} object containing the status of the confirmed payment.
     * @throws EnzonaException If there is an issue with network communication or processing the request/response.
     */
    public ConfirmPaymentResponse confirmPayment(String transactionUuid, ConfirmPaymentRequest request) throws EnzonaException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String url = baseUrl + "payments/" + transactionUuid + "/confirm";
            HttpPost httpPost = new HttpPost(url);

            // Set headers
            httpPost.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + authClient.getAccessToken());
            httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");

            // Serialize request to JSON
            String json = JsonUtil.toJson(request);
            StringEntity entity = new StringEntity(json);
            httpPost.setEntity(entity);

            // Execute HTTP request
            var response = httpClient.execute(httpPost);
            String jsonResponse = EntityUtils.toString(response.getEntity());

            // Check response status code
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new EnzonaException("Failed to confirm payment: " + response.getStatusLine().getStatusCode() + " " + jsonResponse);
            }

            // Deserialize response from JSON
            return JsonUtil.fromJson(jsonResponse, ConfirmPaymentResponse.class);
        } catch (Exception e) {
            throw new EnzonaException("Exception occurred while fetching access token: " + e.getMessage(), e);
        }
    }


    /**
     * Completes a payment for the given transaction UUID.
     * <p>
     * This method sends a POST request to the payment service to mark a transaction as completed. It requires the transaction UUID as a path parameter.
     * The request does not need a payload but expects a successful response to contain various details about the completed payment, such as the amount details,
     * status code, transaction information, and more.
     *
     * @param transactionUuid The unique identifier of the transaction to be completed.
     * @return A {@link CompletePaymentResponse} object containing details about the completed payment transaction.
     * @throws EnzonaException If there is a problem with the network communication.
     */
    public CompletePaymentResponse completePayment(String transactionUuid) throws EnzonaException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String url = baseUrl + "payments/" + transactionUuid + "/complete";
            HttpPost httpPost = new HttpPost(url);

            // Set headers
            httpPost.setHeader("Authorization", "Bearer " + authClient.getAccessToken());
            httpPost.setHeader("Content-Type", "application/json");

            // Execute HTTP request
            var response = httpClient.execute(httpPost);
            String jsonResponse = EntityUtils.toString(response.getEntity());

            // Check response status code
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new EnzonaException("Failed to complete payment: " + response.getStatusLine().getStatusCode() + " " + jsonResponse);
            }

            // Deserialize response from JSON
            return JsonUtil.fromJson(jsonResponse, CompletePaymentResponse.class);
        } catch (Exception e) {
            throw new EnzonaException("Exception occurred while completing payment: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves the details of a refund for a specific transaction.
     * <p>
     * This method makes a GET request to the payment service, querying for details about a particular refund using its transaction UUID.
     * The response includes various pieces of information, such as the transaction status code, the UUID of the parent payment,
     * timestamps for when the transaction was created and last updated, a description, and more.
     *
     * @param transactionUuid The unique identifier of the refund transaction to retrieve details for.
     * @return A {@link RefundDetailsResponse} object containing detailed information about the requested refund.
     * @throws EnzonaException If there is an issue with network communication or processing the request/response.
     */
    public RefundDetailsResponse getRefundDetails(String transactionUuid) throws EnzonaException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String url = baseUrl + "payments/refund/" + transactionUuid;
            HttpGet httpGet = new HttpGet(url);

            // Set headers
            httpGet.setHeader("Authorization", "Bearer " + authClient.getAccessToken());
            httpGet.setHeader("Content-Type", "application/json");

            // Execute HTTP request
            var response = httpClient.execute(httpGet);
            String jsonResponse = EntityUtils.toString(response.getEntity());

            // Check response status code
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new EnzonaException("Failed to retrieve refund details: " + response.getStatusLine().getStatusCode() + " " + jsonResponse);
            }

            // Deserialize response from JSON
            return JsonUtil.fromJson(jsonResponse, RefundDetailsResponse.class);
        } catch (Exception e) {
            throw new EnzonaException("Exception occurred while retrieving refund details: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves a list of refunds based on provided criteria encapsulated within a request object.
     * <p>
     * This method sends a GET request to the payment service, querying for a list of refunds. The query can be filtered by multiple criteria,
     * such as merchant UUID, transaction UUID, commerce refund ID, pagination settings (limit and offset), status filter, date range for the
     * refunds, and the order in which the results are returned. All these parameters are encapsulated in the {@link RefundsListRequest} object,
     * making the method call clean and the request object easy to manage and extend if needed.
     *
     * @param request The {@link RefundsListRequest} object containing all the necessary data to filter the refunds list.
     * @return A {@link RefundsListResponse} object containing a list of refunds that match the query criteria specified in the request object.
     * @throws EnzonaException If there is an issue with network communication or processing the request/response.
     */
    public RefundsListResponse getRefundsList(RefundsListRequest request) throws  EnzonaException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
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
            // Add other parameters as needed

            String queryStr = String.join("&", queryParams);
            String url = baseUrl + "payments/refunds" + (queryStr.isEmpty() ? "" : "?" + queryStr);

            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("Authorization", "Bearer " + authClient.getAccessToken());
            httpGet.setHeader("Content-Type", "application/json");

            // Execute HTTP request
            var response = httpClient.execute(httpGet);
            String jsonResponse = EntityUtils.toString(response.getEntity());

            // Check response status code
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new EnzonaException("Failed to retrieve refunds list: " + response.getStatusLine().getStatusCode() + " " + jsonResponse);
            }

            // Deserialize response from JSON
            return JsonUtil.fromJson(jsonResponse, RefundsListResponse.class);
        } catch (Exception e) {
            throw new EnzonaException("Exception occurred while retrieving refunds list: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a new payment with the specified details.
     * <p>
     * This method constructs a payment creation request with detailed information about the payment, including
     * the merchant UUID, operation ID, amount details, currency, and items involved in the transaction.
     * It sends this data as a POST request to the payment service and expects a response containing
     * the status of the payment, transaction UUID, and other relevant information.
     *
     * @param request The {@link CreatePaymentRequest} object containing all the necessary data to create a new payment.
     * @return A {@link CreatePaymentResponse} object containing the detailed result of the payment creation process.
     * @throws EnzonaException If there is an issue with network communication or processing the request/response.
     */
    public CreatePaymentResponse createPayment(CreatePaymentRequest request) throws EnzonaException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String url = baseUrl + "payments";
            HttpPost httpPost = new HttpPost(url);

            // Set headers
            httpPost.setHeader("Authorization", "Bearer " + authClient.getAccessToken());
            httpPost.setHeader("Content-Type", "application/json");

            // Serialize request to JSON
            String json = JsonUtil.toJson(request);
            StringEntity entity = new StringEntity(json);
            httpPost.setEntity(entity);

            // Execute HTTP request
            var response = httpClient.execute(httpPost);
            String jsonResponse = EntityUtils.toString(response.getEntity());

            // Check response status code
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new EnzonaException("Failed to create payment: " + response.getStatusLine().getStatusCode() + " " + jsonResponse);
            }

            // Deserialize response from JSON
            return JsonUtil.fromJson(jsonResponse, CreatePaymentResponse.class);
        } catch (Exception e) {
            throw new EnzonaException("Exception occurred while creating payment: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves a list of payments based on the specified criteria.
     * <p>
     * This method sends a GET request to the payment service, querying for a list of payments. The query supports
     * multiple filters including merchant UUID, pagination controls (limit and offset), operational filters,
     * status filters, date range filters, and the order in which results are returned. These parameters are
     * encapsulated within the {@link PaymentsListRequest} object to facilitate easy adjustments and additions.
     *
     * @param request The {@link PaymentsListRequest} object containing all the necessary data to filter the payments list.
     * @return A {@link PaymentsListResponse} object containing a list of payments that match the query criteria specified in the request.
     * @throws EnzonaException If there is an issue with network communication or processing the request/response.
     */
    public PaymentsListResponse getPaymentsList(PaymentsListRequest request) throws EnzonaException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            List<String> queryParams = new ArrayList<>();
            if (request.getMerchantUuid() != null) {
                queryParams.add("merchant_uuid=" + URLEncoder.encode(request.getMerchantUuid(), StandardCharsets.UTF_8));
            }
            // Add other parameters similarly
            String queryStr = queryParams.isEmpty() ? "" : "?" + String.join("&", queryParams);

            String url = baseUrl + "payments" + queryStr;
            HttpGet httpGet = new HttpGet(url);

            httpGet.setHeader("Authorization", "Bearer " + authClient.getAccessToken());
            httpGet.setHeader("Content-Type", "application/json");

            var response = httpClient.execute(httpGet);
            String jsonResponse = EntityUtils.toString(response.getEntity());

            if (response.getStatusLine().getStatusCode() != 200) {
                throw new EnzonaException("Failed to retrieve payments list: " + response.getStatusLine().getStatusCode() + " " + jsonResponse);
            }

            return JsonUtil.fromJson(jsonResponse, PaymentsListResponse.class);
        } catch (Exception e) {
            throw new EnzonaException("Exception occurred while retrieving payments list: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves the details of a specific payment made, identified by the transaction UUID.
     * <p>
     * This method sends a GET request to the payment service, querying for detailed information about a payment identified
     * by its transaction UUID. The information returned includes details on the amount, status, timestamps for creation and
     * update, currency, merchant information, items involved in the payment, and more.
     *
     * @param transactionUuid The unique identifier of the payment transaction for which details are being requested.
     * @return A {@link PaymentDetailsResponse} object containing detailed information about the specified payment transaction.
     * @throws EnzonaException If there is an issue with network communication or processing the request/response.
     */
    public PaymentDetailsResponse getPaymentDetails(String transactionUuid) throws EnzonaException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String url = baseUrl + "payments/" + transactionUuid;
            HttpGet httpGet = new HttpGet(url);

            httpGet.setHeader("Authorization", "Bearer " + authClient.getAccessToken());
            httpGet.setHeader("Content-Type", "application/json");

            var response = httpClient.execute(httpGet);
            String jsonResponse = EntityUtils.toString(response.getEntity());

            if (response.getStatusLine().getStatusCode() != 200) {
                throw new EnzonaException("Failed to retrieve payment details: " + response.getStatusLine().getStatusCode() + " " + jsonResponse);
            }

            return JsonUtil.fromJson(jsonResponse, PaymentDetailsResponse.class);
        } catch (Exception e) {
            throw new EnzonaException("Exception occurred while retrieving payment details: " + e.getMessage(), e);
        }
    }

    /**
     * Initiates the checkout process for a payment identified by the given UUID.
     * <p>
     * This method sends a GET request to the payment service to initiate the checkout process for the payment associated with
     * the provided UUID. The process might involve redirecting the user to a payment gateway or simply preparing the payment
     * for final processing.
     *
     * @param uuid The unique identifier of the checkout process to be initiated.
     * @return A {@link CheckoutResponse} object containing a message about the outcome of the checkout initiation process.
     * @throws EnzonaException If there is an issue with network communication or processing the request/response.
     */
    public CheckoutResponse performCheckout(String uuid) throws EnzonaException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String url = baseUrl + "payments/checkout/" + uuid;
            HttpGet httpGet = new HttpGet(url);

            httpGet.setHeader("Authorization", "Bearer " + authClient.getAccessToken());
            httpGet.setHeader("Content-Type", "application/json");

            var response = httpClient.execute(httpGet);
            String jsonResponse = EntityUtils.toString(response.getEntity());

            if (response.getStatusLine().getStatusCode() != 200) {
                throw new EnzonaException("Failed to initiate checkout: " + response.getStatusLine().getStatusCode() + " " + jsonResponse);
            }

            return JsonUtil.fromJson(jsonResponse, CheckoutResponse.class);
        } catch (Exception e) {
            throw new EnzonaException("Exception occurred while initiating checkout: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a receive code for a vendor payment with the specified details.
     * <p>
     * This method sends a POST request to the payment service with details necessary for generating a receive code.
     * The details include the amount, funding source, payment password, and other relevant information.
     * On success, the service returns a message and status indicating the outcome of the operation.
     *
     * @param request The {@link CreateReceiveCodeRequest} object containing all the necessary data to create a receive code.
     * @return A {@link CreateReceiveCodeResponse} object containing the response from the payment service, including a message and status of the operation.
     * @throws EnzonaException If there is an issue with network communication or processing the request/response.
     */
    public CreateReceiveCodeResponse createReceiveCode(CreateReceiveCodeRequest request) throws EnzonaException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String url = baseUrl + "payments/vendor/code";
            HttpPost httpPost = new HttpPost(url);

            httpPost.setHeader("Authorization", "Bearer " + authClient.getAccessToken());
            httpPost.setHeader("Content-Type", "application/json");

            // Serialize request to JSON
            String json = JsonUtil.toJson(request);
            StringEntity entity = new StringEntity(json);
            httpPost.setEntity(entity);

            // Execute HTTP request
            var response = httpClient.execute(httpPost);
            String jsonResponse = EntityUtils.toString(response.getEntity());

            if (response.getStatusLine().getStatusCode() != 200) {
                throw new EnzonaException("Failed to create receive code: " + response.getStatusLine().getStatusCode() + " " + jsonResponse);
            }

            // Deserialize response from JSON
            return JsonUtil.fromJson(jsonResponse, CreateReceiveCodeResponse.class);
        } catch (Exception e) {
            throw new EnzonaException("Exception occurred while creating receive code: " + e.getMessage(), e);
        }
    }

    /**
     * Fetches a list of refunds associated with a specific payment transaction.
     * <p>
     * This method queries the payment service for refunds related to a given transaction, identified by its UUID.
     * The list can be filtered and paginated using various criteria such as status, date range, and order. These
     * parameters are passed as part of a request object, which simplifies handling and allows for future extension.
     *
     * @param request The {@link ListRefundsRequest} object containing the transaction UUID and optional filtering criteria.
     * @return A {@link ListRefundsResponse} containing a list of refunds matching the criteria specified in the request.
     * @throws EnzonaException If there is an issue with network communication or processing the request/response.
     */
    public ListRefundsResponse listRefunds(ListRefundsRequest request) throws EnzonaException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            List<String> queryParams = new ArrayList<>();
            queryParams.add("transaction_uuid=" + URLEncoder.encode(request.getTransactionUuid(), StandardCharsets.UTF_8));

            if (request.getStatusFilter() != null) {
                queryParams.add("status_filter=" + URLEncoder.encode(request.getStatusFilter(), StandardCharsets.UTF_8));
            }
            if (request.getStartDateFilter() != null) {
                queryParams.add("start_date_filter=" + URLEncoder.encode(request.getStartDateFilter(), StandardCharsets.UTF_8));
            }
            if (request.getEndDateFilter() != null) {
                queryParams.add("end_date_filter=" + URLEncoder.encode(request.getEndDateFilter(), StandardCharsets.UTF_8));
            }
            if (request.getOrderFilter() != null) {
                queryParams.add("order_filter=" + URLEncoder.encode(request.getOrderFilter(), StandardCharsets.UTF_8));
            }
            if (request.getLimit() != null) {
                queryParams.add("limit=" + request.getLimit());
            }
            if (request.getOffset() != null) {
                queryParams.add("offset=" + request.getOffset());
            }

            String queryString = String.join("&", queryParams);
            String url = baseUrl + "payments/" + request.getTransactionUuid() + "/refunds" + "?" + queryString;
            HttpGet httpGet = new HttpGet(url);

            httpGet.setHeader("Authorization", "Bearer " + authClient.getAccessToken());
            httpGet.setHeader("Content-Type", "application/json");

            var response = httpClient.execute(httpGet);
            String jsonResponse = EntityUtils.toString(response.getEntity());

            if (response.getStatusLine().getStatusCode() != 200) {
                throw new EnzonaException("Failed to retrieve refunds list: " + response.getStatusLine().getStatusCode() + " " + jsonResponse);
            }

            return JsonUtil.fromJson(jsonResponse, ListRefundsResponse.class);
        } catch (Exception e) {
            throw new EnzonaException("Exception occurred while retrieving refunds list: " + e.getMessage(), e);
        }
    }

    /**
     * Submits a payment for products added to a shopping cart.
     * <p>
     * This method sends a POST request to the shop endpoint with detailed information required for making a payment.
     * The details include cart ID, total amount including shipping, source of funding, payment credentials,
     * description of the purchase, currency, merchant details, shop identifier, and the items being purchased.
     *
     * @param request The {@link PayProductRequest} object containing all the necessary data for the payment transaction.
     * @return A {@link PayProductResponse} object containing the outcome of the payment operation, including a status message.
     * @throws EnzonaException If there is an issue with network communication or processing the request/response.
     */
    public PayProductResponse payProduct(PayProductRequest request) throws EnzonaException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String url = baseUrl + "shop";
            HttpPost httpPost = new HttpPost(url);

            httpPost.setHeader("Authorization", "Bearer " + authClient.getAccessToken());
            httpPost.setHeader("Content-Type", "application/json");

            // Serialize request to JSON
            String json = JsonUtil.toJson(request);
            StringEntity entity = new StringEntity(json);
            httpPost.setEntity(entity);

            // Execute HTTP request
            var response = httpClient.execute(httpPost);
            String jsonResponse = EntityUtils.toString(response.getEntity());

            if (response.getStatusLine().getStatusCode() != 200) {
                throw new EnzonaException("Failed to pay product: " + response.getStatusLine().getStatusCode() + " " + jsonResponse);
            }

            // Deserialize response from JSON
            return JsonUtil.fromJson(jsonResponse, PayProductResponse.class);
        } catch (Exception e) {
            throw new EnzonaException("Exception occurred while paying for product: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a payment order with the specified details.
     * <p>
     * This method sends a POST request to the payment orders endpoint with the necessary information for creating a new payment order.
     * The information includes the merchant operation ID, total amount, description of the payment, and the currency.
     *
     * @param request The {@link CreatePaymentOrderRequest} object containing all the necessary data for the payment order creation.
     * @return A {@link CreatePaymentOrderResponse} object containing the detailed information of the created payment order, including amount details,
     * status code, creation date, transaction UUID, and more.
     * @throws EnzonaException If there is an issue with network communication or processing the request/response.
     */
    public CreatePaymentOrderResponse createPaymentOrder(CreatePaymentOrderRequest request) throws EnzonaException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String url = baseUrl + "payment-orders";
            HttpPost httpPost = new HttpPost(url);

            httpPost.setHeader("Authorization", "Bearer " + authClient.getAccessToken());
            httpPost.setHeader("Content-Type", "application/json");

            // Serialize request to JSON
            String json = JsonUtil.toJson(request);
            StringEntity entity = new StringEntity(json);
            httpPost.setEntity(entity);

            // Execute HTTP request
            var response = httpClient.execute(httpPost);
            String jsonResponse = EntityUtils.toString(response.getEntity());

            if (response.getStatusLine().getStatusCode() != 200) {
                throw new EnzonaException("Failed to create payment order: " + response.getStatusLine().getStatusCode() + " " + jsonResponse);
            }

            // Deserialize response from JSON
            return JsonUtil.fromJson(jsonResponse, CreatePaymentOrderResponse.class);
        } catch (Exception e) {
            throw new EnzonaException("Exception occurred while creating payment order: " + e.getMessage(), e);
        }
    }

    /**
     * Cancels a payment transaction identified by the given UUID.
     * <p>
     * This method sends a POST request to the payment service to cancel a payment transaction. The cancellation is
     * based on the transaction's unique identifier (UUID). The operation expects no payload but requires the
     * transaction UUID as part of the path.
     *
     * @param transactionUuid The unique identifier of the payment transaction to be canceled.
     * @return A {@link CancelPaymentResponse} object containing the outcome of the cancellation operation, including status codes and descriptions.
     * @throws EnzonaException If there is an issue with network communication or processing the request/response.
     */
    public CancelPaymentResponse cancelPayment(String transactionUuid) throws EnzonaException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String url = baseUrl + "payments/" + transactionUuid + "/cancel";
            HttpPost httpPost = new HttpPost(url);

            httpPost.setHeader("Authorization", "Bearer " + authClient.getAccessToken());
            httpPost.setHeader("Content-Type", "application/json");

            // Execute HTTP request
            var response = httpClient.execute(httpPost);
            String jsonResponse = EntityUtils.toString(response.getEntity());

            if (response.getStatusLine().getStatusCode() != 200) {
                throw new EnzonaException("Failed to cancel payment: " + response.getStatusLine().getStatusCode() + " " + jsonResponse);
            }

            // Deserialize response from JSON
            return JsonUtil.fromJson(jsonResponse, CancelPaymentResponse.class);
        } catch (Exception e) {
            throw new EnzonaException("Exception occurred while canceling payment: " + e.getMessage(), e);
        }
    }

    /**
     * Processes a refund for a specified payment transaction.
     * <p>
     * This method sends a POST request to the payment service to process a refund for a payment identified by its transaction UUID.
     * The request includes the amount to be refunded, a unique identifier for the refund operation within the commerce system,
     * the username of the user processing the refund, and a description of the refund.
     *
     * @param transactionUuid The unique identifier of the payment transaction for which the refund is being processed.
     * @param request The {@link RefundPaymentRequest} object containing all the necessary data for processing the refund.
     * @return A {@link RefundPaymentResponse} object containing details about the processed refund, including the status code, parent payment UUID, and other information.
     * @throws EnzonaException If there is an issue with network communication or processing the request/response.
     */
    public RefundPaymentResponse refundPayment(String transactionUuid, RefundPaymentRequest request) throws EnzonaException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String url = baseUrl + "payments/" + transactionUuid + "/refund";
            HttpPost httpPost = new HttpPost(url);

            httpPost.setHeader("Authorization", "Bearer " + authClient.getAccessToken());
            httpPost.setHeader("Content-Type", "application/json");

            // Serialize request to JSON
            String json = JsonUtil.toJson(request);
            StringEntity entity = new StringEntity(json);
            httpPost.setEntity(entity);

            // Execute HTTP request
            var response = httpClient.execute(httpPost);
            String jsonResponse = EntityUtils.toString(response.getEntity());

            if (response.getStatusLine().getStatusCode() != 200) {
                throw new EnzonaException("Failed to process refund: " + response.getStatusLine().getStatusCode() + " " + jsonResponse);
            }

            // Deserialize response from JSON
            return JsonUtil.fromJson(jsonResponse, RefundPaymentResponse.class);
        } catch (Exception e) {
            throw new EnzonaException("Exception occurred while processing refund: " + e.getMessage(), e);
        }
    }

}
