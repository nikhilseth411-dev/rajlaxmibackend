package com.rajlaxmi.jewellers.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** PUT /user/profile — update name and phone */
@Data
public class UpdateProfileRequest {
    @Size(min = 2, max = 50) private String firstName;
    @Size(min = 2, max = 50) private String lastName;

    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid Indian mobile number")
    private String phone;
}
