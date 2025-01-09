package io.github.bmd007.workshop.webauthn.server.webauthn4j.service;

import io.github.bmd007.workshop.webauthn.server.webauthn4j.repository.InMemoryRegistrationStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public final class WebAuthNService {

    private final InMemoryRegistrationStorage userStorage;


    public WebAuthNService(InMemoryRegistrationStorage userStorage) {
        this.userStorage = userStorage;
    }
}
