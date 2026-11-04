package com.chocomall.portal.service;

import com.chocomall.common.api.ResultCode;
import com.chocomall.common.exception.ApiException;
import com.chocomall.common.exception.Asserts;
import com.chocomall.core.store.ExpiringStore;
import com.chocomall.model.entity.Member;
import com.chocomall.model.entity.MemberLevel;
import com.chocomall.model.repository.MemberLevelRepository;
import com.chocomall.model.repository.MemberRepository;
import com.chocomall.portal.dto.PortalDtos.LoginParam;
import com.chocomall.portal.dto.PortalDtos.MemberProfile;
import com.chocomall.portal.dto.PortalDtos.ProfileParam;
import com.chocomall.portal.dto.PortalDtos.RegisterParam;
import com.chocomall.portal.dto.PortalDtos.ResetPasswordParam;
import com.chocomall.portal.dto.PortalDtos.TokenResult;
import com.chocomall.security.JwtTokenService;
import com.chocomall.security.SecurityProperties;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Sign-up with a phone verification code, sign-in with brute-force protection, password reset and profile. */
@Service
@RequiredArgsConstructor
public class SsoService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final int MAX_CODE_ATTEMPTS = 5;

    private final MemberRepository members;
    private final MemberLevelRepository levels;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService tokens;
    private final SecurityProperties security;
    private final ExpiringStore store;
    private final SmsSender sms;

    @Value("${chocomall.sms.expose-code:false}")
    private boolean exposeCode;

    @Value("${chocomall.login.max-attempts:5}")
    private int maxLoginAttempts;

    @Value("${chocomall.login.lockout-minutes:15}")
    private int lockoutMinutes;

    /** Sends a verification code. One request per phone per minute. Returns the code only when exposure is enabled (dev). */
    public String requestAuthCode(String phone) {
        if (store.increment("sms-rate:" + phone, Duration.ofMinutes(1)) > 1) {
            throw new ApiException(ResultCode.TOO_MANY_REQUESTS, "Please wait a minute before requesting another code");
        }
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        store.put("authcode:" + phone, code, CODE_TTL);
        store.delete("authcode-attempts:" + phone);
        sms.send(phone, "Your ChocoMall verification code is " + code + ". It expires in 5 minutes.");
        return exposeCode ? code : null;
    }

    @Transactional
    public TokenResult register(RegisterParam p) {
        verifyCode(p.telephone(), p.authCode());
        Asserts.state(!members.existsByUsername(p.username()), "That username is already taken");
        Asserts.state(!members.existsByPhone(p.telephone()), "That phone number is already registered");
        Member m = new Member();
        m.setUsername(p.username());
        m.setPassword(passwordEncoder.encode(p.password()));
        m.setPhone(p.telephone());
        m.setEmail(p.email());
        m.setNickname(p.username());
        m.setMarketingConsent(Boolean.TRUE.equals(p.marketingConsent()));
        levels.findFirstByDefaultStatusTrue().ifPresent(l -> m.setLevelId(l.getId()));
        members.save(m);
        return token(m.getUsername());
    }

    public TokenResult login(LoginParam p) {
        String key = "member-login:" + p.username().toLowerCase();
        long failures = store.get(key).map(Long::parseLong).orElse(0L);
        if (failures >= maxLoginAttempts) {
            throw new ApiException(ResultCode.TOO_MANY_REQUESTS,
                    "Too many failed sign-in attempts. Try again in " + lockoutMinutes + " minutes.");
        }
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(p.username(), p.password()));
        } catch (BadCredentialsException e) {
            store.increment(key, Duration.ofMinutes(lockoutMinutes));
            throw e;
        }
        store.delete(key);
        return token(p.username());
    }

    @Transactional
    public void resetPassword(ResetPasswordParam p) {
        verifyCode(p.telephone(), p.authCode());
        Member m = Asserts.found(members.findByPhone(p.telephone()), "Account for that phone number");
        m.setPassword(passwordEncoder.encode(p.password()));
    }

    @Transactional(readOnly = true)
    public MemberProfile profile(Long memberId) {
        Member m = Asserts.found(members.findById(memberId), "Member");
        List<MemberLevel> ordered = levels.findAllByOrderByGrowthPointAsc();
        String current = ordered.stream().filter(l -> l.getId().equals(m.getLevelId())).map(MemberLevel::getName).findFirst().orElse(null);
        MemberLevel next = ordered.stream().filter(l -> l.getGrowthPoint() > m.getGrowth()).findFirst().orElse(null);
        return new MemberProfile(m.getId(), m.getUsername(), m.getNickname(), m.getPhone(), m.getEmail(), m.getIcon(), m.getGender(),
                m.getBirthday(), m.getCity(), m.getJob(), m.getPersonalizedSign(), m.getIntegration(), m.getGrowth(), current,
                next == null ? null : next.getName(), next == null ? null : next.getGrowthPoint() - m.getGrowth(), m.isMarketingConsent());
    }

    @Transactional
    public MemberProfile updateProfile(Long memberId, ProfileParam p) {
        Member m = Asserts.found(members.findById(memberId), "Member");
        if (p.nickname() != null) {
            m.setNickname(p.nickname());
        }
        if (p.icon() != null) {
            m.setIcon(p.icon());
        }
        if (p.gender() != null) {
            m.setGender(p.gender());
        }
        if (p.birthday() != null) {
            m.setBirthday(p.birthday());
        }
        if (p.city() != null) {
            m.setCity(p.city());
        }
        if (p.job() != null) {
            m.setJob(p.job());
        }
        if (p.personalizedSign() != null) {
            m.setPersonalizedSign(p.personalizedSign());
        }
        if (p.email() != null) {
            m.setEmail(p.email());
        }
        if (p.marketingConsent() != null) {
            m.setMarketingConsent(p.marketingConsent());
        }
        return profile(memberId);
    }

    private TokenResult token(String username) {
        return new TokenResult(tokens.generate(username), security.jwt().tokenHead().trim(), tokens.expirationSeconds());
    }

    /** Checks the code in constant time; a code can be tried at most five times and is single-use. */
    private void verifyCode(String phone, String code) {
        String attemptsKey = "authcode-attempts:" + phone;
        if (store.increment(attemptsKey, CODE_TTL) > MAX_CODE_ATTEMPTS) {
            store.delete("authcode:" + phone);
            throw new ApiException(ResultCode.TOO_MANY_REQUESTS, "Too many wrong codes. Request a new one.");
        }
        String expected = store.get("authcode:" + phone).orElse(null);
        boolean ok = expected != null && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), code.getBytes(StandardCharsets.UTF_8));
        Asserts.isTrue(ok, "The verification code is wrong or has expired");
        store.delete("authcode:" + phone);
        store.delete(attemptsKey);
    }
}
