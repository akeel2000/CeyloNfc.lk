package com.nfcplatform.packageplan.service;

import com.nfcplatform.common.exception.ResourceNotFoundException;
import com.nfcplatform.common.exception.ValidationException;
import com.nfcplatform.packageplan.dto.PackagePlanRequest;
import com.nfcplatform.packageplan.dto.PackagePlanResponse;
import com.nfcplatform.packageplan.entity.PackagePlan;
import com.nfcplatform.packageplan.repository.PackagePlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for package plan create/update, focused on the two explicit "null means don't
 * change / use the default" fields (sortOrder on update, active/premiumTemplates on create) -
 * the kind of quiet default-handling logic that's easy to get backwards. Pure Mockito, no
 * Spring context/DB.
 */
class PackagePlanServiceTest {

    @Mock
    private PackagePlanRepository packagePlanRepository;

    private PackagePlanService packagePlanService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        packagePlanService = new PackagePlanService(packagePlanRepository);
        when(packagePlanRepository.save(any(PackagePlan.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createRejectsAnUnrecognizedBillingPeriod() {
        assertThatThrownBy(() -> packagePlanService.create(request("NOT_A_PERIOD", null)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createDefaultsActiveToTrueAndPremiumTemplatesToFalseWhenNotSupplied() {
        PackagePlanResponse response = packagePlanService.create(
                new PackagePlanRequest("Starter", null, BigDecimal.TEN, "MONTHLY", null, null, null, null,
                        null, null, null));

        assertThat(response.active()).isTrue();
        assertThat(response.premiumTemplates()).isFalse();
    }

    @Test
    void updateThrowsForAnUnknownPlan() {
        when(packagePlanRepository.findByUuid("uuid")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> packagePlanService.update("uuid", request("MONTHLY", null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateLeavesSortOrderUnchangedWhenNoneIsSupplied() {
        PackagePlan plan = plan();
        plan.setSortOrder(5);
        when(packagePlanRepository.findByUuid("uuid")).thenReturn(Optional.of(plan));

        packagePlanService.update("uuid", request("MONTHLY", null));

        assertThat(plan.getSortOrder()).isEqualTo(5);
    }

    @Test
    void updateOverwritesSortOrderWhenSupplied() {
        PackagePlan plan = plan();
        plan.setSortOrder(5);
        when(packagePlanRepository.findByUuid("uuid")).thenReturn(Optional.of(plan));

        packagePlanService.update("uuid", request("MONTHLY", 9));

        assertThat(plan.getSortOrder()).isEqualTo(9);
    }

    private PackagePlanRequest request(String billingPeriod, Integer sortOrder) {
        return new PackagePlanRequest("Starter", null, BigDecimal.TEN, billingPeriod, null, null, null, null,
                false, true, sortOrder);
    }

    private PackagePlan plan() {
        PackagePlan plan = new PackagePlan();
        plan.setName("Starter");
        return plan;
    }
}
