package kz.ibrazaim.catalog.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@org.springframework.stereotype.Controller
@RequestMapping("/main")
public class Controller {
    @GetMapping
    public String getPage(){
        return "LUXDRM";
    }

}
