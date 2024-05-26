# Building

This project targets JDK 17.

Gradle artifacts are located at `build/libs/`. 

## Using IntelliJ IDEA

* Open repository in IntelliJ IDEA.
* Run / Debug project.

To build artifacts, execute Gradle `shadowJar` task.

## Using command line

### Windows (PowerShell)

```powershell
# If you need to use custom JDK:
# $env:JAVA_HOME="C:/path/to/jdk"

./gradlew.bat shadowJar
```

### Unix:
```bash
# If you need to use custom JDK:
# export JAVA_HOME="/path/to/jdk"

chmod +x gradlew
./gradlew shadowJar
```
