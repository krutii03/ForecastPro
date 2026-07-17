package com.forecastpro.service;

import com.forecastpro.config.AccessDeniedException;
import com.forecastpro.config.BusinessException;
import com.forecastpro.entity.InventoryMovement;
import com.forecastpro.entity.Product;
import com.forecastpro.entity.StockRequest;
import com.forecastpro.entity.StockRequestStatus;
import com.forecastpro.entity.User;
import com.forecastpro.entity.UserRole;
import com.forecastpro.entity.Vendor;
import com.forecastpro.repository.InventoryMovementRepository;
import com.forecastpro.repository.ProductRepository;
import com.forecastpro.repository.StockRequestRepository;
import com.forecastpro.repository.UserRepository;
import com.forecastpro.repository.VendorRepository;
import com.forecastpro.util.ValidationUtil;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.util.List;

@Stateless
public class StockRequestService {

    public record VendorStockRequestSummary(
            long total,
            long pending,
            long activeFulfillment,
            long awaitingWarehouseReceipt,
            long received) {
    }

    @Inject
    private StockRequestRepository stockRequestRepository;

    @Inject
    private ProductRepository productRepository;

    @Inject
    private VendorRepository vendorRepository;

    @Inject
    private UserRepository userRepository;

    @Inject
    private InventoryMovementRepository inventoryMovementRepository;

    @Inject
    private SecurityService securityService;

    public List<StockRequest> findAll(UserRole caller) {
        securityService.requireAuthenticated(caller);
        if (caller == UserRole.VENDOR) {
            throw new AccessDeniedException("Vendor users must use findByVendorUserId.");
        }
        return stockRequestRepository.findAll();
    }

    public List<StockRequest> findByVendorUserId(UserRole caller, Long userId) {
        securityService.requireAuthenticated(caller);
        if (caller != UserRole.VENDOR) {
            throw new AccessDeniedException("Only vendor users can load vendor-scoped requests.");
        }
        if (userId == null) {
            throw new AccessDeniedException("Invalid session.");
        }
        return stockRequestRepository.findByVendorUserId(userId);
    }

    public VendorStockRequestSummary summarizeVendorRequests(UserRole caller, Long userId) {
        securityService.requireAuthenticated(caller);
        if (caller != UserRole.VENDOR || userId == null) {
            throw new AccessDeniedException("Vendor only.");
        }
        long total = stockRequestRepository.countByVendorUserId(userId);
        long pending = stockRequestRepository.countByVendorUserIdAndStatus(userId, StockRequestStatus.PENDING);
        long activeFulfillment =
                stockRequestRepository.countByVendorUserIdAndStatus(userId, StockRequestStatus.APPROVED)
                        + stockRequestRepository.countByVendorUserIdAndStatus(userId, StockRequestStatus.READY_PACKED)
                        + stockRequestRepository.countByVendorUserIdAndStatus(userId, StockRequestStatus.OUT_FOR_DELIVERY)
                        + stockRequestRepository.countByVendorUserIdAndStatus(userId, StockRequestStatus.READY); // legacy
        long awaitingWarehouseReceipt =
                stockRequestRepository.countByVendorUserIdAndStatus(userId, StockRequestStatus.DELIVERED);
        long received =
                stockRequestRepository.countByVendorUserIdAndStatus(userId, StockRequestStatus.COMPLETED)
                        + stockRequestRepository.countByVendorUserIdAndStatus(userId, StockRequestStatus.RECEIVED); // legacy
        return new VendorStockRequestSummary(total, pending, activeFulfillment, awaitingWarehouseReceipt, received);
    }

    public void create(UserRole caller, Long requesterId, Long productId, Long vendorId, int quantity) {
        securityService.requireAdminOrManager(caller);
        if (requesterId == null) {
            throw new BusinessException("Invalid session.");
        }
        ValidationUtil.requirePositive(quantity, "Quantity");
        Product p = productRepository.findById(productId).orElseThrow(() -> new BusinessException("Product not found."));
        Vendor v = vendorRepository.findById(vendorId).orElseThrow(() -> new BusinessException("Vendor not found."));
        userRepository.findById(requesterId).orElseThrow(() -> new BusinessException("User not found."));
        StockRequest r = new StockRequest();
        r.setProduct(p);
        r.setVendor(v);
        r.setRequestedQuantity(quantity);
        r.setStatus(StockRequestStatus.PENDING);
        stockRequestRepository.save(r);
    }

