package com.poc.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.stream.Collectors;

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

    // Command Injection
    @GetMapping("/ping")
    public String pingHost(@RequestParam String host) throws IOException {
        // Vulnerable command construction
        Process p = Runtime.getRuntime().exec("ping -c 1 " + host);
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        return reader.lines().collect(Collectors.joining("\n"));
    }

    //Missing Security Headers
    @GetMapping("/public")
    public String publicInfo() {
        // Intentionally missing security headers
        return "Public information";
    }

    /************ Vulnerable endpoints [end] ************/
}