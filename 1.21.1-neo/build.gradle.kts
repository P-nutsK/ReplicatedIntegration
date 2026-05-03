plugins {
    id("neoforge-mod-conventions")
    kotlin("jvm")
}

val mekanismVersion1_21_1: String by project
val kotlinForForgeVersion1_21_1: String by project

// Mod Dependencies
dependencies {
    implementation("thedarkcolour:kotlinforforge-neoforge:$kotlinForForgeVersion1_21_1")
    compileOnly("mekanism:Mekanism:$mekanismVersion1_21_1:api")
    add("localRuntime", "mekanism:Mekanism:$mekanismVersion1_21_1")
    add("localRuntime", "mekanism:Mekanism:$mekanismVersion1_21_1:additions")
    add("localRuntime", "mekanism:Mekanism:$mekanismVersion1_21_1:generators")
    add("localRuntime", "mekanism:Mekanism:$mekanismVersion1_21_1:tools")
}
