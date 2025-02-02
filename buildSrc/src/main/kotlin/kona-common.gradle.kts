import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("java-library")
    id("maven-publish")
    id("signing")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8

    withSourcesJar()
    withJavadocJar()
}

sourceSets.create("jmh") {
    java.setSrcDirs(listOf("src/jmh/java"))
}

dependencies {
    testImplementation("org.bouncycastle:bcprov-jdk18on:1.72")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
    }

    compileTestJava {
        options.encoding = "UTF-8"
    }

    test {
        useJUnitPlatform()

        filter {
            includeTestsMatching("*Test")
            includeTestsMatching("*Demo")

            // The TLCP and TLS interop tests are not stable on Windows
            if (Os.isFamily(Os.FAMILY_WINDOWS)) {
                excludeTestsMatching("com.tencent.kona.ssl.hybrid.*")
                excludeTestsMatching("com.tencent.kona.ssl.tlcp.*")
                excludeTestsMatching("com.tencent.kona.ssl.tls.*")
            }

            val babasslPathProp = "test.babassl.path"
            val babasslPath = System.getProperty(babasslPathProp, "babassl")

            if (!isBabaSSLAvailable(babasslPath)) {
                // Ignore BabaSSL-related tests if no BabaSSL is available
                excludeTestsMatching("*BabaSSL*Test")
            } else {
                systemProperty(babasslPathProp, babasslPath)
            }
        }

        systemProperty("test.classpath", classpath.joinToString(separator = ":"))

//        if(JavaVersion.current() == JavaVersion.VERSION_11) {
//            jvmArgs("--add-exports", "java.base/jdk.internal.misc=ALL-UNNAMED")
//        } else if(JavaVersion.current() == JavaVersion.VERSION_17) {
//            jvmArgs("--add-exports", "java.base/jdk.internal.access=ALL-UNNAMED")
//        }

        testLogging {
            events = mutableSetOf(
                TestLogEvent.PASSED,
                TestLogEvent.FAILED,
                TestLogEvent.SKIPPED
            )
            showStandardStreams = true
            showExceptions = true
            exceptionFormat = TestExceptionFormat.FULL
            showCauses = true
            showStackTraces = true

            addTestListener(object : TestListener {
                override fun beforeSuite(suite: TestDescriptor?) { }

                override fun afterSuite(
                        descriptor: TestDescriptor, result: TestResult) {
                    if (descriptor.parent == null) {
                        println("Test summary: " +
                                "Passed: ${result.successfulTestCount}, " +
                                "Failed: ${result.failedTestCount}, " +
                                "Skipped: ${result.skippedTestCount}")
                    }
                }

                override fun beforeTest(testDescriptor: TestDescriptor?) { }

                override fun afterTest(
                        descriptor: TestDescriptor?, result: TestResult?) { }
            })
        }
    }

    javadoc {
        options.locale = "en_US"
        isFailOnError = false
    }

    register("jmh", type=JavaExec::class) {
        mainClass.set("org.openjdk.jmh.Main")
        classpath(sourceSets["jmh"].runtimeClasspath)
    }
}

publishing {
    publications {
        create<MavenPublication>("kona") {
            from(components["java"])

            val pomName: String?
            val pomDescription: String?
            if (project.name.contains("crypto")) {
                pomName = "Tencent Kona Crypto Provider"
                pomDescription = "A Java security provider for supporting ShangMi algorithms SM2, SM3 and SM4."
            } else if (project.name.contains("pkix")) {
                pomName = "Tencent Kona PKIX Provider"
                pomDescription = "A Java security provider for supporting ShangMi algorithms in public key infrastructure"
            } else if (project.name.contains("ssl")) {
                pomName = "Tencent Kona SSL Provider"
                pomDescription = "A Java security provider for supporting protocols TLCP, TLS 1.3 (RFC 8998) and TLS 1.2"
            } else {
                pomName = "Tencent Kona Provider"
                pomDescription = "A Java security provider for supporting ShangMi features"
            }

            pom {
                name.set(pomName)
                description.set(pomDescription)
                url.set("https://github.com/Tencent/TencentKonaSMSuite/tree/master/${project.name}")
                licenses {
                    license {
                        name.set("GNU GPL v2.0 license with classpath exception")
                        url.set("https://github.com/Tencent/TencentKonaSMSuite/blob/master/LICENSE.txt")
                    }
                }
            }
        }
    }

    repositories {
        maven {
            val snapshotRepoURL = uri("https://oss.sonatype.org/content/repositories/snapshots")
            val releaseRepoURL = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2")

            url = if (version.toString().endsWith("-SNAPSHOT")) snapshotRepoURL else releaseRepoURL

            // gradle.properties contains the below properties:
            // ossrhUsername=<OSSRH User Name>
            // ossrhPassword=<OSSRH Password>
            name = "ossrh"
            credentials(PasswordCredentials::class)
        }
    }
}

signing {
    sign(publishing.publications["kona"])
}

task<Exec>("signJar") {
    var javaHome = System.getProperty("java.home")

    // java.home is <JAVA_HOME>/jre for JDK 8
    if(JavaVersion.current() == JavaVersion.VERSION_1_8) {
        javaHome = "$javaHome/.."
    }

    val type = System.getProperty("ks.type", "PKCS12")
    val keystore = System.getProperty("ks.path")
    val storepass = System.getProperty("ks.storepass")
    val keypass = System.getProperty("ks.keypass")
    val alias = System.getProperty("ks.alias")

    if (keystore != null) {
        commandLine(
            "${javaHome}/bin/jarsigner",
            "-J-Duser.language=en_US",
            "-storetype", type,
            "-keystore", keystore,
            "-storepass", storepass,
            "-keypass", keypass,
            "build/libs/${project.name}-${project.version}.jar",
            alias
        )
    }
}

// Determine if BabaSSL is available
fun isBabaSSLAvailable(babasslPath: String): Boolean {
    var exitCode : Int = -1
    try {
        val process = ProcessBuilder()
            .command(babasslPath, "version")
            .start()
        process.waitFor(3, TimeUnit.SECONDS)
        exitCode = process.exitValue()
    } catch (e: Exception) {
        println("BabaSSL is unavailable: " + e.cause)
    }

    return exitCode == 0
}
