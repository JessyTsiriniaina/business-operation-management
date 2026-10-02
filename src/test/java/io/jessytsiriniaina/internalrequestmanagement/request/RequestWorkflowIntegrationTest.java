package io.jessytsiriniaina.internalrequestmanagement.request;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.jessytsiriniaina.internalrequestmanagement.entity.Department;
import io.jessytsiriniaina.internalrequestmanagement.entity.RequestType;
import io.jessytsiriniaina.internalrequestmanagement.entity.User;
import io.jessytsiriniaina.internalrequestmanagement.enums.UserRole;
import io.jessytsiriniaina.internalrequestmanagement.repository.DepartmentRepository;
import io.jessytsiriniaina.internalrequestmanagement.repository.RequestTypeRepository;
import io.jessytsiriniaina.internalrequestmanagement.repository.UserRepository;
import io.jessytsiriniaina.internalrequestmanagement.security.JwtTokenProvider;
import io.jessytsiriniaina.internalrequestmanagement.security.UserPrincipal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RequestWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private RequestTypeRepository requestTypeRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtTokenProvider tokenProvider;

    private Department department;
    private RequestType requestType;
    private User employee;
    private User manager;
    private String employeeToken;
    private String managerToken;

    @BeforeEach
    void setUp() {
        department = departmentRepository.findByNameIgnoreCase("IT").orElseThrow();
        requestType = requestTypeRepository.findAll().stream()
                .filter(t -> t.getName().equals("LEAVE"))
                .findFirst()
                .orElseThrow();

        employee = userRepository.save(new User("Emp", "Loyee",
                "emp-" + UUID.randomUUID() + "@local.test",
                passwordEncoder.encode("password123"), UserRole.EMPLOYEE, department));
        manager = userRepository.save(new User("Man", "Ager",
                "mgr-" + UUID.randomUUID() + "@local.test",
                passwordEncoder.encode("password123"), UserRole.MANAGER, department));

        employeeToken = tokenProvider.generateToken(UserPrincipal.fromEntity(employee));
        managerToken = tokenProvider.generateToken(UserPrincipal.fromEntity(manager));
    }

    @Test
    void createRequest_asEmployee_returnsCreated() throws Exception {
        mockMvc.perform(post("/api/requests")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Leave request","description":"Annual leave","priority":"MEDIUM","typeId":%d}
                                """.formatted(requestType.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Leave request"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void listRequests_employeeSeesOnlyOwn() throws Exception {
        createRequest(employeeToken, "Mine");
        createRequest(managerToken, "Managers");

        mockMvc.perform(get("/api/requests")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Mine"));
    }

    @Test
    void approve_asEmployee_returnsForbidden() throws Exception {
        Long id = createRequest(employeeToken, "To approve");

        mockMvc.perform(patch("/api/requests/{id}/approve", id)
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void approve_pendingAsManager_returnsConflict() throws Exception {
        Long id = createRequest(employeeToken, "Still pending");

        mockMvc.perform(patch("/api/requests/{id}/approve", id)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isConflict());
    }

    @Test
    void approve_afterStartProgress_returnsOk() throws Exception {
        Long id = createRequest(employeeToken, "Ready");

        mockMvc.perform(patch("/api/requests/{id}/start-progress", id)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(patch("/api/requests/{id}/approve", id)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void reject_afterStartProgress_returnsOk() throws Exception {
        Long id = createRequest(employeeToken, "To reject");

        mockMvc.perform(patch("/api/requests/{id}/start-progress", id)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/requests/{id}/reject", id)
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"No budget\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    private Long createRequest(String token, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/requests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"%s","description":"Annual leave","priority":"MEDIUM","typeId":%d}
                                """.formatted(title, requestType.getId())))
                .andExpect(status().isCreated())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.parse(body).read("$.id", Long.class);
    }
}
