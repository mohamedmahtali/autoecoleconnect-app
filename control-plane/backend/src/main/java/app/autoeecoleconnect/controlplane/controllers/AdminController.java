package app.autoeecoleconnect.controlplane.controllers;

import app.autoeecoleconnect.controlplane.controllers.dto.OrganisationsAdminResponse;
import app.autoeecoleconnect.controlplane.services.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Super-admin", description = "Vue plateforme — toutes les organisations (SecurityConfig : hasRole(SUPERADMIN))")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @Operation(summary = "Lister toutes les organisations de la plateforme, avec leurs tenants")
    @GetMapping("/organisations")
    public OrganisationsAdminResponse organisations() {
        return adminService.toutesLesOrganisations();
    }
}
