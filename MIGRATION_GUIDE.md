# Intro

Library v3.1 supports only Smart-ID v3 API.
All the previous v2 related code has been removed and all the code necessary for Smart-ID API v3 is under package smartid. 
Some classes could also be used in v3 and for those classes the package did not change.

# Migrating from library v3.2 to v3.3

This release aligns 404 Not Found handling with Smart-ID RP-API 3.2: the server no longer uses the custom status codes `471` and `472` - those cases are now returned as `404` with an [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457.html) problem-details body. The client has been adjusted accordingly.

The following exception classes have been removed:
- `ee.sk.smartid.exception.useraccount.NoSuitableAccountOfRequestedTypeFoundException` (previously thrown on HTTP 471)
- `ee.sk.smartid.exception.useraccount.PersonShouldViewSmartIdPortalException` (previously thrown on HTTP 472)

Both former cases now throw `UserAccountNotFoundException`. The parsed problem-details payload - when present - is exposed via the new accessor `UserAccountNotFoundException.getProblemDetails()`.

Required changes:
- Replace any `catch (NoSuitableAccountOfRequestedTypeFoundException e)` with `catch (UserAccountNotFoundException e)`, then inspect `e.getProblemDetails()` for the error code (e.g. `NO_SUITABLE_ACCOUNT_FOUND`) when you need to distinguish the former 471 case.
- Replace any `catch (PersonShouldViewSmartIdPortalException e)` with `catch (UserAccountNotFoundException e)`. The former 472 case folds into a plain 404 with no `errors` array - `e.getProblemDetails().getErrors()` will be `null`.
- If you previously caught `PersonShouldViewSmartIdPortalException` to also handle `DocumentUnusableException`, note that `DocumentUnusableException` now extends `UserAccountException` directly. Catch `DocumentUnusableException` (or its parent `UserAccountException`) explicitly.

Example:

```java
try {
    connector.initDeviceLinkAuthentication(request, semanticsIdentifier);
} catch (UserAccountNotFoundException e) {
    ProblemDetails problemDetails = e.getProblemDetails();
    if (problemDetails != null && problemDetails.getErrors() != null && !problemDetails.getErrors().isEmpty()) {
        String code = problemDetails.getErrors().get(0).getCode();
        if ("NO_SUITABLE_ACCOUNT_FOUND".equals(code)) {
            // handle former 471 case
        }
    } else {
        // plain 404 - former 472 case or generic "account not found"
    }
}
```

# Migrating from library v3.1 to v3.2

For signing flows are restored legacy RSASSA-PKCS#1 v1.5 algorithms (`SHA256_WITH_RSA_ENCRYPTION`, `SHA384_WITH_RSA_ENCRYPTION`, `SHA512_WITH_RSA_ENCRYPTION`) which are compatible with DigiDoc4j's signing support.
For that reason:
- `SignatureAlgorithm` class is split into `AuthenticationSignatureAlgorithm` and `SigningSignatureAlgorithm`.
- `SignatureValueValidator.validate` last parameter changed from `RsaSsaPssParameters` to `SignatureFactory`

Changes needed in authentication flows:
- change `SignatureAlgorithm` to `AuthenticationSignatureAlgorithm`
- change `SignatureValueValidator.validate` last parameter from `RsaSsaPssParameters` to `new RsaSsaPssSignatureFactory(RsaSsaPssParameters)`

Changes needed in signing flows:
- change `SignatureAlgorithm` to `SigningSignatureAlgorithm`
- suggestion for `SignatureValueValidator.validate` last parameter changes:
  - when using only signature algorithm RSASSA_PSS then use `new RsaSsaPssSignatureFactory(RsaSsaPssParameters)`
  - when using only legacy signature algorithms (`SHA256_WITH_RSA_ENCRYPTION`, `SHA384_WITH_RSA_ENCRYPTION`, `SHA512_WITH_RSA_ENCRYPTION`) then use `new RsaSsaPkcs1SignatureFactory(SigningSignatureAlgorithm)`
  - when both RSASSA_PSS and legacy RSA algorithms are used then possible solution is:
    ```java
    SignatureFactory signatureFactory = signatureResponse.getSignatureAlgorithm().isLegacyRsa()
            ? new RsaSsaPkcs1SignatureFactory(signatureResponse.getSignatureAlgorithm())
            : new RsaSsaPssSignatureFactory(signatureResponse.getRsaSsaPssParameters());
    ```

The following classes are moved from `ee.sk.smartid` to `ee.sk.smartid.signature` so when used then imports need to be adjusted:
- `AuthenticationSignatureAlgorithm`
- `DigestInput`
- `MaskGenAlgorithm`
- `RsaSsaPssParameters`
- `SignableData`
- `SignableHash`
- `SignatureValueValidator`
- `SignatureValueValidatorImpl`
- `SigningSignatureAlgorithm`
- `TrailerField`

