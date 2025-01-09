package io.github.bmd007.workshop.webauthn.server.webauthn4j.dto;

public class StartRegistrationResponse {
    public final boolean success = true;
    //todo all that matters is this request
    public final RegistrationRequest request;
    public final StartRegistrationActions actions = new StartRegistrationActions();
    public StartRegistrationResponse(RegistrationRequest request) {
        this.request = request;
    }
}
