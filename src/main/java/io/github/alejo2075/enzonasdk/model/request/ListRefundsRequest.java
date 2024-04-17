package io.github.alejo2075.enzonasdk.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListRefundsRequest {
    private String transactionUuid; // This will be part of the path, not a query parameter
    private String limit;
    private String offset;
    private String statusFilter;
    private String startDateFilter;
    private String endDateFilter;
    private String orderFilter;
}
