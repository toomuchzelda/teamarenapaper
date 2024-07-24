import io.papermc.paperweight.userdev.ReobfArtifactConfiguration

plugins {
    id("java")
    id("io.papermc.paperweight.userdev") version "1.7.1"
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
    implementation("org.xerial:sqlite-jdbc:3.41.2.2")
    compileOnly("io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT")
    paperweight.paperDevBundle("1.20.6-R0.1-SNAPSHOT")
    compileOnly("com.comphenix.protocol:ProtocolLib:5.2.0-SNAPSHOT")
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

paperweight {
    reobfArtifactConfiguration.set(ReobfArtifactConfiguration.MOJANG_PRODUCTION)
}


tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
