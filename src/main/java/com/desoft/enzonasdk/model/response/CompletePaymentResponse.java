package com.desoft.enzonasdk.model.response;

import lombok.Data;
import java.util.List;

@Data
public class CompletePaymentResponse {
    private Amount amount;
    private String statusCode;
    private String createdAt;
    private String description;
    private String transactionUuid;
    private String merchantOpId;
    private String updateAt;
    private String currency;
    private List<Link> links;
    private String merchantUuid;
    private String invoiceNumber;
    private List<Item> items;
    private String terminalId;

    @Data
    public static class Amount {
        private double total;
        private Details details;

        @Data
        public static class Details {
            private double shipping;
            private double discount;
            private double tax;
            private double tip;
        }
    }

    @Data
    public static class Link {
        private String method;
        private String rel;
        private String href;
    }

    @Data
    public static class Item {
        private int quantity;
        private double price;
        private String name;
        private String description;
        private double tax;
    }
}

