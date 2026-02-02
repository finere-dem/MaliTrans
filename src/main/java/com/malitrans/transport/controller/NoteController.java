package com.malitrans.transport.controller;

import com.malitrans.transport.dto.NoteDTO;
import com.malitrans.transport.service.NoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notes")
public class NoteController {

    private final NoteService service;

    public NoteController(NoteService service) {
        this.service = service;
    }

    @Operation(summary = "Envoyer une note")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Note envoyée")})
    @PostMapping
    public ResponseEntity<NoteDTO> send(@RequestBody NoteDTO dto) {
        return ResponseEntity.ok(service.sendNote(dto));
    }

    @Operation(summary = "Notes données", 
               description = "Retourne toutes les notes données par l'utilisateur authentifié. " +
                           "userId est automatiquement extrait du JWT (pas de paramètre).")
    @ApiResponses({@ApiResponse(responseCode = "200")})
    @GetMapping("/from/me")
    public List<NoteDTO> notesFrom() {
        // SECURITY: Extract authenticated user ID from JWT (prevent IDOR)
        Long authenticatedUserId = com.malitrans.transport.security.SecurityUtil.getCurrentUserId();
        return service.notesGiven(authenticatedUserId);
    }

    @Operation(summary = "Notes reçues", 
               description = "Retourne toutes les notes reçues par l'utilisateur authentifié. " +
                           "userId est automatiquement extrait du JWT (pas de paramètre).")
    @ApiResponses({@ApiResponse(responseCode = "200")})
    @GetMapping("/to/me")
    public List<NoteDTO> notesTo() {
        // SECURITY: Extract authenticated user ID from JWT (prevent IDOR)
        Long authenticatedUserId = com.malitrans.transport.security.SecurityUtil.getCurrentUserId();
        return service.notesReceived(authenticatedUserId);
    }
}
