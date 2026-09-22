package com.librarymanagement.notification;

import com.librarymanagement.auth.UserAccount;
import com.librarymanagement.config.AppProperties;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notifications;
    private final JavaMailSender mailSender;
    private final AppProperties properties;

    public NotificationService(NotificationRepository notifications, JavaMailSender mailSender, AppProperties properties) {
        this.notifications = notifications;
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Transactional
    public Notification notify(UserAccount user, String title, String message, NotificationType type, String dedupeKey) {
        if (user == null) return null;
        if (dedupeKey != null && notifications.existsByDedupeKey(dedupeKey)) return null;

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setRead(false);
        notification.setDedupeKey(dedupeKey);
        notifications.save(notification);

        if (properties.isMailEnabled()) {
            try {
                SimpleMailMessage email = new SimpleMailMessage();
                email.setTo(user.getEmail());
                email.setSubject(properties.getLibraryName() + " — " + title);
                email.setText(message + "\n\nOpen your account: " + properties.getBaseUrl() + "/my/account");
                mailSender.send(email);
            } catch (RuntimeException ex) {
                log.warn("Email delivery failed for notification {}: {}", notification.getId(), ex.getMessage());
            }
        }
        return notification;
    }

    @Transactional(readOnly = true)
    public List<Notification> latest(Long userId) {
        return notifications.findTop20ByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notifications.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markRead(Long notificationId, Long userId) {
        notifications.findByIdAndUserId(notificationId, userId).ifPresent(n -> n.setRead(true));
    }
}
