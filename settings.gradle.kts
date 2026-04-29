rootProject.name = "Stayed"

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
        Group = "AelinElf"
        Name = "Stayed"
        Main = "dev.hearthbound.HearthboundPlugin"
    }
}