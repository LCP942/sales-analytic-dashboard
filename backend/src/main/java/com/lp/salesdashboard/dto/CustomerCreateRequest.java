package com.lp.salesdashboard.dto;

/**
 * Request body for POST /api/customers.
 * All fields are required; validation is enforced by the service layer.
 */
public record CustomerCreateRequest(String name, String email, String city) {}
