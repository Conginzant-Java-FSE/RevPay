package com.revpay.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class PaginatedResponse<T> {

    private boolean success;
    private List<T> data;
    private PaginationResponse pagination;
}
