package io.jessytsiriniaina.internalrequestmanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.jessytsiriniaina.internalrequestmanagement.dto.department.CreateDepartmentDto;
import io.jessytsiriniaina.internalrequestmanagement.dto.department.UpdateDepartmentDto;
import io.jessytsiriniaina.internalrequestmanagement.entity.Department;
import io.jessytsiriniaina.internalrequestmanagement.exception.BusinessException;
import io.jessytsiriniaina.internalrequestmanagement.exception.ResourceNotFoundException;
import io.jessytsiriniaina.internalrequestmanagement.repository.DepartmentRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    private DepartmentService departmentService;

    @BeforeEach
    void setUp() {
        departmentService = new DepartmentService(departmentRepository);
    }

    @Test
    void create_duplicate_throwsConflict() {
        when(departmentRepository.existsByNameIgnoreCase("Finance")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> departmentService.create(new CreateDepartmentDto("Finance", "desc")));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void create_ok() {
        when(departmentRepository.existsByNameIgnoreCase("NewDept")).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenAnswer(inv -> {
            Department saved = inv.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 5L);
            return saved;
        });

        var response = departmentService.create(new CreateDepartmentDto("NewDept", "desc"));
        assertEquals("NewDept", response.name());
    }

    @Test
    void update_toExistingName_throwsConflict() {
        Department dept = department(1L, "Old");
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(dept));
        when(departmentRepository.existsByNameIgnoreCaseAndIdNot("Finance", 1L)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> departmentService.update(1L, new UpdateDepartmentDto("Finance", "desc")));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void findById_unknown_throwsNotFound() {
        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> departmentService.findById(99L));
    }

    private static Department department(Long id, String name) {
        Department department = new Department(name, "desc");
        ReflectionTestUtils.setField(department, "id", id);
        return department;
    }
}
