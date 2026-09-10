package com.routemate.backend.auth.service;

import com.routemate.backend.auth.dto.AuthResponse;
import com.routemate.backend.auth.dto.LoginRequest;
import com.routemate.backend.auth.dto.RegisterRequest;
import com.routemate.backend.auth.security.CustomUserDetails;
import com.routemate.backend.auth.security.JwtService;
import com.routemate.backend.common.exception.BusinessRuleViolationException;
import com.routemate.backend.user.model.User;
import com.routemate.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailAndDeletedAtIsNull(request.getEmail())) {
            throw new BusinessRuleViolationException("Email is already in use");
        }

        User user = User.create(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getName()
        );

        user = userRepository.save(user);
        
        CustomUserDetails userDetails = new CustomUserDetails(user);
        String jwtToken = jwtService.generateToken(userDetails);
        
        return buildAuthResponse(jwtToken, user);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        
        User user = userRepository.findByEmailAndDeletedAtIsNull(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found for email: " + request.getEmail()));
                
        CustomUserDetails userDetails = new CustomUserDetails(user);
        String jwtToken = jwtService.generateToken(userDetails);
        
        return buildAuthResponse(jwtToken, user);
    }

    private AuthResponse buildAuthResponse(String token, User user) {
        return AuthResponse.builder()
                .token(token)
                .user(AuthResponse.UserDto.builder()
                        .id(String.valueOf(user.getPublicId()))
                        .email(user.getEmail())
                        .displayName(user.getDisplayName())
                        .rating(user.getRating())
                        .build())
                .build();
    }
}
