import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "1.6.10"
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
  implementation("io.ktor:ktor-server-core:2.0.0-eap-256")
  implementation("io.ktor:ktor-network:2.0.0-eap-256")
  implementation("io.ktor:ktor-server-netty:2.0.0-eap-256")

  val ktorVersion = "1.6.7"
  val koinVersion = "3.1.5"
  val exposedVersion = "0.37.3"

  // implementation("ch.qos.logback:logback-classic:1.2.10")

  implementation("com.squareup.moshi:moshi:1.13.0")
  implementation("com.squareup.moshi:moshi-kotlin:1.13.0")

  implementation("io.insert-koin:koin-core:$koinVersion")
  implementation("io.insert-koin:koin-logger-slf4j:$koinVersion")

  implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
  implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
  implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
  implementation("org.mariadb.jdbc:mariadb-java-client:3.0.3")
  implementation("com.h2database:h2:2.1.210")

  implementation("org.slf4j:slf4j-log4j12:2.0.0-alpha6")
  implementation("com.jcabi:jcabi-log:0.20.1")
  implementation("io.github.microutils:kotlin-logging-jvm:2.1.21")
}

tasks.withType<KotlinCompile> {
  kotlinOptions.jvmTarget = "12"
}

application {
  mainClass.set("MainKt")
}
