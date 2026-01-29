plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinSerialization)
    application
}

group = "com.bhanit.apps.echo"
version = "1.0.0"
application {
    mainClass.set("io.ktor.server.netty.EngineMain")
    
    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation(projects.shared)
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation(libs.ktor.serverAuth)
    implementation(libs.ktor.serverAuthJwt)
    implementation(libs.ktor.serverContentNegotiation)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.serverCors)
    implementation(libs.ktor.serialization.kotlinx.json)
    testImplementation(libs.ktor.serverTestHost)
    implementation(libs.koin.ktor)
    testImplementation(libs.kotlin.testJunit)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.json)
    implementation(libs.exposed.java.time)
    implementation(libs.postgres)
    implementation(libs.h2)
    implementation(libs.hikaricp)
    implementation(libs.firebase.admin) {
        exclude(group = "com.google.guava", module = "listenablefuture")
    }
    implementation("com.cloudinary:cloudinary-http44:1.36.0")
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.serialization)
    implementation(libs.ktor.client.okhttp)
}