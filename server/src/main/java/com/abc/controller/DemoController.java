package com.abc.controller;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/demo")
public class DemoController {

    @PostMapping
    public Map<String, String> post(@RequestBody Map<String, String> map) {
        try {
            Thread.sleep(100 + new Random().nextInt(1000));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        map.put("success", "true");
        return map;
    }

    @GetMapping
    public Map<String, String> get() {
        try {
            Thread.sleep(100 + new Random().nextInt(1000));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Map<String, String> map = new HashMap<>();
        map.put("success", "true");
        return map;
    }

}
