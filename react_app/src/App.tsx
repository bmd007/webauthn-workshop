import React from 'react';
import logo from './logo.svg';
import './App.css';
import {create, get} from "@github/webauthn-json";

function App() {

    async function registerWebAuthn(sameDevice = true) {
        const registerRequestBody = {
            displayName: 'mahdi',
            credentialNickname: 'this-device-credential-123',
            authenticatorAttachment: sameDevice ? "platform" : "cross-platform"
        };
        const registrationRequestResponseBody = (await fetch('https://local.bmd007.github.io:8080/v1/credentials/registrations/requests?validatedUsername=mahdi', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(registerRequestBody)
        }));
        const registrationRequest = await registrationRequestResponseBody.json();
        const options = {
            publicKey: registrationRequest.publicKeyCredentialCreationOptions,
        };
        const credential = await create(options);
        const registrationResultResponseBody = await fetch('https://local.bmd007.github.io:8080/v1/credentials/registrations/results', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({requestId: registrationRequest.requestId, credential})
        });
        const finishRegistrationResponse = await registrationResultResponseBody.json();
        if (finishRegistrationResponse.success && finishRegistrationResponse.username) {
            alert(`Enabled password less login for ${finishRegistrationResponse.username}`);
        } else {
            alert(`Failed to enable password less login for ${finishRegistrationResponse.username}`);
        }
    }

    // only chrome and YubiKey support username-less and userhandle-less login
    async function webauthnAuthenticationWithNoUsernameAndNoUserHandle() {
        const authenticateRequestBody = {
            username: null,
            userHandle: null
        };
        const authenticationRequestResponseBody = (await fetch('https://local.bmd007.github.io:8080/v1/authentications/assertions/requests', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(authenticateRequestBody)
        }));
        const authenticationRequest = await authenticationRequestResponseBody.json();
        const options = {
            publicKey: authenticationRequest.publicKeyCredentialRequestOptions,
        };
        const credential = await get(options);
        const authenticationResultResponseBody = (await fetch('https://local.bmd007.github.io:8080/v1/authentications/results', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({requestId: authenticationRequest.requestId, credential})
        }));
        const authenticationResult = await authenticationResultResponseBody.json();
        if (authenticationResult.success) {
            alert(`webauthn authentication successful`);
        } else {
            alert(`webauthn authentication failed`);
        }
    }

    return (
        <div className="App">
            <header className="App-header">
                <img src={logo} className="App-logo" alt="logo"/>
                <div>
                    <button onClick={_ => registerWebAuthn(true)}>Enroll this Device</button>
                    <button onClick={_ => registerWebAuthn(false)}>Enroll another Device</button>
                </div>
                <button onClick={webauthnAuthenticationWithNoUsernameAndNoUserHandle}>Username/Passwordless login</button>
            </header>
        </div>
    );
}

export default App;
