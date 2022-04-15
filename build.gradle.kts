import org.gradle.process.internal.ExecException
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.ByteArrayOutputStream

plugins {
  kotlin("jvm") version "1.6.10"
  kotlin("plugin.jpa") version "1.6.10"
  kotlin("plugin.allopen") version "1.6.10"
  id("com.github.gmazzo.buildconfig") version "3.0.3"
  application
}

group = "jp.assasans.protanki.server"
version = "0.1.0"

repositories {
  mavenCentral()
  maven {
    url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap")
    name = "ktor-eap"
  }
}

dependencies {
  implementation(kotlin("stdlib"))

  implementation("io.ktor:ktor-server-core:2.0.0-beta-1")
  implementation("io.ktor:ktor-network:2.0.0-beta-1")
  implementation("io.ktor:ktor-server-netty:2.0.0-beta-1")

  val koinVersion = "3.1.5"

  implementation("org.hibernate.orm:hibernate-core:6.0.0.Final")
  implementation("org.hibernate.validator:hibernate-validator:7.0.4.Final")
  implementation("org.hibernate.orm:hibernate-hikaricp:6.0.0.Final")
  implementation("jakarta.el:jakarta.el-api:4.0.0")
  implementation("org.glassfish:jakarta.el:4.0.2")
  implementation("com.zaxxer:HikariCP:5.0.1")

  // Database drivers
  implementation("com.h2database:h2:2.1.210")
  implementation("org.mariadb.jdbc:mariadb-java-client:3.0.4")

  implementation("com.squareup.moshi:moshi:1.13.0")
  implementation("com.squareup.moshi:moshi-kotlin:1.13.0")
  implementation("com.squareup.moshi:moshi-adapters:1.13.0")

  implementation("io.insert-koin:koin-core:$koinVersion")
  implementation("io.insert-koin:koin-logger-slf4j:$koinVersion")

  implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.3.2")

  implementation("org.reflections:reflections:0.10.2")

  implementation("ch.qos.logback:logback-classic:1.2.11")
  implementation("io.github.microutils:kotlin-logging:2.1.21")
}

tasks.withType<KotlinCompile> {
  kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.ExperimentalStdlibApi"
  kotlinOptions.jvmTarget = "17"
}

sourceSets {
  main {
    resources {
      exclude("data")
    }
  }
}

buildConfig {
  useKotlinOutput()
  packageName("jp.assasans.protanki.server")

  val git = Git()
  val validTree = git.isInstalled && git.isInsideWorkTree
  buildConfigField("String", "GIT_BRANCH", if(validTree) "\"${git.branch}\"" else "UNKNOWN")
  buildConfigField("String", "GIT_COMMIT_HASH", if(validTree) "\"${git.hash}\"" else "\"${project.version}\"")
  buildConfigField("Boolean", "GIT_IS_DIRTY", if(validTree) "${git.isDirty}" else "false")
}

tasks {
  wrapper {
    gradleVersion = "7.4.1"
    distributionType = Wrapper.DistributionType.BIN
  }

  jar {
    val git = Git()
    val gitVersion = if(git.isInstalled && git.isInsideWorkTree) git.toString() else "UNKNOWN-${project.version}"

    archiveVersion.set(gitVersion)

    manifest {
      attributes["Main-Class"] = application.mainClass
      attributes["Implementation-Version"] = gitVersion
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    configurations.compileClasspath.get().forEach {
      from(if(it.isDirectory) it else zipTree(it))
    }

    dependsOn("copyRuntimeResources")
  }

  register<Sync>("copyRuntimeResources") {
    // Copy runtime resources to the jar directory
    from("$projectDir/src/main/resources/data")
    into(layout.buildDirectory.dir("libs/data"))
  }
}

noArg {
  annotation("jakarta.persistence.Embeddable")
  annotation("jakarta.persistence.Entity")
  annotation("jakarta.persistence.MappedSuperclass")
}

allOpen {
  annotation("jakarta.persistence.Embeddable")
  annotation("jakarta.persistence.Entity")
  annotation("jakarta.persistence.MappedSuperclass")
}

application {
  mainClass.set("jp.assasans.protanki.server.MainKt")
}

class Git {
  val isInstalled: Boolean by lazy {
    val stdout = ByteArrayOutputStream()
    try {
      exec {
        commandLine("git", "--version")

        isIgnoreExitValue = true
        standardOutput = stdout
      }.exitValue == 0
    } catch(exception: ExecException) {
      false
    }
  }

  val isInsideWorkTree: Boolean by lazy {
    val stdout = ByteArrayOutputStream()
    exec {
      commandLine("git", "rev-parse", "--is-inside-work-tree")
      isIgnoreExitValue = true
      standardOutput = stdout
    }.exitValue == 0
  }

  val branch: String by lazy {
    val stdout = ByteArrayOutputStream()
    exec {
      commandLine("git", "rev-parse", "--abbrev-ref", "HEAD")
      standardOutput = stdout
    }
    stdout.toString().trim()
  }

  val hash: String by lazy {
    val stdout = ByteArrayOutputStream()
    exec {
      commandLine("git", "rev-parse", "HEAD")
      standardOutput = stdout
    }
    stdout.toString().trim()
  }

  val isDirty: Boolean by lazy {
    exec {
      commandLine("git", "diff-index", "--quiet", "HEAD", "--")
      isIgnoreExitValue = true
    }.exitValue == 1
  }

  override fun toString(): String = "$branch+${hash.take(8)}${if(isDirty) "-dirty" else ""}"
}
