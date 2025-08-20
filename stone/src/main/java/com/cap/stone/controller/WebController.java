package com.cap.stone.controller;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

//Serves the react static files
@Controller
public class WebController {
    
    @GetMapping("/")
    public String index() {
        return "index.html";
    }
    
    @GetMapping("/upload")
    public String upload() {
        return "index.html";
    }
    
    @GetMapping("/sessions")
    public String sessions() {
        return "index.html";
    }
}