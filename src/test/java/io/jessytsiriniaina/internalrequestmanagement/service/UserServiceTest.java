package io.jessytsiriniaina.internalrequestmanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.jessytsiriniaina.internalrequestmanagement.dto.user.CreateUserDto;
import io.jessytsiriniaina.internalrequestmanagement.dto.user.UpdateUserDto;
import io.jessytsiriniaina.internalrequestmanagement.entity.Department;
import io.jessytsiriniaina.internalrequestmanagement.entity.User;
import io.jessytsiriniaina.internalrequestmanagement.enums.UserRole;
import io.jessytsiriniaina.internalrequestmanagement.exception.BusinessException;
import io.jessytsiriniaina.internalrequestmanagement.exception.ResourceNotFoundException;
import io.jessytsiriniaina.internalrequestmanagement.repository.DepartmentRepository;
import io.jessytsiriniaina.internalrequestmanagement.repository.UserRepository;
import io.jessytsiriniaina.internalrequestmanagement.security.UserPrincipal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, departmentRepository, passwordEncoder);
    }

    @Test
    void findById_employeeReadingOther_throwsForbidden() {
        UserPrincipal self = principal(1L, "self@local.test", UserRole.EMPLOYEE, null);

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.findById(9L, self));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void findById_self_ok() {
        User self = user(1L, "self@local.test", UserRole.EMPLOYEE, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(self));

        var response = userService.findById(1L, principal(1L, "self@local.test", UserRole.EMPLOYEE, null));
        assertEquals("self@local.test", response.email());
    }

    @Test
    void findById_managerReadingOther_ok() {
        User other = user(9L, "other@local.test", UserRole.EMPLOYEE, null);
        when(userRepository.findById(9L)).thenReturn(Optional.of(other));

        var response = userService.findById(9L, principal(2L, "manager@local.test", UserRole.MANAGER, 1L));
        assertEquals("other@local.test", response.email());
    }

    @Test
    void findById_adminReadingOther_ok() {
        User other = user(9L, "other@local.test", UserRole.EMPLOYEE, null);
        when(userRepository.findById(9L)).thenReturn(Optional.of(other));

        var response = userService.findById(9L, principal(3L, "admin@local.test", UserRole.ADMIN, null));
        assertEquals("other@local.test", response.email());
    }

    @Test
    void findById_unknown_throwsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.findById(99L, principal(3L, "admin@local.test", UserRole.ADMIN, null)));
    }

    @Test
    void create_duplicateEmail_throwsConflict() {
        when(userRepository.existsByEmailIgnoreCase("dup@local.test")).thenReturn(true);

        CreateUserDto dto = new CreateUserDto("A", "B", "dup@local.test", "password123", UserRole.EMPLOYEE, null);

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.create(dto));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void create_ok() {
        when(userRepository.existsByEmailIgnoreCase("new@local.test")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User saved = inv.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 7L);
            return saved;
        });

        CreateUserDto dto = new CreateUserDto("New", "User", "new@local.test", "password123", UserRole.EMPLOYEE, null);

        var response = userService.create(dto);
        assertEquals("new@local.test", response.email());
    }

    @Test
    void update_duplicateEmail_throwsConflict() {
        User existing = user(1L, "old@local.test", UserRole.EMPLOYEE, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmailIgnoreCase("taken@local.test")).thenReturn(true);

        UpdateUserDto dto = new UpdateUserDto(null, null, "taken@local.test", null, null);

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.update(1L, dto));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    private static User user(Long id, String email, UserRole role, Department department) {
        User user = new User("Test", "User", email, "encoded-password", role, department);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static UserPrincipal principal(Long id, String email, UserRole role, Long departmentId) {
        return new UserPrincipal(id, email, "encoded-password", role, departmentId,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
    }
}
