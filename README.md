the following steps to replay:
1. git clone https://github.com/ceshannon/test-speech-sdk.git and replace the subscription key in file com.lenovo.speech.MicroSoftTranslationTask at line 56.
2. build the file: clean package spring-boot:repackage and copy the jar file to your Ubuntu server.
3. run the application in your Ubuntu server: java -jar test-speech-sdk-0.0.1-SNAPSHOT.jar
4. run test class com.lenovo.sdk.ms.MicrosoftSpeechTest under src/test/java, note replace the uri to your Ubuntu server address.
5. repeat step 4 for 10+ times, and wait the server application crash.