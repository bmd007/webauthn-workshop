package io.github.bmd007.workshop.webauthn.server.webauthn4j.dto;

import lombok.Value;

@Value
public class StartRegistrationActions {
    String finish = "https://localhost.localdomain/register/finish";
}
