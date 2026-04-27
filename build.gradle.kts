plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
    id("org.jetbrains.intellij") version "1.17.3"
}

group = "dev.proofly"
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
}

intellij {
    version.set(providers.gradleProperty("platformVersion").get())
    type.set(providers.gradleProperty("platformType").get())
    plugins.set(emptyList())
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.9.22")
    testImplementation("junit:junit:4.13.2")
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
            freeCompilerArgs = listOf("-Xjsr305=strict")
        }
    }

    patchPluginXml {
        sinceBuild.set(providers.gradleProperty("pluginSinceBuild"))
        untilBuild.set(providers.gradleProperty("pluginUntilBuild"))
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }

    test {
        useJUnit()
    }
}
