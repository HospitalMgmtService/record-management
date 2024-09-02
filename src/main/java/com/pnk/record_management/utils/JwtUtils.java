package com.pnk.record_management.utils;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;


@RequiredArgsConstructor // injected by Constructor, no longer need of @Autowire
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class JwtUtils {

    /*
     * Get the JWT from the SecurityContextHolder
     * */
    public static Map<String, Object> extractDataFromJWT() {
        Map<String, Object> extractedData = new HashMap<>();

        var context = SecurityContextHolder.getContext();
        extractedData.put("context", context);

        var authentication = context.getAuthentication();
        extractedData.put("authentication", authentication);

        String name = authentication.getName();
        extractedData.put("name", name);

        var jwtToken = authentication.getCredentials().toString();
        extractedData.put("jwtToken", jwtToken);

        log.info(">> extractDataFromJWT::extractedData: {}", extractedData);
        return extractedData;
    }


    public static String decodeJWTPayload(String base64Payload) {
        byte[] decodedBytes = Base64.getDecoder().decode(base64Payload);

        String decodedPayload = new String(decodedBytes);
        log.info(">> getPayloadFromToken::decodedPayload: {}", decodedPayload);

        return decodedPayload;
    }
}
