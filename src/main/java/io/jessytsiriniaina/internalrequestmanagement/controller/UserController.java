package io.jessytsiriniaina.internalrequestmanagement.controller;

import io.jessytsiriniaina.internalrequestmanagement.dto.user.CreateUserDto;
import io.jessytsiriniaina.internalrequestmanagement.dto.user.UpdateUserDto;
import io.jessytsiriniaina.internalrequestmanagement.dto.user.UserResponseDto;
import io.jessytsiriniaina.internalrequestmanagement.security.UserPrincipal;
import io.jessytsiriniaina.internalrequestmanagement.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "User directory and administration. JWT required on all endpoints.")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Returns the profile of the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current user.",
                    content = @Content(schema = @Schema(implementation = UserResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT.")
    })
    public ResponseEntity<UserResponseDto> getMe(@Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.getMe(principal));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "List users", description = "Paginated user list. MANAGER or ADMIN only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of users."),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT."),
            @ApiResponse(responseCode = "403", description = "EMPLOYEE role cannot list users.")
    })
    public ResponseEntity<Page<UserResponseDto>> findAll(@ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(userService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by id", description = "Self, MANAGER or ADMIN. EMPLOYEE reading another user gets 403.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found.",
                    content = @Content(schema = @Schema(implementation = UserResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT."),
            @ApiResponse(responseCode = "403", description = "EMPLOYEE reading another user."),
            @ApiResponse(responseCode = "404", description = "User not found.")
    })
    public ResponseEntity<UserResponseDto> findById(
            @Parameter(description = "User id", example = "1") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.findById(id, principal));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create user", description = "ADMIN only. Use to create MANAGER/ADMIN accounts.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created.",
                    content = @Content(schema = @Schema(implementation = UserResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation failure or malformed JSON."),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT."),
            @ApiResponse(responseCode = "403", description = "Non-ADMIN caller."),
            @ApiResponse(responseCode = "409", description = "Email already registered.")
    })
    public ResponseEntity<UserResponseDto> create(@Valid @RequestBody CreateUserDto dto) {
        UserResponseDto created = userService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update user", description = "ADMIN only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User updated.",
                    content = @Content(schema = @Schema(implementation = UserResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation failure or malformed JSON."),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT."),
            @ApiResponse(responseCode = "403", description = "Non-ADMIN caller."),
            @ApiResponse(responseCode = "404", description = "User not found."),
            @ApiResponse(responseCode = "409", description = "Email already in use.")
    })
    public ResponseEntity<UserResponseDto> update(
            @Parameter(description = "User id", example = "1") @PathVariable Long id,
            @Valid @RequestBody UpdateUserDto dto) {
        return ResponseEntity.ok(userService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete user", description = "ADMIN only.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User deleted."),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT."),
            @ApiResponse(responseCode = "403", description = "Non-ADMIN caller."),
            @ApiResponse(responseCode = "404", description = "User not found.")
    })
    public ResponseEntity<Void> delete(@Parameter(description = "User id", example = "1") @PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
