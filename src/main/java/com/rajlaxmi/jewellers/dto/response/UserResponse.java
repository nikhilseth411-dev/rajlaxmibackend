package com.rajlaxmi.jewellers.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rajlaxmi.jewellers.enums.Role;
import lombok.*;
import java.time.LocalDateTime;

/** Safe user representation — NEVER includes password */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private Role role;
    private boolean isEmailVerified;
    private boolean isActive;
    private LocalDateTime createdAt;
}
