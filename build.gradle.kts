plugins {
    java
    id("xyz.srnyx.gradle-galaxy") version "c265727"
    id("com.gradleup.shadow") version "9.6.1"
    id("me.modmuss50.mod-publish-plugin") version "675051c"
    id("io.papermc.hangar-publish-plugin") version "0.1.4"
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

group = "xyz.srnyx"
description = "Each player has a limited number of lives. If you die, you are punished"

galaxy.minecraft {
    spigotAPI("1.8.8")
    annoyingAPI("45ae893")

    folia = true

    dependency {
        optional {
            repositories.add(PLACEHOLDER_API)
            group = "me.clip"
            artifact = "placeholderapi"
            version = "2.12.2"

            pluginYml = "PlaceholderAPI"
            modrinth = "placeholderapi"
            hangar = "PlaceholderAPI"
        }
        optional {
            repositories.add(ENGINE_HUB)
            group = "com.sk89q.worldguard"
            artifact = "worldguard-bukkit"
            version = "7.0.0"

            pluginYml = "WorldGuard"
            modrinth = "worldguard"
            curseforge = "worldguard"
        }
    }

    pluginYml {
        developerData(SRNYX)

        command("lives") {
            description = "Manage the lives of a player"
        }
        command("lifereload") {
            aliases.addAll("llreload", "limitedlivesreload")
            description = "Reloads the plugin"

            permission("reload")
        }

        permission("bypass") {
            description = "Player will bypass the lives system (i.e. infinite lives)"
            default = FALSE
        }
        permission("max.#") {
            description = "Gives the player their own specific max lives"
            default = FALSE
        }
        permission("convert") {
            description = "Allows the player to use /lives convert"
        }
        permission("give") {
            description = "Allows the player to use /lives give"
        }
        permission("get.self") {
            description = "Allows the player to use /lives get on themselves"
            default = TRUE
        }
        permission("get.other") {
            description = "Allows the player to use /lives get on others"
        }
        permission("set.self") {
            description = "Allows the player to use /lives set on themselves"
        }
        permission("set.other") {
            description = "Allows the player to use /lives set on others"
        }
        permission("add.self") {
            description = "Allows the player to use /lives add on themselves"
        }
        permission("add.other") {
            description = "Allows the player to use /lives add on others"
        }
        permission("remove.self") {
            description = "Allows the player to use /lives remove on themselves"
        }
        permission("remove.other") {
            description = "Allows the player to use /lives remove on others"
        }
        permission("withdraw.self") {
            description = "Allows the player to use /lives withdraw on themselves"
        }
        permission("withdraw.other") {
            description = "Allows the player to use /lives withdraw on others"
        }
    }

    platformPublishing {
        modrinth("LvTKDASD")
        hangar("LimitedLives")
        spigot("109078")
        curseforge("846826")
        github("srnyx/limited-lives")

        projectData("limited-lives")
    }
}
