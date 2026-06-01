package cn.edu.sdu.java.server.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.services.PunishmentService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/punishment")
@Tag(name = "Punishment", description = "Student punishment management APIs")
public class PunishmentController {
    private final PunishmentService punishmentService;

    public PunishmentController(PunishmentService punishmentService) {
        this.punishmentService = punishmentService;
    }

    @Operation(summary = "Get punishment list")
    @PostMapping("/getPunishmentList")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public DataResponse getPunishmentList(@Valid @RequestBody DataRequest dataRequest) {
        return punishmentService.getPunishmentList(dataRequest);
    }

    @Operation(summary = "Save punishment")
    @PostMapping("/punishmentSave")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public DataResponse punishmentSave(@Valid @RequestBody DataRequest dataRequest) {
        return punishmentService.punishmentSave(dataRequest);
    }

    @Operation(summary = "Delete punishment")
    @PostMapping("/punishmentDelete")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public DataResponse punishmentDelete(@Valid @RequestBody DataRequest dataRequest) {
        return punishmentService.punishmentDelete(dataRequest);
    }

    @Operation(summary = "Revoke punishment")
    @PostMapping("/revokePunishment")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public DataResponse revokePunishment(@Valid @RequestBody DataRequest dataRequest) {
        return punishmentService.revokePunishment(dataRequest);
    }
}
