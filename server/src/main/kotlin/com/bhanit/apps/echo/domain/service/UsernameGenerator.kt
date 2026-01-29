package com.bhanit.apps.echo.domain.service

import com.bhanit.apps.echo.shared.domain.model.Inspiration
import kotlin.random.Random

object UsernameGenerator {

    // ---------- CORE CONFIG ----------

    private const val MIN_LENGTH = 8
    private const val MAX_LENGTH = 20
    private val alphaNum = "abcdefghijklmnopqrstuvwxyz0123456789"

    // ---------- CHARACTER POOLS ----------

    private val animeNames = listOf(
        // Dragon Ball
        "goku", "vegeta", "gohan", "trunks", "piccolo", "frieza", "cell", "buu",

        // Naruto
        "naruto", "sasuke", "sakura", "kakashi", "itachi", "jiraiya", "hinata",
        "madara", "obito", "pain", "minato", "shikamaru", "gaara",

        // One Piece
        "luffy", "zoro", "sanji", "nami", "robin", "ace", "law",
        "shanks", "usopp", "chopper", "brook",

        // Attack on Titan
        "eren", "mikasa", "levi", "armin", "reiner", "annie", "zeke",

        // Jujutsu Kaisen
        "gojo", "yuji", "megumi", "nobara", "sukuna", "toji",

        // Demon Slayer
        "tanjiro", "nezuko", "zenitsu", "inosuke", "rengoku", "akaza",

        // Death Note
        "light", "ryuk", "misa", "near",

        // My Hero Academia
        "deku", "bakugo", "todoroki", "allmight", "aizawa", "hawks"
    )

    private val pokemonNames = listOf(
        "pikachu", "raichu",
        "bulbasaur", "ivysaur", "venusaur",
        "charmander", "charmeleon", "charizard",
        "squirtle", "wartortle", "blastoise",

        "eevee", "vaporeon", "jolteon", "flareon",
        "umbreon", "espeon", "leafeon", "glaceon", "sylveon",

        "mew", "mewtwo", "snorlax", "lucario", "gengar",
        "greninja", "dragonite", "lapras", "gyarados",

        "lugia", "hooh", "rayquaza", "dialga", "palkia"
    )

    private val cartoonNames = listOf(
        "oswald", "mickey", "minnie", "donald", "goofy", "pluto",
        "tom", "jerry", "spike",

        "scooby", "shaggy", "velma", "daphne", "fred",

        "bugs", "daffy", "tweety", "sylvester",

        "popeye", "olive", "bluto",

        "woody", "buzz", "rex", "forky"
    )

    private val disneyNames = listOf(
        "elsa", "anna", "olaf", "kristoff",
        "simba", "nala", "mufasa", "scar",
        "aladdin", "jasmine", "genie",
        "moana", "maui",
        "ariel", "ursula", "triton",
        "belle", "beast",
        "rapunzel", "flynn",
        "stitch", "lilo",
        "woody", "buzz", "nemo", "dory",
        "lightning", "mater"
    )

    private val movieHeroNames = listOf(
        // DC
        "batman", "superman", "flash", "aquaman", "wonderwoman",
        "joker", "harley", "bane",

        // Marvel
        "ironman", "thor", "hulk", "spiderman", "deadpool",
        "wolverine", "logan", "cyclops",
        "blackwidow", "hawkeye", "vision",
        "loki", "thanos",

        // Other movies
        "neo", "trinity", "morpheus",
        "johnwick", "rocky", "rambo"
    )

    private val moneyHeistNames = listOf(
        "tokyo", "berlin", "rio", "denver",
        "nairobi", "professor", "palermo",
        "helsinki", "oslo", "stockholm"
    )

    private val ghostNames = listOf(
        "casper", "slimer", "beetlejuice", "noface",
        "boo", "samara", "chucky", "dracula",
        "freddy", "jason", "pennywise",
        "phantom", "specter", "polter"
    )

    private val mythNames = listOf(
        // Greek
        "zeus", "hades", "ares", "apollo", "athena", "hera", "poseidon",

        // Norse
        "odin", "thor", "loki", "fenrir", "heimdall",

        // Egyptian
        "anubis", "ra", "osiris", "seth",

        // General fantasy
        "kraken", "hydra", "cerberus", "phoenix"
    )

    // ---------- FANTASY CONSTRUCTION ----------

    private val prefixes = listOf(
        "echo", "nova", "luna", "aura", "mist", "halo",
        "ember", "flux", "drift", "zen", "pulse",
        "gok", "kai", "shin", "zenn", "nar", "luf",
        "tok", "berl", "rio", "hexa",
        "valor", "iron", "arc", "myth"
    )

    private val middles = listOf(
        "kai", "ra", "zen", "no", "rin", "to", "mi", "ka"
    )

    private val suffixes = listOf(
        "fox", "wing", "flare", "spark", "veil",
        "stone", "bloom", "dash", "wave", "core"
    )

    // ---------- CATEGORY MAP ----------

    private val categoryMap: Map<Inspiration, List<String>> = mapOf(
        Inspiration.ANIME to animeNames,
        Inspiration.POKEMON to pokemonNames,
        Inspiration.CARTOON to cartoonNames,
        Inspiration.DISNEY to disneyNames,
        Inspiration.MOVIE_HERO to movieHeroNames,
        Inspiration.MONEY_HEIST to moneyHeistNames,
        Inspiration.GHOST to ghostNames,
        Inspiration.MYTH to mythNames
    )

    // ---------- WEIGHTING (Default Bias) ----------

    private val weightedCategories = listOf(
        Inspiration.ANIME, Inspiration.ANIME, Inspiration.ANIME,
        Inspiration.POKEMON,
        Inspiration.MOVIE_HERO,
        Inspiration.DISNEY,
        Inspiration.FANTASY
    )

    // ---------- PUBLIC API ----------

    fun generate(
        inspiration: Inspiration? = null
    ): String {
        repeat(20) {
            val base = when (inspiration) {
                null, Inspiration.ANY -> generateWeighted()
                Inspiration.FANTASY -> generateFantasy()
                Inspiration.HALLOWEEN -> generateFrom(ghostNames + mythNames)
                else -> generateFrom(categoryMap[inspiration] ?: emptyList())
            }

            val username = "${base}_${randomSuffix(3)}".lowercase()

            if (username.length in MIN_LENGTH..MAX_LENGTH) {
                return username
            }
        }
        // Fallback if 20 attempts fail (unlikely)
        return "user_${randomSuffix(8)}"
    }

    // ---------- INTERNAL HELPERS ----------

    private fun generateWeighted(): String {
        return when (val pick = weightedCategories.random()) {
            Inspiration.FANTASY -> generateFantasy()
            else -> generateFrom(categoryMap[pick] ?: emptyList())
        }
    }

    private fun generateFrom(pool: List<String>): String {
        if (pool.isEmpty()) return generateFantasy()
        return pool.random()
    }

    private fun generateFantasy(): String {
        val base = when (Random.nextInt(4)) {
            0 -> prefixes.random() + suffixes.random()
            1 -> prefixes.random() + middles.random()
            2 -> middles.random() + suffixes.random()
            else -> prefixes.random() + middles.random() + suffixes.random()
        }
        return base.take(16)
    }

    private fun randomSuffix(length: Int): String =
        (1..length).map { alphaNum.random() }.joinToString("")
}
