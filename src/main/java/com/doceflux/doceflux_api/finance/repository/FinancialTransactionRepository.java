package com.doceflux.doceflux_api.finance.repository;

import com.doceflux.doceflux_api.finance.domain.FinancialTransaction;
import com.doceflux.doceflux_api.finance.domain.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface FinancialTransactionRepository
        extends JpaRepository<FinancialTransaction, UUID>, JpaSpecificationExecutor<FinancialTransaction> {

    long countByTransactionDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("""
            select sum(t.amount) from FinancialTransaction t
            where t.type = :type and t.transactionDate between :startDate and :endDate
            """)
    BigDecimal sumAmountByTypeAndPeriod(@Param("type") TransactionType type,
                                        @Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate);
}
