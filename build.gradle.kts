
plugins {
    //kotlin("jvm") version "1.7.10"
    kotlin("multiplatform") version "1.7.10"
    kotlin("plugin.allopen") version "1.7.10"
    kotlin("plugin.serialization") version "1.7.10"
    `maven-publish`
}


repositories {
    mavenCentral()
    mavenLocal()
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/ProgramGrader/dynamodb-priority-queue")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

group = "bbs"
version = project.findProperty("version")!!
description = "priority queue"
//java.sourceCompatibility = JavaVersion.VERSION_1_8

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
        withJava()
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    // TODO revert these back to dynamodb v2
    sourceSets {
        val jvmMain by getting{
            dependencies {
                implementation("com.amazonaws:aws-java-sdk-lambda:1.12.364")
                implementation("io.quarkiverse.amazonservices:quarkus-amazon-dynamodb:1.4.0")
                implementation("io.quarkiverse.amazonservices:quarkus-amazon-dynamodb-enhanced:1.4.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
                implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
                implementation("software.amazon.awssdk:url-connection-client:2.19.19")
            }
        }
        val jvmTest by getting {
            dependencies {
//                implementation("io.rest-assured:rest-assured")
                implementation("io.kotest:kotest-runner-junit5:5.5.4")
//                implementation("io.quarkus:quarkus-junit5")
                implementation("org.jetbrains.kotlin:kotlin-test:1.8.0-RC")
            }
        }
    }
}