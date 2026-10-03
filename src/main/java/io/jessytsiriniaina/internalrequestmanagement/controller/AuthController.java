package io.jessytsiriniaina.internalrequestmanagement.controller;

import io.jessytsiriniaina.internalrequestmanagement.dto.auth.AuthResponseDto;
import io.jessytsiriniaina.internalrequestmanagement.dto.auth.LoginRequestDto;
import io.jessytsiriniaina.internalrequestmanagement.dto.auth.RegisterRequestDto;
import io.jessytsiriniaina.internalrequestmanagement.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Public sign-up and login. No JWT required.")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @SecurityRequirements
    @Operation(
            summary = "Register a new employee",
            description = "Creates an EMPLOYEE account. The request body has no `role` field: sending an unknown "
                    + "field such as `\"role\"` is rejected with 400. First ADMIN is bootstrapped via SQL, never via this endpoint.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registered. Returns JWT + user.",
                    content = @Content(schema = @Schema(implementation = AuthResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation failure, malformed JSON, or unknown field (e.g. `role`). Error envelope: {timestamp, status, error, message, path}."),
            @ApiResponse(responseCode = "409", description = "Email already registered. Error envelope: {timestamp, status, error, message, path}.")
    })
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto dto) {
        AuthResponseDto resp = authService.register(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Log in", description = "Authenticates with email + password and returns a JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authenticated. Returns JWT + user.",
                    content = @Content(schema = @Schema(implementation = AuthResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation failure or malformed JSON. Error envelope: {timestamp, status, error, message, path}."),
            @ApiResponse(responseCode = "401", description = "Invalid credentials. Error envelope: {timestamp, status, error, message, path}.")
    })
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto dto) {
        AuthResponseDto resp = authService.login(dto);
        return ResponseEntity.ok(resp);
    }
}
