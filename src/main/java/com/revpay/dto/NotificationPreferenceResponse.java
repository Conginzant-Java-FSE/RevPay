package com.revpay.dto;

import com.revpay.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationPreferenceResponse {

    private NotificationType notificationType;
    private Boolean isEnabled;
}