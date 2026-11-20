package com.doceflux.doceflux_api.finance.controller;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import com.doceflux.doceflux_api.finance.domain.FinancialTransaction;
import com.doceflux.doceflux_api.finance.domain.TransactionType;
import com.doceflux.doceflux_api.finance.dto.CashFlowResponse;
import com.doceflux.doceflux_api.finance.service.FinanceService;
import com.doceflux.doceflux_api.shared.BusinessRuleException;
import com.doceflux.doceflux_api.shared.ResourceNotFoundException;
import com.doceflux.doceflux_api.auth.security.SecurityConfig;
import com.doceflux.doceflux_api.auth.security.TokenService;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FinanceController.class)
@Import({SecurityConfig.class, TokenService.class})
class FinanceControllerTest {

    private static final String TODAY = LocalDate.now().toString();

    private static final UUID STORE = UUID.randomUUID();
    private static final UUID OTHER_STORE = UUID.randomUUID();

    private static RequestPostProcessor auth() {
        return auth(STORE);
    }

    private static RequestPostProcessor auth(UUID storeId) {
        return jwt().jwt(j -> j.claim("storeId", storeId.toString()));
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FinanceService financeService;

    private ResultActions post(String url, String json) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url).with(auth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private void assertIncomeRejected(String json, String field, String message) throws Exception {
        post("/api/finance/incomes", json)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors." + field).value(message));
        verify(financeService, never()).registerIncome(any(), any());
    }

    private void assertExpenseRejected(String json, String field, String message) throws Exception {
        post("/api/finance/expenses", json)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors." + field).value(message));
        verify(financeService, never()).registerExpense(any(), any());
    }

    @Test
    void registersIncome() throws Exception {
        UUID orderId = UUID.randomUUID();
        FinancialTransaction income = FinancialTransaction.income(STORE, 
                "Sinal", new BigDecimal("30.00"), LocalDate.now(), "Encomendas", orderId);
        when(financeService.registerIncome(eq(STORE), any())).thenReturn(income);

        post("/api/finance/incomes", """
                {"description":"Sinal","amount":30.00,"date":"%s","category":"Encomendas","orderId":"%s"}
                """.formatted(TODAY, orderId))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/finance/transactions/" + income.getId()))
                .andExpect(jsonPath("$.type").value("INCOME"))
                .andExpect(jsonPath("$.amount").value(30.00))
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.category").value("Encomendas"));
    }

    @Test
    void rejectsIncomeWithoutRequiredFields() throws Exception {
        assertIncomeRejected("""
                {"amount":10,"date":"%s"}
                """.formatted(TODAY), "description", "A descrição é obrigatória");
        assertIncomeRejected("""
                {"description":"x","date":"%s"}
                """.formatted(TODAY), "amount", "O valor é obrigatório");
        assertIncomeRejected("""
                {"description":"x","amount":10}
                """, "date", "A data de recebimento é obrigatória");
    }

    @Test
    void rejectsIncomeWithInvalidAmount() throws Exception {
        assertIncomeRejected("""
                {"description":"x","amount":0,"date":"%s"}
                """.formatted(TODAY), "amount", "O valor deve ser maior que zero");
        assertIncomeRejected("""
                {"description":"x","amount":-1,"date":"%s"}
                """.formatted(TODAY), "amount", "O valor deve ser maior que zero");
        assertIncomeRejected("""
                {"description":"x","amount":1.234,"date":"%s"}
                """.formatted(TODAY), "amount",
                "O valor deve ter no máximo 10 dígitos inteiros e 2 casas decimais");
    }

    @Test
    void acceptsIncomeAndExpenseWithFutureDate() throws Exception {
        String future = LocalDate.now().plusMonths(2).toString();
        when(financeService.registerIncome(eq(STORE), any())).thenReturn(
                FinancialTransaction.income(STORE, "x", BigDecimal.TEN, LocalDate.now().plusMonths(2), null, null));
        when(financeService.registerExpense(eq(STORE), any())).thenReturn(
                FinancialTransaction.expense(STORE, "x", BigDecimal.TEN, LocalDate.now().plusMonths(2), null));

        post("/api/finance/incomes", "{\"description\":\"x\",\"amount\":10,\"date\":\"" + future + "\"}")
                .andExpect(status().isCreated());
        post("/api/finance/expenses", "{\"description\":\"x\",\"amount\":10,\"date\":\"" + future + "\"}")
                .andExpect(status().isCreated());
    }

    @Test
    void acceptsUpdateWithFutureDate() throws Exception {
        UUID id = UUID.randomUUID();
        String future = LocalDate.now().plusDays(10).toString();
        when(financeService.update(eq(id), eq(STORE), any())).thenReturn(
                FinancialTransaction.expense(STORE, "x", BigDecimal.TEN, LocalDate.now().plusDays(10), null));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/finance/transactions/{id}", id).with(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"x\",\"amount\":10,\"date\":\"" + future + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void returnsNotFoundWhenIncomeOrderDoesNotExist() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(financeService.registerIncome(eq(STORE), any()))
                .thenThrow(new ResourceNotFoundException("Pedido não encontrado: " + orderId));

        post("/api/finance/incomes", """
                {"description":"x","amount":10,"date":"%s","orderId":"%s"}
                """.formatted(TODAY, orderId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Pedido não encontrado: " + orderId));
    }

    @Test
    void registersExpense() throws Exception {
        FinancialTransaction expense = FinancialTransaction.expense(STORE, 
                "Farinha", new BigDecimal("120.00"), LocalDate.now(), "Insumos");
        when(financeService.registerExpense(eq(STORE), any())).thenReturn(expense);

        post("/api/finance/expenses", """
                {"description":"Farinha","amount":120.00,"date":"%s","category":"Insumos"}
                """.formatted(TODAY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("EXPENSE"))
                .andExpect(jsonPath("$.amount").value(120.00))
                .andExpect(jsonPath("$.orderId").doesNotExist());
    }

    @Test
    void rejectsExpenseWithoutRequiredFields() throws Exception {
        assertExpenseRejected("""
                {"amount":10,"date":"%s"}
                """.formatted(TODAY), "description", "A descrição é obrigatória");
        assertExpenseRejected("""
                {"description":"x","date":"%s"}
                """.formatted(TODAY), "amount", "O valor é obrigatório");
        assertExpenseRejected("""
                {"description":"x","amount":10}
                """, "date", "A data da despesa é obrigatória");
    }

    @Test
    void rejectsExpenseWithInvalidAmountOrFutureDate() throws Exception {
        assertExpenseRejected("""
                {"description":"x","amount":0,"date":"%s"}
                """.formatted(TODAY), "amount", "O valor deve ser maior que zero");
        assertExpenseRejected("""
                {"description":"x","amount":-5,"date":"%s"}
                """.formatted(TODAY), "amount", "O valor deve ser maior que zero");
    }

    @Test
    void listsTransactions() throws Exception {
        when(financeService.findAll(STORE, null, null, null, null)).thenReturn(List.of(
                FinancialTransaction.expense(STORE, "Farinha", new BigDecimal("10.00"), LocalDate.now(), null),
                FinancialTransaction.income(STORE, "Venda", new BigDecimal("40.00"), LocalDate.now(), null, null)));

        mockMvc.perform(get("/api/finance/transactions").with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value("EXPENSE"))
                .andExpect(jsonPath("$[1].amount").value(40.00));
    }

    @Test
    void listsTransactionsWithCombinedFilters() throws Exception {
        UUID orderId = UUID.randomUUID();
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 10, 31);
        when(financeService.findAll(STORE, TransactionType.INCOME, orderId, start, end)).thenReturn(List.of(
                FinancialTransaction.income(STORE, "Sinal", new BigDecimal("30.00"), start, null, orderId)));

        mockMvc.perform(get("/api/finance/transactions").with(auth())
                        .param("type", "INCOME")
                        .param("orderId", orderId.toString())
                        .param("startDate", "2026-10-01")
                        .param("endDate", "2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].orderId").value(orderId.toString()));
    }

    @Test
    void returnsEmptyListWhenFiltersMatchNothing() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(financeService.findAll(STORE, TransactionType.EXPENSE, orderId, null, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/finance/transactions").with(auth())
                        .param("type", "EXPENSE").param("orderId", orderId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void rejectsListingWithStartAfterEnd() throws Exception {
        when(financeService.findAll(any(), any(), any(), any(), any()))
                .thenThrow(new BusinessRuleException("A data inicial não pode ser posterior à data final"));

        mockMvc.perform(get("/api/finance/transactions").with(auth())
                        .param("startDate", "2026-11-01").param("endDate", "2026-10-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("A data inicial não pode ser posterior à data final"));
    }

    @Test
    void rejectsListingWithInvalidFilterFormats() throws Exception {
        mockMvc.perform(get("/api/finance/transactions").with(auth()).param("type", "OUTRO"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/finance/transactions").with(auth()).param("orderId", "nao-e-uuid"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/finance/transactions").with(auth()).param("startDate", "01/10/2026"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/finance/transactions").with(auth()).param("endDate", "amanha"))
                .andExpect(status().isBadRequest());

        verify(financeService, never()).findAll(any(), any(), any(), any(), any());
    }

    @Test
    void deletesTransaction() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/finance/transactions/{id}", id).with(auth()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(financeService).delete(id, STORE);
    }

    @Test
    void deleteReturnsNotFoundForUnknownTransaction() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("Movimentação não encontrada: " + id))
                .when(financeService).delete(id, STORE);

        mockMvc.perform(delete("/api/finance/transactions/{id}", id).with(auth()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Movimentação não encontrada: " + id));
    }

    @Test
    void deleteRejectsInvalidUuid() throws Exception {
        mockMvc.perform(delete("/api/finance/transactions/{id}", "nao-e-uuid").with(auth()))
                .andExpect(status().isBadRequest());

        verify(financeService, never()).delete(any(), any());
    }

    @Test
    void returnsExistingTransaction() throws Exception {
        FinancialTransaction transaction = FinancialTransaction.expense(STORE, 
                "Farinha", new BigDecimal("10.00"), LocalDate.now(), null);
        when(financeService.findById(transaction.getId(), STORE)).thenReturn(transaction);

        mockMvc.perform(get("/api/finance/transactions/{id}", transaction.getId()).with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transaction.getId().toString()));
    }

    @Test
    void returnsNotFoundForUnknownTransaction() throws Exception {
        UUID id = UUID.randomUUID();
        when(financeService.findById(id, STORE))
                .thenThrow(new ResourceNotFoundException("Movimentação não encontrada: " + id));

        mockMvc.perform(get("/api/finance/transactions/{id}", id).with(auth()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Movimentação não encontrada: " + id));
    }

    @Test
    void returnsCashFlow() throws Exception {
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 10, 31);
        when(financeService.cashFlow(STORE, start, end)).thenReturn(new CashFlowResponse(
                start, end, new BigDecimal("300.00"), new BigDecimal("100.50"), new BigDecimal("199.50")));

        mockMvc.perform(get("/api/finance/cash-flow").with(auth()).param("startDate", "2026-10-01").param("endDate", "2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startDate").value("2026-10-01"))
                .andExpect(jsonPath("$.endDate").value("2026-10-31"))
                .andExpect(jsonPath("$.totalIncome").value(300.00))
                .andExpect(jsonPath("$.totalExpense").value(100.50))
                .andExpect(jsonPath("$.balance").value(199.50));
    }

    @Test
    void rejectsInvalidCashFlowPeriod() throws Exception {
        when(financeService.cashFlow(any(), any(), any()))
                .thenThrow(new BusinessRuleException("A data inicial não pode ser posterior à data final"));

        mockMvc.perform(get("/api/finance/cash-flow").with(auth()).param("startDate", "2026-11-01").param("endDate", "2026-10-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("A data inicial não pode ser posterior à data final"));
    }

    @Test
    void rejectsCashFlowWithoutPeriod() throws Exception {
        mockMvc.perform(get("/api/finance/cash-flow").with(auth()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transactionOfAnotherStoreReturnsNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(financeService.findById(id, OTHER_STORE))
                .thenThrow(new ResourceNotFoundException("Movimentação não encontrada: " + id));

        mockMvc.perform(get("/api/finance/transactions/{id}", id).with(auth(OTHER_STORE)))
                .andExpect(status().isNotFound());
        verify(financeService).findById(id, OTHER_STORE);
    }

    @Test
    void updatesTransaction() throws Exception {
        UUID id = UUID.randomUUID();
        FinancialTransaction updated = FinancialTransaction.expense(STORE, "Farinha", new BigDecimal("12.00"),
                LocalDate.now(), "Insumos");
        when(financeService.update(eq(id), eq(STORE), any())).thenReturn(updated);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/finance/transactions/{id}", id).with(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Farinha\",\"amount\":12.00,\"date\":\"" + TODAY
                                + "\",\"category\":\"Insumos\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Farinha"));
    }

    @Test
    void updateRejectsInvalidBody() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/finance/transactions/{id}", UUID.randomUUID()).with(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"\",\"amount\":0,\"date\":\"" + TODAY + "\"}"))
                .andExpect(status().isBadRequest());

        verify(financeService, never()).update(any(), any(), any());
    }

    @Test
    void deletesPeriodOfAuthenticatedStore() throws Exception {
        mockMvc.perform(delete("/api/finance/transactions")
                        .param("startDate", "2026-10-01").param("endDate", "2026-10-31").with(auth()))
                .andExpect(status().isNoContent());

        verify(financeService).deleteByPeriod(STORE, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
    }

    @Test
    void periodDeletionRequiresBothDatesAndValidRange() throws Exception {
        mockMvc.perform(delete("/api/finance/transactions").param("startDate", "2026-10-01").with(auth()))
                .andExpect(status().isBadRequest());

        doThrow(new BusinessRuleException("A data inicial não pode ser posterior à data final"))
                .when(financeService).deleteByPeriod(any(), any(), any());
        mockMvc.perform(delete("/api/finance/transactions")
                        .param("startDate", "2026-11-01").param("endDate", "2026-10-01").with(auth()))
                .andExpect(status().isBadRequest());
    }
}
