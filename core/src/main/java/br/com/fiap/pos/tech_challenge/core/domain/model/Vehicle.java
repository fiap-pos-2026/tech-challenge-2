package br.com.fiap.pos.tech_challenge.core.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * @author johncgo
 * @since 2026-06-24
 */
@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public class Vehicle {

    private Long id;

    private UUID uuid;

    private String licensePlate;

    private String make;

    private String model;

    private int year;

    private Customer customer;

    private LocalDateTime createdAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vehicle other)) return false;
        return uuid != null && java.util.Objects.equals(uuid, other.uuid);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
