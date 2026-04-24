rootProject.name = "dev.hearthbound"

plugins {
    // See documentation on https://scaffoldit.dev
    id("dev.scaffoldit") version "0.2.+"
}

hytale {
    usePatchline("release")
    useVersion("latest")

    repositories {
    }

    dependencies {
    }

    manifest {
        Group = "Hearthbound"
        Name = "Hearthbound"
        Main = "dev.hearthbound.HearthboundPlugin"
    }
}