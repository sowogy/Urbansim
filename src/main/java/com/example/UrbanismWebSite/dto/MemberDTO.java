package com.example.UrbanismWebSite.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MemberDTO {
    private Long id;
    private String name;
    private String email;
    private String phone;
}
