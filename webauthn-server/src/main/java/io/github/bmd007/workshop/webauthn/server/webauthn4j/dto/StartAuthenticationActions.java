package io.github.bmd007.workshop.webauthn.server.webauthn4j.dto;

import lombok.Value;

@Value
public class StartAuthenticationActions {
    String finish = "https://localhost.localdomain/authenticate/finish";
}
