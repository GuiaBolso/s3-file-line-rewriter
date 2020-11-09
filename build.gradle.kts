import com.novoda.gradle.release.PublishExtension
import info.solidsoft.gradle.pitest.PitestPluginExtension
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        mavenCentral()
        jcenter()
    }

    dependencies {
        classpath("com.novoda:bintray-release:0.9.1")
    }
}


plugins {
    kotlin("jvm") version "1.4.10"
    `maven-publish`
    id("org.jetbrains.dokka") version "0.9.17"
    id("io.gitlab.arturbosch.detekt").version("1.14.2")
    id("info.solidsoft.pitest") version "1.4.5"
}

apply(plugin = "com.novoda.bintray-release")

group = "br.com.guiabolso"
version = System.getenv("RELEASE_VERSION") ?: "local"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.1")

    // S3
    api("com.amazonaws:aws-java-sdk-s3:1.11.488")
    testImplementation("com.adobe.testing:s3mock-junit5:2.1.16")
    
    // S3 Stream Sender
    implementation("com.github.alexmojaki:s3-stream-upload:2.1.0")

    // Kotest
    testImplementation("io.kotest:kotest-runner-junit5-jvm:4.3.1")
    testImplementation("io.kotest:kotest-assertions-core-jvm:4.3.1")
    testImplementation("io.kotest:kotest-plugins-pitest-jvm:4.3.1")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

val sourcesJar by tasks.registering(Jar::class) {
    classifier = "sources"
    from(sourceSets.getByName("main").allSource)
}

val javadocJar by tasks.registering(Jar::class) {
    val javadoc = tasks["dokka"] as DokkaTask
    javadoc.outputFormat = "javadoc"
    javadoc.outputDirectory = "$buildDir/javadoc"
    dependsOn(javadoc)
    classifier = "javadoc"
    from(javadoc.outputDirectory)
}

detekt {
    input = files("src/main/kotlin", "src/test/kotlin")
}

publishing {
    publications {

        register("maven", MavenPublication::class) {
            from(components["java"])
            artifact(sourcesJar.get())
            artifact(javadocJar.get())

            pom {
                name.set("S3-file-line-rewriter")
                description.set("S3-file-line-rewriter")
                url.set("https://github.com/GuiaBolso/s3-file-line-rewriter")


                scm {
                    connection.set("scm:git:https://github.com/GuiaBolso/s3-file-line-rewriter/")
                    developerConnection.set("scm:git:https://github.com/GuiaBolso/")
                    url.set("https://github.com/GuiaBolso/s3-file-line-rewriter")
                }

                licenses {
                    license {
                        name.set("The Apache 2.0 License")
                        url.set("https://opensource.org/licenses/Apache-2.0")
                    }
                }
            }
        }
    }
}

configure<PublishExtension> {
    artifactId = "s3-file-line-rewriter"
    autoPublish = true
    desc = "S3 File Line Rewriter"
    groupId = "br.com.guiabolso"
    userOrg = "gb-opensource"
    setLicences("APACHE-2.0")
    publishVersion = version.toString()
    uploadName = "s3-file-line-rewriter"
    website = "https://github.com/GuiaBolso/s3-file-line-rewriter"
    setPublications("maven")
}

configure<PitestPluginExtension> {
    testPlugin.set("Kotest")
    targetClasses.set(listOf("br.com.guiabolso.*"))
    targetTests.set(listOf("br.com.guiabolso.*"))
    mutationThreshold.set(80)
    avoidCallsTo.set(listOf("kotlin.jvm.internal", "kotlin.io.CloseableKt", "java.io.Closeable.close", "kotlin.ResultKt")
    )
}

tasks {
    "check" {
        dependsOn("pitest")
    }
}
