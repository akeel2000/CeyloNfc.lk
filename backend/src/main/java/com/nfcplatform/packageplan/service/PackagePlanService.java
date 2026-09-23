package com.nfcplatform.packageplan.service;

import com.nfcplatform.common.exception.ResourceNotFoundException;
import com.nfcplatform.common.exception.ValidationException;
import com.nfcplatform.packageplan.dto.PackagePlanRequest;
import com.nfcplatform.packageplan.dto.PackagePlanResponse;
import com.nfcplatform.packageplan.entity.BillingPeriod;
import com.nfcplatform.packageplan.entity.PackagePlan;
import com.nfcplatform.packageplan.repository.PackagePlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PackagePlanService {

    private final PackagePlanRepository packagePlanRepository;

    @Transactional
    public PackagePlanResponse create(PackagePlanRequest request) {
        PackagePlan plan = new PackagePlan();
        apply(plan, request);
        return PackagePlanResponse.from(packagePlanRepository.save(plan));
    }

    @Transactional
    public PackagePlanResponse update(String uuid, PackagePlanRequest request) {
        PackagePlan plan = packagePlanRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Package plan was not found"));
        apply(plan, request);
        return PackagePlanResponse.from(packagePlanRepository.save(plan));
    }

    @Transactional(readOnly = true)
    public List<PackagePlanResponse> listAllForAdmin() {
        return packagePlanRepository.findAllByOrderBySortOrderAsc().stream()
                .map(PackagePlanResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PackagePlanResponse> listActiveForPublic() {
        return packagePlanRepository.findAllByActiveTrueOrderBySortOrderAsc().stream()
                .map(PackagePlanResponse::from)
                .toList();
    }

    private void apply(PackagePlan plan, PackagePlanRequest request) {
        plan.setName(request.name());
        plan.setDescription(request.description());
        plan.setPrice(request.price());
        plan.setBillingPeriod(parseBillingPeriod(request.billingPeriod()));
        plan.setCardLimit(request.cardLimit());
        plan.setProfileLimit(request.profileLimit());
        plan.setReviewLocationLimit(request.reviewLocationLimit());
        plan.setMenuLimit(request.menuLimit());
        plan.setPremiumTemplates(Boolean.TRUE.equals(request.premiumTemplates()));
        plan.setActive(request.active() == null || request.active());
        if (request.sortOrder() != null) plan.setSortOrder(request.sortOrder());
    }

    private BillingPeriod parseBillingPeriod(String value) {
        try {
            return BillingPeriod.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid billing period: " + value);
        }
    }
}
