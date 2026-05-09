import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    id("net.fabricmc.fabric-loom") apply false
    id("net.fabricmc.fabric-loom-remap") apply false
    id("net.neoforged.moddev") apply false
    id("net.neoforged.moddev.legacyforge") apply false
}

tasks.named<Wrapper>("wrapper").configure {
    distributionType = Wrapper.DistributionType.BIN
}

with(System.getProperties()) {
    val version = get("java.version")
    val vmVersion = get("java.vm.version")
    val vendor = get("java.vendor")
    val arch = get("os.arch")
    println("Configuring with Java: $version, JVM: $vmVersion ($vendor), Arch: $arch")
}

subprojects {
    val modVersion: String by project
    val modGroupId: String by project

    version = modVersion
    group = modGroupId

    tasks.withType<AbstractArchiveTask>().configureEach {
        archiveVersion.set("v$modVersion")
    }

    repositories {
//        flatDir {
//            dir("libs")
//        }

        exclusiveContent {
            forRepository {
                maven {
                    url = uri("https://cursemaven.com")
                }
            }
            filter {
                includeGroup("curse.maven")
            }
        }

        exclusiveContent {
            forRepository {
                maven {
                    name = "Modrinth"
                    url = uri("https://api.modrinth.com/maven")
                }
            }
            filter {
                includeGroup("maven.modrinth")
            }
        }

        maven {
            name = "ParchmentMC"
            url = uri("https://maven.parchmentmc.org")
        }
        // appeng
        maven {
            name = "ModMaven"
            url = uri("https://modmaven.dev/")

        }

        maven {
            name = "Kotlin for Forge"
            url = uri("https://thedarkcolour.github.io/KotlinForForge/")
        }

        maven {
            name = "covers1624"
            url = uri("https://maven.covers1624.net/")
            content {
                includeGroup("com.brandon3055.brandonscore")
                includeGroup("io.codechicken")
            }
        }

        maven {
            name = "Curse Maven"
            url = uri("https://cursemaven.com")
            content {
                includeGroup("curse.maven")
            }
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        doFirst {
            with(javaCompiler.get().metadata) {
                println("Compiling with Java: $javaRuntimeVersion, JVM: $jvmVersion ($vendor)")
            }
        }
        options.encoding = "UTF-8"
    }

    tasks.withType<KotlinCompile>().configureEach {
        val kotlinJvmTarget = project.findProperty("javaVersion")?.toString() ?: "17"
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(kotlinJvmTarget))
            freeCompilerArgs.add("-Xjvm-default=all")
        }
    }

    afterEvaluate {
        tasks.withType<JavaExec>().configureEach {
            standardInput = System.`in`
        }
    }

    plugins.withId("idea") {
        configure<IdeaModel> {
            module {
                isDownloadSources = true
                isDownloadJavadoc = true
            }
        }
    }
}
