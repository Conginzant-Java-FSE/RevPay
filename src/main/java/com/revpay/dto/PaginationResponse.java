package com.revpay.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PaginationResponse {

    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}