package com.nfcplatform.notification.service;

import com.nfcplatform.common.dto.PageResponse;
import com.nfcplatform.common.exception.ResourceNotFoundException;
import com.nfcplatform.notification.dto.NotificationResponse;
import com.nfcplatform.notification.entity.Notification;
import com.nfcplatform.notification.repository.NotificationRepository;
import com.nfcplatform.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void notify(Long userId, String type, String title, String body, String link) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setLink(link);
        notificationRepository.save(notification);
    }

    @Transactional
    public void notifyUsers(Collection<User> recipients, String type, String title, String body, String link) {
        recipients.forEach(user -> notify(user.getId(), type, title, body, link));
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> listForUser(Long userId, Pageable pageable) {
        Page<Notification> page = notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId, pageable);
        return PageResponse.of(page, NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public NotificationResponse markRead(String uuid, Long userId) {
        Notification notification = notificationRepository.findByUuidAndUserId(uuid, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification was not found"));
        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
            notification = notificationRepository.save(notification);
        }
        return NotificationResponse.from(notification);
    }

    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllReadForUser(userId, Instant.now());
    }
}
