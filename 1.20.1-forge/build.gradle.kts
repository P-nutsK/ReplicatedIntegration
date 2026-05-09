@file:Suppress("PropertyName")

plugins {
    id("legacyforge-mod-conventions")
    kotlin("jvm")
}

// Mod Dependencies
dependencies {
    fun mekclassifier(name: String) = variantOf(libs.mekanism.mc1201) {
        classifier(name)
    }

    fun universal(dependency: Provider<MinimalExternalModuleDependency>) = variantOf(dependency) {
        classifier("universal")
    }

    modImplementation(libs.kff.mc1201)
    modImplementation(libs.replication.mc1201)
    modImplementation(libs.titanium.mc1201)
    modImplementation(libs.ae2.mc1201)
    modImplementation(libs.aae.mc1201)
    // aaeについてくる
    modCompileOnly(libs.ae2addonlib.mc1201)
    // aaeに不可欠
    modRuntimeOnly(libs.geckolib.mc1201)

    // developer dependency
    modRuntimeOnly(libs.tmrv.mc1201)
    modRuntimeOnly(libs.emi.mc1201)
    modRuntimeOnly(libs.jade.mc1201)

    // ae2 dependency
    modRuntimeOnly(libs.guideme.mc1201)

    // draconic evolution dependency
    modRuntimeOnly(universal(libs.brandonscore.mc1201))
    modRuntimeOnly(universal(libs.codechickenlib.mc1201))
    modImplementation(libs.draconic.evolution.mc1201)

    modCompileOnly(mekclassifier("api"))
    modRuntimeOnly(libs.mekanism.mc1201)

    modRuntimeOnly(mekclassifier("additions"))
    modRuntimeOnly(mekclassifier("generators"))
    modRuntimeOnly(mekclassifier("tools"))
}

tasks.withType<JavaCompile>().configureEach {
    dependsOn(tasks.named("processResources"))
}

val syncDevLaunchResources = tasks.register<Sync>("syncDevLaunchResources") {
    // Keep Gradle's canonical processResources output untouched, then mirror it for IntelliJ's
    // direct devlaunch path. This avoids making processResources copy into a path that later
    // overlaps with its own inputs.
    from(layout.buildDirectory.dir("resources/main"))
    into(layout.buildDirectory.dir("sourceSets/main"))
}

tasks.withType<ProcessResources>().configureEach {
    finalizedBy(syncDevLaunchResources)
}
