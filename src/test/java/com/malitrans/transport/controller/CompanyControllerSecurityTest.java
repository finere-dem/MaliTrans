package com.malitrans.transport.controller;

import com.malitrans.transport.dto.DriverSummaryDTO;
import com.malitrans.transport.dto.PaginatedResponse;
import com.malitrans.transport.model.DeliveryCompany;
import com.malitrans.transport.model.Role;
import com.malitrans.transport.model.Utilisateur;
import com.malitrans.transport.repository.UtilisateurRepository;
import com.malitrans.transport.security.CustomUserDetailsService;
import com.malitrans.transport.security.JwtTokenUtil;
import com.malitrans.transport.service.CompanyService;
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
public class CompanyControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CompanyService companyService;

    @MockBean
    private UtilisateurRepository utilisateurRepository;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JwtTokenUtil jwtTokenUtil;

    private Utilisateur companyManager1;
    private Utilisateur companyManager2;
    private DeliveryCompany company1;
    private DeliveryCompany company2;

    @BeforeEach
    void setUp() {
        // Setup Company 1
        company1 = new DeliveryCompany();
        company1.setId(1L);
        company1.setName("Company One");
        company1.setActive(true);

        // Setup Company 2
        company2 = new DeliveryCompany();
        company2.setId(2L);
        company2.setName("Company Two");
        company2.setActive(true);

        // Setup Company Manager 1 (belongs to Company 1)
        companyManager1 = new Utilisateur();
        companyManager1.setId(1L);
        companyManager1.setUsername("manager1");
        companyManager1.setPassword("$2a$10$encrypted");
        companyManager1.setRole(Role.COMPANY_MANAGER);
        companyManager1.setCompany(company1);
        companyManager1.setEnabled(true);

        // Setup Company Manager 2 (belongs to Company 2)
        companyManager2 = new Utilisateur();
        companyManager2.setId(2L);
        companyManager2.setUsername("manager2");
        companyManager2.setPassword("$2a$10$encrypted");
        companyManager2.setRole(Role.COMPANY_MANAGER);
        companyManager2.setCompany(company2);
        companyManager2.setEnabled(true);

        // Mock UserDetails for Company Manager 1
        UserDetails manager1UserDetails = User.builder()
                .username("manager1")
                .password("$2a$10$encrypted")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("COMPANY_MANAGER")))
                .build();

        // Mock UserDetails for Company Manager 2
        UserDetails manager2UserDetails = User.builder()
                .username("manager2")
                .password("$2a$10$encrypted")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("COMPANY_MANAGER")))
                .build();

        when(utilisateurRepository.findByUsername("manager1")).thenReturn(Optional.of(companyManager1));
        when(utilisateurRepository.findByUsername("manager2")).thenReturn(Optional.of(companyManager2));
        when(customUserDetailsService.loadUserByUsername("manager1")).thenReturn(manager1UserDetails);
        when(customUserDetailsService.loadUserByUsername("manager2")).thenReturn(manager2UserDetails);

        // Mock CompanyService.getCompanyDrivers to return valid PaginatedResponse (controller uses SecurityUtil + this)
        PaginatedResponse<DriverSummaryDTO> emptyFleet = new PaginatedResponse<>(
                List.of(),
                new PaginatedResponse.Meta(0, 1, 0, 20)
        );
        when(companyService.getCompanyDrivers(anyLong(), anyInt(), anyInt(), any(), any()))
                .thenReturn(emptyFleet);
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
    void testCompanyManagerCanAccessFleetList() throws Exception {
        mockMvc.perform(get("/company/drivers")
                        .with(authentication(auth("manager1", "COMPANY_MANAGER")))
                        .param("page", "1")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.meta").exists())
                .andExpect(jsonPath("$.meta.currentPage").value(1))
                .andExpect(jsonPath("$.meta.pageSize").value(20));
    }

    @Test
    void testSupplierCanAccessFleetList() throws Exception {
        mockMvc.perform(get("/company/drivers")
                        .with(authentication(auth("manager2", "SUPPLIER")))
                        .param("page", "1")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.meta").exists());
    }

    @Test
    void testChauffeurCannotAccessFleetList() throws Exception {
        mockMvc.perform(get("/company/drivers")
                        .with(authentication(auth("chauffeur1", "CHAUFFEUR")))
                        .param("page", "1")
                        .param("limit", "20"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUnauthenticatedUserCannotAccessFleetList() throws Exception {
        mockMvc.perform(get("/company/drivers")
                        .param("page", "1")
                        .param("limit", "20"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 401 && status != 403) {
                        throw new AssertionError("Expected 401 or 403 but was " + status);
                    }
                });
    }

    @Test
    void testFleetListWithFilters() throws Exception {
        mockMvc.perform(get("/company/drivers")
                        .with(authentication(auth("manager1", "COMPANY_MANAGER")))
                        .param("page", "1")
                        .param("limit", "20")
                        .param("status", "ACTIVE")
                        .param("q", "driver"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.meta").exists());
    }

    @Test
    void testFleetListPagination() throws Exception {
        when(companyService.getCompanyDrivers(eq(1L), eq(2), eq(10), any(), any()))
                .thenReturn(new PaginatedResponse<>(List.of(), new PaginatedResponse.Meta(0, 2, 0, 10)));
        mockMvc.perform(get("/company/drivers")
                        .with(authentication(auth("manager1", "COMPANY_MANAGER")))
                        .param("page", "2")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.currentPage").value(2))
                .andExpect(jsonPath("$.meta.pageSize").value(10));
    }
}

