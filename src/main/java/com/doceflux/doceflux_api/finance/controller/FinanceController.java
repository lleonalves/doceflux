package com.doceflux.doceflux_api.finance.controller;

import com.doceflux.doceflux_api.finance.dto.CashFlowResponse;
import com.doceflux.doceflux_api.finance.dto.ExpenseRequest;
import com.doceflux.doceflux_api.finance.dto.IncomeRequest;
import com.doceflux.doceflux_api.finance.dto.TransactionResponse;
import com.doceflux.doceflux_api.finance.service.FinanceService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/finance")
public class FinanceController {

    private final FinanceService financeService;

    public FinanceController(FinanceService financeService) {
        this.financeService = financeService;
    }

    @PostMapping("/incomes")
    public ResponseEntity<TransactionResponse> registerIncome(@Valid @RequestBody IncomeRequest request) {
        return created(TransactionResponse.from(financeService.registerIncome(request)));
    }

    @PostMapping("/expenses")
    public ResponseEntity<TransactionResponse> registerExpense(@Valid @RequestBody ExpenseRequest request) {
        return created(TransactionResponse.from(financeService.registerExpense(request)));
    }

    @GetMapping("/transactions")
    public List<TransactionResponse> findAll() {
        return financeService.findAll().stream().map(TransactionResponse::from).toList();
    }

    @GetMapping("/transactions/{id}")
    public TransactionResponse findById(@PathVariable UUID id) {
        return TransactionResponse.from(financeService.findById(id));
    }

    @GetMapping("/cash-flow")
    public CashFlowResponse cashFlow(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return financeService.cashFlow(startDate, endDate);
    }

    private ResponseEntity<TransactionResponse> created(TransactionResponse response) {
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/finance/transactions/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.status(HttpStatus.CREATED).location(location).body(response);
    }
}
