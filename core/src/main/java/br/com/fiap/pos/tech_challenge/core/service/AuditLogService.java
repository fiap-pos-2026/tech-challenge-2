package br.com.fiap.pos.tech_challenge.core.service;

import br.com.fiap.pos.tech_challenge.core.domain.SecurityAuditLog;
import br.com.fiap.pos.tech_challenge.core.domain.User;
import br.com.fiap.pos.tech_challenge.core.enums.AuditEventType;
import br.com.fiap.pos.tech_challenge.core.repository.SecurityAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author johncgo
 * @since 2026-06-26
 */
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final SecurityAuditLogRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void register(AuditEventType eventType, User user, String attemptIdentifier, String result, String details) {
        SecurityAuditLog log = new SecurityAuditLog();
        log.setEventType(eventType);
        log.setUser(user);
        log.setAttemptIdentifier(attemptIdentifier);
        log.setResult(result);
        log.setDetails(details);
        repository.save(log);
    }
}
