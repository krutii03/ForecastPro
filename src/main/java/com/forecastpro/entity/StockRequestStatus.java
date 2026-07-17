package com.forecastpro.entity;

/**
 * Stored as string in {@code stock_requests.status}.
 */
public enum StockRequestStatus {
    /** Request created by Admin/Manager, awaiting vendor confirmation. */
    PENDING,
    /** Vendor accepted the request. */
    APPROVED,
    /** Vendor has packed and made it ready. */
    READY_PACKED,
    /** Vendor has dispatched. */
    OUT_FOR_DELIVERY,
    /** Vendor marked delivered to warehouse. */
    DELIVERED,
    /** Admin added delivered quantity into stock; terminal state. */
    COMPLETED,

    /** Request declined by vendor or cancelled by admin/manager while pending. */
    REJECTED,

    /**
     * Legacy statuses kept to avoid breaking existing rows.
     */
    READY,
    RECEIVED
}
