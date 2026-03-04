package com.revpay.dto;

import com.revpay.enums.NotificationType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class UpdateNotificationPreferenceRequest {



    @NotEmpty(message = "Preferences list cannot be empty")
    @Valid
    private List<PreferenceItem> preferences;

    @Data
    public static class PreferenceItem {

        @NotNull(message = "Notification type is required")
        private NotificationType notificationType;

        @NotNull(message = "isEnabled flag is required")
        private Boolean isEnabled;
    }
}
