package com.desoft.enzonasdk.model.request;

import lombok.Data;
import java.util.List;

@Data
public class PayProductRequest {
    private String cartId;
    private Amount amount;
    private String fundingSourceUuid;
    private String paymentPassword;
    private String fingerprint;
    private String description;
    private String currency;
    private String merchantUuid;
    private String idShop;
    private List<Item> items;

    @Data
    public static class Amount {
        private double total;
        private double shipping;
    }

    @Data
    public static class Item {
        private int quantity;
        private double price;
        private String productId;
        private String productName;
    }
}
