package io.github.bmd007.workshop.webauthn.server.webauthn4j.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.yubico.webauthn.data.AuthenticatorData;
import lombok.AllArgsConstructor;
import lombok.Value;
import io.github.bmd007.workshop.webauthn.server.webauthn4j.domain.CredentialRegistration;

import java.util.Collection;

@Value
@AllArgsConstructor
public class SuccessfulAuthenticationResult {
    boolean success = true;
    AssertionRequestWrapper request;
    AssertionResponse response;
    Collection<CredentialRegistration> registrations;

    @JsonSerialize(using = AuthDataSerializer.class)
    AuthenticatorData authData;

    String username;

    public SuccessfulAuthenticationResult(
            AssertionRequestWrapper request,
            AssertionResponse response,
            Collection<CredentialRegistration> registrations,
            String username) {
        this(
                request,
                response,
                registrations,
                response.credential().getResponse().getParsedAuthenticatorData(),
                username);
    }
}
