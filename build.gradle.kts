plugins {
    java
    id("io.papermc.paperweight.userdev") version "1.3.3"
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
    compileOnly("io.papermc.paper:paper-api:1.18.2-R0.1-SNAPSHOT")
    paperDevBundle("1.18.2-R0.1-SNAPSHOT")
    compileOnly("com.comphenix.protocol:ProtocolLib:4.8.0")
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
