# Webauthn workshop

This repository is a playground for learning how to implement a Webauthn based authentication solution. 
The project contains a web application that allows users to register and authenticate using Webauthn. 
The backend is built using Spring Boot.

## links:
- [https://www.w3.org/TR/webauthn-2/](https://www.w3.org/TR/webauthn-2/)
- [Webauthn Demo](https://webauthn.io/)
- [Webauthn Guide](https://webauthn.guide/)
- [Webauthn - Yubikey](https://developers.yubico.com/WebAuthn/)
- [Webauthn API](https://developer.mozilla.org/en-US/docs/Web/API/Web_Authentication_API)
- [Webauthn google Demo](https://github.com/google/webauthndemo)
- [https://webauthn.wtf/](https://webauthn.wtf/)
- [https://webauthn.me/](https://webauthn.me/)
- [Webauthn Playground](https://webauthn.passwordless.id/demos/playground.html)
- [https://www.yubico.com/authentication-standards/webauthn/](https://www.yubico.com/authentication-standards/webauthn/)
- [Webauthn Java Server](https://developers.yubico.com/java-webauthn-server/)
- [Authenticators list](https://webauthn.passwordless.id/demos/authenticators.html)


## TODO
- [ ] Improve UI/UX.
  - [ ] Customize the webauthn UI.
- [ ] Upgrade to the latest version of com.yubico:webauthn-server-core and refactor accordingly.
  - [ ] Clean up not required dependencies.
  - [ ] Check if [attestation](webauthn-server%2Fsrc%2Fmain%2Fjava%2Fcom%2Fyubico%2Fwebauthn%2Fattestation) is actually needed to be part of this repo.
- [ ] Understand the certificate setup of webauthn itself.
- [ ] Understand how to recognize and limit possible authenticators.
- [ ] Use Rsocket instead of HTTP.
- [ ] Try wehauthn4j and compare.
  - [ ] Even add spring security to the mix
    -  [ ] Is there a reactive spring security started available?
- [ ] Add Flutter client.
- [ ] Add a pure HTML/JS client
- [ ] Contribute to the com.yubico:webauthn-server-core project and suggest improvements regarding use of optional.
- [ ] Dockerization of the project.
- [ ] Backend in other languages?
- [ ] Add a Github action to build the components on master

## Requirements
- Java 21 (requires JAVA_HOME to be set
- node 18.17
- npm 10

# Local development setup:
## certification
```shell
echo '127.0.0.1 local.bmd007.github.io' | sudo tee -a /etc/hosts
brew install mkcert
mkcert -install
cd react_app/ssl/generated
mkcert local.bmd007.github.io '*.local.bmd007.github.io' localhost 127.0.0.1 ::1

#Enter 'password' for the password of keystore, as input for the following command:
openssl pkcs12 -export -in local.bmd007.github.io+4.pem -inkey local.bmd007.github.io+4-key.pem -out keystore.p12 -name localdev

cp keystore.p12 ../../../webauthn-server/src/main/resources/ssl/generated
```

## Starting the applications
```shell
# ports 8080 and 3000 should be free !
cd webauthn-server && ./gradlew bootRun
cd ..
cd react_app && npm start
```
open [https://local.bmd007.github.io:3000](https://local.bmd007.github.io:3000) in browser
