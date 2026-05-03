plugins {
    id("legacyforge-mod-conventions")
    kotlin("jvm")
}

val mekanismVersion1_20_1: String by project
val kotlinForForgeVersion1_20_1: String by project

// Mod Dependencies
dependencies {
    implementation("thedarkcolour:kotlinforforge:$kotlinForForgeVersion1_20_1")
    compileOnly("mekanism:Mekanism:$mekanismVersion1_20_1:api")
    add("localRuntime", "mekanism:Mekanism:$mekanismVersion1_20_1")
    add("localRuntime", "mekanism:Mekanism:$mekanismVersion1_20_1:additions")
    add("localRuntime", "mekanism:Mekanism:$mekanismVersion1_20_1:generators")
    add("localRuntime", "mekanism:Mekanism:$mekanismVersion1_20_1:tools")
}

sourceSets.configureEach {
    val outputDir = layout.buildDirectory.dir("sourceSets/$name")
    output.setResourcesDir(outputDir)
    java.destinationDirectory.set(outputDir)
    kotlin.destinationDirectory.set(outputDir)
}
