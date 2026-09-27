package io.jessytsiriniaina.internalrequestmanagement.service;

import io.jessytsiriniaina.internalrequestmanagement.dto.user.CreateUserDto;
import io.jessytsiriniaina.internalrequestmanagement.dto.user.UpdateUserDto;
import io.jessytsiriniaina.internalrequestmanagement.dto.user.UserResponseDto;
import io.jessytsiriniaina.internalrequestmanagement.entity.Department;
import io.jessytsiriniaina.internalrequestmanagement.entity.User;
import io.jessytsiriniaina.internalrequestmanagement.enums.UserRole;
import io.jessytsiriniaina.internalrequestmanagement.exception.BusinessException;
import io.jessytsiriniaina.internalrequestmanagement.exception.ResourceNotFoundException;
import io.jessytsiriniaina.internalrequestmanagement.mapper.UserMapper;
import io.jessytsiriniaina.internalrequestmanagement.repository.DepartmentRepository;
import io.jessytsiriniaina.internalrequestmanagement.repository.UserRepository;
import io.jessytsiriniaina.internalrequestmanagement.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, DepartmentRepository departmentRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserResponseDto getMe(UserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + principal.getId()));
        return UserMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public Page<UserResponseDto> findAll(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponseDto findById(Long id, UserPrincipal principal) {
        if (principal != null
                && principal.getRole() != UserRole.ADMIN
                && principal.getRole() != UserRole.MANAGER
                && !id.equals(principal.getId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "You can only view your own user profile");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        return UserMapper.toResponse(user);
    }

    public UserResponseDto create(CreateUserDto dto) {
        if (userRepository.existsByEmailIgnoreCase(dto.email())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Email already in use: " + dto.email());
        }
        Department dept = null;
        if (dto.departmentId() != null) {
            dept = departmentRepository.findById(dto.departmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + dto.departmentId()));
        }
        User user = new User(dto.firstName(), dto.lastName(), dto.email(),
                passwordEncoder.encode(dto.password()), dto.role(), dept);
        User saved = userRepository.save(user);
        return UserMapper.toResponse(saved);
    }

    public UserResponseDto update(Long id, UpdateUserDto dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));

        if (dto.email() != null && !dto.email().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmailIgnoreCase(dto.email())) {
                throw new BusinessException(HttpStatus.CONFLICT, "Email already in use: " + dto.email());
            }
            user.setEmail(dto.email());
        }
        if (dto.firstName() != null) user.setFirstName(dto.firstName());
        if (dto.lastName() != null) user.setLastName(dto.lastName());
        if (dto.role() != null) user.setRole(dto.role());
        if (dto.departmentId() != null) {
            Department dept = departmentRepository.findById(dto.departmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + dto.departmentId()));
            user.setDepartment(dept);
        } else if (dto.departmentId() == null && dto.email() == null && dto.firstName() == null && dto.lastName() == null && dto.role() == null) {
            // no-op, keep department
        }
        // Allow clearing department if explicitly passed as null? For now, if dto.departmentId is null and other fields present, keep existing.
        User saved = userRepository.save(user);
        return UserMapper.toResponse(saved);
    }

    public void delete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        userRepository.delete(user);
    }
}
