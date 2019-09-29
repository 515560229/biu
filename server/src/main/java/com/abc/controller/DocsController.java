package com.abc.controller;

import com.abc.vo.Json;
import org.apache.commons.io.IOUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;

@RestController
@RequestMapping("/docs")
public class DocsController {

    @GetMapping(value = "/get", produces="text/json;charset=utf-8")
    public Json get(@RequestParam(name = "name") String name) {
        InputStream stream = getClass().getResourceAsStream("/docs/" + name);
        try {
            return Json.succ("get docs").data("message", IOUtils.toString(stream, "UTF-8"));
        } catch (IOException e) {
            return Json.fail("get docs");
        }
    }
}
