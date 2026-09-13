package com.app.dto;

import com.app.enums.Governorate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Governorate shipping info exposed by the API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GovernorateResponseDto {

    private int code;
    private String name;
    private String arabicName;
    private double shippingPrice;
    private int deliveryDays;

    public static GovernorateResponseDto from(Governorate governorate) {
        return new GovernorateResponseDto(
                governorate.getCode(),
                governorate.name(),
                governorate.getArabicName(),
                governorate.getShippingPrice(),
                governorate.getDeliveryDays()
        );
    }
}
