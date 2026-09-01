package br.com.fiap.pos.tech_challenge.core.application.serviceorder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

final class ServiceOrderPolicy {

    static final long APPROVAL_WINDOW_HOURS = 168L;

    private ServiceOrderPolicy() {
    }

    static Instant approvalDeadlineFromNow() {
        return Instant.now().plus(APPROVAL_WINDOW_HOURS, ChronoUnit.HOURS);
    }
}
