plugins {
    id("java")
    id("io.papermc.paperweight.userdev") version "1.5.5"
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://papermc.io/repo/repository/maven-public/")
    }

    maven {
        url = uri("https://repo.dmulloy2.net/repository/public/")
    }
}

dependencies {
    // https://mvnrepository.com/artifact/org.xerial/sqlite-jdbc
    //paper has a sqlite driver at runtime
    implementation("org.xerial:sqlite-jdbc:3.39.2.0")
    compileOnly("io.papermc.paper:paper-api:1.19.4-R0.1-SNAPSHOT")
    paperweight.paperDevBundle("1.19.4-R0.1-SNAPSHOT")
    compileOnly("com.comphenix.protocol:ProtocolLib:5.0.0")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

group = "me.toomuchzelda"
version = "1.0-FOREVER"
description = "TeamArenaPaper"
java.sourceCompatibility = JavaVersion.VERSION_17

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks {
    assemble {
        dependsOn(reobfJar)
    }
}