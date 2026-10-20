package com.enis.sectio.user.bootstrap;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private final AdminBootstrapService adminBootstrapService;

    public AdminBootstrapRunner(
            AdminBootstrapService adminBootstrapService) {
        this.adminBootstrapService = adminBootstrapService;
    }

    @Override
    public void run(ApplicationArguments args) {
        adminBootstrapService.bootstrapAdmin();
    }
}