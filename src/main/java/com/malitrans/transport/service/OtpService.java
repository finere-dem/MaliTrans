package com.malitrans.transport.service;

import com.malitrans.transport.model.OtpCode;
import com.malitrans.transport.model.OtpType;
import com.malitrans.transport.model.Utilisateur;
import com.malitrans.transport.repository.OtpCodeRepository;
import com.malitrans.transport.util.PhoneUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Random;

@Service
public class OtpService {

    private static final int CODE_LENGTH = 6;
    private static final Random RANDOM = new Random();

    @Value("${app.otp.expiration-minutes:5}")
    private int expirationMinutes;

    private final OtpCodeRepository otpCodeRepository;
    private final SmsService smsService;
    private final PhoneUtil phoneUtil;

    public OtpService(OtpCodeRepository otpCodeRepository,
                      SmsService smsService,
                      PhoneUtil phoneUtil) {
        this.otpCodeRepository = otpCodeRepository;
        this.smsService = smsService;
        this.phoneUtil = phoneUtil;
    }

    /**
     * Crée un OTP de type REGISTRATION pour l'utilisateur, l'enregistre (5 min) et envoie le code par SMS (mock console).
     * Remplace tout OTP REGISTRATION existant pour cet utilisateur.
     */
    @Transactional
    public void createOtpForRegistration(Utilisateur user) {
        if (user.getPhone() == null || user.getPhone().isBlank()) {
            throw new IllegalStateException("User phone is required to send registration OTP");
        }
        deleteExistingRegistrationOtpForUser(user);
        String code = generateCode();
        OtpCode otp = new OtpCode();
        otp.setCode(code);
        otp.setExpiryDate(Instant.now().plusSeconds(expirationMinutes * 60L));
        otp.setType(OtpType.REGISTRATION);
        otp.setUser(user);
        otpCodeRepository.save(otp);
        smsService.sendOtp(user.getPhone(), code);
    }

    /**
     * Vérifie le code OTP pour la finalisation de l'inscription.
     * @param phone Numéro (sera normalisé en format international)
     * @param code Code à 6 chiffres
     * @return L'utilisateur activé (enabled=true)
     * @throws IllegalArgumentException si code invalide ou expiré
     */
    @Transactional
    public Utilisateur verifyRegistration(String phone, String code) {
        String normalizedPhone = phoneUtil.toInternational(phone);
        if (normalizedPhone == null || code == null || code.isBlank()) {
            throw new IllegalArgumentException("Phone and code are required");
        }
        String trimmedCode = code.trim();
        if (trimmedCode.length() != CODE_LENGTH || !trimmedCode.matches("\\d+")) {
            throw new IllegalArgumentException("Code must be 6 digits");
        }
        OtpCode otp = otpCodeRepository.findByCodeAndType(trimmedCode, OtpType.REGISTRATION)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired code"));
        if (otp.getExpiryDate().isBefore(Instant.now())) {
            otpCodeRepository.delete(otp);
            throw new IllegalArgumentException("Code has expired");
        }
        Utilisateur user = otp.getUser();
        String userPhone = user.getPhone();
        if (userPhone == null || !userPhone.equals(normalizedPhone)) {
            throw new IllegalArgumentException("Invalid or expired code");
        }
        user.setEnabled(true);
        otpCodeRepository.delete(otp);
        return user;
    }

    private void deleteExistingRegistrationOtpForUser(Utilisateur user) {
        otpCodeRepository.deleteByUserAndType(user, OtpType.REGISTRATION);
    }

    private String generateCode() {
        int max = (int) Math.pow(10, CODE_LENGTH);
        int value = RANDOM.nextInt(max);
        return String.format("%0" + CODE_LENGTH + "d", value);
    }
}
