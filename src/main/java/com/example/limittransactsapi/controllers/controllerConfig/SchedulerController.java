package com.example.limittransactsapi.controllers.controllerConfig;

import com.example.limittransactsapi.config.Config; // Импортируйте класс конфигурации
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/scheduler")
public class SchedulerController {

    private final Config config;

    @Autowired
    public SchedulerController(Config config) {
        this.config = config;
    }

    @PostMapping("/enable")
    public String enableScheduler() {
        config.setSchedulerEnabled(true);
        return "Scheduler enabled";
    }

    @PostMapping("/disable")
    public String disableScheduler() {
        config.setSchedulerEnabled(false);
        return "Scheduler disabled";
    }

    @GetMapping("/status")
    public String getSchedulerStatus() {
        return config.isSchedulerEnabled() ? "Scheduler is enabled" : "Scheduler is disabled";
    }
}
