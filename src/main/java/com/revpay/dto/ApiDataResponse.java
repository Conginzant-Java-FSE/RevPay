package com.revpay.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiDataResponse<T> {
    private boolean success;
    private String message;
    private T data;
}
