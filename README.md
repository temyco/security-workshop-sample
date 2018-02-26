# Secrets Keeper

Secrets Keeper is a simple secure application that uses Android Key Store API, Fingerprint API and Confirm Credentials API to keep your secrets in safe.

## Requirements

- Support Android 18 + Devices
- Allow user to access application only if Lock Screen is set
- Protect user password with Encryption
- Protect user secrets with Encryption
- Allow user to access Secrets with Fingerprint
- Add additional Confirm Credentials  protection

## Technologies Stack

- Android 18+
- Kotlin
- AndroidKeyStore API
- Fingerprint API
- Confirm Credentials API

## Application Design

- [Application Design](/pages/design.md)
- [Application Workflow Charts](/pages/workflow.md)


## Environment setup

To be able to build this project on your local machine, please follow the below instructions:

- Download and install latest [Android Studio 3.0](https://developer.android.com/studio/preview/index.html)
- Download and install all dependencies that Gradle ask's you to
- Download and install Android Virtual Device(AVD), with API 18, using build in Android Virtual Device Manager from Android Studio
- To cover compatibility issues and support all of the features, you will also need to install AVD API 23 and AVD API 24

## Workshop Guide

Project structure was specially designed for workshops. Check the [Workshop page](/pages/workshop.md) to try encryption in action, learn how to work
with it on Android, cover compatibility issues, try fingerprint and confirm credentials APIs in a way of gradually completing 
workshop stages, like in video game.

## Security Code Snippets

Check out some [general code snippets](/pages/code-snippets.md).

## License

Except as otherwise noted, the Secrets Keeper application is licensed under the Apache License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.html).

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

## Resources

### Kotlin

- https://kotlinlang.org/docs/reference/kotlin-doc.html

### Cryptography

- http://docs.oracle.com/javase/7/docs/technotes/guides/security/crypto/CryptoSpec.html
- https://developer.android.com/training/articles/keystore.html
- http://developer.android.com/training/articles/keystore.html#SupportedAlgorithms
- https://source.android.com/security/keystore/
- https://www.amazon.com/Android-Security-Internals-In-Depth-Architecture/dp/1593275811
- http://nelenkov.blogspot.com/
- https://doridori.github.io/android-security-the-forgetful-keystore/
- https://www.coursera.org/learn/crypto

### Fingerprint & Credentials API

- https://developer.android.com/about/versions/marshmallow/android-6.0.html#fingerprint-authentication
- https://developer.android.com/about/versions/marshmallow/android-6.0.html#confirm-credential
- https://github.com/googlesamples/android-FingerprintDialog
- https://github.com/googlesamples/android-ConfirmCredential
- https://www.youtube.com/watch?v=VOn7VrTRlA4

### Libraries

- https://android-arsenal.com/ (Security and Fingerprint tags)
- https://github.com/yakivmospan/scytale

### Key Attestation

- https://developer.android.com/training/articles/security-key-attestation.html
- https://developer.android.com/training/safetynet/index.html
- https://github.com/googlesamples/android-key-attestation
- https://github.com/googlesamples/android-play-safetynet/
