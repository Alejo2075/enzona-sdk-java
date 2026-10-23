package example;

import io.github.alejo2075.enzona.Environment;
import io.github.alejo2075.enzona.EnzonaClient;
import io.github.alejo2075.enzona.exception.AuthenticationException;
import io.github.alejo2075.enzona.exception.EnzonaErrorCode;
import io.github.alejo2075.enzona.exception.RateLimitException;
import io.github.alejo2075.enzona.exception.TokenException;
import io.github.alejo2075.enzona.exception.ValidationException;
import io.github.alejo2075.enzona.http.RetryPolicy;
import io.github.alejo2075.enzona.model.Payment;
import io.github.alejo2075.enzona.model.PaymentQuery;
import io.github.alejo2075.enzona.model.PaymentStatus;
import io.github.alejo2075.enzona.model.Refund;
import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

/** Timeouts, retries for reads, asynchronous calls and error handling. */
public final class AdvancedUsage {

    private AdvancedUsage() {}

    public static EnzonaClient tunedClient() {
        // snippet:tuned:start
        EnzonaClient client = EnzonaClient.builder()
                .credentials(System.getenv("ENZONA_CONSUMER_KEY"), System.getenv("ENZONA_CONSUMER_SECRET"))
                .environment(Environment.PRODUCTION)
                .connectTimeout(Duration.ofSeconds(5))
                .requestTimeout(Duration.ofSeconds(20))
                // Retries apply to reads only; create, complete, cancel and refund are never retried.
                .retryPolicy(RetryPolicy.exponentialBackoff(3, Duration.ofMillis(200), Duration.ofSeconds(2)))
                .build();
        // snippet:tuned:end
        return client;
    }

    public static CompletableFuture<PaymentStatus> statusAsync(EnzonaClient client, String transactionUuid) {
        // snippet:async:start
        return client.payments()
                .getAsync(transactionUuid)
                .thenApply(Payment::status);
        // snippet:async:end
    }

    public static long pendingTodayCount(EnzonaClient client) {
        // snippet:list:start
        return client.payments()
                .list(PaymentQuery.builder()
                        .status(PaymentStatus.PENDING)
                        .startDate(LocalDate.now())
                        .limit(50)
                        .build())
                .payments()
                .size();
        // snippet:list:end
    }

    /** Returns a short message for the operator; never branch on the text ENZONA sends. */
    public static String refundSafely(EnzonaClient client, String transactionUuid) {
        // snippet:errors:start
        try {
            Refund refund = client.refunds().create(transactionUuid);
            return "Refunded: " + refund.transactionUuid();
        } catch (ValidationException e) {
            // The numeric ENZONA code is stable; the message is Spanish prose and may change.
            if (e.knownErrorCode().orElse(EnzonaErrorCode.UNKNOWN) == EnzonaErrorCode.PAYMENT_ALREADY_FULLY_REFUNDED) {
                return "Already refunded";
            }
            return "ENZONA rejected the refund, code " + e.errorCode();
        } catch (RateLimitException e) {
            return "Too many requests, retry in " + e.retryAfter().orElse(Duration.ofSeconds(5));
        } catch (AuthenticationException | TokenException e) {
            return "Check the consumer key, the secret and the environment";
        }
        // snippet:errors:end
    }
}