    /**
     * Vendor fulfillment: APPROVED→READY_PACKED, READY_PACKED→OUT_FOR_DELIVERY, OUT_FOR_DELIVERY→DELIVERED.
     * Reject: PENDING→REJECTED (vendor on own requests, or admin/manager on any pending request).
     *
     * Admin adds delivered quantity to stock via {@link #addToInventory(UserRole, Long, Long)}.
     */
    public void transitionStatus(UserRole caller, Long actorId, Long requestId, StockRequestStatus next) {
        securityService.requireAuthenticated(caller);
        if (actorId == null || requestId == null) {
            throw new BusinessException("Invalid request.");
        }
        StockRequest r = stockRequestRepository.findByIdFetched(requestId);
        if (r == null) {
            throw new BusinessException("Request not found.");
        }
        StockRequestStatus current = normalize(r.getStatus());

        if (next == StockRequestStatus.REJECTED) {
            rejectPending(caller, actorId, r, current);
            return;
        }

        if (caller == UserRole.VENDOR) {
            requireVendorOwns(actorId, r);
            assertVendorTransition(current, next);
            r.setStatus(next);
            stockRequestRepository.save(r);
            return;
        }
        throw new AccessDeniedException("Only vendors can update request status.");
    }

    private void rejectPending(UserRole caller, Long actorId, StockRequest r, StockRequestStatus current) {
        if (current != StockRequestStatus.PENDING) {
            throw new BusinessException("Only pending requests can be rejected.");
        }
        if (caller == UserRole.VENDOR) {
            requireVendorOwns(actorId, r);
        } else if (caller == UserRole.ADMIN || caller == UserRole.SALES_MANAGER) {
            // Admin / manager may cancel a pending request before vendor accepts.
        } else {
            throw new AccessDeniedException("Not allowed to reject stock requests.");
        }
        r.setStatus(StockRequestStatus.REJECTED);
        stockRequestRepository.save(r);
    }

    private static void assertVendorTransition(StockRequestStatus current, StockRequestStatus next) {
        if (current == StockRequestStatus.COMPLETED) {
            throw new BusinessException("Request is already completed.");
        }
        if (current == StockRequestStatus.PENDING && next == StockRequestStatus.APPROVED) {
            return;
        }
        if (current == StockRequestStatus.APPROVED && next == StockRequestStatus.READY_PACKED) {
            return;
        }
        if (current == StockRequestStatus.READY_PACKED && next == StockRequestStatus.OUT_FOR_DELIVERY) {
            return;
        }
        if (current == StockRequestStatus.OUT_FOR_DELIVERY && next == StockRequestStatus.DELIVERED) {
            return;
        }
        throw new BusinessException("Invalid status transition for vendor.");
    }

    /**
     * Admin only: DELIVERED → COMPLETED.
     * Updates product stock and writes an inventory movement row exactly once.
     */
    public void addToInventory(UserRole caller, Long actorId, Long requestId) {
        securityService.requireAdmin(caller);
        if (actorId == null || requestId == null) {
            throw new BusinessException("Invalid request.");
        }
        StockRequest r = stockRequestRepository.findByIdFetched(requestId);
        if (r == null) {
            throw new BusinessException("Request not found.");
        }
        StockRequestStatus current = normalize(r.getStatus());
        if (current == StockRequestStatus.COMPLETED) {
            throw new BusinessException("Already added to stock.");
        }
        if (current != StockRequestStatus.DELIVERED) {
            throw new BusinessException("Can only add to stock when status is DELIVERED.");
        }

        User actor = userRepository.findById(actorId).orElseThrow(() -> new BusinessException("User not found."));

        productRepository.adjustStock(r.getProduct().getId(), r.getRequestedQuantity());
        InventoryMovement row = new InventoryMovement();
        row.setProduct(r.getProduct());
        row.setQuantityAdded(r.getRequestedQuantity());
        row.setSource(InventoryService.sourceLabelForVendor(r.getVendor()));
        inventoryMovementRepository.save(row);

        r.setStatus(StockRequestStatus.COMPLETED);
        stockRequestRepository.save(r);
    }

    private static StockRequestStatus normalize(StockRequestStatus s) {
        if (s == null) {
            return StockRequestStatus.PENDING;
        }
        return switch (s) {
            case RECEIVED -> StockRequestStatus.COMPLETED; // legacy terminal state
            case READY -> StockRequestStatus.READY_PACKED; // legacy mid state
            default -> s;
        };
    }

    private static void requireVendorOwns(Long actorId, StockRequest r) {
        User linked = r.getVendor() != null ? r.getVendor().getUser() : null;
        if (linked == null || !linked.getId().equals(actorId)) {
            throw new AccessDeniedException("You can only update requests for your vendor account.");
        }
    }
}
