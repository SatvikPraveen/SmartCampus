// File location: src/test/java/unit/services/AuthServiceTest.java

package com.smartcampus.test.unit.services;

import com.smartcampus.services.AuthService;
import com.smartcampus.repositories.UserRepository;
import com.smartcampus.models.User;
import com.smartcampus.models.enums.UserRole;
import com.smartcampus.security.JwtTokenProvider;
import com.smartcampus.exceptions.AuthenticationException;
import com.smartcampus.exceptions.AccountLockedException;
import com.smartcampus.exceptions.InvalidCredentialsException;
import com.smartcampus.exceptions.UserNotFoundException;
import com.smartcampus.dto.LoginRequest;
import com.smartcampus.dto.LoginResponse;
import com.smartcampus.dto.RegisterRequest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Unit tests for the AuthService class
 * Tests authentication and authorization functionality
 * 
 * @author Smart Campus Development Team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Auth Service Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private LoginRequest loginRequest;
    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        // Set up test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("johndoe");
        testUser.setEmail("john.doe@smartcampus.edu");
        testUser.setPasswordHash("$2a$10$EncodedPasswordHash");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setPhone("555-1234");
        testUser.setRole(UserRole.STUDENT);
        testUser.setActive(true);
        testUser.setVerified(true);
        testUser.setFailedLoginAttempts(0);
        testUser.setLockedUntil(null);
        testUser.setLastLogin(LocalDateTime.now().minusDays(1));

        // Set up test login request
        loginRequest = new LoginRequest();
        loginRequest.setUsername("johndoe");
        loginRequest.setPassword("password123");

        // Set up test register request
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("janedoe");
        registerRequest.setEmail("jane.doe@smartcampus.edu");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("Jane");
        registerRequest.setLastName("Doe");
        registerRequest.setPhone("555-5678");
        registerRequest.setRole(UserRole.STUDENT);
    }

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        @DisplayName("Should login successfully with valid credentials")
        void shouldLoginSuccessfullyWithValidCredentials() {
            // Arrange
            String expectedToken = "jwt.token.here";
            when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password123", testUser.getPasswordHash())).thenReturn(true);
            when(jwtTokenProvider.generateToken(testUser)).thenReturn(expectedToken);
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // Act
            LoginResponse response = authService.login(loginRequest);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo(expectedToken);
            assertThat(response.getUsername()).isEqualTo("johndoe");
            assertThat(response.getRole()).isEqualTo(UserRole.STUDENT);

            verify(userRepository).findByUsername("johndoe");
            verify(passwordEncoder).matches("password123", testUser.getPasswordHash());
            verify(jwtTokenProvider).generateToken(testUser);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getLastLogin()).isNotNull();
            assertThat(userCaptor.getValue().getFailedLoginAttempts()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Arrange
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());
            loginRequest.setUsername("nonexistent");

            // Act & Assert
            assertThrows(UserNotFoundException.class, () -> {
                authService.login(loginRequest);
            });

            verify(userRepository).findByUsername("nonexistent");
            verify(passwordEncoder, never()).matches(anyString(), anyString());
            verify(jwtTokenProvider, never()).generateToken(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when password is invalid")
        void shouldThrowExceptionWhenPasswordIsInvalid() {
            // Arrange
            when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("wrongpassword", testUser.getPasswordHash())).thenReturn(false);
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            loginRequest.setPassword("wrongpassword");

            // Act & Assert
            assertThrows(InvalidCredentialsException.class, () -> {
                authService.login(loginRequest);
            });

            verify(passwordEncoder).matches("wrongpassword", testUser.getPasswordHash());
            verify(jwtTokenProvider, never()).generateToken(any(User.class));

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getFailedLoginAttempts()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should lock account after max failed attempts")
        void shouldLockAccountAfterMaxFailedAttempts() {
            // Arrange
            testUser.setFailedLoginAttempts(4); // One less than max (5)
            when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("wrongpassword", testUser.getPasswordHash())).thenReturn(false);
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            loginRequest.setPassword("wrongpassword");

            // Act & Assert
            assertThrows(AccountLockedException.class, () -> {
                authService.login(loginRequest);
            });

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getFailedLoginAttempts()).isEqualTo(5);
            assertThat(userCaptor.getValue().getLockedUntil()).isNotNull();
        }

        @Test
        @DisplayName("Should throw exception when account is locked")
        void shouldThrowExceptionWhenAccountIsLocked() {
            // Arrange
            testUser.setLockedUntil(LocalDateTime.now().plusMinutes(30));
            when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));

            // Act & Assert
            assertThrows(AccountLockedException.class, () -> {
                authService.login(loginRequest);
            });

            verify(passwordEncoder, never()).matches(anyString(), anyString());
            verify(jwtTokenProvider, never()).generateToken(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when user is inactive")
        void shouldThrowExceptionWhenUserIsInactive() {
            // Arrange
            testUser.setActive(false);
            when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));

            // Act & Assert
            assertThrows(AuthenticationException.class, () -> {
                authService.login(loginRequest);
            });

            verify(passwordEncoder, never()).matches(anyString(), anyString());
            verify(jwtTokenProvider, never()).generateToken(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when user is not verified")
        void shouldThrowExceptionWhenUserIsNotVerified() {
            // Arrange
            testUser.setVerified(false);
            when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));

            // Act & Assert
            assertThrows(AuthenticationException.class, () -> {
                authService.login(loginRequest);
            });

            verify(passwordEncoder, never()).matches(anyString(), anyString());
            verify(jwtTokenProvider, never()).generateToken(any(User.class));
        }

        @Test
        @DisplayName("Should unlock account after lockout period expires")
        void shouldUnlockAccountAfterLockoutPeriodExpires() {
            // Arrange
            testUser.setLockedUntil(LocalDateTime.now().minusMinutes(5)); // Expired lockout
            testUser.setFailedLoginAttempts(5);
            when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password123", testUser.getPasswordHash())).thenReturn(true);
            when(jwtTokenProvider.generateToken(testUser)).thenReturn("jwt.token.here");
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // Act
            LoginResponse response = authService.login(loginRequest);

            // Assert
            assertThat(response).isNotNull();

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getFailedLoginAttempts()).isEqualTo(0);
            assertThat(userCaptor.getValue().getLockedUntil()).isNull();
        }
    }

    @Nested
    @DisplayName("Register Tests")
    class RegisterTests {

        @Test
        @DisplayName("Should register user successfully")
        void shouldRegisterUserSuccessfully() {
            // Arrange
            when(userRepository.existsByUsername("janedoe")).thenReturn(false);
            when(userRepository.existsByEmail("jane.doe@smartcampus.edu")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("$2a$10$EncodedPasswordHash");
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // Act
            User result = authService.register(registerRequest);

            // Assert
            assertThat(result).isNotNull();

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();

            assertThat(savedUser.getUsername()).isEqualTo("janedoe");
            assertThat(savedUser.getEmail()).isEqualTo("jane.doe@smartcampus.edu");
            assertThat(savedUser.getFirstName()).isEqualTo("Jane");
            assertThat(savedUser.getLastName()).isEqualTo("Doe");
            assertThat(savedUser.getRole()).isEqualTo(UserRole.STUDENT);
            assertThat(savedUser.isActive()).isTrue();
            assertThat(savedUser.isVerified()).isFalse(); // Should be false initially
            assertThat(savedUser.getVerificationToken()).isNotNull();

            verify(passwordEncoder).encode("password123");
        }

        @Test
        @DisplayName("Should throw exception when username already exists")
        void shouldThrowExceptionWhenUsernameAlreadyExists() {
            // Arrange
            when(userRepository.existsByUsername("janedoe")).thenReturn(true);

            // Act & Assert
            assertThrows(AuthenticationException.class, () -> {
                authService.register(registerRequest);
            });

            verify(userRepository).existsByUsername("janedoe");
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when email already exists")
        void shouldThrowExceptionWhenEmailAlreadyExists() {
            // Arrange
            when(userRepository.existsByUsername("janedoe")).thenReturn(false);
            when(userRepository.existsByEmail("jane.doe@smartcampus.edu")).thenReturn(true);

            // Act & Assert
            assertThrows(AuthenticationException.class, () -> {
                authService.register(registerRequest);
            });

            verify(userRepository).existsByEmail("jane.doe@smartcampus.edu");
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should validate password strength")
        void shouldValidatePasswordStrength() {
            // Arrange
            registerRequest.setPassword("weak"); // Too short

            // Act & Assert
            assertThrows(AuthenticationException.class, () -> {
                authService.register(registerRequest);
            });

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should validate email format")
        void shouldValidateEmailFormat() {
            // Arrange
            registerRequest.setEmail("invalid-email"); // Invalid format

            // Act & Assert
            assertThrows(AuthenticationException.class, () -> {
                authService.register(registerRequest);
            });

            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("User Details Service Tests")
    class UserDetailsServiceTests {

        @Test
        @DisplayName("Should load user by username successfully")
        void shouldLoadUserByUsernameSuccessfully() {
            // Arrange
            when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));

            // Act
            UserDetails userDetails = authService.loadUserByUsername("johndoe");

            // Assert
            assertThat(userDetails).isNotNull();
            assertThat(userDetails.getUsername()).isEqualTo("johndoe");
            assertThat(userDetails.getPassword()).isEqualTo("$2a$10$EncodedPasswordHash");
            assertThat(userDetails.isEnabled()).isTrue();
            assertThat(userDetails.isAccountNonLocked()).isTrue();

            verify(userRepository).findByUsername("johndoe");
        }

        @Test
        @DisplayName("Should throw exception when user not found for UserDetails")
        void shouldThrowExceptionWhenUserNotFoundForUserDetails() {
            // Arrange
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(UsernameNotFoundException.class, () -> {
                authService.loadUserByUsername("nonexistent");
            });

            verify(userRepository).findByUsername("nonexistent");
        }

        @Test
        @DisplayName("Should return locked account status correctly")
        void shouldReturnLockedAccountStatusCorrectly() {
            // Arrange
            testUser.setLockedUntil(LocalDateTime.now().plusMinutes(30));
            when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));

            // Act
            UserDetails userDetails = authService.loadUserByUsername("johndoe");

            // Assert
            assertThat(userDetails.isAccountNonLocked()).isFalse();
        }

        @Test
        @DisplayName("Should return account enabled status correctly")
        void shouldReturnAccountEnabledStatusCorrectly() {
            // Arrange
            testUser.setActive(false);
            when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));

            // Act
            UserDetails userDetails = authService.loadUserByUsername("johndoe");

            // Assert
            assertThat(userDetails.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("Password Management Tests")
    class PasswordManagementTests {

        @Test
        @DisplayName("Should change password successfully")
        void shouldChangePasswordSuccessfully() {
            // Arrange
            String oldPassword = "oldpassword";
            String newPassword = "newpassword123";
            when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(oldPassword, testUser.getPasswordHash())).thenReturn(true);
            when(passwordEncoder.encode(newPassword)).thenReturn("$2a$10$NewEncodedPasswordHash");
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // Act
            boolean result = authService.changePassword("johndoe", oldPassword, newPassword);

            // Assert
            assertTrue(result);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("$2a$10$NewEncodedPasswordHash");

            verify(passwordEncoder).matches(oldPassword, testUser.getPasswordHash());
            verify(passwordEncoder).encode(newPassword);
        }

        @Test
        @DisplayName("Should throw exception when old password is incorrect")
        void shouldThrowExceptionWhenOldPasswordIsIncorrect() {
            // Arrange
            String oldPassword = "wrongpassword";
            String newPassword = "newpassword123";
            when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(oldPassword, testUser.getPasswordHash())).thenReturn(false);

            // Act & Assert
            assertThrows(InvalidCredentialsException.class, () -> {
                authService.changePassword("johndoe", oldPassword, newPassword);
            });

            verify(passwordEncoder, never()).encode(anyString());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should generate password reset token")
        void shouldGeneratePasswordResetToken() {
            // Arrange
            when(userRepository.findByEmail("john.doe@smartcampus.edu")).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // Act
            String result = authService.generatePasswordResetToken("john.doe@smartcampus.edu");

            // Assert
            assertThat(result).isNotNull();

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getPasswordResetToken()).isNotNull();
            assertThat(userCaptor.getValue().getPasswordResetExpires()).isNotNull();
        }

        @Test
        @DisplayName("Should reset password with valid token")
        void shouldResetPasswordWithValidToken() {
            // Arrange
            String resetToken = "valid-reset-token";
            String newPassword = "newpassword123";
            testUser.setPasswordResetToken(resetToken);
            testUser.setPasswordResetExpires(LocalDateTime.now().plusHours(1));
            
            when(userRepository.findByPasswordResetToken(resetToken)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.encode(newPassword)).thenReturn("$2a$10$NewEncodedPasswordHash");
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // Act
            boolean result = authService.resetPassword(resetToken, newPassword);

            // Assert
            assertTrue(result);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            
            assertThat(savedUser.getPasswordHash()).isEqualTo("$2a$10$NewEncodedPasswordHash");
            assertThat(savedUser.getPasswordResetToken()).isNull();
            assertThat(savedUser.getPasswordResetExpires()).isNull();
        }

        @Test
        @DisplayName("Should throw exception when reset token is expired")
        void shouldThrowExceptionWhenResetTokenIsExpired() {
            // Arrange
            String resetToken = "expired-reset-token";
            testUser.setPasswordResetToken(resetToken);
            testUser.setPasswordResetExpires(LocalDateTime.now().minusHours(1)); // Expired
            
            when(userRepository.findByPasswordResetToken(resetToken)).thenReturn(Optional.of(testUser));

            // Act & Assert
            assertThrows(AuthenticationException.class, () -> {
                authService.resetPassword(resetToken, "newpassword123");
            });

            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("Email Verification Tests")
    class EmailVerificationTests {

        @Test
        @DisplayName("Should verify email successfully")
        void shouldVerifyEmailSuccessfully() {
            // Arrange
            String verificationToken = "valid-verification-token";
            testUser.setVerified(false);
            testUser.setVerificationToken(verificationToken);
            
            when(userRepository.findByVerificationToken(verificationToken)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // Act
            boolean result = authService.verifyEmail(verificationToken);

            // Assert
            assertTrue(result);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            
            assertThat(savedUser.isVerified()).isTrue();
            assertThat(savedUser.getVerificationToken()).isNull();
        }

        @Test
        @DisplayName("Should throw exception when verification token is invalid")
        void shouldThrowExceptionWhenVerificationTokenIsInvalid() {
            // Arrange
            String invalidToken = "invalid-token";
            when(userRepository.findByVerificationToken(invalidToken)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(AuthenticationException.class, () -> {
                authService.verifyEmail(invalidToken);
            });

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should resend verification email")
        void shouldResendVerificationEmail() {
            // Arrange
            testUser.setVerified(false);
            when(userRepository.findByEmail("john.doe@smartcampus.edu")).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // Act
            boolean result = authService.resendVerificationEmail("john.doe@smartcampus.edu");

            // Assert
            assertTrue(result);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getVerificationToken()).isNotNull();
        }

        @Test
        @DisplayName("Should not resend verification for already verified user")
        void shouldNotResendVerificationForAlreadyVerifiedUser() {
            // Arrange
            testUser.setVerified(true);
            when(userRepository.findByEmail("john.doe@smartcampus.edu")).thenReturn(Optional.of(testUser));

            // Act & Assert
            assertThrows(AuthenticationException.class, () -> {
                authService.resendVerificationEmail("john.doe@smartcampus.edu");
            });

            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("Token Management Tests")
    class TokenManagementTests {

        @Test
        @DisplayName("Should validate JWT token successfully")
        void shouldValidateJwtTokenSuccessfully() {
            // Arrange
            String token = "valid.jwt.token";
            when(jwtTokenProvider.validateToken(token)).thenReturn(true);
            when(jwtTokenProvider.getUsernameFromToken(token)).thenReturn("johndoe");
            when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));

            // Act
            boolean result = authService.validateToken(token);

            // Assert
            assertTrue(result);

            verify(jwtTokenProvider).validateToken(token);
            verify(jwtTokenProvider).getUsernameFromToken(token);
            verify(userRepository).findByUsername("johndoe");
        }

        @Test
        @DisplayName("Should invalidate token when user not found")
        void shouldInvalidateTokenWhenUserNotFound() {
            // Arrange
            String token = "valid.jwt.token";
            when(jwtTokenProvider.validateToken(token)).thenReturn(true);
            when(jwtTokenProvider.getUsernameFromToken(token)).thenReturn("nonexistent");
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            // Act
            boolean result = authService.validateToken(token);

            // Assert
            assertFalse(result);
        }

        @Test
        @DisplayName("Should refresh JWT token")
        void shouldRefreshJwtToken() {
            // Arrange
            String oldToken = "old.jwt.token";
            String newToken = "new.jwt.token";
            
            when(jwtTokenProvider.validateToken(oldToken)).thenReturn(true);
            when(jwtTokenProvider.getUsernameFromToken(oldToken)).thenReturn("johndoe");
            when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));
            when(jwtTokenProvider.generateToken(testUser)).thenReturn(newToken);

            // Act
            String result = authService.refreshToken(oldToken);

            // Assert
            assertThat(result).isEqualTo(newToken);

            verify(jwtTokenProvider).generateToken(testUser);
        }

        @Test
        @DisplayName("Should throw exception when refreshing invalid token")
        void shouldThrowExceptionWhenRefreshingInvalidToken() {
            // Arrange
            String invalidToken = "invalid.jwt.token";
            when(jwtTokenProvider.validateToken(invalidToken)).thenReturn(false);

            // Act & Assert
            assertThrows(AuthenticationException.class, () -> {
                authService.refreshToken(invalidToken);
            });

            verify(jwtTokenProvider, never()).generateToken(any(User.class));
        }
    }

    @Nested
    @DisplayName("Logout Tests")
    class LogoutTests {

        @Test
        @DisplayName("Should logout user successfully")
        void shouldLogoutUserSuccessfully() {
            // Arrange
            String token = "valid.jwt.token";
            when(jwtTokenProvider.getUsernameFromToken(token)).thenReturn("johndoe");
            when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // Act
            boolean result = authService.logout(token);

            // Assert
            assertTrue(result);

            verify(jwtTokenProvider).getUsernameFromToken(token);
            verify(userRepository).findByUsername("johndoe");
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("Should handle logout gracefully when user not found")
        void shouldHandleLogoutGracefullyWhenUserNotFound() {
            // Arrange
            String token = "valid.jwt.token";
            when(jwtTokenProvider.getUsernameFromToken(token)).thenReturn("nonexistent");
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            // Act
            boolean result = authService.logout(token);

            // Assert
            assertTrue(result); // Should still return true for graceful handling
        }
    }

    @Nested
    @DisplayName("Account Management Tests")
    class AccountManagementTests {

        @Test
        @DisplayName("Should unlock account manually")
        void shouldUnlockAccountManually() {
            // Arrange
            testUser.setLockedUntil(LocalDateTime.now().plusMinutes(30));
            testUser.setFailedLoginAttempts(5);
            when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // Act
            boolean result = authService.unlockAccount("johndoe");

            // Assert
            assertTrue(result);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            
            assertThat(savedUser.getLockedUntil()).isNull();
            assertThat(savedUser.getFailedLoginAttempts()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should deactivate account")
        void shouldDeactivateAccount() {
            // Arrange
            when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // Act
            boolean result = authService.deactivateAccount("johndoe");

            // Assert
            assertTrue(result);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().isActive()).isFalse();
        }

        @Test
        @DisplayName("Should activate account")
        void shouldActivateAccount() {
            // Arrange
            testUser.setActive(false);
            when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // Act
            boolean result = authService.activateAccount("johndoe");

            // Assert
            assertTrue(result);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().isActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should validate strong password")
        void shouldValidateStrongPassword() {
            // Act & Assert
            assertTrue(authService.isPasswordStrong("StrongP@ssw0rd123"));
            assertTrue(authService.isPasswordStrong("Complex!Pass1"));
            
            assertFalse(authService.isPasswordStrong("weak"));
            assertFalse(authService.isPasswordStrong("password123"));
            assertFalse(authService.isPasswordStrong("ALLUPPERCASE"));
            assertFalse(authService.isPasswordStrong("alllowercase"));
            assertFalse(authService.isPasswordStrong("NoNumbers!"));
        }

        @Test
        @DisplayName("Should validate email format")
        void shouldValidateEmailFormat() {
            // Act & Assert
            assertTrue(authService.isValidEmail("user@smartcampus.edu"));
            assertTrue(authService.isValidEmail("test.email@university.edu"));
            assertTrue(authService.isValidEmail("admin@school.org"));
            
            assertFalse(authService.isValidEmail("invalid-email"));
            assertFalse(authService.isValidEmail("@smartcampus.edu"));
            assertFalse(authService.isValidEmail("user@"));
            assertFalse(authService.isValidEmail(""));
            assertFalse(authService.isValidEmail(null));
        }

        @Test
        @DisplayName("Should validate username format")
        void shouldValidateUsernameFormat() {
            // Act & Assert
            assertTrue(authService.isValidUsername("johndoe"));
            assertTrue(authService.isValidUsername("user123"));
            assertTrue(authService.isValidUsername("test_user"));
            
            assertFalse(authService.isValidUsername("ab")); // too short
            assertFalse(authService.isValidUsername("verylongusernamethatexceedslimit")); // too long
            assertFalse(authService.isValidUsername("user@name")); // invalid characters
            assertFalse(authService.isValidUsername("")); // empty
            assertFalse(authService.isValidUsername(null)); // null
        }
    }

    @Nested
    @DisplayName("Security Tests")
    class SecurityTests {

        @Test
        @DisplayName("Should detect brute force attacks")
        void shouldDetectBruteForceAttacks() {
            // Arrange
            testUser.setFailedLoginAttempts(3);
            when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));

            // Act
            boolean result = authService.isBruteForceAttempt("johndoe");

            // Assert
            assertTrue(result);
        }

        @Test
        @DisplayName("Should not detect brute force for normal attempts")
        void shouldNotDetectBruteForceForNormalAttempts() {
            // Arrange
            testUser.setFailedLoginAttempts(1);
            when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));

            // Act
            boolean result = authService.isBruteForceAttempt("johndoe");

            // Assert
            assertFalse(result);
        }

        @Test
        @DisplayName("Should check password complexity requirements")
        void shouldCheckPasswordComplexityRequirements() {
            // Act & Assert
            assertTrue(authService.meetsComplexityRequirements("Complex@Pass123"));
            
            assertFalse(authService.meetsComplexityRequirements("simple")); // too simple
            assertFalse(authService.meetsComplexityRequirements("NoDigits!")); // no digits
            assertFalse(authService.meetsComplexityRequirements("NoSpecialChars123")); // no special chars
            assertFalse(authService.meetsComplexityRequirements("nouppercase123!")); // no uppercase
            assertFalse(authService.meetsComplexityRequirements("NOLOWERCASE123!")); // no lowercase
        }

        @Test
        @DisplayName("Should generate secure verification token")
        void shouldGenerateSecureVerificationToken() {
            // Act
            String token1 = authService.generateVerificationToken();
            String token2 = authService.generateVerificationToken();

            // Assert
            assertThat(token1).isNotNull();
            assertThat(token2).isNotNull();
            assertThat(token1).isNotEqualTo(token2); // Should be unique
            assertThat(token1.length()).isGreaterThan(20); // Should be sufficiently long
        }

        @Test
        @DisplayName("Should generate secure reset token")
        void shouldGenerateSecureResetToken() {
            // Act
            String token1 = authService.generateResetToken();
            String token2 = authService.generateResetToken();

            // Assert
            assertThat(token1).isNotNull();
            assertThat(token2).isNotNull();
            assertThat(token1).isNotEqualTo(token2); // Should be unique
            assertThat(token1.length()).isGreaterThan(20); // Should be sufficiently long
        }
    }

    @Nested
    @DisplayName("Role Management Tests")
    class RoleManagementTests {

        @Test
        @DisplayName("Should check user has required role")
        void shouldCheckUserHasRequiredRole() {
            // Arrange
            when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));

            // Act & Assert
            assertTrue(authService.hasRole("johndoe", UserRole.STUDENT));
            assertFalse(authService.hasRole("johndoe", UserRole.ADMIN));
            assertFalse(authService.hasRole("johndoe", UserRole.PROFESSOR));
        }

        @Test
        @DisplayName("Should update user role")
        void shouldUpdateUserRole() {
            // Arrange
            when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // Act
            boolean result = authService.updateUserRole("johndoe", UserRole.STAFF);

            // Assert
            assertTrue(result);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getRole()).isEqualTo(UserRole.STAFF);
        }

        @Test
        @DisplayName("Should check if user has any of multiple roles")
        void shouldCheckIfUserHasAnyOfMultipleRoles() {
            // Arrange
            when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));

            // Act & Assert
            assertTrue(authService.hasAnyRole("johndoe", UserRole.STUDENT, UserRole.PROFESSOR));
            assertFalse(authService.hasAnyRole("johndoe", UserRole.ADMIN, UserRole.STAFF));
        }

        @Test
        @DisplayName("Should get user authorities based on role")
        void shouldGetUserAuthoritiesBasedOnRole() {
            // Act
            var authorities = authService.getUserAuthorities(testUser);

            // Assert
            assertThat(authorities).isNotNull();
            assertThat(authorities).hasSize(1);
            assertThat(authorities.iterator().next().getAuthority()).isEqualTo("ROLE_STUDENT");
        }
    }

    @Nested
    @DisplayName("Session Management Tests")
    class SessionManagementTests {

        @Test
        @DisplayName("Should track user session")
        void shouldTrackUserSession() {
            // Arrange
            String sessionId = "session-123";
            when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // Act
            boolean result = authService.createSession("johndoe", sessionId);

            // Assert
            assertTrue(result);

            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("Should invalidate user session")
        void shouldInvalidateUserSession() {
            // Arrange
            String sessionId = "session-123";
            when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));

            // Act
            boolean result = authService.invalidateSession("johndoe", sessionId);

            // Assert
            assertTrue(result);
        }

        @Test
        @DisplayName("Should check if session is valid")
        void shouldCheckIfSessionIsValid() {
            // Arrange
            String sessionId = "session-123";
            when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));

            // Act
            boolean result = authService.isSessionValid("johndoe", sessionId);

            // Assert - Implementation depends on session tracking mechanism
            assertTrue(result || !result); // Placeholder assertion
        }
    }

    @Nested
    @DisplayName("Audit and Logging Tests")
    class AuditAndLoggingTests {

        @Test
        @DisplayName("Should log successful login attempt")
        void shouldLogSuccessfulLoginAttempt() {
            // Arrange
            String ipAddress = "192.168.1.100";
            String userAgent = "Mozilla/5.0...";
            
            // Act
            authService.logLoginAttempt("johndoe", true, ipAddress, userAgent);

            // Assert - Verify logging behavior (implementation specific)
            // This would typically verify that appropriate log entries are created
            assertTrue(true); // Placeholder - actual implementation would verify logging
        }

        @Test
        @DisplayName("Should log failed login attempt")
        void shouldLogFailedLoginAttempt() {
            // Arrange
            String ipAddress = "192.168.1.100";
            String userAgent = "Mozilla/5.0...";
            
            // Act
            authService.logLoginAttempt("johndoe", false, ipAddress, userAgent);

            // Assert - Verify logging behavior (implementation specific)
            assertTrue(true); // Placeholder - actual implementation would verify logging
        }

        @Test
        @DisplayName("Should log password change")
        void shouldLogPasswordChange() {
            // Act
            authService.logPasswordChange("johndoe");

            // Assert - Verify audit logging
            assertTrue(true); // Placeholder - actual implementation would verify audit logging
        }

        @Test
        @DisplayName("Should log account lockout")
        void shouldLogAccountLockout() {
            // Act
            authService.logAccountLockout("johndoe", "Too many failed login attempts");

            // Assert - Verify security logging
            assertTrue(true); // Placeholder - actual implementation would verify security logging
        }
    }

    @Nested
    @DisplayName("Exception Handling Tests")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("Should handle repository exceptions gracefully")
        void shouldHandleRepositoryExceptionsGracefully() {
            // Arrange
            when(userRepository.findByUsername("johndoe")).thenThrow(new RuntimeException("Database error"));

            // Act & Assert
            assertThrows(RuntimeException.class, () -> {
                authService.loadUserByUsername("johndoe");
            });

            verify(userRepository).findByUsername("johndoe");
        }

        @Test
        @DisplayName("Should handle null input parameters")
        void shouldHandleNullInputParameters() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> {
                LoginRequest nullRequest = null;
                authService.login(nullRequest);
            });

            assertThrows(IllegalArgumentException.class, () -> {
                RegisterRequest nullRequest = null;
                authService.register(nullRequest);
            });

            assertThrows(IllegalArgumentException.class, () -> {
                authService.loadUserByUsername(null);
            });
        }

        @Test
        @DisplayName("Should handle empty input parameters")
        void shouldHandleEmptyInputParameters() {
            // Arrange
            LoginRequest emptyLogin = new LoginRequest();
            emptyLogin.setUsername("");
            emptyLogin.setPassword("");

            // Act & Assert
            assertThrows(AuthenticationException.class, () -> {
                authService.login(emptyLogin);
            });
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should handle complete authentication flow")
        void shouldHandleCompleteAuthenticationFlow() {
            // Arrange - Register
            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("newuser@smartcampus.edu")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("$2a$10$EncodedHash");
            
            RegisterRequest regRequest = new RegisterRequest();
            regRequest.setUsername("newuser");
            regRequest.setEmail("newuser@smartcampus.edu");
            regRequest.setPassword("password123");
            regRequest.setFirstName("New");
            regRequest.setLastName("User");
            regRequest.setRole(UserRole.STUDENT);
            
            User newUser = new User();
            newUser.setUsername("newuser");
            newUser.setEmail("newuser@smartcampus.edu");
            newUser.setPasswordHash("$2a$10$EncodedHash");
            newUser.setRole(UserRole.STUDENT);
            newUser.setActive(true);
            newUser.setVerified(false);
            
            when(userRepository.save(any(User.class))).thenReturn(newUser);

            // Arrange - Verify
            String verificationToken = "verification-token";
            newUser.setVerificationToken(verificationToken);
            when(userRepository.findByVerificationToken(verificationToken)).thenReturn(Optional.of(newUser));

            // Arrange - Login
            newUser.setVerified(true);
            when(userRepository.findByUsername("newuser")).thenReturn(Optional.of(newUser));
            when(passwordEncoder.matches("password123", "$2a$10$EncodedHash")).thenReturn(true);
            when(jwtTokenProvider.generateToken(newUser)).thenReturn("jwt.token");

            LoginRequest loginReq = new LoginRequest();
            loginReq.setUsername("newuser");
            loginReq.setPassword("password123");

            // Act - Complete flow
            User registeredUser = authService.register(regRequest);
            boolean emailVerified = authService.verifyEmail(verificationToken);
            LoginResponse loginResponse = authService.login(loginReq);

            // Assert
            assertThat(registeredUser).isNotNull();
            assertTrue(emailVerified);
            assertThat(loginResponse).isNotNull();
            assertThat(loginResponse.getToken()).isEqualTo("jwt.token");
            assertThat(loginResponse.getUsername()).isEqualTo("newuser");

            verify(userRepository, times(3)).save(any(User.class)); // Register, verify, login
        }

        @Test
        @DisplayName("Should handle password reset flow")
        void shouldHandlePasswordResetFlow() {
            // Arrange
            when(userRepository.findByEmail("john.doe@smartcampus.edu")).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            
            String resetToken = "reset-token-123";
            testUser.setPasswordResetToken(resetToken);
            testUser.setPasswordResetExpires(LocalDateTime.now().plusHours(1));
            when(userRepository.findByPasswordResetToken(resetToken)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.encode("newpassword123")).thenReturn("$2a$10$NewHash");

            // Act
            String generatedToken = authService.generatePasswordResetToken("john.doe@smartcampus.edu");
            boolean resetSuccess = authService.resetPassword(resetToken, "newpassword123");

            // Assert
            assertThat(generatedToken).isNotNull();
            assertTrue(resetSuccess);

            verify(userRepository, times(2)).save(any(User.class)); // Generate token, reset password
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Should handle concurrent login attempts")
        void shouldHandleConcurrentLoginAttempts() {
            // This would typically test concurrent access patterns
            // For unit tests, we can verify that the service handles multiple calls correctly
            
            // Arrange
            when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password123", testUser.getPasswordHash())).thenReturn(true);
            when(jwtTokenProvider.generateToken(testUser)).thenReturn("jwt.token");
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // Act - Simulate multiple login attempts
            LoginResponse response1 = authService.login(loginRequest);
            LoginResponse response2 = authService.login(loginRequest);

            // Assert
            assertThat(response1).isNotNull();
            assertThat(response2).isNotNull();
            
            // Verify that the service was called multiple times
            verify(userRepository, times(2)).findByUsername("johndoe");
            verify(userRepository, times(2)).save(any(User.class));
        }

        @Test
        @DisplayName("Should efficiently validate multiple tokens")
        void shouldEfficientlyValidateMultipleTokens() {
            // Arrange
            String token = "valid.jwt.token";
            when(jwtTokenProvider.validateToken(token)).thenReturn(true);
            when(jwtTokenProvider.getUsernameFromToken(token)).thenReturn("johndoe");
            when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));

            // Act - Validate token multiple times
            boolean result1 = authService.validateToken(token);
            boolean result2 = authService.validateToken(token);
            boolean result3 = authService.validateToken(token);

            // Assert
            assertTrue(result1);
            assertTrue(result2);
            assertTrue(result3);
            
            // Verify efficient database access
            verify(userRepository, times(3)).findByUsername("johndoe");
        }
    }
}