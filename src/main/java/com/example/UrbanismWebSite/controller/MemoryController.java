package com.example.UrbanismWebSite.controller;


import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class MemoryController {
    @GetMapping("/check-memory")
    public void checkMemory() {
        Runtime runtime = Runtime.getRuntime();

        long total = runtime.totalMemory();
        long free = runtime.freeMemory();
        long used = total - free;

        log.info("Total : " + total);
        log.info("Used : " + used);
    }
}