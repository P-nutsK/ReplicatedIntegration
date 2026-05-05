plugins {
    id("legacyforge-mod-conventions")
    kotlin("jvm")
}

val mekanismVersion1_20_1: String by project
val kotlinForForgeVersion1_20_1: String by project
val emiVersion1_20_1: String by project
val jadeFileId1_20_1: String by project
val replicationVersion1_20_1: String by project
val titaniumVersion1_20_1: String by project

// Mod Dependencies
dependencies {
    modImplementation("thedarkcolour:kotlinforforge:$kotlinForForgeVersion1_20_1")

    modImplementation("curse.maven:replication-638351:$replicationVersion1_20_1")
    modImplementation("curse.maven:titanium-287342:$titaniumVersion1_20_1")
    modRuntimeOnly("curse.maven:tmrv-1194921:7983491")
    modRuntimeOnly("maven.modrinth:emi:$emiVersion1_20_1")
    modRuntimeOnly("curse.maven:jade-324717:$jadeFileId1_20_1")

    modCompileOnly("mekanism:Mekanism:$mekanismVersion1_20_1:api")
    modRuntimeOnly("mekanism:Mekanism:$mekanismVersion1_20_1")
    modRuntimeOnly("mekanism:Mekanism:$mekanismVersion1_20_1:additions")
    modRuntimeOnly("mekanism:Mekanism:$mekanismVersion1_20_1:generators")
    modRuntimeOnly("mekanism:Mekanism:$mekanismVersion1_20_1:tools")
}

sourceSets.configureEach {
    val outputDir = layout.buildDirectory.dir("sourceSets/$name")
    output.setResourcesDir(outputDir)
    java.destinationDirectory.set(outputDir)
    kotlin.destinationDirectory.set(outputDir)
}

tasks.withType<JavaCompile>().configureEach {
    dependsOn(tasks.named("processResources"))
}
