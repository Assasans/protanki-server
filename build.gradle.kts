import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "1.6.10"
  kotlin("plugin.jpa") version "1.6.10"
  kotlin("plugin.allopen") version "1.6.10"
  id("com.github.gmazzo.buildconfig") version "3.0.3"
  id("com.github.johnrengelman.shadow") version "7.1.2"
  distribution
  application
}

group = "jp.assasans.protanki.server"
version = "0.1.0"

val gitBranch = ""
var gitCommitHash = ""

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
  implementation("io.ktor:ktor-server-content-negotiation:2.0.0-beta-1")
  implementation("io.ktor:ktor-client-core:2.0.0-beta-1")
  implementation("io.ktor:ktor-client-cio:2.0.0-beta-1")
  implementation("io.ktor:ktor-client-websockets:2.0.0-beta-1")

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

  implementation("com.github.ajalt.clikt:clikt:3.5.0")

  testImplementation(kotlin("test"))
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

  buildConfigField("String", "VERSION", "\"${project.version}\"")
  buildConfigField("String", "GIT_BRANCH", "\"$gitBranch\"")
  buildConfigField("String", "GIT_COMMIT_HASH", "\"$gitCommitHash\"")
}

distributions {
  all {
    contents {
      from("src/main/resources/data") {
        into("data")
      }
    }
  }
}

tasks {
  wrapper {
    gradleVersion = "7.4.1"
    distributionType = Wrapper.DistributionType.BIN
  }

  jar {
    val gitSuffix = if(gitBranch.isNotEmpty() && gitCommitHash.isNotEmpty()) "/$gitBranch+${gitCommitHash.take(8)}" else ""

    archiveBaseName.set("protanki-server")
    archiveVersion.set("${project.version}")

    manifest {
      attributes["Main-Class"] = application.mainClass
      attributes["Implementation-Version"] = "${project.version}$gitSuffix"
    }

    dependsOn("copyDependencies")
    dependsOn("copyRuntimeResources")
  }

  shadowJar {
    val gitSuffix = if(gitBranch.isNotEmpty() && gitCommitHash.isNotEmpty()) "/$gitBranch+${gitCommitHash.take(8)}" else ""

    archiveBaseName.set("protanki-server")
    archiveVersion.set("${project.version}")

    manifest {
      attributes["Main-Class"] = application.mainClass
      attributes["Implementation-Version"] = "${project.version}$gitSuffix"
    }

    dependsOn("copyRuntimeResources")
  }

  register<Sync>("copyRuntimeResources") {
    // Copy runtime resources to the jar directory
    from("$projectDir/src/main/resources/data")
    into("$buildDir/data")
  }

  register<Sync>("copyDependencies") {
    from(configurations.default)
    into("$buildDir/dependencies")
  }

  test {
    useJUnitPlatform()
  }

  startScripts {
    doLast {
      windowsScript.writeText(windowsScript.readText().replace(Regex("set CLASSPATH=.*"), "set CLASSPATH=%APP_HOME%/lib/*"))
    }
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
