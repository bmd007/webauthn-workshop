package io.github.bmd007.workshop.webauthn.server.webauthn4j.resource;

import com.webauthn4j.data.RegistrationRequest;
import com.yubico.webauthn.data.AuthenticatorAttachment;
import com.yubico.webauthn.data.ByteArray;
import io.github.bmd007.workshop.webauthn.server.webauthn4j.service.WebAuthNService;
import jakarta.validation.constraints.NotBlank;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@CrossOrigin(origins = {
        "https://local.bmd007.github.io:3000",
        "https://localhost:3000",
}, originPatterns = {"https://*.local.bmd007.github.io"})
@Slf4j
@RestController
@RequestMapping("v1")
public class WebAuthNResource {
    //todo use rsocket instead of rest ==> rename the rest style path to streaming verb style path
    private final WebAuthNService webAuthNService;

    public WebAuthNResource(WebAuthNService webAuthNService) {
        this.webAuthNService = webAuthNService;
    }

    public record RegisterRequestBody(@NotBlank String displayName,
                                      @NotBlank String credentialNickname,
                                      @NonNull AuthenticatorAttachment authenticatorAttachment) {
    }

    @GetMapping
    public String hello() {
        return "Hello, WebAuthN!";
    }

    @DeleteMapping
    public void deleteAll() {
        webAuthNService.deleteAll();
    }

    @PostMapping("credentials/registrations/requests")
    public RegistrationRequest startRegistration(@RequestBody RegisterRequestBody registerRequest, @RequestParam String validatedUsername) {
        //todo validatedUsername should be fetched from a session or an accessToken instead of query param
        return webAuthNService.startRegistration(
                validatedUsername,
                registerRequest.displayName,
                Optional.ofNullable(registerRequest.credentialNickname),
                registerRequest.authenticatorAttachment);
    }

    @PostMapping("credentials/registrations/results")
    public SuccessfulRegistrationResult finishRegistration(@RequestBody RegistrationResponse registrationResponse) {
        return webAuthNService.finishRegistration(registrationResponse);
    }

    public record AuthenticateRequestBody(@Nullable String username, @Nullable ByteArray userHandle) {
    }

    @PostMapping(value = "authentications/assertions/requests")
    public AssertionRequestWrapper startAuthentication(@RequestBody AuthenticateRequestBody authenticateRequestBody) {
        return Optional.ofNullable(authenticateRequestBody.username())
                .map(webAuthNService::startAuthentication)
                .orElseGet(() ->
                        Optional.ofNullable(authenticateRequestBody.userHandle)
                                .map(webAuthNService::startAuthentication)
                                .orElseGet(webAuthNService::startAuthentication)
                );
    }

    @PostMapping(value = "authentications/results")
    public SuccessfulAuthenticationResult finishAuthentication(@RequestBody AssertionResponse assertionResponse) {
        return webAuthNService.finishAuthentication(assertionResponse);
    }
}
