import com.diffplug.gradle.spotless.SpotlessExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.4.1"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.spring") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    id("com.diffplug.spotless") version "7.0.1"
}

group = "com.sparetimedevs"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_21

repositories {
    mavenCentral()
// 	mavenLocal()
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.boot:spring-boot-starter-web") {
        // This needs to be excluded, else Spring Boot is still making use of Jackson instead of Kotlinx Serialization.
        exclude("org.springframework.boot", "spring-boot-starter-json")
    }
    val coroutinesVersion = "1.10.1"
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:$coroutinesVersion")
    val serializationVersion = "1.8.0"
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
    val arrowVersion = "1.2.4"
    implementation("io.arrow-kt:arrow-core:$arrowVersion")
    val amiMusicSdkVersion = "0.0.1-SNAPSHOT"
    implementation("com.sparetimedevs.ami:ami-music-sdk-kotlin:$amiMusicSdkVersion")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

configure<SpotlessExtension> {
    kotlin {
        target("**/kotlin/**/*.kt")
        targetExclude("**/build/**/*.*")
        ktlint("1.5.0")
        licenseHeaderFile("$rootDir/LICENSE.header.template")
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint("1.5.0")
    }
}

afterEvaluate {
    val spotlessApply = tasks.findByName("spotlessApply")
    tasks.withType<KotlinCompile> { dependsOn(spotlessApply) }
}
