Generate Production Key:

keytool -genkey -v -keystore blend.keystore -alias blendit -keyalg RSA -keysize 2048 -validity 10000
Signing Password: mypassword

Export Production Hash:

keytool -exportcert -alias citizenglobal -keystore citizenglobal.keystore | openssl sha1 -binary | openssl base64
Signing Password: mypassword

*** Notice that the alias is not always the same as the keystore name!!  Alias is generated when you make the keystore using IDE

Facebook Development Hash:
keytool -exportcert -alias androiddebugkey -keystore ~/.android/debug.keystore | openssl sha1 -binary | openssl base64
Signing Password: android

https://developers.facebook.com/docs/mobile/android/build/
https://developers.facebook.com/docs/android/getting-started/