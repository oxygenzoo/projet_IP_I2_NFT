package com.nft.backend.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthenticatedUserService {

    private static final Pattern STRING_CLAIM = Pattern.compile("\"%s\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern NUMBER_CLAIM = Pattern.compile("\"%s\"\\s*:\\s*(\\d+)");

    private final String jwtSecret;

    public record AuthenticatedUser(UUID id, String email) {
    }

    public AuthenticatedUserService(@Value("${supabase.jwt-secret:${app.jwt.secret:}}") String jwtSecret) {
        this.jwtSecret = jwtSecret == null ? "" : jwtSecret.trim();
    }

    public Optional<AuthenticatedUser> currentUser() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return Optional.empty();
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return Optional.empty();
        }

        return Optional.of(parseBearerToken(authorization.substring("Bearer ".length()).trim()));
    }

    public Optional<UUID> currentUserId() {
        return currentUser().map(AuthenticatedUser::id);
    }

    public UUID requireCurrentUserId() {
        return currentUserId()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required"));
    }

    private AuthenticatedUser parseBearerToken(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid bearer token");
        }

        if (!jwtSecret.isBlank() && !isValidSignature(parts[0] + "." + parts[1], parts[2])) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid bearer token signature");
        }

        try {
            String json = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            Long exp = numberClaim(json, "exp");
            if (exp != null && Instant.ofEpochSecond(exp).isBefore(Instant.now())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bearer token expired");
            }

            String sub = stringClaim(json, "sub");
            if (sub.isBlank()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bearer token is missing subject");
            }

            return new AuthenticatedUser(UUID.fromString(sub), stringClaim(json, "email"));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid bearer token subject", exception);
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid bearer token", exception);
        }
    }

    private boolean isValidSignature(String signedContent, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = mac.doFinal(signedContent.getBytes(StandardCharsets.UTF_8));
            byte[] actual = Base64.getUrlDecoder().decode(signature);
            return constantTimeEquals(expected, actual);
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unable to verify bearer token", exception);
        }
    }

    private boolean constantTimeEquals(byte[] expected, byte[] actual) {
        if (expected.length != actual.length) {
            return false;
        }

        int result = 0;
        for (int index = 0; index < expected.length; index++) {
            result |= expected[index] ^ actual[index];
        }
        return result == 0;
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }

        return null;
    }

    private String stringClaim(String json, String claim) {
        Matcher matcher = Pattern.compile(String.format(STRING_CLAIM.pattern(), Pattern.quote(claim))).matcher(json);
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    private Long numberClaim(String json, String claim) {
        Matcher matcher = Pattern.compile(String.format(NUMBER_CLAIM.pattern(), Pattern.quote(claim))).matcher(json);
        return matcher.find() ? Long.parseLong(matcher.group(1)) : null;
    }
}
