package com.malitrans.transport.controller;

import com.malitrans.transport.dto.PaginatedResponse;
import com.malitrans.transport.dto.RideRequestDTO;
import com.malitrans.transport.model.Role;
import com.malitrans.transport.model.Utilisateur;
import com.malitrans.transport.repository.UtilisateurRepository;
import com.malitrans.transport.security.CustomUserDetailsService;
import com.malitrans.transport.security.JwtTokenUtil;
import com.malitrans.transport.service.RideRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RideRequestService rideRequestService;

    @MockBean
    private UtilisateurRepository utilisateurRepository;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JwtTokenUtil jwtTokenUtil;

    private Utilisateur chauffeur;
    private Utilisateur supplier;

    @BeforeEach
    void setUp() {
        // Setup CHAUFFEUR user
        chauffeur = new Utilisateur();
        chauffeur.setId(1L);
        chauffeur.setUsername("driver1");
        chauffeur.setPassword("$2a$10$encrypted");
        chauffeur.setRole(Role.CHAUFFEUR);
        chauffeur.setEnabled(true);

        // Setup SUPPLIER user
        supplier = new Utilisateur();
        supplier.setId(2L);
        supplier.setUsername("supplier1");
        supplier.setPassword("$2a$10$encrypted");
        supplier.setRole(Role.SUPPLIER);
        supplier.setEnabled(true);

        // Mock UserDetails for CHAUFFEUR
        UserDetails chauffeurUserDetails = User.builder()
                .username("driver1")
                .password("$2a$10$encrypted")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("CHAUFFEUR")))
                .build();

        // Mock UserDetails for SUPPLIER
        UserDetails supplierUserDetails = User.builder()
                .username("supplier1")
                .password("$2a$10$encrypted")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("SUPPLIER")))
                .build();

        when(utilisateurRepository.findByUsername("driver1")).thenReturn(Optional.of(chauffeur));
        when(utilisateurRepository.findByUsername("supplier1")).thenReturn(Optional.of(supplier));
        when(customUserDetailsService.loadUserByUsername("driver1")).thenReturn(chauffeurUserDetails);
        when(customUserDetailsService.loadUserByUsername("supplier1")).thenReturn(supplierUserDetails);

        // Mock RideRequestService so controller does not hit DB (SecurityUtil returns user from mock repo)
        when(rideRequestService.getActiveRidesForChauffeur(1L)).thenReturn(List.of());
        when(rideRequestService.historyForChauffeur(eq(1L), anyInt(), anyInt()))
                .thenReturn(new PaginatedResponse<>(List.of(), new PaginatedResponse.Meta(0, 1, 0, 20)));
    }

    /** SecurityUtil expects principal = String (username). Attach auth to request so it's visible during the request. */
    private UsernamePasswordAuthenticationToken auth(String username, String authority) {
        return new UsernamePasswordAuthenticationToken(
                username,
                null,
                Collections.singletonList(new SimpleGrantedAuthority(authority))
        );
    }

    @Test
    void testChauffeurCanAccessActiveRides() throws Exception {
        mockMvc.perform(get("/ride/chauffeur/active")
                        .with(authentication(auth("driver1", "CHAUFFEUR"))))
                .andExpect(status().isOk());
    }

    @Test
    void testChauffeurCanAccessHistory() throws Exception {
        mockMvc.perform(get("/ride/chauffeur/history")
                        .with(authentication(auth("driver1", "CHAUFFEUR")))
                        .param("page", "1")
                        .param("limit", "20"))
                .andExpect(status().isOk());
    }

    @Test
    void testSupplierCannotAccessChauffeurActiveRides() throws Exception {
        mockMvc.perform(get("/ride/chauffeur/active")
                        .with(authentication(auth("supplier1", "SUPPLIER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void testSupplierCannotAccessChauffeurHistory() throws Exception {
        mockMvc.perform(get("/ride/chauffeur/history")
                        .with(authentication(auth("supplier1", "SUPPLIER")))
                        .param("page", "1")
                        .param("limit", "20"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUnauthenticatedUserCannotAccessChauffeurActiveRides() throws Exception {
        mockMvc.perform(get("/ride/chauffeur/active"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 401 && status != 403) {
                        throw new AssertionError("Expected 401 or 403 but was " + status);
                    }
                });
    }

    @Test
    void testUnauthenticatedUserCannotAccessChauffeurHistory() throws Exception {
        mockMvc.perform(get("/ride/chauffeur/history")
                        .param("page", "1")
                        .param("limit", "20"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 401 && status != 403) {
                        throw new AssertionError("Expected 401 or 403 but was " + status);
                    }
                });
    }
}

