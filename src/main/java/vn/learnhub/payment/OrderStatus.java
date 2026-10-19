package vn.learnhub.payment;

public enum OrderStatus {
    PENDING, UNDERPAID, PAID, CANCELLED, FAILED, REFUNDED;

    public boolean isAwaitingPayment() {
        return this == PENDING || this == UNDERPAID;
    }
}
