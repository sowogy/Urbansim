package com.example.UrbanismWebSite.controller;

import lombok.Builder;
import lombok.Data;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


/** AWS Health Check를 위한 클래스 */
@Profile("health")
@RestController
public class HealthController {
    @GetMapping("/health")
    public Status getHealth(){
        return Status.builder().status("UP").build();
    }

    @Data
    @Builder
    private static class Status{
        public String status;
    }
}
