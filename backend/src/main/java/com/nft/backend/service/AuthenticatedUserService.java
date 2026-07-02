package com.nft.backend.service;

import java.nio.charset.StandardCharsets;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECFieldFp;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.EllipticCurve;
import java.security.spec.RSAPublicKeySpec;
import java.time.Instant;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthenticatedUserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticatedUserService.class);
    private static final String AUTHENTICATED_USER_ATTRIBUTE = AuthenticatedUserService.class.getName() + ".user";
    private static final Pattern STRING_CLAIM = Pattern.compile("\"%s\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern NUMBER_CLAIM = Pattern.compile("\"%s\"\\s*:\\s*(\\d+)");

    private final String jwtSecret;
    private final String supabaseUrl;
    private final String jwksUrl;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private volatile JwksCache jwksCache;

    public record AuthenticatedUser(UUID id, String email) {
    }

    private record JwksCache(List<Map<String, Object>> keys, Instant expiresAt) {
    }

    public AuthenticatedUserService(
            @Value("${supabase.jwt-secret:${app.jwt.secret:}}") String jwtSecret,
            @Value("${supabase.url:}") String supabaseUrl,
            @Value("${supabase.jwks-url:}") String jwksUrl) {
        this.jwtSecret = jwtSecret == null ? "" : jwtSecret.trim();
        this.supabaseUrl = supabaseUrl == null ? "" : supabaseUrl.trim().replaceAll("/+$", "");
        this.jwksUrl = jwksUrl == null || jwksUrl.isBlank()
                ? this.supabaseUrl + "/auth/v1/.well-known/jwks.json"
                : jwksUrl.trim();
    }

    public Optional<AuthenticatedUser> currentUser() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return Optional.empty();
        }

        Object cachedUser = request.getAttribute(AUTHENTICATED_USER_ATTRIBUTE);
        if (cachedUser instanceof AuthenticatedUser user) {
            return Optional.of(user);
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            LOGGER.warn("Authentication failed: missing Authorization bearer header for {}", request.getRequestURI());
            return Optional.empty();
        }

        AuthenticatedUser user = parseBearerToken(authorization.substring("Bearer ".length()).trim(), request.getRequestURI());
        request.setAttribute(AUTHENTICATED_USER_ATTRIBUTE, user);
        return Optional.of(user);
    }

    public Optional<UUID> currentUserId() {
        return currentUser().map(AuthenticatedUser::id);
    }

    public UUID requireCurrentUserId() {
        return requireCurrentUser().id();
    }

    public AuthenticatedUser requireCurrentUser() {
        return currentUser()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required"));
    }

    private AuthenticatedUser parseBearerToken(String token, String requestUri) {
        if (token.isBlank()) {
            LOGGER.warn("Authentication failed: empty bearer token for {}", requestUri);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            LOGGER.warn("Authentication failed: malformed bearer token for {}", requestUri);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid bearer token");
        }

        String headerJson;
        try {
            headerJson = decodePart(parts[0]);
        } catch (IllegalArgumentException exception) {
            LOGGER.warn("Authentication failed: invalid bearer token header for {}", requestUri);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid bearer token", exception);
        }
        String algorithm = stringClaim(headerJson, "alg");
        String keyId = stringClaim(headerJson, "kid");
        if (!isValidSignature(algorithm, keyId, parts[0] + "." + parts[1], parts[2])) {
            LOGGER.warn("Authentication failed: invalid bearer token signature for {}", requestUri);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid bearer token signature");
        }

        try {
            String json = decodePart(parts[1]);
            Long exp = numberClaim(json, "exp");
            if (exp != null && Instant.ofEpochSecond(exp).isBefore(Instant.now())) {
                LOGGER.warn("Authentication failed: expired bearer token for {}", requestUri);
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bearer token expired");
            }

            String sub = stringClaim(json, "sub");
            if (sub.isBlank()) {
                LOGGER.warn("Authentication failed: bearer token missing subject for {}", requestUri);
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bearer token is missing subject");
            }

            String issuer = stringClaim(json, "iss");
            if (!isExpectedIssuer(issuer)) {
                LOGGER.warn("Authentication failed: unexpected token issuer '{}' for {}", issuer, requestUri);
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid bearer token issuer");
            }

            AuthenticatedUser user = new AuthenticatedUser(UUID.fromString(sub), stringClaim(json, "email"));
            LOGGER.debug("Authenticated Supabase user id={} emailPresent={} for {}", user.id(), !user.email().isBlank(), requestUri);
            return user;
        } catch (IllegalArgumentException exception) {
            LOGGER.warn("Authentication failed: invalid bearer token subject for {}", requestUri);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid bearer token subject", exception);
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid bearer token", exception);
        }
    }

    private boolean isExpectedIssuer(String issuer) {
        if (supabaseUrl.isBlank()) {
            return true;
        }
        return (supabaseUrl + "/auth/v1").equals(issuer);
    }

    private boolean isValidSignature(String algorithm, String keyId, String signedContent, String signature) {
        if ("HS256".equalsIgnoreCase(algorithm)) {
            if (jwtSecret.isBlank()) {
                LOGGER.error("Authentication failed: SUPABASE_JWT_SECRET is required for HS256 tokens");
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT validation is not configured");
            }
            return isValidHmacSignature(signedContent, signature);
        }

        if ("RS256".equalsIgnoreCase(algorithm)) {
            return isValidRsaSignature(keyId, signedContent, signature);
        }

        if ("ES256".equalsIgnoreCase(algorithm)) {
            return isValidEcSignature(keyId, signedContent, signature);
        }

        LOGGER.warn("Authentication failed: unsupported JWT algorithm '{}'", algorithm);
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unsupported bearer token algorithm");
    }

    private boolean isValidHmacSignature(String signedContent, String signature) {
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

    private boolean isValidRsaSignature(String keyId, String signedContent, String signature) {
        try {
            Map<String, Object> jwk = jwkFor(keyId);
            BigInteger modulus = unsignedBigInteger((String) jwk.get("n"));
            BigInteger exponent = unsignedBigInteger((String) jwk.get("e"));
            RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new RSAPublicKeySpec(modulus, exponent));
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(publicKey);
            verifier.update(signedContent.getBytes(StandardCharsets.UTF_8));
            return verifier.verify(Base64.getUrlDecoder().decode(signature));
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unable to verify bearer token", exception);
        }
    }

    private boolean isValidEcSignature(String keyId, String signedContent, String signature) {
        try {
            Map<String, Object> jwk = jwkFor(keyId);
            ECParameterSpec params = ecParameters();
            ECPoint point = new ECPoint(unsignedBigInteger((String) jwk.get("x")), unsignedBigInteger((String) jwk.get("y")));
            ECPublicKey publicKey = (ECPublicKey) KeyFactory.getInstance("EC")
                    .generatePublic(new ECPublicKeySpec(point, params));
            Signature verifier = Signature.getInstance("SHA256withECDSA");
            verifier.initVerify(publicKey);
            verifier.update(signedContent.getBytes(StandardCharsets.UTF_8));
            return verifier.verify(ecdsaJoseToDer(Base64.getUrlDecoder().decode(signature)));
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unable to verify bearer token", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> jwkFor(String keyId) {
        if (keyId == null || keyId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bearer token is missing key id");
        }

        JwksCache cache = jwksCache;
        if (cache == null || cache.expiresAt().isBefore(Instant.now())) {
            cache = loadJwks();
            jwksCache = cache;
        }

        return cache.keys().stream()
                .filter((key) -> keyId.equals(key.get("kid")))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bearer token key not found"));
    }

    @SuppressWarnings("unchecked")
    private JwksCache loadJwks() {
        if (jwksUrl == null || jwksUrl.isBlank()) {
            LOGGER.error("Authentication failed: Supabase JWKS URL is not configured");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT validation is not configured");
        }

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(jwksUrl))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                LOGGER.warn("Authentication failed: JWKS endpoint returned {}", response.statusCode());
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unable to load JWT keys");
            }

            List<Map<String, Object>> keys = parseJwksKeys(response.body());
            if (keys.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid JWT keys");
            }

            return new JwksCache(keys, Instant.now().plus(Duration.ofMinutes(10)));
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            LOGGER.warn("Authentication failed: unable to load JWKS from {}", jwksUrl);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unable to load JWT keys", exception);
        }
    }

    private ECParameterSpec ecParameters() throws Exception {
        AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
        parameters.init(new ECGenParameterSpec("secp256r1"));
        return parameters.getParameterSpec(ECParameterSpec.class);
    }

    private byte[] ecdsaJoseToDer(byte[] joseSignature) {
        if (joseSignature.length != 64) {
            return joseSignature;
        }

        byte[] r = derInteger(joseSignature, 0, 32);
        byte[] s = derInteger(joseSignature, 32, 32);
        int length = 2 + r.length + 2 + s.length;
        byte[] der = new byte[2 + length];
        der[0] = 0x30;
        der[1] = (byte) length;
        der[2] = 0x02;
        der[3] = (byte) r.length;
        System.arraycopy(r, 0, der, 4, r.length);
        int sOffset = 4 + r.length;
        der[sOffset] = 0x02;
        der[sOffset + 1] = (byte) s.length;
        System.arraycopy(s, 0, der, sOffset + 2, s.length);
        return der;
    }

    private byte[] derInteger(byte[] source, int offset, int length) {
        int first = offset;
        int end = offset + length;
        while (first < end - 1 && source[first] == 0) {
            first++;
        }

        int valueLength = end - first;
        boolean needsPadding = (source[first] & 0x80) != 0;
        byte[] result = new byte[valueLength + (needsPadding ? 1 : 0)];
        if (needsPadding) {
            result[0] = 0;
        }
        System.arraycopy(source, first, result, needsPadding ? 1 : 0, valueLength);
        return result;
    }

    private BigInteger unsignedBigInteger(String value) {
        return new BigInteger(1, Base64.getUrlDecoder().decode(value));
    }

    private String decodePart(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private List<Map<String, Object>> parseJwksKeys(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }

        return Pattern.compile("\\{[^{}]*\"kid\"[^{}]*}")
                .matcher(json)
                .results()
                .map((match) -> jwkFromJson(match.group()))
                .filter((jwk) -> !jwk.isEmpty())
                .toList();
    }

    private Map<String, Object> jwkFromJson(String json) {
        Map<String, Object> jwk = new HashMap<>();
        putIfPresent(jwk, "kid", stringClaim(json, "kid"));
        putIfPresent(jwk, "kty", stringClaim(json, "kty"));
        putIfPresent(jwk, "alg", stringClaim(json, "alg"));
        putIfPresent(jwk, "n", stringClaim(json, "n"));
        putIfPresent(jwk, "e", stringClaim(json, "e"));
        putIfPresent(jwk, "x", stringClaim(json, "x"));
        putIfPresent(jwk, "y", stringClaim(json, "y"));
        return jwk;
    }

    private void putIfPresent(Map<String, Object> target, String key, String value) {
        if (value != null && !value.isBlank()) {
            target.put(key, value);
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
