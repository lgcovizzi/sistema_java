package com.example.sistemajava.user;

import com.example.sistemajava.email.EmailService;
import com.example.sistemajava.token.PasswordResetTokenRepository;
import com.example.sistemajava.token.VerificationTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UserServiceTest {
    private UserRepository userRepository;
    private VerificationTokenRepository verificationTokenRepository;
    private PasswordResetTokenRepository passwordResetTokenRepository;
    private PasswordEncoder passwordEncoder;
    private EmailService emailService;
    private UserService userService;

    @BeforeEach
    void setup() {
        userRepository = mock(UserRepository.class);
        verificationTokenRepository = mock(VerificationTokenRepository.class);
        passwordResetTokenRepository = mock(PasswordResetTokenRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        emailService = mock(EmailService.class);
        userService = new UserService(userRepository, verificationTokenRepository, passwordResetTokenRepository, passwordEncoder, emailService);
    }

    @Test
    void first_user_is_admin() {
        when(userRepository.existsByEmail("a@a.com")).thenReturn(false);
        when(userRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode("x"))
                .thenReturn("hash");
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(userCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        User user = userService.register("a@a.com", "x", "12345678901");

        assertEquals(Role.ADMIN, user.getRole());
        assertEquals("hash", user.getPasswordHash());
        verify(emailService, times(1)).send(eq("a@a.com"), anyString(), contains("/activate?token="));
    }

    @Test
    void next_users_are_user() {
        when(userRepository.existsByEmail("b@b.com")).thenReturn(false);
        when(userRepository.count()).thenReturn(1L);
        when(passwordEncoder.encode("x"))
                .thenReturn("hash");
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(userCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        User user = userService.register("b@b.com", "x", "12345678902");

        assertEquals(Role.USER, user.getRole());
    }
}


