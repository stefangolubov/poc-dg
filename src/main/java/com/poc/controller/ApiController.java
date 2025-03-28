package com.poc.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    // Original endpoint
    @GetMapping("/hello")
    public String sayHello(@RequestParam(required = false) String name) {
        return "Hello, " + (name != null ? name : "World") + "!";
    }

    @GetMapping("/health")
    public String healthCheck() {
        return "OK";
    }

    /************** Vulnerable endpoints **************/

    // SQL Injection Vulnerability
    @GetMapping("/users")
    public String getUser(@RequestParam String id) {
        String query = "SELECT * FROM users WHERE id = " + id; // SQLi vulnerability
        return "Simulated query: " + query;
    }

    // Information Exposure (Actuator Endpoint)
    @GetMapping("/admin/env")
    public Map<String, String> getEnvironment() {
        // Exposes all environment variables
        return System.getenv();
    }

    // XSS (Cross-Site Scripting) Vulnerability
    @GetMapping("/greet")
    public String greetUser(@RequestParam String username) {
        // No XSS protection
        return "<h1>Welcome, " + username + "!</h1>";
    }

    /************ Vulnerable endpoints [end] ************/
}