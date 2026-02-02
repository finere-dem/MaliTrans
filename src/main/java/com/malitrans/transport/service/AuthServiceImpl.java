package com.malitrans.transport.service;

import com.malitrans.transport.dto.AuthResponse;
import com.malitrans.transport.dto.RefreshTokenRequest;
import com.malitrans.transport.dto.RegisterDTO;
import com.malitrans.transport.exception.TokenRefreshException;
import com.malitrans.transport.model.DeliveryCompany;
import com.malitrans.transport.model.RefreshToken;
import com.malitrans.transport.model.Role;
import com.malitrans.transport.model.UserStatus;
import com.malitrans.transport.model.Utilisateur;
import com.malitrans.transport.repository.UtilisateurRepository;
import com.malitrans.transport.security.JwtTokenUtil;
import com.malitrans.transport.util.PhoneUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthServiceImpl implements AuthService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final DeliveryCompanyService deliveryCompanyService;
    private final PhoneUtil phoneUtil;
    private final OtpService otpService;

    public AuthServiceImpl(UtilisateurRepository utilisateurRepository,
                          PasswordEncoder passwordEncoder,
                          JwtTokenUtil jwtTokenUtil,
                          AuthenticationManager authenticationManager,
                          RefreshTokenService refreshTokenService,
                          DeliveryCompanyService deliveryCompanyService,
                          PhoneUtil phoneUtil,
                          OtpService otpService) {
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenUtil = jwtTokenUtil;
        this.authenticationManager = authenticationManager;
        this.refreshTokenService = refreshTokenService;
        this.deliveryCompanyService = deliveryCompanyService;
        this.phoneUtil = phoneUtil;
        this.otpService = otpService;
    }

    @Override
    @Transactional
    public void register(RegisterDTO registerDTO) {
        if (registerDTO.getPhone() == null || registerDTO.getPhone().isBlank()) {
            throw new IllegalArgumentException("Phone number is required for registration");
        }
        String normalizedPhone = phoneUtil.toInternational(registerDTO.getPhone());
        if (normalizedPhone == null) {
            throw new IllegalArgumentException("Invalid phone number format");
        }

        if (utilisateurRepository.existsByUsername(registerDTO.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + registerDTO.getUsername());
        }
        if (utilisateurRepository.existsByPhone(normalizedPhone)) {
            throw new IllegalArgumentException("Phone number already exists: " + normalizedPhone);
        }

        Role role = convertRole(registerDTO.getRole());
        if (role == null) {
            throw new IllegalArgumentException("Invalid role. Must be 'CLIENT' or 'DRIVER'");
        }

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setUsername(registerDTO.getUsername());
        utilisateur.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        utilisateur.setFirstName(registerDTO.getFirstName());
        utilisateur.setLastName(registerDTO.getLastName());
        utilisateur.setPhone(normalizedPhone); // Stockage au format international (ex: +223...)
        utilisateur.setRole(role);
        utilisateur.setEnabled(false); // Activé après vérification OTP via POST /auth/verify-registration

        if (role == Role.CHAUFFEUR) {
            if (registerDTO.getCompanyId() == null) {
                throw new IllegalArgumentException("Company ID is required when registering as a driver");
            }
            DeliveryCompany company = deliveryCompanyService.findByIdAndActive(registerDTO.getCompanyId())
                    .orElseThrow(() -> new IllegalArgumentException("Company not found or inactive with ID: " + registerDTO.getCompanyId()));
            utilisateur.setCompany(company);
            utilisateur.setVehicleType(registerDTO.getVehicleType());
            utilisateur.setStatus(UserStatus.PENDING_COMPANY_VERIFICATION);
        } else {
            utilisateur.setStatus(UserStatus.ACTIVE);
        }

        utilisateurRepository.save(utilisateur);
        otpService.createOtpForRegistration(utilisateur);
    }

    @Override
    @Transactional
    public AuthResponse verifyRegistration(String phone, String code) {
        Utilisateur utilisateur = otpService.verifyRegistration(phone, code);
        String roleString = utilisateur.getRole() != null ? utilisateur.getRole().name() : "CLIENT";
        String accessToken = jwtTokenUtil.generateToken(utilisateur.getUsername(), List.of(roleString));
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(utilisateur.getId());
        return new AuthResponse(accessToken, refreshToken.getToken(), utilisateur.getUsername(), roleString, utilisateur.getId());
    }

    @Override
    public AuthResponse login(String username, String password) {
        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );

            // Get user entity for role and userId
            Utilisateur utilisateur = utilisateurRepository.findByUsername(username)
                    .orElseThrow(() -> new BadCredentialsException("User not found"));

            // Validate user has an ID (should never be null for persisted entities)
            if (utilisateur.getId() == null) {
                throw new IllegalStateException("User entity missing ID. Database integrity issue.");
            }

            // Get role as String for JWT token
            String roleString = utilisateur.getRole() != null ? utilisateur.getRole().name() : "CLIENT";
            
            // Generate JWT access token with roles claim
            String accessToken = jwtTokenUtil.generateToken(username, List.of(roleString));

            // Generate refresh token
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(utilisateur.getId());

            // Return response with access token, refresh token, username, role, and userId
            return new AuthResponse(accessToken, refreshToken.getToken(), username, roleString, utilisateur.getId());
        } catch (Exception e) {
            throw new BadCredentialsException("Invalid username or password", e);
        }
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        // Verify refresh token
        RefreshToken refreshToken = refreshTokenService.verifyExpiration(requestRefreshToken);
        
        // Get user from refresh token
        Utilisateur user = refreshToken.getUser();
        
        // Get role as String for JWT token
        String roleString = user.getRole() != null ? user.getRole().name() : "CLIENT";
        
        // Generate new access token with roles claim
        String newAccessToken = jwtTokenUtil.generateToken(user.getUsername(), List.of(roleString));
        
        // Return new access token with same refresh token
        return new AuthResponse(newAccessToken, refreshToken.getToken(), user.getUsername(), roleString, user.getId());
    }

    /**
     * Convert role string from DTO to Role enum
     * Accepts "DRIVER" and converts to "CHAUFFEUR" for internal use
     */
    private Role convertRole(String roleString) {
        if (roleString == null) {
            return null;
        }
        
        String upperRole = roleString.toUpperCase().trim();
        
        // Convert "DRIVER" to "CHAUFFEUR" for compatibility
        if ("DRIVER".equals(upperRole)) {
            return Role.CHAUFFEUR;
        }
        
        // Try to match other roles directly
        try {
            return Role.valueOf(upperRole);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

