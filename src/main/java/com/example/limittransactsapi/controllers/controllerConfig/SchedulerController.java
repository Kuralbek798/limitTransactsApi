package com.example.limittransactsapi.controllers.controllerConfig;

import com.example.limittransactsapi.services.ShedullerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/scheduler")
public class SchedulerController {

    private final ShedullerService shedullerService;

    @Autowired
    public SchedulerController( ShedullerService shedullerService) {
        this.shedullerService = shedullerService;
    }

    @PostMapping("/enable")
    public String enableScheduler() {
        shedullerService.setSchedulerEnabled(true);
        return "Scheduler enabled";
    }

    @PostMapping("/disable")
    public String disableScheduler() {
        shedullerService.setSchedulerEnabled(false);
        return "Scheduler disabled";
    }

    @GetMapping("/status")
    public String getSchedulerStatus() {
        return shedullerService.isSchedulerEnabled() ? "Scheduler is enabled" : "Scheduler is disabled";
    }
}
