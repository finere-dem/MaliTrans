package com.malitrans.transport.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Normalise les numéros de téléphone au format international E.164 (ex: +22370123456).
 */
@Component
public class PhoneUtil {

    private static final Pattern DIGITS_ONLY = Pattern.compile("[^0-9]");

    @Value("${app.phone.default-country-code:+223}")
    private String defaultCountryCode;

    /**
     * Normalise le numéro au format international E.164.
     * - Si le numéro commence par "+", on conserve et on ne garde que les chiffres + le "+".
     * - Sinon, on préfixe par le code pays par défaut (ex: +223 pour le Mali).
     *
     * @param rawPhone Numéro saisi (ex: "01 23 45 67 89", "70123456", "+223 70 12 34 56")
     * @return Numéro normalisé (ex: "+22370123456") ou null si rawPhone est null/vide
     */
    public String toInternational(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            return null;
        }
        String trimmed = rawPhone.trim();
        String digits = DIGITS_ONLY.matcher(trimmed).replaceAll("");
        if (digits.isEmpty()) {
            return null;
        }
        if (trimmed.startsWith("+")) {
            return "+" + digits;
        }
        String countryDigits = DIGITS_ONLY.matcher(defaultCountryCode).replaceAll("");
        if (countryDigits.isEmpty()) {
            countryDigits = "223";
        }
        if (!countryDigits.startsWith("+")) {
            countryDigits = "+" + countryDigits;
        }
        return countryDigits + digits;
    }
}
