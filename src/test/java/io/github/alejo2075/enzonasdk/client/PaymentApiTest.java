package io.github.alejo2075.enzonasdk.client;

import io.github.alejo2075.enzonasdk.exception.EnzonaException;
import io.github.alejo2075.enzonasdk.model.request.*;
import io.github.alejo2075.enzonasdk.model.response.*;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.http.client.methods.CloseableHttpResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class PaymentApiTest {

    @Mock
    private CloseableHttpClient mockHttpClient;
    @Mock
    private CloseableHttpResponse mockResponse;
    @Mock
    private StatusLine mockStatusLine;

    private PaymentApi paymentApi;

    @BeforeEach
    public void setUp() {
        paymentApi = new PaymentApi("consumerKey", "consumerSecret");
        paymentApi.setHttpClient(mockHttpClient);
    }

    // ConfirmPayment
    @Test
    @DisplayName("ConfirmPayment fails with EnzonaException on server error")
    public void testConfirmPaymentFailure() throws Exception {
        when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(500);
        when(mockResponse.getEntity()).thenReturn(new StringEntity("{\"error\":\"Internal Server Error\"}", "UTF-8"));

        ConfirmPaymentRequest request = new ConfirmPaymentRequest();
        EnzonaException exception = assertThrows(EnzonaException.class, () -> paymentApi.confirmPayment("uuid", request));
        assertTrue(exception.getMessage().contains("Internal Server Error"), "Exception should contain 'Internal Server Error'");
    }

    @Test
    @DisplayName("ConfirmPayment succeeds with valid response")
    public void testConfirmPaymentSuccess() throws Exception {
        String jsonResponse = "{\"status\":\"success\"}";
        when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(200);
        when(mockResponse.getEntity()).thenReturn(new StringEntity(jsonResponse));

        ConfirmPaymentRequest request = new ConfirmPaymentRequest();
        ConfirmPaymentResponse response = paymentApi.confirmPayment("uuid", request);

        assertNotNull(response);
        assertEquals("success", response.getStatus());
    }

    // CompletePayment
    @Test
    @DisplayName("CompletePayment fails with EnzonaException on bad request")
    public void testCompletePaymentFailure() throws Exception {
        when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(400);
        when(mockResponse.getEntity()).thenReturn(new StringEntity("{\"error\":\"Bad Request\"}"));

        EnzonaException exception = assertThrows(EnzonaException.class, () -> paymentApi.completePayment("uuid"));
        assertTrue(exception.getMessage().contains("Bad Request"), "Exception should contain 'Bad Request'");
    }

    @Test
    @DisplayName("CompletePayment succeeds with valid response")
    public void testCompletePaymentSuccess() throws Exception {
        String jsonResponse = "{\"status\":\"completed\"}";
        when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(200);
        when(mockResponse.getEntity()).thenReturn(new StringEntity(jsonResponse));

        CompletePaymentResponse response = paymentApi.completePayment("uuid");

        assertNotNull(response);
        assertEquals("completed", response.getStatusCode());
    }

    // GetRefundDetails
    @Test
    @DisplayName("GetRefundDetails fails with EnzonaException on not found")
    public void testGetRefundDetailsFailure() throws Exception {
        when(mockHttpClient.execute(any(HttpGet.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(404);
        when(mockResponse.getEntity()).thenReturn(new StringEntity("{\"error\":\"Not Found\"}"));

        EnzonaException exception = assertThrows(EnzonaException.class, () -> paymentApi.getRefundDetails("uuid"));
        assertTrue(exception.getMessage().contains("Not Found"), "Exception should contain 'Not Found'");
    }

    @Test
    @DisplayName("GetRefundDetails succeeds with valid response")
    public void testGetRefundDetailsSuccess() throws Exception {
        String jsonResponse = "{\"transaction_status_code\":\"completed\", \"parent_payment_uuid\":\"parentUuid\"}";
        when(mockHttpClient.execute(any(HttpGet.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(200);
        when(mockResponse.getEntity()).thenReturn(new StringEntity(jsonResponse));

        RefundDetailsResponse response = paymentApi.getRefundDetails("uuid");

        assertNotNull(response);
        assertEquals("completed", response.getTransactionStatusCode());
        assertEquals("parentUuid", response.getParentPaymentUuid());
    }

    // GetRefundsList
    @Test
    @DisplayName("GetRefundsList fails with EnzonaException on not found")
    public void testGetRefundsListFailure() throws Exception {
        RefundsListRequest request = new RefundsListRequest(); // Assuming this is properly populated
        when(mockHttpClient.execute(any(HttpGet.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(404);
        when(mockResponse.getEntity()).thenReturn(new StringEntity("{\"error\":\"Not Found\"}"));

        EnzonaException exception = assertThrows(EnzonaException.class, () -> paymentApi.getRefundsList(request));
        assertTrue(exception.getMessage().contains("Not Found"), "Exception should contain 'Not Found'");
    }

    @Test
    @DisplayName("GetRefundsList succeeds with valid response")
    public void testGetRefundsListSuccess() throws Exception {
        String jsonResponse = "{\"refunds\":[{\"transaction_code\":2000}]}";
        RefundsListRequest request = new RefundsListRequest(); // Assuming this is properly populated
        when(mockHttpClient.execute(any(HttpGet.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(200);
        when(mockResponse.getEntity()).thenReturn(new StringEntity(jsonResponse));

        RefundsListResponse response = paymentApi.getRefundsList(request);

        assertNotNull(response);
        assertEquals(2000, response.getRefunds().get(0).getTransactionCode());
    }

    // CreatePayment
    @Test
    @DisplayName("CreatePayment fails with EnzonaException on bad request")
    public void testCreatePaymentFailure() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest(); // Assuming this is properly populated
        when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(400);
        when(mockResponse.getEntity()).thenReturn(new StringEntity("{\"error\":\"Bad Request\"}"));

        EnzonaException exception = assertThrows(EnzonaException.class, () -> paymentApi.createPayment(request));
        assertTrue(exception.getMessage().contains("Bad Request"), "Exception should contain 'Bad Request'");
    }

    @Test
    @DisplayName("CreatePayment succeeds with valid response")
    public void testCreatePaymentSuccess() throws Exception {
        String jsonResponse = "{\"status\":\"success\", \"transaction_uuid\":\"uuid\"}";
        CreatePaymentRequest request = new CreatePaymentRequest(); // Assuming this is properly populated
        when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(200);
        when(mockResponse.getEntity()).thenReturn(new StringEntity(jsonResponse));

        CreatePaymentResponse response = paymentApi.createPayment(request);

        assertNotNull(response);
        assertEquals("success", response.getStatusCode());
        assertEquals("uuid", response.getTransactionUuid());
    }

    // PerformCheckout
    @Test
    @DisplayName("PerformCheckout fails with EnzonaException on not found")
    public void testPerformCheckoutFailure() throws Exception {
        when(mockHttpClient.execute(any(HttpGet.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(404);
        when(mockResponse.getEntity()).thenReturn(new StringEntity("{\"error\":\"Not Found\"}", "UTF-8"));

        EnzonaException exception = assertThrows(EnzonaException.class, () -> paymentApi.performCheckout("invalidUuid"));
        assertTrue(exception.getMessage().contains("Not Found"), "Exception should contain 'Not Found'");
    }

    @Test
    @DisplayName("PerformCheckout succeeds with valid response")
    public void testPerformCheckoutSuccess() throws Exception {
        String jsonResponse = "{\"message\":\"Checkout initiated successfully\"}";
        when(mockHttpClient.execute(any(HttpGet.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(200);
        when(mockResponse.getEntity()).thenReturn(new StringEntity(jsonResponse));

        CheckoutResponse response = paymentApi.performCheckout("validUuid");

        assertNotNull(response);
        assertEquals("Checkout initiated successfully", response.getMessage());
    }

    // CreateReceiveCode
    @Test
    @DisplayName("CreateReceiveCode fails with EnzonaException on bad request")
    public void testCreateReceiveCodeFailure() throws Exception {
        CreateReceiveCodeRequest request = new CreateReceiveCodeRequest(); // Assuming this is properly populated
        when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(400);
        when(mockResponse.getEntity()).thenReturn(new StringEntity("{\"error\":\"Bad Request\"}", "UTF-8"));

        EnzonaException exception = assertThrows(EnzonaException.class, () -> paymentApi.createReceiveCode(request));
        assertTrue(exception.getMessage().contains("Bad Request"), "Exception should contain 'Bad Request'");
    }

    @Test
    @DisplayName("CreateReceiveCode succeeds with valid response")
    public void testCreateReceiveCodeSuccess() throws Exception {
        String jsonResponse = "{\"message\":\"Receive code created successfully\", \"status\":\"success\"}";
        CreateReceiveCodeRequest request = new CreateReceiveCodeRequest(); // Assuming this is properly populated
        when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(200);
        when(mockResponse.getEntity()).thenReturn(new StringEntity(jsonResponse));

        CreateReceiveCodeResponse response = paymentApi.createReceiveCode(request);

        assertNotNull(response);
        assertEquals("success", response.getStatus());
        assertEquals("Receive code created successfully", response.getMensaje());
    }

    // ListRefunds
    @Test
    @DisplayName("ListRefunds fails with EnzonaException on not found")
    public void testListRefundsFailure() throws Exception {
        ListRefundsRequest request = new ListRefundsRequest(); // Properly populated
        when(mockHttpClient.execute(any(HttpGet.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(404);
        when(mockResponse.getEntity()).thenReturn(new StringEntity("{\"error\":\"Not Found\"}", "UTF-8"));

        EnzonaException exception = assertThrows(EnzonaException.class, () -> paymentApi.listRefunds(request));
        assertTrue(exception.getMessage().contains("Not Found"), "Exception should contain 'Not Found'");
    }

    @Test
    @DisplayName("ListRefunds succeeds with valid response")
    public void testListRefundsSuccess() throws Exception {
        String jsonResponse = "{\"refunds\":[{\"amount\":{\"total\":50.0},\"status\":\"processed\"}]}";
        ListRefundsRequest request = new ListRefundsRequest(); // Properly populated
        when(mockHttpClient.execute(any(HttpGet.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(200);
        when(mockResponse.getEntity()).thenReturn(new StringEntity(jsonResponse));

        ListRefundsResponse response = paymentApi.listRefunds(request);

        assertNotNull(response);
        assertFalse(response.getRefunds().isEmpty());
        assertEquals(50.0, response.getRefunds().get(0).getAmount().getTotal(), 0.01);
        assertEquals("processed", response.getRefunds().get(0).getStatusCode());
    }

    // PayProduct
    @Test
    @DisplayName("PayProduct fails with EnzonaException on bad request")
    public void testPayProductFailure() throws Exception {
        PayProductRequest request = new PayProductRequest(); // Properly populated
        when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(400);
        when(mockResponse.getEntity()).thenReturn(new StringEntity("{\"error\":\"Bad Request\"}", "UTF-8"));

        EnzonaException exception = assertThrows(EnzonaException.class, () -> paymentApi.payProduct(request));
        assertTrue(exception.getMessage().contains("Bad Request"), "Exception should contain 'Bad Request'");
    }

    @Test
    @DisplayName("PayProduct succeeds with valid response")
    public void testPayProductSuccess() throws Exception {
        String jsonResponse = "{\"message\":\"Payment successful\", \"status\":\"success\"}";
        PayProductRequest request = new PayProductRequest(); // Properly populated
        when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(200);
        when(mockResponse.getEntity()).thenReturn(new StringEntity(jsonResponse));

        PayProductResponse response = paymentApi.payProduct(request);

        assertNotNull(response);
        assertEquals("success", response.getStatus());
        assertEquals("Payment successful", response.getMessage());
    }

    // CreatePaymentOrder
    @Test
    @DisplayName("CreatePaymentOrder fails with EnzonaException on bad request")
    public void testCreatePaymentOrderFailure() throws Exception {
        CreatePaymentOrderRequest request = new CreatePaymentOrderRequest(); // Properly populated
        when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(400);
        when(mockResponse.getEntity()).thenReturn(new StringEntity("{\"error\":\"Bad Request\"}", "UTF-8"));

        EnzonaException exception = assertThrows(EnzonaException.class, () -> paymentApi.createPaymentOrder(request));
        assertTrue(exception.getMessage().contains("Bad Request"), "Exception should contain 'Bad Request'");
    }

    @Test
    @DisplayName("CreatePaymentOrder succeeds with valid response")
    public void testCreatePaymentOrderSuccess() throws Exception {
        String jsonResponse = "{\"status\":\"success\", \"transaction_uuid\":\"uuid\"}";
        CreatePaymentOrderRequest request = new CreatePaymentOrderRequest(); // Properly populated
        when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(200);
        when(mockResponse.getEntity()).thenReturn(new StringEntity(jsonResponse));

        CreatePaymentOrderResponse response = paymentApi.createPaymentOrder(request);

        assertNotNull(response);
        assertEquals("success", response.getStatus());
        assertEquals("uuid", response.getTransactionUuid());
    }

    // CancelPayment
    @Test
    @DisplayName("CancelPayment fails with EnzonaException on bad request")
    public void testCancelPaymentFailure() throws Exception {
        when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(400);
        when(mockResponse.getEntity()).thenReturn(new StringEntity("{\"error\":\"Bad Request\"}", "UTF-8"));

        EnzonaException exception = assertThrows(EnzonaException.class, () -> paymentApi.cancelPayment("invalidUuid"));
        assertTrue(exception.getMessage().contains("Bad Request"), "Exception should contain 'Bad Request'");
    }

    @Test
    @DisplayName("CancelPayment succeeds with valid response")
    public void testCancelPaymentSuccess() throws Exception {
        String jsonResponse = "{\"status\":\"success\"}";
        when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(200);
        when(mockResponse.getEntity()).thenReturn(new StringEntity(jsonResponse));

        CancelPaymentResponse response = paymentApi.cancelPayment("validUuid");

        assertNotNull(response);
        assertEquals("success", response.getStatusCode());
    }

    // RefundPayment
    @Test
    @DisplayName("RefundPayment fails with EnzonaException on bad request")
    public void testRefundPaymentFailure() throws Exception {
        RefundPaymentRequest request = new RefundPaymentRequest(); // Properly populated
        when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(400);
        when(mockResponse.getEntity()).thenReturn(new StringEntity("{\"error\":\"Bad Request\"}", "UTF-8"));

        EnzonaException exception = assertThrows(EnzonaException.class, () -> paymentApi.refundPayment("invalidUuid", request));
        assertTrue(exception.getMessage().contains("Bad Request"), "Exception should contain 'Bad Request'");
    }

    @Test
    @DisplayName("RefundPayment succeeds with valid response")
    public void testRefundPaymentSuccess() throws Exception {
        String jsonResponse = "{\"status\":\"success\", \"description\":\"Refund processed\"}";
        RefundPaymentRequest request = new RefundPaymentRequest(); // Properly populated
        when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(200);
        when(mockResponse.getEntity()).thenReturn(new StringEntity(jsonResponse));

        RefundPaymentResponse response = paymentApi.refundPayment("validUuid", request);

        assertNotNull(response);
        assertEquals("success", response.getStatusCode());
        assertEquals("Refund processed", response.getDescription());
    }


}
