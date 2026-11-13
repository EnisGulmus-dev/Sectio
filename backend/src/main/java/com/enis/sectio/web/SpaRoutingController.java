package com.enis.sectio.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaRoutingController {

    @GetMapping({
            "/{path:^(?!api$|actuator$|assets$|error$)[^.]*}",
            "/{path:^(?!api$|actuator$|assets$|error$)[^.]*}/**"
    })
    public String forwardClientRoutes() {
        return "forward:/index.html";
    }
}
