plugins {
    id("neoforge-mod-conventions")
    kotlin("jvm")
}

val mekanismVersion1_21_1: String by project
val kotlinForForgeVersion1_21_1: String by project
val emiVersion1_21_1: String by project
val jadeFileId1_21_1: String by project
val titaniumVersion1_21_1: String by project
val replicationVersion1_21_1: String by project

// Mod Dependencies
dependencies {
    implementation("thedarkcolour:kotlinforforge-neoforge:$kotlinForForgeVersion1_21_1")
    implementation("com.hrznstudio:titanium:$titaniumVersion1_21_1")
    implementation("curse.maven:replication-638351:$replicationVersion1_21_1")
    // depend (optional)
    compileOnly("mekanism:Mekanism:$mekanismVersion1_21_1:api")
    // for develop
    runtimeOnly("mekanism:Mekanism:$mekanismVersion1_21_1")
    // runtimeOnly("mekanism:Mekanism:$mekanismVersion1_21_1:additions")
    runtimeOnly("mekanism:Mekanism:$mekanismVersion1_21_1:generators")
    runtimeOnly("mekanism:Mekanism:$mekanismVersion1_21_1:tools")
    runtimeOnly("maven.modrinth:emi:${emiVersion1_21_1}")
    runtimeOnly("curse.maven:jade-324717:${jadeFileId1_21_1}")
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

tasks.withType<org.gradle.language.jvm.tasks.ProcessResources>().configureEach {
    finalizedBy(syncDevLaunchResources)
}
