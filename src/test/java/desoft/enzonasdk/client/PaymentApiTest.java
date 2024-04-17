package desoft.enzonasdk.client;

public class PaymentApiTest {

    /*@Mock
    private CloseableHttpClient mockHttpClient;
    @Mock
    private CloseableHttpResponse mockResponse;
    @Mock
    private StatusLine mockStatusLine;

    private PaymentApi paymentApi;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        paymentApi = new PaymentApi("consumerKey", "consumerSecret");
        paymentApi.setHttpClient(mockHttpClient);
    }

    @Test(expected = EnzonaException.class)
    public void testConfirmPaymentFailure() throws Exception {
        when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(500);
        when(mockResponse.getEntity()).thenReturn(new StringEntity("{\"error\":\"Internal Server Error\"}"));

        ConfirmPaymentRequest request = new ConfirmPaymentRequest(); // Populate request object accordingly
        paymentApi.confirmPayment("uuid", request);
    }

    @Test
    public void testConfirmPaymentSuccess() throws Exception {
        when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(200);
        when(mockResponse.getEntity()).thenReturn(new StringEntity("{\"status\":\"success\"}"));

        ConfirmPaymentRequest request = new ConfirmPaymentRequest(); // Populate request object accordingly
        ConfirmPaymentResponse response = paymentApi.confirmPayment("uuid", request);

        assertNotNull(response);
        assertEquals("success", response.getStatus());
    }

    @Test(expected = EnzonaException.class)
    public void testCompletePaymentFailure() throws Exception {
        when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(400);
        when(mockResponse.getEntity()).thenReturn(new StringEntity("{\"error\":\"Bad Request\"}"));

        paymentApi.completePayment("uuid");
    }

    @Test
    public void testCompletePaymentSuccess() throws Exception {
        when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(200);
        when(mockResponse.getEntity()).thenReturn(new StringEntity("{\"status\":\"completed\"}"));

        CompletePaymentResponse response = paymentApi.completePayment("uuid");

        assertNotNull(response);
        assertEquals("completed", response.getStatusCode());
    }

    @Test(expected = EnzonaException.class)
    public void testGetRefundDetailsFailure() throws Exception {
        when(mockHttpClient.execute(any(HttpGet.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(404);
        when(mockResponse.getEntity()).thenReturn(new StringEntity("{\"error\":\"Not Found\"}"));

        paymentApi.getRefundDetails("uuid");
    }

    @Test
    public void testGetRefundDetailsSuccess() throws Exception {
        String jsonResponse = "{\"transaction_status_code\":\"completed\",\"parent_payment_uuid\":\"parentUuid\"}";
        when(mockHttpClient.execute(any(HttpGet.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(200);
        when(mockResponse.getEntity()).thenReturn(new StringEntity(jsonResponse));

        RefundDetailsResponse response = paymentApi.getRefundDetails("uuid");

        assertNotNull(response);
        assertEquals("completed", response.getTransactionStatusCode());
        assertEquals("parentUuid", response.getParentPaymentUuid());
    }

    @Test(expected = EnzonaException.class)
    public void testGetRefundsListFailure() throws Exception {
        when(mockHttpClient.execute(any(HttpGet.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(404);
        when(mockResponse.getEntity()).thenReturn(new StringEntity("{\"error\":\"Not Found\"}"));

        RefundsListRequest request = new RefundsListRequest(); // Populate request object as needed
        paymentApi.getRefundsList(request);
    }

    @Test
    public void testGetRefundsListSuccess() throws Exception {
        String jsonResponse = "{\"refunds\":[{\"transaction_code\":2000}]}";
        when(mockHttpClient.execute(any(HttpGet.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(200);
        when(mockResponse.getEntity()).thenReturn(new StringEntity(jsonResponse));

        RefundsListRequest request = new RefundsListRequest(); // Populate request object as needed
        RefundsListResponse response = paymentApi.getRefundsList(request);

        assertNotNull(response);
        assertEquals(2000, response.getRefunds().get(0).getTransactionCode());
    }

    @Test(expected = EnzonaException.class)
    public void testCreatePaymentFailure() throws Exception {
        when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(400);
        when(mockResponse.getEntity()).thenReturn(new StringEntity("{\"error\":\"Bad Request\"}"));

        CreatePaymentRequest request = new CreatePaymentRequest(); // Populate request object accordingly
        paymentApi.createPayment(request);
    }

    @Test
    public void testCreatePaymentSuccess() throws Exception {
        String jsonResponse = "{\"status\":\"success\", \"transaction_uuid\":\"uuid\"}";
        when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(200);
        when(mockResponse.getEntity()).thenReturn(new StringEntity(jsonResponse));

        CreatePaymentRequest request = new CreatePaymentRequest(); // Populate request object accordingly
        CreatePaymentResponse response = paymentApi.createPayment(request);

        assertNotNull(response);
        assertEquals("success", response.getStatusCode());
        assertEquals("uuid", response.getTransactionUuid());
    }

    @Test(expected = EnzonaException.class)
    public void testGetPaymentsListFailure() throws Exception {
        when(mockHttpClient.execute(any(HttpGet.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(400);
        when(mockResponse.getEntity()).thenReturn(new StringEntity("{\"error\":\"Bad Request\"}"));

        PaymentsListRequest request = new PaymentsListRequest(); // Populate with test criteria
        paymentApi.getPaymentsList(request);
    }

    @Test
    public void testGetPaymentsListSuccess() throws Exception {
        String jsonResponse = "{\"payments\":[{\"amount\":{\"total\":100.0},\"currency\":\"USD\"}]}";
        when(mockHttpClient.execute(any(HttpGet.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(200);
        when(mockResponse.getEntity()).thenReturn(new StringEntity(jsonResponse));

        PaymentsListRequest request = new PaymentsListRequest(); // Populate with test criteria
        PaymentsListResponse response = paymentApi.getPaymentsList(request);

        assertNotNull(response);
        assertFalse(response.getPayments().isEmpty());
        assertEquals(100.0, response.getPayments().get(0).getAmount().getTotal(), 0.01);
    }

    @Test(expected = EnzonaException.class)
    public void testGetPaymentDetailsFailure() throws Exception {
        when(mockHttpClient.execute(any(HttpGet.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(404);
        when(mockResponse.getEntity()).thenReturn(new StringEntity("{\"error\":\"Not Found\"}"));

        paymentApi.getPaymentDetails("invalidUuid");
    }

    @Test
    public void testGetPaymentDetailsSuccess() throws Exception {
        String jsonResponse = "{\"amount\":{\"total\":200.0},\"status_code\":\"completed\"}";
        when(mockHttpClient.execute(any(HttpGet.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(200);
        when(mockResponse.getEntity()).thenReturn(new StringEntity(jsonResponse));

        PaymentDetailsResponse response = paymentApi.getPaymentDetails("validUuid");

        assertNotNull(response);
        assertEquals(200.0, response.getAmount().getTotal(), 0.01);
        assertEquals("completed", response.getStatusCode());
    }

    @Test(expected = EnzonaException.class)
    public void testPerformCheckoutFailure() throws Exception {
        when(mockHttpClient.execute(any(HttpGet.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(404);
        when(mockResponse.getEntity()).thenReturn(new StringEntity("{\"error\":\"Not Found\"}"));

        paymentApi.performCheckout("invalidUuid");
    }

    @Test
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

    @Test(expected = EnzonaException.class)
    public void testCreateReceiveCodeFailure() throws Exception {
        when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(400);
        when(mockResponse.getEntity()).thenReturn(new StringEntity("{\"error\":\"Bad Request\"}"));

        CreateReceiveCodeRequest request = new CreateReceiveCodeRequest(); // Populate request object as needed
        paymentApi.createReceiveCode(request);
    }

    @Test
    public void testCreateReceiveCodeSuccess() throws Exception {
        String jsonResponse = "{\"message\":\"Receive code created successfully\", \"status\":\"success\"}";
        when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(200);
        when(mockResponse.getEntity()).thenReturn(new StringEntity(jsonResponse));

        CreateReceiveCodeRequest request = new CreateReceiveCodeRequest(); // Populate request object as needed
        CreateReceiveCodeResponse response = paymentApi.createReceiveCode(request);

        assertNotNull(response);
        assertEquals("success", response.getStatus());
        assertEquals("Receive code created successfully", response.getMensaje());
    }

    @Test(expected = EnzonaException.class)
    public void testListRefundsFailure() throws Exception {
        when(mockHttpClient.execute(any(HttpGet.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(400);
        when(mockResponse.getEntity()).thenReturn(new StringEntity("{\"error\":\"Bad Request\"}"));

        ListRefundsRequest request = new ListRefundsRequest();
        request.setTransactionUuid("validUuid");
        paymentApi.listRefunds(request);
    }

    @Test
    public void testListRefundsSuccess() throws Exception {
        String jsonResponse = "{\"refunds\":[{\"amount\":{\"total\":50.0},\"status\":\"processed\"}]}";
        when(mockHttpClient.execute(any(HttpGet.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(200);
        when(mockResponse.getEntity()).thenReturn(new StringEntity(jsonResponse));

        ListRefundsRequest request = new ListRefundsRequest();
        request.setTransactionUuid("validUuid");
        ListRefundsResponse response = paymentApi.listRefunds(request);

        assertNotNull(response);
        assertFalse(response.getRefunds().isEmpty());
        assertEquals(50.0, response.getRefunds().get(0).getAmount().getTotal(), 0.01);
        assertEquals("processed", response.getRefunds().get(0).getStatusCode());
    }

    @Test(expected = EnzonaException.class)
    public void testPayProductFailure() throws Exception {
        when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(400);
        when(mockResponse.getEntity()).thenReturn(new StringEntity("{\"error\":\"Bad Request\"}"));

        PayProductRequest request = new PayProductRequest(); // Populate request object accordingly
        paymentApi.payProduct(request);
    }

    @Test
    public void testPayProductSuccess() throws Exception {
        String jsonResponse = "{\"message\":\"Payment successful\", \"status\":\"success\"}";
        when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(200);
        when(mockResponse.getEntity()).thenReturn(new StringEntity(jsonResponse));

        PayProductRequest request = new PayProductRequest(); // Populate request object accordingly
        PayProductResponse response = paymentApi.payProduct(request);

        assertNotNull(response);
        assertEquals("success", response.getStatus());
        assertEquals("Payment successful", response.getMensaje());
    }

    @Test(expected = EnzonaException.class)
    public void testCreatePaymentOrderFailure() throws Exception {
        when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(400);
        when(mockResponse.getEntity()).thenReturn(new StringEntity("{\"error\":\"Bad Request\"}"));

        CreatePaymentOrderRequest request = new CreatePaymentOrderRequest(); // Populate request object as needed
        paymentApi.createPaymentOrder(request);
    }

    @Test
    public void testCreatePaymentOrderSuccess() throws Exception {
        String jsonResponse = "{\"status\":\"success\", \"transaction_uuid\":\"uuid\"}";
        when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(200);
        when(mockResponse.getEntity()).thenReturn(new StringEntity(jsonResponse));

        CreatePaymentOrderRequest request = new CreatePaymentOrderRequest(); // Populate request object as needed
        CreatePaymentOrderResponse response = paymentApi.createPaymentOrder(request);

        assertNotNull(response);
        assertEquals("success", response.getStatusCode());
        assertEquals("uuid", response.getTransactionUuid());
    }

    @Test(expected = EnzonaException.class)
    public void testCancelPaymentFailure() throws Exception {
        when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(400);
        when(mockResponse.getEntity()).thenReturn(new StringEntity("{\"error\":\"Bad Request\"}"));

        paymentApi.cancelPayment("invalidUuid");
    }

    @Test
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

    @Test(expected = EnzonaException.class)
    public void testRefundPaymentFailure() throws Exception {
        when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(400);
        when(mockResponse.getEntity()).thenReturn(new StringEntity("{\"error\":\"Bad Request\"}"));

        RefundPaymentRequest request = new RefundPaymentRequest(); // Populate request object as needed
        paymentApi.refundPayment("invalidUuid", request);
    }

    @Test
    public void testRefundPaymentSuccess() throws Exception {
        String jsonResponse = "{\"status\":\"success\", \"description\":\"Refund processed\"}";
        when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(200);
        when(mockResponse.getEntity()).thenReturn(new StringEntity(jsonResponse));

        RefundPaymentRequest request = new RefundPaymentRequest(); // Populate request object as needed
        RefundPaymentResponse response = paymentApi.refundPayment("validUuid", request);

        assertNotNull(response);
        assertEquals("success", response.getStatusCode());
        assertEquals("Refund processed", response.getDescription());
    }*/


}
