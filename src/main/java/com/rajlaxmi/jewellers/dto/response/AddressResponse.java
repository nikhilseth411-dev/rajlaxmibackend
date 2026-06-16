package com.rajlaxmi.jewellers.dto.response;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AddressResponse {
    private Long id;
    private String fullName;
    private String phone;
    private String streetAddress;
    private String landmark;
    private String city;
    private String district;
    private String state;
    private String pincode;
    private String addressType;
    private boolean isDefault;
}
