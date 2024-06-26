package io.github.alejo2075.enzonasdk.model.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class PaymentDetailsResponse {
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
    public static class Link {
        private String method;
        private String rel;
        private String href;
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
