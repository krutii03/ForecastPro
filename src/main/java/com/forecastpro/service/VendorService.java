package com.forecastpro.service;

import com.forecastpro.config.BusinessException;
import com.forecastpro.entity.UserRole;
import com.forecastpro.entity.Vendor;
import com.forecastpro.repository.VendorRepository;
import com.forecastpro.util.ValidationUtil;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;

@Stateless
public class VendorService {

    @Inject
    private VendorRepository vendorRepository;

    @Inject
    private SecurityService securityService;

    /**
     * Vendor profile for a login user ({@code SELECT v FROM Vendor v WHERE v.user.id = :uid}).
     */
    public Optional<Vendor> findVendorForUser(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return vendorRepository.findByUserId(userId);
    }

    /** Same as {@link #findVendorForUser(Long)} but throws if no row exists. */
    public Vendor getLoggedInVendor(Long userId) {
        return findVendorForUser(userId)
                .orElseThrow(() -> new BusinessException("No vendor profile linked to this user."));
    }

    /**
     * All vendor rows ({@code SELECT v FROM Vendor v}).
     */
    public List<Vendor> findAll(UserRole caller) {
        securityService.requireAuthenticated(caller);
        return vendorRepository.findAll();
    }

    public List<Vendor> listForUi(UserRole caller) {
        securityService.requireAuthenticated(caller);
        if (caller == UserRole.ADMIN) {
            return vendorRepository.findAll();
        }
        return vendorRepository.findAllActiveOrdered();
    }

    public Vendor save(UserRole caller, Long id, String name, String contactEmail, String phone, String address,
                       boolean active) {
        securityService.requireAdmin(caller);
        ValidationUtil.requireNonBlank(name, "Vendor name");
        Vendor v;
        if (id == null) {
            v = new Vendor();
        } else {
            v = vendorRepository.findById(id).orElseThrow(() -> new BusinessException("Vendor not found."));
        }
        v.setName(name.trim());
        v.setContactEmail(contactEmail != null ? contactEmail.trim() : null);
        v.setPhone(phone != null ? phone.trim() : null);
        v.setAddress(address != null ? address.trim() : null);
        v.setActive(active);
        return vendorRepository.save(v);
    }

    public void delete(UserRole caller, Long id) {
        securityService.requireAdmin(caller);
        Vendor v = vendorRepository.findById(id).orElseThrow(() -> new BusinessException("Vendor not found."));
        vendorRepository.delete(v);
    }
}
