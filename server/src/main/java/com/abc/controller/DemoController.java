package com.abc.controller;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/demo")
public class DemoController {

    @PostMapping
    public Map<String, String> post(@RequestBody Map<String, String> map) {
        map.put("success", "true");
        return map;
    }

    @GetMapping
    public Map<String, String> get(@RequestBody Map<String, String> map) {
        map.put("success", "true");
        return map;
    }

}
