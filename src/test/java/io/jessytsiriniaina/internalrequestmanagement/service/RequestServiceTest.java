package io.jessytsiriniaina.internalrequestmanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jessytsiriniaina.internalrequestmanagement.dto.request.AssignRequestDto;
import io.jessytsiriniaina.internalrequestmanagement.dto.request.CancelRequestDto;
import io.jessytsiriniaina.internalrequestmanagement.dto.request.CreateRequestDto;
import io.jessytsiriniaina.internalrequestmanagement.dto.request.RejectRequestDto;
import io.jessytsiriniaina.internalrequestmanagement.dto.request.UpdateRequestDto;
import io.jessytsiriniaina.internalrequestmanagement.entity.Department;
import io.jessytsiriniaina.internalrequestmanagement.entity.Request;
import io.jessytsiriniaina.internalrequestmanagement.entity.RequestType;
import io.jessytsiriniaina.internalrequestmanagement.entity.User;
import io.jessytsiriniaina.internalrequestmanagement.enums.RequestPriority;
import io.jessytsiriniaina.internalrequestmanagement.enums.RequestStatus;
import io.jessytsiriniaina.internalrequestmanagement.enums.UserRole;
import io.jessytsiriniaina.internalrequestmanagement.exception.BusinessException;
import io.jessytsiriniaina.internalrequestmanagement.repository.RequestCommentRepository;
import io.jessytsiriniaina.internalrequestmanagement.repository.RequestRepository;
import io.jessytsiriniaina.internalrequestmanagement.repository.RequestTypeRepository;
import io.jessytsiriniaina.internalrequestmanagement.repository.UserRepository;
import io.jessytsiriniaina.internalrequestmanagement.security.UserPrincipal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RequestServiceTest {

    @Mock
    private RequestRepository requestRepository;
    @Mock
    private RequestTypeRepository requestTypeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RequestCommentRepository requestCommentRepository;

    private RequestService requestService;

    @BeforeEach
    void setUp() {
        requestService = new RequestService(requestRepository, requestTypeRepository, userRepository, requestCommentRepository);
    }

    @Test
    void create_urgentWithoutDescription_throwsBadRequest() {
        User creator = user(1L, "emp@local.test", UserRole.EMPLOYEE, null);
        UserPrincipal principal = principal(creator);

        CreateRequestDto dto = new CreateRequestDto("Title", "  ", RequestPriority.URGENT, 1L, null, null);

        BusinessException ex = assertThrows(BusinessException.class, () -> requestService.create(dto, principal));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(requestRepository, never()).save(any());
    }

    @Test
    void create_urgentWithJustification_ok() {
        User creator = user(1L, "emp@local.test", UserRole.EMPLOYEE, null);
        UserPrincipal principal = principal(creator);
        when(requestTypeRepository.findById(1L)).thenReturn(Optional.of(requestType(1L, "LEAVE")));
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(requestRepository.save(any(Request.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateRequestDto dto = new CreateRequestDto("Title", "Need laptop for onboarding", RequestPriority.URGENT, 1L, null, null);

        var response = requestService.create(dto, principal);
        assertEquals("Title", response.title());
    }

    @ParameterizedTest
    @EnumSource(value = RequestStatus.class, names = {"IN_PROGRESS", "APPROVED", "REJECTED", "CANCELLED"})
    void update_nonPending_throwsConflict(RequestStatus status) {
        User creator = user(1L, "emp@local.test", UserRole.EMPLOYEE, null);
        Request request = request(10L, status, creator);
        when(requestRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(request));

        UpdateRequestDto dto = new UpdateRequestDto("T", "D", RequestPriority.LOW, 1L, null);

        BusinessException ex = assertThrows(
                BusinessException.class, () -> requestService.update(10L, dto, principal(creator)));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void update_employeeCannotUpdateOthersRequest() {
        User owner = user(1L, "owner@local.test", UserRole.EMPLOYEE, null);
        User other = user(2L, "other@local.test", UserRole.EMPLOYEE, null);
        when(requestRepository.findByIdAndDeletedFalse(10L))
                .thenReturn(Optional.of(request(10L, RequestStatus.PENDING, owner)));

        UpdateRequestDto dto = new UpdateRequestDto("T", "D", RequestPriority.LOW, 1L, null);

        BusinessException ex =
                assertThrows(BusinessException.class, () -> requestService.update(10L, dto, principal(other)));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void startProgress_pending_ok() {
        User creator = user(1L, "emp@local.test", UserRole.EMPLOYEE, null);
        when(requestRepository.findByIdAndDeletedFalse(10L))
                .thenReturn(Optional.of(request(10L, RequestStatus.PENDING, creator)));
        when(requestRepository.save(any(Request.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = requestService.startProgress(10L);
        assertEquals(RequestStatus.IN_PROGRESS, response.status());
    }

    @ParameterizedTest
    @EnumSource(value = RequestStatus.class, names = {"IN_PROGRESS", "APPROVED", "REJECTED", "CANCELLED"})
    void startProgress_nonPending_throwsConflict(RequestStatus status) {
        User creator = user(1L, "emp@local.test", UserRole.EMPLOYEE, null);
        when(requestRepository.findByIdAndDeletedFalse(10L))
                .thenReturn(Optional.of(request(10L, status, creator)));

        BusinessException ex = assertThrows(BusinessException.class, () -> requestService.startProgress(10L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void approve_inProgress_ok() {
        User creator = user(1L, "emp@local.test", UserRole.EMPLOYEE, null);
        when(requestRepository.findByIdAndDeletedFalse(10L))
                .thenReturn(Optional.of(request(10L, RequestStatus.IN_PROGRESS, creator)));
        when(requestRepository.save(any(Request.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = requestService.approve(10L);
        assertEquals(RequestStatus.APPROVED, response.status());
    }

    @ParameterizedTest
    @EnumSource(value = RequestStatus.class, names = {"PENDING", "APPROVED", "REJECTED", "CANCELLED"})
    void approve_notInProgress_throwsConflict(RequestStatus status) {
        User creator = user(1L, "emp@local.test", UserRole.EMPLOYEE, null);
        when(requestRepository.findByIdAndDeletedFalse(10L))
                .thenReturn(Optional.of(request(10L, status, creator)));

        BusinessException ex = assertThrows(BusinessException.class, () -> requestService.approve(10L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void reject_pending_throwsConflict() {
        User creator = user(1L, "emp@local.test", UserRole.EMPLOYEE, null);
        when(requestRepository.findByIdAndDeletedFalse(10L))
                .thenReturn(Optional.of(request(10L, RequestStatus.PENDING, creator)));

        BusinessException ex = assertThrows(
                BusinessException.class, () -> requestService.reject(10L, new RejectRequestDto("no budget")));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void reject_inProgress_ok() {
        User creator = user(1L, "emp@local.test", UserRole.EMPLOYEE, null);
        when(requestRepository.findByIdAndDeletedFalse(10L))
                .thenReturn(Optional.of(request(10L, RequestStatus.IN_PROGRESS, creator)));
        when(requestRepository.save(any(Request.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = requestService.reject(10L, new RejectRequestDto("no budget"));
        assertEquals(RequestStatus.REJECTED, response.status());
        assertEquals("no budget", response.rejectionReason());
    }

    @Test
    void cancel_approved_throwsConflict() {
        User creator = user(1L, "emp@local.test", UserRole.EMPLOYEE, null);
        when(requestRepository.findByIdAndDeletedFalse(10L))
                .thenReturn(Optional.of(request(10L, RequestStatus.APPROVED, creator)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> requestService.cancel(10L, new CancelRequestDto("changed mind"), principal(creator)));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void cancel_employeeCannotCancelOthersRequest() {
        User owner = user(1L, "owner@local.test", UserRole.EMPLOYEE, null);
        User other = user(2L, "other@local.test", UserRole.EMPLOYEE, null);
        when(requestRepository.findByIdAndDeletedFalse(10L))
                .thenReturn(Optional.of(request(10L, RequestStatus.PENDING, owner)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> requestService.cancel(10L, new CancelRequestDto("changed mind"), principal(other)));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @ParameterizedTest
    @EnumSource(value = RequestStatus.class, names = {"APPROVED", "REJECTED", "CANCELLED"})
    void assign_terminalStatus_throwsConflict(RequestStatus status) {
        User creator = user(1L, "emp@local.test", UserRole.EMPLOYEE, null);
        when(requestRepository.findByIdAndDeletedFalse(10L))
                .thenReturn(Optional.of(request(10L, status, creator)));

        BusinessException ex = assertThrows(
                BusinessException.class, () -> requestService.assign(10L, new AssignRequestDto(2L)));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void delete_employeeCannotDeleteOthersRequest() {
        User owner = user(1L, "owner@local.test", UserRole.EMPLOYEE, null);
        User other = user(2L, "other@local.test", UserRole.EMPLOYEE, null);
        when(requestRepository.findByIdAndDeletedFalse(10L))
                .thenReturn(Optional.of(request(10L, RequestStatus.PENDING, owner)));

        BusinessException ex =
                assertThrows(BusinessException.class, () -> requestService.delete(10L, principal(other)));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verify(requestRepository, never()).save(any());
    }

    private static User user(Long id, String email, UserRole role, Department department) {
        User user = new User("Test", "User", email, "encoded-password", role, department);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static Department department(Long id, String name) {
        Department department = new Department(name, "desc");
        ReflectionTestUtils.setField(department, "id", id);
        return department;
    }

    private static RequestType requestType(Long id, String name) {
        RequestType type = new RequestType(name, "desc", true);
        ReflectionTestUtils.setField(type, "id", id);
        return type;
    }

    private static Request request(Long id, RequestStatus status, User creator) {
        RequestType type = requestType(1L, "LEAVE");
        Request request = new Request("Title", "Description", RequestPriority.MEDIUM, type, creator);
        request.setStatus(status);
        ReflectionTestUtils.setField(request, "id", id);
        return request;
    }

    private static UserPrincipal principal(User user) {
        Long deptId = user.getDepartment() != null ? user.getDepartment().getId() : null;
        return new UserPrincipal(user.getId(), user.getEmail(), user.getPassword(), user.getRole(), deptId,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
    }
}
