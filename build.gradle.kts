plugins {
    id("java")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.18"
    id("xyz.jpenilla.run-paper") version "3.0.0-beta.1"
}

repositories {
    mavenCentral()
}

dependencies {
    // https://mvnrepository.com/artifact/org.xerial/sqlite-jdbc
    //paper has a sqlite driver at runtime
    implementation("org.xerial:sqlite-jdbc:3.41.2.2")
    paperweight.paperDevBundle("1.21.11-R0.1-SNAPSHOT")
    compileOnly("net.dmulloy2:ProtocolLib:5.4.0")

    testImplementation(platform("org.junit:junit-bom:5.11.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

group = "me.toomuchzelda"
version = "1.0-FOREVER"
description = "TeamArenaPaper"
java.sourceCompatibility = JavaVersion.VERSION_21

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    test {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }

    runServer {
        minecraftVersion("1.21.8")
        downloadPlugins {
            url("https://github.com/dmulloy2/ProtocolLib/releases/download/dev-build/ProtocolLib.jar")
        }
    }
}


tasks.withType(xyz.jpenilla.runtask.task.AbstractRun::class) {
    // use JetBrains JDK for enhanced class redefinition
    javaLauncher = javaToolchains.launcherFor {
        vendor = JvmVendorSpec.JETBRAINS
        languageVersion = JavaLanguageVersion.of(21)
    }
    jvmArgs(
        "-XX:+AllowEnhancedClassRedefinition",
        "-Dnet.kyori.ansi.colorLevel=truecolor", // IntelliJ console workaround
        "-Dnet.kyori.adventure.text.warnWhenLegacyFormattingDetected=false", // disable nag when loading legacy maps
    )
}