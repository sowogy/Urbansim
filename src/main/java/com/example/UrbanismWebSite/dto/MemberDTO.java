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
    public String getFormattedPhone(){
        if(phone == null && phone.length() != 11){
            return phone;
        }
        return phone.replaceAll("(\\d{3})(\\d{4})(\\d{4})", "$1-$2-$3");
    }
}
