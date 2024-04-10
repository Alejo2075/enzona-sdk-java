package com.desoft.enzonasdk.model.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class RefundsListResponse {
    private List<Refund> refunds;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Refund {
        private int transactionCode;
        private Amount amount;
        private String statusCode;
        private String transactionSignature;
        private String transactionDenom;
        private String transactionUuid;
        private String avatar;
        private String transactionDescription;
        private String transactionUpdatedAt;
        private String lastname;
        private String name;
        private String statusDenom;
        private String currency;
        private String transactionCreatedAt;
        private double invoiceNumber;
        private List<Item> items;
        private double terminalId;
        private String username;

        @Getter
        @Setter
        @NoArgsConstructor
        public static class Amount {
            private double total;
            private Details details;

            @Getter
            @Setter
            @NoArgsConstructor
            public static class Details {
                private double shipping;
                private double totalRefunded;
                private double discount;
                private double tax;
                private double tip;
                private double refunded;
            }
        }

        @Getter
        @Setter
        @NoArgsConstructor
        public static class Item {
            private int quantity;
            private double price;
            private String name;
            private String description;
            private double tax;
        }
    }
}
