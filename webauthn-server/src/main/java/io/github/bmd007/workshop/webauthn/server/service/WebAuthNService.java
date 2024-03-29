package io.github.bmd007.workshop.webauthn.server.service;

import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.AssertionResult;
import com.yubico.webauthn.FinishAssertionOptions;
import com.yubico.webauthn.FinishRegistrationOptions;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.RegistrationResult;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.StartAssertionOptions;
import com.yubico.webauthn.StartRegistrationOptions;
import com.yubico.webauthn.attestation.YubicoJsonMetadataService;
import com.yubico.webauthn.data.AssertionExtensionInputs;
import com.yubico.webauthn.data.AttestationConveyancePreference;
import com.yubico.webauthn.data.AuthenticatorAttachment;
import com.yubico.webauthn.data.AuthenticatorSelectionCriteria;
import com.yubico.webauthn.data.AuthenticatorTransport;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialRequestOptions;
import com.yubico.webauthn.data.RegistrationExtensionInputs;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import com.yubico.webauthn.data.ResidentKeyRequirement;
import com.yubico.webauthn.data.UserIdentity;
import com.yubico.webauthn.data.UserVerificationRequirement;
import com.yubico.webauthn.exception.AssertionFailedException;
import com.yubico.webauthn.exception.RegistrationFailedException;
import com.yubico.webauthn.extension.appid.AppId;
import com.yubico.webauthn.extension.appid.InvalidAppIdException;
import io.github.bmd007.workshop.webauthn.server.domain.CredentialRegistration;
import io.github.bmd007.workshop.webauthn.server.dto.AssertionRequestWrapper;
import io.github.bmd007.workshop.webauthn.server.dto.RegistrationResponse;
import io.github.bmd007.workshop.webauthn.server.dto.SuccessfulAuthenticationResult;
import io.github.bmd007.workshop.webauthn.server.dto.SuccessfulRegistrationResult;
import io.github.bmd007.workshop.webauthn.server.repository.InMemoryRegistrationStorage;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import io.github.bmd007.workshop.webauthn.server.dto.AssertionResponse;
import io.github.bmd007.workshop.webauthn.server.dto.RegistrationRequest;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public final class WebAuthNService {

    private final InMemoryRegistrationStorage userStorage;
    private final RelyingParty swedishRelyingParty;
    private final YubicoJsonMetadataService metadataService = new YubicoJsonMetadataService();
    private final Map<ByteArray, AssertionRequestWrapper> assertRequestStorage = new ConcurrentHashMap<>();
    private final Map<ByteArray, RegistrationRequest> registerRequestStorage = new ConcurrentHashMap<>();

    public WebAuthNService(InMemoryRegistrationStorage userStorage) throws InvalidAppIdException {
        this.userStorage = userStorage;
        swedishRelyingParty = RelyingParty.builder()
                .identity(RelyingPartyIdentity.builder().id("local.bmd007.github.io").name("WebAuthn workshop").build())
                .credentialRepository(this.userStorage)
                .origins(Set.of("https://local.bmd007.github.io"))
                .allowOriginPort(true)
                .allowOriginSubdomain(true)
                .attestationConveyancePreference(AttestationConveyancePreference.DIRECT)
                .attestationTrustSource(metadataService)
                .allowUntrustedAttestation(true)
//                .validateSignatureCounter(true)
                .appId(new AppId("https://local.bmd007.github.io"))
                .build();
    }

    public static ByteArray randomUUIDByteArray() {
        UUID uuid = UUID.randomUUID();
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return new ByteArray(bb.array());
    }

    public RegistrationRequest startRegistration(@NotBlank String username,
                                                 @NotBlank String displayName,
                                                 Optional<String> credentialNickname,
                                                 AuthenticatorAttachment authenticatorAttachment) {
        final UserIdentity userIdentity =
                Optional.ofNullable(userStorage.getRegistrationsByUsername(username))
                        .stream()
                        .flatMap(Collection::stream)
                        .findAny()
                        .map(CredentialRegistration::getUserIdentity)
                        .orElseGet(() -> UserIdentity.builder()
                                .name(username)
                                .displayName(displayName)
                                .id(randomUUIDByteArray())
                                .build()
                        );
        var authenticatorSelectionCriteria = AuthenticatorSelectionCriteria.builder()
                .residentKey(ResidentKeyRequirement.REQUIRED)
                .authenticatorAttachment(authenticatorAttachment)
                .userVerification(UserVerificationRequirement.PREFERRED)
                .build();
        var registrationExtensionInputs = RegistrationExtensionInputs.builder()
                .credProps()
                .uvm()
                .build();
        var startRegistrationOptions = StartRegistrationOptions.builder()
                .user(userIdentity)
                .authenticatorSelection(authenticatorSelectionCriteria)
                .timeout(999_999_999L)
                .extensions(registrationExtensionInputs)
                .build();
        var publicKeyCredentialCreationOptions = swedishRelyingParty.startRegistration(startRegistrationOptions);
        var registrationRequest = new RegistrationRequest(username,
                credentialNickname,
                randomUUIDByteArray(),
                publicKeyCredentialCreationOptions);
        registerRequestStorage.put(registrationRequest.getRequestId(), registrationRequest);
        return registrationRequest;
    }

    public SuccessfulRegistrationResult finishRegistration(RegistrationResponse registrationResponse) {
        var registrationRequest =
                Optional.ofNullable(registerRequestStorage.get(registrationResponse.requestId()))
                        .orElseThrow(() ->
                                new ResponseStatusException(HttpStatus.NOT_FOUND, "registration request not found"));
        registerRequestStorage.remove(registrationRequest.getRequestId());
        try {
            var registrationResult = swedishRelyingParty.finishRegistration(
                    FinishRegistrationOptions.builder()
                            .request(registrationRequest.getPublicKeyCredentialCreationOptions())
                            .response(registrationResponse.credential())
                            .build()
            );
            var credentialsRegistration = addRegistration(
                    registrationRequest.getPublicKeyCredentialCreationOptions().getUser(),
                    registrationRequest.getCredentialNickname(),
                    registrationResult);
            return new SuccessfulRegistrationResult(
                    registrationRequest,
                    registrationResponse,
                    credentialsRegistration,
                    registrationResult.isAttestationTrusted());
        } catch (RegistrationFailedException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    private CredentialRegistration addRegistration(
            UserIdentity userIdentity,
            Optional<String> nickname,
            RegistrationResult result) {
        var registeredCredential = RegisteredCredential.builder()
                .credentialId(result.getKeyId().getId())
                .userHandle(userIdentity.getId())
                .publicKeyCose(result.getPublicKeyCose())
                .signatureCount(result.getSignatureCount())
                .build();
        SortedSet<AuthenticatorTransport> transports = result.getKeyId().getTransports().orElseGet(TreeSet::new);
        Optional<Object> attestationMetadata = metadataService.findEntries(result).stream().findAny();
        return addRegistration(
                userIdentity,
                nickname,
                registeredCredential,
                transports,
                attestationMetadata);
    }

    private CredentialRegistration addRegistration(
            UserIdentity userIdentity,
            Optional<String> nickname,
            RegisteredCredential credential,
            SortedSet<AuthenticatorTransport> transports,
            Optional<Object> attestationMetadata) {
        var credentialRegistration = CredentialRegistration.builder()
                .userIdentity(userIdentity)
                .credentialNickname(nickname)
                .registrationTime(Instant.now())
                .credential(credential)
                .transports(transports)
                .attestationMetadata(attestationMetadata)
                .build();
        log.info("Adding registration: user: {}, nickname: {}, credential: {}",
                userIdentity,
                nickname,
                credential);
        userStorage.addRegistrationByUsername(userIdentity.getName(), credentialRegistration);
        return credentialRegistration;
    }

    public AssertionRequestWrapper startAuthentication(ByteArray userHandle) {
        return startAuthentication(null, userHandle);
    }

    public AssertionRequestWrapper startAuthentication(String username) {
        return startAuthentication(username, null);
    }

    public AssertionRequestWrapper startAuthentication() {
        return startAuthentication(null, null);
    }

    private AssertionRequestWrapper startAuthentication(String username, ByteArray userHandle) {
        var assertionExtensionInputs = AssertionExtensionInputs.builder()
                .uvm()
                .build();
        var startAssertionOptions = StartAssertionOptions.builder()
                .userVerification(UserVerificationRequirement.REQUIRED)//username less flow on chrome, needs this to be REQUIRED
                .extensions(assertionExtensionInputs)
                .timeout(999_999_999L)
                .username(Optional.ofNullable(username))
                .userHandle(Optional.ofNullable(userHandle))
                .build();
        AssertionRequest assertionRequest = swedishRelyingParty.startAssertion(startAssertionOptions);
        PublicKeyCredentialRequestOptions publicKeyOptionsWithAllowCredentials = assertionRequest.getPublicKeyCredentialRequestOptions()
                .toBuilder()
                .allowCredentials(List.of())
                .build();
        AssertionRequest improvedAssertionRequest = assertionRequest.toBuilder()
                .publicKeyCredentialRequestOptions(publicKeyOptionsWithAllowCredentials)
                .build();
        var assertionRequestWrapper = new AssertionRequestWrapper(randomUUIDByteArray(), improvedAssertionRequest);
        assertRequestStorage.put(assertionRequestWrapper.getRequestId(), assertionRequestWrapper);
        return assertionRequestWrapper;
    }

    public SuccessfulAuthenticationResult finishAuthentication(AssertionResponse assertionResponse) {
        AssertionRequestWrapper request =
                Optional.ofNullable(assertRequestStorage.get(assertionResponse.requestId()))
                        .orElseThrow(() ->
                                new ResponseStatusException(HttpStatus.NOT_FOUND, "assertion request not found"));
        assertRequestStorage.remove(assertionResponse.requestId());
        try {
            var finishAssertionOptions = FinishAssertionOptions.builder()
                    .request(request.getRequest())
                    .response(assertionResponse.credential())
                    .build();
            var assertionResult = swedishRelyingParty.finishAssertion(finishAssertionOptions);
            if (assertionResult.isSuccess()) {
                updateSignatureCountForUser(assertionResponse, assertionResult);
                var registrationsByUsername = userStorage.getRegistrationsByUsername(assertionResult.getUsername());
                return new SuccessfulAuthenticationResult(
                        request,
                        assertionResponse,
                        registrationsByUsername,
                        assertionResult.getUsername());
            } else {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Assertion failed.");
            }
        } catch (AssertionFailedException e) {
            log.error("Assertion failed", e);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Assertion failed unexpectedly; this is likely a bug.", e);
        }
    }

    private void updateSignatureCountForUser(AssertionResponse assertionResponse, AssertionResult assertionResult) {
        log.info(
                "update signature count for user {}, credential {}",
                assertionResult.getUsername(),
                assertionResponse.credential().getId());
        userStorage.updateSignatureCount(assertionResult);
    }

    public void deleteAll() {
        userStorage.removeAll();
        assertRequestStorage.clear();
        registerRequestStorage.clear();
    }
}
