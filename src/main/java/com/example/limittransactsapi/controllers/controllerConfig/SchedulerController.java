package com.example.limittransactsapi.controllers.controllerConfig;

import com.example.limittransactsapi.config.SchedulerConfig; // Импортируйте класс конфигурации
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/scheduler")
public class SchedulerController {

    private final SchedulerConfig schedulerConfig;

    @Autowired
    public SchedulerController(SchedulerConfig schedulerConfig) {
        this.schedulerConfig = schedulerConfig;
    }

    @PostMapping("/enable")
    public String enableScheduler() {
        schedulerConfig.setSchedulerEnabled(true);
        return "Scheduler enabled";
    }

    @PostMapping("/disable")
    public String disableScheduler() {
        schedulerConfig.setSchedulerEnabled(false);
        return "Scheduler disabled";
    }

    @GetMapping("/status")
    public String getSchedulerStatus() {
        return schedulerConfig.isSchedulerEnabled() ? "Scheduler is enabled" : "Scheduler is disabled";
    }
}
