package io.github.alejo2075.enzonasdk.model.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CreatePaymentRequest {
    private String merchantUuid;
    private long merchantOpId;
    private Amount amount;
    private String description;
    private String returnUrl;
    private String currency;
    private List<Item> items;
    private long invoiceNumber;
    private String cancelUrl;
    private String buyerIdentityCode;
    private long terminalId;

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
            private double discount;
            private double tax;
            private double tip;
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