For legacy RSA with DigiDoc4j there is new chapter in README.md: [Legacy algorithms for signing with DigiDoc4j](./README.md#legacy-algorithms-for-signing-with-digidoc4j)

# Migrating from Smart-ID v2 to Smart-ID v3 API

## Migrating authentication

Smart-ID v3 authentication offers new methods to construct builders  `createDeviceLinkAuthentication()` and `createNotificationAuthentication()` to create authentication session request builders.
It is recommended to start using device-link authentication flows from Smart-ID API v3 as these are more secure.

### Overview of V2 authentication flow

1. Create authentication hash
2. Generate verification code from authentication hash
3. Verification code can be shown to the user
4. Create builder and set values.
5. Call build method (`authenticate()`) to create authentication session and to start polling for session status.
6. After session status is `COMPLETE` response will be checked in the build method.
7. Use `AuthenticationResponseValidator` to validate the certificate and the signature in the response.

### Moving to V3 authentication flow

1. Replace generating authentication hash with generating RP challenge using `RpChallengeGenerator.generate()`
2. [Create device-link authentication builder and set values](README.md#examples-of-initiating-a-device-link-authentication-session) and start authentication session by calling build-method `initAuthenticationSession()`
3. Replace showing verification code with showing device link or QR-code. Recommended to use device link for same device and QR-code for cross-device authentication.
   - [Create device link or QR-code](README.md#generating-qr-code-or-device-link) from values in session response and display it to the user. QR-code should be recreated after every second.
4. Querying session status can be done in parallel while displaying device content. Check out [session status poller](README.md#example-of-using-session-status-poller-to-query-final-sessions-status). `ee.sk.smartid.SmartIdClient` provides method `getSessionsStatusPoller()` to get version specific session status poller.
5. When session status state is `COMPLETE` polling will be stopped and [response should be checked](README.md#example-of-validating-the-authentication-sessions-response) with `DeviceLinkAuthenticationResponseValidator` or `NotificationAuthenticationResponseValidator` (depending on the flow). They will validate required fields, certificate and signature value in sessions status, and they will also handle errors.
6. If everything is ok `AuthenticationIdentity` will be returned. AuthenticationIdentity is same as used for V2.

## Migrating signing

Signing migration will be focusing on moving to signature flow when device link authentication has been completed before. 

### Overview of V2 signing flow

1. Set values for certificate choice builder and call build method. Should return certificate as a response.
2. Use queried certificate to create DataToSign object. Requires DigiDoc4j library.
3. Create SignableData from DataToSign.
4. Create verification code from SignableData
5. Create signature builder and set values.
6. Call build method (`sign()`) to create signing session and to start polling for session status.
7. After session status is `COMPLETE` response will be checked in the build method. And signed document will be returned.

### Moving to V3 signing flow - with DigiDoc4j library

DigiDoc4j library does not currently support signing with signature algorithm RSASSA-PSS. Support will be added in the future. 
There is a possible workaround to use DigiDoc4j library and DSS library together to create ASICS container and sign it with Smart-ID v3 API.
Steps below include examples how to set up DataToSign for signing with RSASSA-PSS and how validate returned signature value. 

#### Steps to migrate 

1. Replace certificate choice builder with`CertificateByDocumentNumberRequestBuilder`. SmartID client `ee.sk.smartid.SmartIdClient` provides method `createCertificateByDocumentNumber()` for easier access. Call build method `.getCertificateByDocumentNumber()` to get the certificate. Checkout example [here](README.md#example-of-querying-certificate-by-document-number).
2. Use `SignableData` to create digested value for signing. Example for setting up DataToSign with DSS: https://github.com/SK-EID/smart-id-java-demo/blob/81880330822f7d86a9205e597f24bca42c72d87b/src/main/java/ee/sk/siddemo/services/SmartIdDeviceLinkSignatureService.java#L181
3. Use `ee.sk.smartid.SmartIdClient` to [create session request builder](README.md#examples-of-initiating-a-device-link-signature-session) `createDeviceLinkSignature()` and call build method `initSignatureSession()` to start the signing session.
4. Replace showing verification code with showing device link or QR-code. [Create device link or QR-code](README.md#generating-qr-code-or-device-link) from values in session response and display it to the user. QR-code should be recreated after every second.
5. Poll for session status until its complete.
6. Validate session response with `SignatureResponseValidator`. `SignatureSessionResponse` will be returned when everything is ok.
7. Validate signature value. Example for validating signature value: https://github.com/SK-EID/smart-id-java-demo/blob/81880330822f7d86a9205e597f24bca42c72d87b/src/main/java/ee/sk/siddemo/services/SmartIdSignatureService.java#L65

### Moving to V3 signing flow without DigiDoc4j library

NB! Without DigiDoc4j library integrator has to provide implementation for creating signed container.
Smart-id-java-client only provides means to validate that signature response has required fields and returned signature value is valid.

1. Replace certificate choice builder with`CertificateByDocumentNumberRequestBuilder`. SmartID client `ee.sk.smartid.SmartIdClient` provides method `createCertificateByDocumentNumber()` for easier access. Call build method `.getCertificateByDocumentNumber()` to get the certificate. Checkout example [here](README.md#example-of-querying-certificate-by-document-number).
2. Use `SignableData` to create digested value for signing.
3. Use `ee.sk.smartid.SmartIdClient` to [create session request builder](README.md#examples-of-initiating-a-device-link-signature-session) `createDeviceLinkSignature()` and call build method `initSignatureSession()` to start the signing session.
4. Replace showing verification code with showing device link or QR-code. [Create device link or QR-code](README.md#generating-qr-code-or-device-link) from values in session response and display it to the user. QR-code should be recreated after every second.
5. Poll for session status until its complete.
6. Validate session status response with `SignatureResponseValidator`. `SignatureSessionResponse` will be returned when everything is ok.
7. Validate signature value with `SignatureValueValidator`
