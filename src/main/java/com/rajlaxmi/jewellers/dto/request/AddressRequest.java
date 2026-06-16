package com.rajlaxmi.jewellers.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddressRequest {
    @NotBlank private String fullName;

    @NotBlank
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid Indian mobile number")
    private String phone;

    @NotBlank @Size(max = 200) private String streetAddress;
    @Size(max = 100) private String landmark;
    @NotBlank @Size(max = 100) private String city;
    @NotBlank @Size(max = 100) private String district;
    @NotBlank @Size(max = 50) private String state;

    @NotBlank
    @Pattern(regexp = "^\\d{6}$", message = "Invalid 6-digit pincode")
    private String pincode;

    private String addressType = "HOME";
    private boolean isDefault = false;
}
