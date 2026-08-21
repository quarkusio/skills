package com.coffeeshop.web.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebController.class);

    @Value("${streamUrl}")
    private String streamUrl;

    @Value("${storeId}")
    private String storeId;

    @GetMapping("/")
    public String getIndex(Model model) {
        LOGGER.debug("Rendering coffeeshop page with streamUrl={} storeId={}", streamUrl, storeId);
        model.addAttribute("streamUrl", streamUrl);
        model.addAttribute("storeId", storeId);
        // Return the Thymeleaf view name: src/main/resources/templates/coffeeshop.html
        return "coffeeshop";
    }
}
