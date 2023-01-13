/*
 * This file was generated by the Gradle 'init' task.
 *
 * This project uses @Incubating APIs which are subject to change.
 */

//val quarkusPlatformGroupId ="io.quarkus.platform"
//val quarkusPlatformArtifactId = "quarkus-bom"
//val quarkusPlatformVersion = "2.12.2.Final"

plugins {
    //kotlin("jvm") version "1.7.10"
    kotlin("multiplatform") version "1.7.10"
    kotlin("plugin.allopen") version "1.7.10"
    kotlin("plugin.serialization") version "1.7.10"
    `maven-publish`
}


repositories {
    mavenCentral()
}

publishing {
    publications {
        create<MavenPublication>("default") {
            from(components["kotlin"])
        }
    }

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

//allprojects {
//    repositories {
//        mavenCentral()
//        maven {
//            name = "GitHubPackages"
//            url = uri("https://maven.pkg.github.com/ProgramGrader/amazon-dynamodb-priority-queue")
//            credentials {
//                username = System.getenv("GITHUB_ACTOR")
//                password = System.getenv("GITHUB_TOKEN")
//            }
//        }
//        }
//    }



group = "bbs"
version = project.findProperty("version")!!
description = "priority queue"
//java.sourceCompatibility = JavaVersion.VERSION_1_8



//tasks.withType<JavaCompile>() {
//    options.encoding = "UTF-8"
//}
//
//java {
//    sourceCompatibility = JavaVersion.VERSION_17
//    targetCompatibility = JavaVersion.VERSION_17
//}
//
//allOpen {
//    annotation("javax.ws.rs.Path")
//    annotation("javax.enterprise.context.ApplicationScoped")
//    annotation("io.quarkus.test.junit.QuarkusTest")
//}
//
//tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
//    kotlinOptions.jvmTarget = JavaVersion.VERSION_17.toString()
//    kotlinOptions.javaParameters = true
//}
//dependencies{
//    implementation(enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))
//}


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
    sourceSets {
        val jvmMain by getting{
            dependencies {
                implementation("software.amazon.jsii:jsii-runtime:1.72.0")
                implementation("software.amazon.awscdk:core:1.11.0.DEVPREVIEW")
                implementation("software.amazon.awscdk:lambda:1.11.0.DEVPREVIEW")
                implementation("software.amazon.awscdk:dynamodb:1.11.0.DEVPREVIEW")
                implementation("software.amazon.awscdk:s3:1.11.0.DEVPREVIEW")
                implementation("software.amazon.awscdk:sns:1.11.0.DEVPREVIEW")
                implementation("com.amazonaws:aws-lambda-java-core:1.2.2")
                implementation("com.amazonaws:aws-lambda-java-events:3.11.0")
                implementation("com.amazonaws:aws-java-sdk-sts:1.12.364")
                implementation("com.amazonaws:aws-java-sdk-lambda:1.12.364")
                implementation("com.amazonaws:aws-java-sdk-dynamodb:1.12.364")
                implementation("com.amazonaws:aws-java-sdk-sns:1.12.351")
                implementation("org.apache.logging.log4j:log4j-core:2.19.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")

                implementation("commons-logging:commons-logging:1.2")
                implementation("org.junit.jupiter:junit-jupiter:5.9.0")
                implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.0-RC")

                implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

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