package com.nfcplatform.profile.repository;

import com.nfcplatform.profile.entity.SocialLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SocialLinkRepository extends JpaRepository<SocialLink, Long> {

    List<SocialLink> findAllByClientIdOrderByDisplayOrderAsc(Long clientId);

    List<SocialLink> findAllByClientIdAndEnabledTrueOrderByDisplayOrderAsc(Long clientId);

    void deleteAllByClientId(Long clientId);
}
