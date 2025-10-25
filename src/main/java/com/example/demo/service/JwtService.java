package com.example.demo.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Slf4j
@Service
public class JwtService {

    private static final String SIGNER_KEY = "eQ0elqeCs4ul76KFSm5/1qycbAsHYBG8eQ0elqeCs4ul76KFSm5/1qycbAsHYBG8";

    public String generateToken(String email, String role) {
        try {
            byte[] keyBytes = SIGNER_KEY.getBytes();

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(email)
                    .issuer("localhost")
                    .issueTime(new Date())
                    .expirationTime(new Date(Instant.now().plus(8, ChronoUnit.HOURS).toEpochMilli()))
                    .claim("role", role)
                    .build();

            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS256)
                    .type(JOSEObjectType.JWT)
                    .build();

            SignedJWT signedJWT = new SignedJWT(header, claimsSet);
            JWSSigner signer = new MACSigner(keyBytes);
            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (Exception e) {
            log.error("Cannot create token", e);
            throw new RuntimeException("Token generation failed", e);
        }
    }

    public boolean verifyToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWSVerifier verifier = new MACVerifier(SIGNER_KEY);
            boolean isValidSignature = signedJWT.verify(verifier);

            Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
            boolean isNotExpired = new Date().before(expirationTime);

            return isValidSignature && isNotExpired;
        } catch (Exception e) {
            log.error("Token verification failed", e);
            return false;
        }
    }

    public String extractEmail(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return signedJWT.getJWTClaimsSet().getSubject();
        } catch (Exception e) {
            log.error("Cannot extract email from token", e);
            return null;
        }
    }

    public String extractRole(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return signedJWT.getJWTClaimsSet().getStringClaim("role");
        } catch (ParseException e) {
            log.error("Cannot extract role from token", e);
            throw new RuntimeException("Invalid token", e);
        }
    }
}