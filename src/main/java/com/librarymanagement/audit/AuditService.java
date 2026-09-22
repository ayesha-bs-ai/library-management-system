package com.librarymanagement.audit;

import com.librarymanagement.auth.CurrentUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {
    private final AuditEventRepository events;
    private final CurrentUserService currentUsers;

    public AuditService(AuditEventRepository events, CurrentUserService currentUsers) {
        this.events = events;
        this.currentUsers = currentUsers;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String action, String entityType, String entityId, String details) {
        AuditEvent event = new AuditEvent();
        event.setActorEmail(currentUsers.currentEmailOrSystem());
        event.setAction(action);
        event.setEntityType(entityType);
        event.setEntityId(entityId);
        event.setDetails(details == null ? "" : details);
        events.save(event);
    }

    @Transactional(readOnly = true)
    public Page<AuditEvent> recent(int page) {
        return events.findAllByOrderByCreatedAtDesc(PageRequest.of(Math.max(page, 0), 30));
    }
}
