**A project for Information Retrieval Course**

## Installing instructions

**Requirements**:

- Java 14
- Gradle 6.3 see _gradle/wrapper/gradlew-wrapper.properties_. Intellij
   downloades it automatically.

**Build project:**

    gradlew build

**Run project:**

    gradlew gui:run

The engine will only index .txt files.

As a test folder, a portion of reuters dataset was used(8k .txt files).

**Known Issues when trying to build/run the project:**

**Error**:

There might be an error stating: "... has been compiled by a more recent version"

**Workarounds**:

1. Include jdk14/bin in the path

2. Tell Gradle JVM to use JDK 14. In Intellij IDE, you can go to settings -> gradle -> Gradle JVM -> Choose version 14 JDK.

3. If the problem persists, for windows users, you can try deleting the following folder: C:\ProgramData\Oracle\Java\javapath