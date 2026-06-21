plugins {
    kotlin("jvm") version "2.4.0-RC"
    id("com.gradleup.shadow") version "9.4.1"
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")

    // twilight
    maven("https://repo.flyte.gg/releases")

    // fawe
    maven("https://maven.enginehub.org/repo/")
}

dependencies {
    // paper / default mc
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // twilight
    implementation("gg.flyte:twilight:1.1.22")

    // lamp
    implementation("io.github.revxrsal:lamp.common:4.0.0-rc.14")
    implementation("io.github.revxrsal:lamp.bukkit:4.0.0-rc.14")

    // fawe
    implementation(platform("com.intellectualsites.bom:bom-newest:1.56")) // Ref: https://github.com/IntellectualSites/bom
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Core")
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit") { isTransitive = false }

    // JetBrains Exposed core & DAO/DSL APIs (db)
    implementation("org.jetbrains.exposed:exposed-core:0.50.0") // Use latest stable
    implementation("org.jetbrains.exposed:exposed-dao:0.50.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.50.0")

    // HikariCP for connection pooling (db)
    implementation("com.zaxxer:HikariCP:7.0.2")

    // Database Driver (e.g., MySQL or MariaDB driver) (db)
    implementation("com.mysql:mysql-connector-j:8.3.0")

    // Gson for JSON (kit serialization)
    implementation("com.google.code.gson:gson:2.10.1")

    // Redis client (Lettuce)
    implementation("io.lettuce:lettuce-core:6.2.4.RELEASE")
    // ProtocolLib for packet-level listeners (compileOnly; server should provide the plugin)
    compileOnly("net.dmulloy2:ProtocolLib:5.4.0")

}

kotlin {
    jvmToolchain(21)
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion("1.21.11")
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    processResources {
        val props = mapOf("version" to version)
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }
}
