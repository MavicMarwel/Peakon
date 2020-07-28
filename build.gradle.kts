import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "com.pls.act"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
}

plugins {
    kotlin("jvm") version "1.3.72"
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("gradle-plugin"))

    implementation("com.github.nwillc:poink:0.4.6") // IO xlsx
    implementation("org.nield:kotlin-statistics:1.2.1")

    testImplementation("io.kotest:kotest-runner-junit5-jvm:4.0.6")
    testImplementation("io.kotest:kotest-assertions-core-jvm:4.0.6")
}