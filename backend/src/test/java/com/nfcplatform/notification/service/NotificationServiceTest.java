package com.nfcplatform.notification.service;

import com.nfcplatform.common.exception.ResourceNotFoundException;
import com.nfcplatform.notification.dto.NotificationResponse;
import com.nfcplatform.notification.entity.Notification;
import com.nfcplatform.notification.repository.NotificationRepository;
import com.nfcplatform.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for notification fan-out and the read/unread lifecycle. Pure Mockito, no Spring
 * context/DB.
 */
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        notificationService = new NotificationService(notificationRepository);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void notifyUsersCreatesOneNotificationPerRecipient() {
        User first = user(1L);
        User second = user(2L);

        notificationService.notifyUsers(List.of(first, second), "NEW_LEAD", "New lead", "Acme Ltd", "/admin/leads");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(Notification::getUserId).containsExactlyInAnyOrder(1L, 2L);
        assertThat(captor.getAllValues()).allMatch(n -> n.getType().equals("NEW_LEAD") && n.getLink().equals("/admin/leads"));
    }

    @Test
    void markReadThrowsForANotificationThatDoesNotBelongToThisUser() {
        when(notificationRepository.findByUuidAndUserId("notif-uuid", 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markRead("notif-uuid", 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void markReadSetsTheTimestampOnAnUnreadNotification() {
        Notification unread = new Notification();
        when(notificationRepository.findByUuidAndUserId("notif-uuid", 1L)).thenReturn(Optional.of(unread));

        NotificationResponse response = notificationService.markRead("notif-uuid", 1L);

        assertThat(unread.getReadAt()).isNotNull();
        assertThat(response.read()).isTrue();
    }

    @Test
    void markReadIsIdempotentAndDoesNotResaveAnAlreadyReadNotification() {
        Notification alreadyRead = new Notification();
        Instant originalReadAt = Instant.now().minusSeconds(3600);
        alreadyRead.setReadAt(originalReadAt);
        when(notificationRepository.findByUuidAndUserId("notif-uuid", 1L)).thenReturn(Optional.of(alreadyRead));

        notificationService.markRead("notif-uuid", 1L);

        assertThat(alreadyRead.getReadAt()).isEqualTo(originalReadAt);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void unreadCountDelegatesToTheRepository() {
        when(notificationRepository.countByUserIdAndReadAtIsNull(1L)).thenReturn(3L);

        assertThat(notificationService.unreadCount(1L)).isEqualTo(3L);
    }

    @Test
    void markAllReadDelegatesToTheRepositorysBulkUpdate() {
        notificationService.markAllRead(1L);

        verify(notificationRepository).markAllReadForUser(org.mockito.ArgumentMatchers.eq(1L), any(Instant.class));
    }

    private User user(long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("user" + id + "@test.local");
        user.setPasswordHash("hash");
        return user;
    }
}
