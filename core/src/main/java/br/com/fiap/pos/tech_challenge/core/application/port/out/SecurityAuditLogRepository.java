package br.com.fiap.pos.tech_challenge.core.application.port.out;

import br.com.fiap.pos.tech_challenge.core.domain.model.SecurityAuditLog;

public interface SecurityAuditLogRepository {
    SecurityAuditLog save(SecurityAuditLog log);
}
