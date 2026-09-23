package com.nfcplatform.analytics.repository;

import com.nfcplatform.analytics.entity.AnalyticsEvent;
import com.nfcplatform.analytics.entity.AnalyticsEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent, Long> {

    long countByClientIdAndEventTypeAndCreatedAtAfter(Long clientId, AnalyticsEventType eventType, Instant since);

    @Query(value = "SELECT DATE(created_at) AS day, event_type AS eventType, COUNT(*) AS total " +
            "FROM analytics_events WHERE client_id = :clientId AND created_at >= :since " +
            "GROUP BY DATE(created_at), event_type ORDER BY day ASC", nativeQuery = true)
    List<DailyCount> findDailyCounts(@Param("clientId") Long clientId, @Param("since") Instant since);

    @Query(value = "SELECT COALESCE(device_type, 'unknown') AS device, COUNT(*) AS total " +
            "FROM analytics_events WHERE client_id = :clientId AND created_at >= :since " +
            "GROUP BY COALESCE(device_type, 'unknown')", nativeQuery = true)
    List<DeviceCount> findDeviceBreakdown(@Param("clientId") Long clientId, @Param("since") Instant since);

    @Query(value = "SELECT n.serial_number AS label, COUNT(*) AS total FROM analytics_events e " +
            "JOIN nfc_cards n ON n.id = e.nfc_card_id " +
            "WHERE e.client_id = :clientId AND e.created_at >= :since AND e.nfc_card_id IS NOT NULL " +
            "GROUP BY n.serial_number ORDER BY total DESC LIMIT 5", nativeQuery = true)
    List<TopItemCount> findTopCards(@Param("clientId") Long clientId, @Param("since") Instant since);

    interface DailyCount {
        java.sql.Date getDay();
        String getEventType();
        Long getTotal();
    }

    interface DeviceCount {
        String getDevice();
        Long getTotal();
    }

    interface TopItemCount {
        String getLabel();
        Long getTotal();
    }
}
