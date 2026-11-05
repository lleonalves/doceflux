package com.doceflux.doceflux_api.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "stores")
public class Store {

    @Id
    private UUID id;

    @Column(name = "store_name", nullable = false, length = 150)
    private String storeName;

    @Column(name = "main_manager", nullable = false, length = 120)
    private String mainManager;

    @Column(name = "responsible_manager", nullable = false, length = 120)
    private String responsibleManager;

    @Column(name = "company_start_date", nullable = false)
    private LocalDate companyStartDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Store() {
    }

    public Store(String storeName, String mainManager, String responsibleManager, LocalDate companyStartDate) {
        this.id = UUID.randomUUID();
        this.storeName = storeName;
        this.mainManager = mainManager;
        this.responsibleManager = responsibleManager;
        this.companyStartDate = companyStartDate;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getStoreName() {
        return storeName;
    }

    public String getMainManager() {
        return mainManager;
    }

    public String getResponsibleManager() {
        return responsibleManager;
    }

    public LocalDate getCompanyStartDate() {
        return companyStartDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
