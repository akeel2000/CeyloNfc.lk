package com.nfcplatform.profile.repository;

import com.nfcplatform.profile.entity.BusinessHour;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BusinessHourRepository extends JpaRepository<BusinessHour, Long> {

    List<BusinessHour> findAllByCompanyProfileIdOrderByDayOfWeekAsc(Long companyProfileId);

    void deleteAllByCompanyProfileId(Long companyProfileId);
}
