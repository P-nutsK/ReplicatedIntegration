plugins {
    id("neoforge-mod-conventions")
    kotlin("jvm")
}

// Mod Dependencies
dependencies {
    fun mekclassifier(name: String) = variantOf(libs.mekanism.mc1211) {
        classifier(name)
    }

    implementation(libs.kff.mc1211)
    implementation(libs.titanium.mc1211)
    implementation(libs.replication.mc1211)
    implementation(libs.ae2.mc1211)
    implementation(libs.aae.mc1211)
    implementation(libs.draconic.evolution.mc1211)

    // depend (optional)
    compileOnly(mekclassifier("api"))
    compileOnly(libs.ae2addonlib.mc1211)

    // for develop
    runtimeOnly(libs.mekanism.mc1211)
    runtimeOnly(mekclassifier("additions"))

    runtimeOnly(mekclassifier("generators"))

    runtimeOnly(mekclassifier("tools"))
    runtimeOnly(libs.geckolib.mc1211)
    runtimeOnly(libs.brandonscore.mc1211)
    runtimeOnly(libs.codechickenlib.mc1211)
    runtimeOnly(libs.curios.mc1211)
    // ↓どこかに勝手にJEIを入れてる野蛮なmodがある.
//    runtimeOnly(libs.tmrv.mc1211)
    runtimeOnly(libs.emi.mc1211)
    runtimeOnly(libs.jade.mc1211)
    runtimeOnly(libs.guideme.mc1211)

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
