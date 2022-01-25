import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "1.6.10"
  application
}

group = "jp.assasans.protanki.server"
version = "0.1.0"

repositories {
  mavenCentral()
}

dependencies {
  implementation(kotlin("stdlib"))

  val ktorVersion = "1.6.7"
  val koinVersion = "3.1.5"

  implementation("io.ktor:ktor-server-core:$ktorVersion")
  implementation("io.ktor:ktor-network:$ktorVersion")
  implementation("io.ktor:ktor-server-netty:$ktorVersion")
  // implementation("ch.qos.logback:logback-classic:1.2.10")

  implementation("com.squareup.moshi:moshi:1.13.0")
  implementation("com.squareup.moshi:moshi-kotlin:1.13.0")

  implementation("io.insert-koin:koin-core:$koinVersion")
  implementation("io.insert-koin:koin-logger-slf4j:$koinVersion")

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
