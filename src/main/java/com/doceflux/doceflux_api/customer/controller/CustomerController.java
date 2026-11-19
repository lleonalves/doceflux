package com.doceflux.doceflux_api.customer.controller;

import com.doceflux.doceflux_api.customer.dto.CustomerRequest;
import com.doceflux.doceflux_api.customer.dto.CustomerResponse;
import com.doceflux.doceflux_api.customer.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                   @Valid @RequestBody CustomerRequest request) {
        CustomerResponse response = CustomerResponse.from(customerService.create(storeId(jwt), request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.status(HttpStatus.CREATED).location(location).body(response);
    }

    @GetMapping
    public List<CustomerResponse> findAll(@AuthenticationPrincipal Jwt jwt,
                                          @RequestParam(required = false) String search) {
        return customerService.search(storeId(jwt), search).stream().map(CustomerResponse::from).toList();
    }

    @PutMapping("/{id}")
    public CustomerResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                   @Valid @RequestBody CustomerRequest request) {
        return CustomerResponse.from(customerService.update(id, storeId(jwt), request));
    }

    @GetMapping("/{id}")
    public CustomerResponse findById(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return CustomerResponse.from(customerService.findById(id, storeId(jwt)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        customerService.delete(id, storeId(jwt));
    }

    private static UUID storeId(Jwt jwt) {
        return UUID.fromString(jwt.getClaimAsString("storeId"));
    }
}
