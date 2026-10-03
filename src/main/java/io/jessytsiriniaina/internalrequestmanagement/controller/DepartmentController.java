package io.jessytsiriniaina.internalrequestmanagement.controller;

import io.jessytsiriniaina.internalrequestmanagement.dto.department.CreateDepartmentDto;
import io.jessytsiriniaina.internalrequestmanagement.dto.department.DepartmentResponseDto;
import io.jessytsiriniaina.internalrequestmanagement.dto.department.UpdateDepartmentDto;
import io.jessytsiriniaina.internalrequestmanagement.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/departments")
@Tag(name = "Departments", description = "Department directory. Reads are public; writes need MANAGER/ADMIN.")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create department", description = "MANAGER or ADMIN only.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Department created.",
                    content = @Content(schema = @Schema(implementation = DepartmentResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation failure or malformed JSON."),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT."),
            @ApiResponse(responseCode = "403", description = "EMPLOYEE caller."),
            @ApiResponse(responseCode = "409", description = "Department name already exists.")
    })
    public ResponseEntity<DepartmentResponseDto> create(@Valid @RequestBody CreateDepartmentDto dto) {
        DepartmentResponseDto created = departmentService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @SecurityRequirements
    @Operation(summary = "List departments", description = "Public. Paginated, sorted by name by default.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of departments.")
    })
    public ResponseEntity<Page<DepartmentResponseDto>> findAll(
            @ParameterObject @PageableDefault(size = 10, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(departmentService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @SecurityRequirements
    @Operation(summary = "Get department by id", description = "Public.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Department found.",
                    content = @Content(schema = @Schema(implementation = DepartmentResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Department not found.")
    })
    public ResponseEntity<DepartmentResponseDto> findById(
            @Parameter(description = "Department id", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(departmentService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update department", description = "MANAGER or ADMIN only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Department updated.",
                    content = @Content(schema = @Schema(implementation = DepartmentResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation failure or malformed JSON."),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT."),
            @ApiResponse(responseCode = "403", description = "EMPLOYEE caller."),
            @ApiResponse(responseCode = "404", description = "Department not found."),
            @ApiResponse(responseCode = "409", description = "Department name already exists.")
    })
    public ResponseEntity<DepartmentResponseDto> update(
            @Parameter(description = "Department id", example = "1") @PathVariable Long id,
            @Valid @RequestBody UpdateDepartmentDto dto) {
        return ResponseEntity.ok(departmentService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete department", description = "ADMIN only.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Department deleted."),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT."),
            @ApiResponse(responseCode = "403", description = "Non-ADMIN caller."),
            @ApiResponse(responseCode = "404", description = "Department not found.")
    })
    public ResponseEntity<Void> delete(@Parameter(description = "Department id", example = "1") @PathVariable Long id) {
        departmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
