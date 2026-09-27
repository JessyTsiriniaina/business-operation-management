package io.jessytsiriniaina.internalrequestmanagement.service;

import io.jessytsiriniaina.internalrequestmanagement.config.JwtProperties;
import io.jessytsiriniaina.internalrequestmanagement.dto.auth.AuthResponseDto;
import io.jessytsiriniaina.internalrequestmanagement.dto.auth.LoginRequestDto;
import io.jessytsiriniaina.internalrequestmanagement.dto.auth.RegisterRequestDto;
import io.jessytsiriniaina.internalrequestmanagement.dto.user.UserResponseDto;
import io.jessytsiriniaina.internalrequestmanagement.entity.Department;
import io.jessytsiriniaina.internalrequestmanagement.entity.User;
import io.jessytsiriniaina.internalrequestmanagement.enums.UserRole;
import io.jessytsiriniaina.internalrequestmanagement.exception.BusinessException;
import io.jessytsiriniaina.internalrequestmanagement.exception.ResourceNotFoundException;
import io.jessytsiriniaina.internalrequestmanagement.mapper.UserMapper;
import io.jessytsiriniaina.internalrequestmanagement.repository.DepartmentRepository;
import io.jessytsiriniaina.internalrequestmanagement.repository.UserRepository;
import io.jessytsiriniaina.internalrequestmanagement.security.JwtTokenProvider;
import io.jessytsiriniaina.internalrequestmanagement.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final JwtProperties jwtProperties;

    public AuthService(UserRepository userRepository,
                       DepartmentRepository departmentRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider tokenProvider,
                       JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public AuthResponseDto register(RegisterRequestDto dto) {
        if (userRepository.existsByEmailIgnoreCase(dto.email())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Email already in use: " + dto.email());
        }
        Department dept = null;
        if (dto.departmentId() != null) {
            dept = departmentRepository.findById(dto.departmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + dto.departmentId()));
        }
        User user = new User(dto.firstName(), dto.lastName(), dto.email(),
                passwordEncoder.encode(dto.password()), UserRole.EMPLOYEE, dept);
        User saved = userRepository.save(user);
        UserPrincipal principal = UserPrincipal.fromEntity(saved);
        String token = tokenProvider.generateToken(principal);
        UserResponseDto userDto = UserMapper.toResponse(saved);
        return AuthResponseDto.of(token, jwtProperties.expirationMs(), userDto);
    }

    @Transactional(readOnly = true)
    public AuthResponseDto login(LoginRequestDto dto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.email(), dto.password()));
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String token = tokenProvider.generateToken(principal);
        User user = userRepository.findByEmailIgnoreCase(principal.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + principal.getUsername()));
        UserResponseDto userDto = UserMapper.toResponse(user);
        return AuthResponseDto.of(token, jwtProperties.expirationMs(), userDto);
    }
}
