package com.ssmath.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.serialization.Serializable

enum class CelebrationCategory(val label: String) {
    POKEMONS("Pokémons"), OTHER("Other"), MEGA_POKEMONS("Mega Pokémons")
}

@Serializable
enum class Celebration(
    val label: String,
    val description: String,
    val category: CelebrationCategory = CelebrationCategory.OTHER,
    val ndex: Int? = null,
    val ndexSuffix: String = ""
) {
    DOLPHINS("Dolphins", "Dolphins jumping out of the water saying Hurray!!"),
    WHALES("Whales", "Whales jumping out of the water saying Hurray!!"),
    ANCHOVIES("Anchovies", "Anchovies jumping out of the water saying Hurray!!"),
    PARTY("Party", "Congratulations party with balloons and confetti"),
    CANDY_SHOWER("Candy shower", "A colorful shower of candy"),
    PIKACHU("Pikachu", "Pikachu running toward you and zapping lightning", CelebrationCategory.POKEMONS, ndex = 25),
    SQUIRTLE("Squirtle", "Squirtle shooting water from his mouth", CelebrationCategory.POKEMONS, ndex = 7),
    BULBASAUR("Bulbasaur", "Bulbasaur shooting leaves from his bulb", CelebrationCategory.POKEMONS, ndex = 1),
    CHARMANDER("Charmander", "Charmander shooting fire into the air", CelebrationCategory.POKEMONS, ndex = 4),
    JIGGLYPUFF("Jigglypuff", "Jigglypuff rolling and jumping", CelebrationCategory.POKEMONS, ndex = 39),
    PALAFIN("Palafin", "Palafin leaping through sparkling water", CelebrationCategory.POKEMONS, ndex = 964),
    FINIZEN("Finizen", "Finizen jumping through bubbles", CelebrationCategory.POKEMONS, ndex = 963),
    WAILMER("Wailmer", "Wailmer bouncing and spraying water", CelebrationCategory.POKEMONS, ndex = 320),
    WAILORD("Wailord", "Wailord gliding and spouting water", CelebrationCategory.POKEMONS, ndex = 321),
    BOUFFALANT("Bouffalant", "Bouffalant charging and leaping in celebration", CelebrationCategory.POKEMONS, ndex = 626),
    VELUZA("Veluza", "Veluza darting through the water", CelebrationCategory.POKEMONS, ndex = 976),
    MANTYKE("Mantyke", "Mantyke flapping and jumping above the waves", CelebrationCategory.POKEMONS, ndex = 458),
    MANTINE("Mantine", "Mantine soaring over the sea with a little fish", CelebrationCategory.POKEMONS, ndex = 226),
    IVYSAUR("Ivysaur", 2),
    VENUSAUR("Venusaur", 3),
    CHARMELEON("Charmeleon", 5),
    CHARIZARD("Charizard", 6),
    WARTORTLE("Wartortle", 8),
    BLASTOISE("Blastoise", 9),
    CATERPIE("Caterpie", 10),
    METAPOD("Metapod", 11),
    BUTTERFREE("Butterfree", 12),
    WEEDLE("Weedle", 13),
    KAKUNA("Kakuna", 14),
    BEEDRILL("Beedrill", 15),
    PIDGEY("Pidgey", 16),
    PIDGEOTTO("Pidgeotto", 17),
    PIDGEOT("Pidgeot", 18),
    RATTATA("Rattata", 19),
    RATICATE("Raticate", 20),
    SPEAROW("Spearow", 21),
    FEAROW("Fearow", 22),
    EKANS("Ekans", 23),
    ARBOK("Arbok", 24),
    RAICHU("Raichu", 26),
    SANDSHREW("Sandshrew", 27),
    SANDSLASH("Sandslash", 28),
    NIDORAN_FEMALE("Nidoran♀", 29),
    NIDORINA("Nidorina", 30),
    NIDOQUEEN("Nidoqueen", 31),
    NIDORAN_MALE("Nidoran♂", 32),
    NIDORINO("Nidorino", 33),
    NIDOKING("Nidoking", 34),
    CLEFAIRY("Clefairy", 35),
    CLEFABLE("Clefable", 36),
    VULPIX("Vulpix", 37),
    NINETALES("Ninetales", 38),
    WIGGLYTUFF("Wigglytuff", 40),
    ZUBAT("Zubat", 41),
    GOLBAT("Golbat", 42),
    ODDISH("Oddish", 43),
    GLOOM("Gloom", 44),
    VILEPLUME("Vileplume", 45),
    PARAS("Paras", 46),
    PARASECT("Parasect", 47),
    VENONAT("Venonat", 48),
    VENOMOTH("Venomoth", 49),
    DIGLETT("Diglett", 50),
    DUGTRIO("Dugtrio", 51),
    MEOWTH("Meowth", 52),
    PERSIAN("Persian", 53),
    PSYDUCK("Psyduck", 54),
    GOLDUCK("Golduck", 55),
    MANKEY("Mankey", 56),
    PRIMEAPE("Primeape", 57),
    GROWLITHE("Growlithe", 58),
    ARCANINE("Arcanine", 59),
    POLIWAG("Poliwag", 60),
    POLIWHIRL("Poliwhirl", 61),
    POLIWRATH("Poliwrath", 62),
    ABRA("Abra", 63),
    KADABRA("Kadabra", 64),
    ALAKAZAM("Alakazam", 65),
    MACHOP("Machop", 66),
    MACHOKE("Machoke", 67),
    MACHAMP("Machamp", 68),
    BELLSPROUT("Bellsprout", 69),
    WEEPINBELL("Weepinbell", 70),
    VICTREEBEL("Victreebel", 71),
    TENTACOOL("Tentacool", 72),
    TENTACRUEL_GEODUDE("TentacruelGeodude", 73),
    GOLEM("Golem", 76),
    PONYTA("Ponyta", 77),
    RAPIDASH("Rapidash", 78),
    SLOWPOKE("Slowpoke", 79),
    SLOWBRO("Slowbro", 80),
    MAGNEMITE("Magnemite", 81),
    MAGNETON("Magneton", 82),
    FARFETCHD("Farfetch'd", 83),
    DODUO("Doduo", 84),
    DODRIO("Dodrio", 85),
    SEEL("Seel", 86),
    DEWGONG("Dewgong", 87),
    GRIMER("Grimer", 88),
    MUK("Muk", 89),
    SHELLDER("Shellder", 90),
    CLOYSTER("Cloyster", 91),
    GASTLY("Gastly", 92),
    HAUNTER("Haunter", 93),
    GENGAR("Gengar", 94),
    ONIX("Onix", 95),
    DROWZEE("Drowzee", 96),
    HYPNO("Hypno", 97),
    KRABBY("Krabby", 98),
    KINGLER("Kingler", 99),
    VOLTORB("Voltorb", 100),
    ELECTRODE("Electrode", 101),
    EXEGGCUTE("Exeggcute", 102),
    EXEGGUTOR("Exeggutor", 103),
    CUBONE("Cubone", 104),
    MAROWAK("Marowak", 105),
    HITMONLEE("Hitmonlee", 106),
    HITMONCHAN("Hitmonchan", 107),
    LICKITUNG("Lickitung", 108),
    KOFFING("Koffing", 109),
    WEEZING("Weezing", 110),
    RHYHORN("Rhyhorn", 111),
    RHYDON("Rhydon", 112),
    CHANSEY("Chansey", 113),
    TANGELA("Tangela", 114),
    KANGASKHAN("Kangaskhan", 115),
    HORSEA("Horsea", 116),
    SEADRA("Seadra", 117),
    GOLDEEN("Goldeen", 118),
    SEAKING("Seaking", 119),
    STARYU("Staryu", 120),
    STARMIE("Starmie", 121),
    MR_MIME("Mr. Mime", 122),
    SCYTHER("Scyther", 123),
    JYNX("Jynx", 124),
    ELECTABUZZ("Electabuzz", 125),
    MAGMAR("Magmar", 126),
    PINSIR("Pinsir", 127),
    TAUROS("Tauros", 128),
    MAGIKARP("Magikarp", 129),
    GYARADOS("Gyarados", 130),
    LAPRAS("Lapras", 131),
    DITTO("Ditto", 132),
    EEVEE("Eevee", 133),
    VAPOREON("Vaporeon", 134),
    JOLTEON("Jolteon", 135),
    FLAREON("Flareon", 136),
    PORYGON("Porygon", 137),
    OMANYTE("Omanyte", 138),
    OMASTAR("Omastar", 139),
    KABUTO("Kabuto", 140),
    KABUTOPS("Kabutops", 141),
    AERODACTYL("Aerodactyl", 142),
    SNORLAX("Snorlax", 143),
    ARTICUNO("Articuno", 144),
    ZAPDOS("Zapdos", 145),
    MOLTRES("Moltres", 146),
    DRATINI("Dratini", 147),
    DRAGONAIR("Dragonair", 148),
    DRAGONITE("Dragonite", 149),
    MEWTWO("Mewtwo", 150),
    MEW("Mew", 151),
    MEGA_MEGANIUM("Mega Meganium", 154, "", "ruffles its cascading flower collar in a sunlit garden"),
    MEGA_EMBOAR("Mega Emboar", 500, "", "stomps and swings its fiery mane over volcanic stone"),
    MEGA_FERALIGATR("Mega Feraligatr", 160, "", "opens its enormous dorsal jaw beside rushing water"),
    MEGA_BEEDRILL("Mega Beedrill", 15, "", "beats four wings and sweeps its twin lance arms through a meadow"),
    MEGA_PIDGEOT("Mega Pidgeot", 18, "", "fans its broad wings and streaming crimson crest in mountain winds"),
    MEGA_AMPHAROS("Mega Ampharos", 181, "", "shakes its flowing white wool and glowing red orbs beneath storm clouds"),
    MEGA_GYARADOS("Mega Gyarados", 130, "", "surges through ocean spray with immense red-edged fins"),
    MEGA_BARBARACLE("Mega Barbaracle", 689, "", "raises its many rocky claw arms on a wave-washed reef"),
    MEGA_STARMIE("Mega Starmie", 121, "", "strides on long star points while its central jewel pulses"),
    MEGA_FLOETTE("Mega Floette", 670, "", "floats beside its gigantic eternal flower in a moonlit garden"),
    MEGA_MEOWSTIC("Mega Meowstic", 678, "", "unfurls its luminous psychic ears beneath a starry sky"),
    MEGA_PYROAR("Mega Pyroar", 668, "", "shakes its magnificent flame-colored mane on a warm savanna"),
    MEGA_RAICHU_X("Mega Raichu X", 26, "x", "leaps with muscular electric fists and a star-tipped tail"),
    MEGA_RAICHU_Y("Mega Raichu Y", 26, "y", "hovers on a floating lightning tail with streaming ears"),
    MEGA_CLEFABLE("Mega Clefable", 36, "", "flutters its celestial wings above a moonlit clearing"),
    MEGA_ALAKAZAM("Mega Alakazam", 65, "", "levitates cross-legged while five spoons orbit its long beard"),
    MEGA_GENGAR("Mega Gengar", 94, "", "rises through a shadow pool with its third eye shining"),
    MEGA_SCOLIPEDE("Mega Scolipede", 545, "", "undulates its armored segments and towering horns through a forest"),
    MEGA_VICTREEBEL("Mega Victreebel", 71, "", "swings its oversized pitcher and trailing vines in a jungle"),
    MEGA_MEDICHAM("Mega Medicham", 308, "", "balances on one foot and fans four spectral arms"),
    MEGA_MANECTRIC("Mega Manectric", 310, "", "bounds under thunderclouds beneath its giant lightning-bolt mane"),
    MEGA_GARDEVOIR("Mega Gardevoir", 282, "", "turns in a flowing white ball gown among drifting fairy lights"),
    MEGA_GALLADE("Mega Gallade", 475, "", "sweeps its blade arms and white cape through a moonlit glade"),
    MEGA_HOUNDOOM("Mega Houndoom", 229, "", "bares its curled horns and bone breastplate over glowing embers"),
    MEGA_ALTARIA("Mega Altaria", 334, "", "flaps its blue wings beneath a huge billowing cloud of cotton"),
    MEGA_AUDINO("Mega Audino", 531, "", "waves its white coat and curled feelers in a healing garden"),
    MEGA_LOPUNNY("Mega Lopunny", 428, "", "kicks and swings its long segmented ears across a grassy arena"),
    MEGA_BANETTE("Mega Banette", 354, "", "opens its zipper seams and reaches out with spectral pink claws"),
    MEGA_CAMERUPT("Mega Camerupt", 323, "", "stamps beneath its erupting volcano hump on a lava plain"),
    MEGA_EXCADRILL("Mega Excadrill", 530, "", "spins its enormous steel drills above glittering underground ore"),
    MEGA_GARCHOMP("Mega Garchomp", 445, "", "sweeps two immense scythe arms above desert sand"),
    MEGA_GARCHOMP_Z("Mega Garchomp Z", 445, "z", "darts on narrow wing blades through a high-altitude slipstream"),
    MEGA_SABLEYE("Mega Sableye", 302, "", "peeks around its enormous ruby shield in a crystal cavern"),
    MEGA_MAWILE("Mega Mawile", 303, "", "swings two huge black jaw ponytails beside a fairy-lit cave"),
    MEGA_ABSOL("Mega Absol", 359, "", "bounds with wing-shaped white fur and a curved horn across a moonlit ridge"),
    MEGA_ABSOL_Z("Mega Absol Z", 359, "z", "slashes the air with sweeping black blades and streaming white fur"),
    MEGA_LUCARIO("Mega Lucario", 448, "", "strikes an aura stance with red paws and trailing black appendages"),
    MEGA_LUCARIO_Z("Mega Lucario Z", 448, "z", "balances in a flowing white aura mantle with sharpened golden accents"),
    MEGA_SLOWBRO("Mega Slowbro", 80, "", "wobbles upright inside its enormous spiral shell beside a quiet lagoon"),
    MEGA_SHARPEDO("Mega Sharpedo", 319, "", "swims with its toothed saw snout and golden scars through a reef"),
    MEGA_EELEKTROSS("Mega Eelektross", 604, "", "coils its long electric body and sweeps broad fins over a tidal pool"),
    MEGA_DRAGONITE("Mega Dragonite", 149, "", "beats its head wings and spreads white feathered wings above the clouds"),
    MEGA_VENUSAUR("Mega Venusaur", 3, "", "shakes its towering palm flower and forehead blossom in a jungle"),
    MEGA_CHARIZARD_X("Mega Charizard X", 6, "", "flaps dark wings and breathes blue fire above volcanic cliffs"),
    MEGA_CHARIZARD_Y("Mega Charizard Y", 6, "z", "spreads enormous orange wings and three horns in the sunlit sky"),
    MEGA_BLASTOISE("Mega Blastoise", 9, "", "aims its giant central cannon and armored wrist cannons across a bay"),
    MEGA_MALAMAR("Mega Malamar", 687, "", "twists its hypnotic mantle and tentacle crown beneath an aurora"),
    MEGA_DRAGALGE("Mega Dragalge", 691, "", "sways its elaborate seaweed branches in a kelp forest"),
    MEGA_GLALIE("Mega Glalie", 362, "", "opens its enormous frozen jaw beneath falling snow"),
    MEGA_FROSLASS("Mega Froslass", 478, "", "fans its icy kimono sleeves across a frozen moonlit lake"),
    MEGA_ABOMASNOW("Mega Abomasnow", 460, "", "swings its shaggy arms and icy back spires in a snowy forest"),
    MEGA_SCIZOR("Mega Scizor", 212, "", "snaps its armored serrated pincers beside steel-bright stones"),
    MEGA_PINSIR("Mega Pinsir", 127, "", "beats its translucent wings and raises thorned antlers above a forest"),
    MEGA_HERACROSS("Mega Heracross", 214, "", "braces its enormous arms beneath a forked beetle horn"),
    MEGA_HAWLUCHA("Mega Hawlucha", 701, "", "spreads a flamboyant feather cape and kicks above a rocky arena"),
    MEGA_SCRAFTY("Mega Scrafty", 560, "", "swings its ragged skin jacket and tall crest in a desert arena"),
    MEGA_CHANDELURE("Mega Chandelure", 609, "", "sways its elaborate candle arms and ghost flames in an old hall"),
    MEGA_AERODACTYL("Mega Aerodactyl", 142, "", "beats stone-spiked wings and bares its rocky beard over cliffs"),
    MEGA_STEELIX("Mega Steelix", 208, "", "coils diamond-studded steel segments through a crystal cavern"),
    MEGA_AGGRON("Mega Aggron", 306, "", "stomps on armored legs beneath an immense steel head crest"),
    MEGA_TYRANITAR("Mega Tyranitar", 248, "", "roars beneath towering jagged armor spikes in a sandstorm"),
    MEGA_GRENINJA("Mega Greninja", 658, "", "spins a giant water shuriken and flicks its long tongue scarf"),
    MEGA_FALINKS("Mega Falinks", 870, "", "marches as a linked armored formation beneath a towering helmet"),
    MEGA_CHESNAUGHT("Mega Chesnaught", 652, "", "braces its enormous spiked shield shell in a wooded arena"),
    MEGA_SKARMORY("Mega Skarmory", 227, "", "fans layered metal feathers and a bladed crest above a canyon"),
    MEGA_DELPHOX("Mega Delphox", 655, "", "swirls its long sorcerer robes and blazing wand among floating sparks"),
    MEGA_SALAMENCE("Mega Salamence", 373, "", "banks beneath a vast crimson crescent wing over mountains"),
    MEGA_KANGASKHAN("Mega Kangaskhan", 115, "", "stomps alongside its grown joey in a sunlit grassland"),
    MEGA_DRAMPA("Mega Drampa", 780, "", "winds its long cloudlike body and flowing mane above misty peaks"),
    MEGA_METAGROSS("Mega Metagross", 376, "", "hovers with four giant claw arms and a golden face cross"),
    MEGA_ZYGARDE("Mega Zygarde", 718, "", "unfurls its geometric dragon body and glowing green cells over ruins"),
    MEGA_DIANCIE("Mega Diancie", 719, "", "turns beneath a vast pink diamond crown and crystal gown"),
    MEGA_MEWTWO_X("Mega Mewtwo X", 150, "", "flexes its powerful limbs and curls its violet tail in a psychic arena"),
    MEGA_MEWTWO_Y("Mega Mewtwo Y", 150, "z", "floats with a long head-tail and small delicate limbs among stars"),
    MEGA_SCOVILLAIN("Mega Scovillain", 952, "", "bobs its two pepper heads and flickering flames in a hot garden"),
    MEGA_GLIMMORA("Mega Glimmora", 970, "", "opens its layered crystalline petals above sparkling mineral blooms"),
    MEGA_TATSUGIRI("Mega Tatsugiri", 978, "", "rides a curling orange dragon wave above the sea"),
    MEGA_TATSUGIRI_C("Mega Tatsugiri", 978, "c", "swoops in a drooping pink dragon curl over a lagoon"),
    MEGA_TATSUGIRI_D("Mega Tatsugiri", 978, "d", "stretches a golden dragon ribbon through foaming surf"),
    MEGA_BAXCALIBUR("Mega Baxcalibur", 998, "", "swings its immense icy dorsal blade across a frozen valley"),
    MEGA_SCEPTILE("Mega Sceptile", 254, "", "swings its enormous red-seeded evergreen tail through a forest"),
    MEGA_BLAZIKEN("Mega Blaziken", 257, "", "kicks with black-feathered legs and streaming wrist flames"),
    MEGA_SWAMPERT("Mega Swampert", 260, "", "pounds its gigantic forearms into a rain-soaked marsh"),
    MEGA_CHIMECHO("Mega Chimecho", 358, "", "rings its floating bell and billowing prayer streamers in the breeze"),
    MEGA_GOLISOPOD("Mega Golisopod", 768, "", "opens its layered armor and swings long claws on a rocky shore"),
    MEGA_GOLURK("Mega Golurk", 623, "", "raises enormous stone fists as its ancient seal glows among ruins"),
    MEGA_STARAPTOR("Mega Staraptor", 398, "", "sweeps its dark wings and flowing crest through a mountain gale"),
    MEGA_CRABOMINABLE("Mega Crabominable", 740, "", "swings huge furry pincers and stamps across a snowy beach"),
    MEGA_HEATRAN("Mega Heatran", 485, "", "crawls on metal claws with molten heat rising from its shell"),
    MEGA_DARKRAI("Mega Darkrai", 491, "", "unfurls shadowy ribbons beneath its white plume in a moonlit dream"),
    MEGA_LATIAS("Mega Latias", 380, "", "glides with swept violet wings and golden eyes through soft clouds"),
    MEGA_LATIOS("Mega Latios", 381, "", "banks with angular violet wings and crimson eyes above a blue horizon"),
    MEGA_RAYQUAZA("Mega Rayquaza", 384, "", "coils its emerald body and long golden streamers through the stratosphere"),
    MEGA_MAGEARNA("Mega Magearna", 801, "", "turns its ornate mechanical skirt and opens delicate gear arms"),
    MEGA_ZERAORA("Mega Zeraora", 807, "", "lunges with charged claws and a sweeping electric mane");

    constructor(label: String, ndex: Int) : this(
        label, "$label ${kantoSceneProfile(ndex).action}", CelebrationCategory.POKEMONS, ndex
    )

    constructor(label: String, ndex: Int, ndexSuffix: String, megaAction: String) : this(
        label, "Mega Pokémon — $label $megaAction",
        CelebrationCategory.MEGA_POKEMONS, ndex, ndexSuffix
    )

    val collectionLabel: String
        get() = ndex?.let { "#${it.toString().padStart(4, '0')}$ndexSuffix $label" } ?: label

    companion object {
        val pokemons: List<Celebration> = entries.filter { it.category == CelebrationCategory.POKEMONS }
        val megaPokemons: List<Celebration> = entries.filter { it.category == CelebrationCategory.MEGA_POKEMONS }
        val standardCelebrations: List<Celebration> = entries.filter { it.category != CelebrationCategory.MEGA_POKEMONS }

        fun select(
            questionCount: Int,
            random: Random = Random.Default,
            collected: Set<Celebration> = emptySet(),
            gameMode: GameMode = GameMode.REWARDS,
            isMultiplicationMatrix: Boolean = false
        ): Celebration? {
            val pool = when {
                gameMode == GameMode.PRACTICE -> entries.filter { it.category == CelebrationCategory.OTHER }
                isMultiplicationMatrix && questionCount == MultiplicationMatrix.QUESTION_COUNT -> megaPokemons
                else -> standardCelebrations
            }
            val eligible = pool.filter { it.isEligibleFor(questionCount, gameMode, isMultiplicationMatrix) }
            val collectionComplete = collected.containsAll(pool)
            return eligible.filter { collectionComplete || it !in collected }.randomOrNull(random)
        }
    }
}

internal fun Celebration.isEligibleFor(
    questionCount: Int,
    gameMode: GameMode = GameMode.REWARDS,
    isMultiplicationMatrix: Boolean = false
): Boolean = when (category) {
    CelebrationCategory.OTHER -> true
    CelebrationCategory.POKEMONS -> gameMode == GameMode.REWARDS && questionCount >= 15
    CelebrationCategory.MEGA_POKEMONS -> gameMode == GameMode.REWARDS && isMultiplicationMatrix &&
        questionCount == MultiplicationMatrix.QUESTION_COUNT
}

internal const val CELEBRATION_DURATION_MS = 7_200

@Composable
internal fun CelebrationDialog(
    celebration: Celebration,
    replay: Boolean = false,
    onPresented: () -> Unit = {},
    hasNextCelebration: Boolean = false,
    onFinished: () -> Unit
) {
    val progress = remember(celebration) { Animatable(0f) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val finish by rememberUpdatedState(onFinished)
    val presented by rememberUpdatedState(onPresented)
    LaunchedEffect(celebration, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            presented()
            val remaining = ((1f - progress.value) * CELEBRATION_DURATION_MS).toInt().coerceAtLeast(1)
            progress.animateTo(1f, tween(remaining, easing = LinearEasing))
            finish()
        }
    }
    AlertDialog(
        onDismissRequest = onFinished,
        title = { Text(if (replay) celebration.label else "Congratulations!") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (!replay) {
                    Text("You answered every question!", modifier = Modifier.padding(bottom = 16.dp))
                    Text(celebration.label, style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), textAlign = TextAlign.Center)
                }
                if (celebration.category == CelebrationCategory.MEGA_POKEMONS) {
                    Text("Mega Pokémon", style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp), textAlign = TextAlign.Center)
                    Text(celebration.collectionLabel, style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), textAlign = TextAlign.Center)
                }
                CelebrationScene(celebration, progress = { progress.value })
            }
        },
        confirmButton = {
            TextButton(onClick = onFinished, modifier = Modifier.testTag(if (replay) "close-celebration" else "view-results")) {
                Text(if (replay) "Close" else if (hasNextCelebration) "Next celebration" else "View results")
            }
        }
    )
}

@Composable
internal fun CelebrationScene(celebration: Celebration, progress: () -> Float) {
    Box(Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(16.dp))
        .testTag("celebration-${celebration.name}").semantics {
            contentDescription = celebration.description
            role = Role.Image
        }) {
        Canvas(Modifier.matchParentSize()) {
            drawCelebrationArtwork(celebration, progress())
        }
        Column(Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(color = Color.White, contentColor = Color(0xFF123C61), shape = RoundedCornerShape(16.dp)) {
                Text("Hurray!!", style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }
            Canvas(Modifier.size(16.dp, 8.dp)) {
                drawPath(Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width / 2f, size.height)
                    close()
                }, Color.White)
            }
        }
    }
}

internal fun DrawScope.drawCelebrationArtwork(celebration: Celebration, progress: Float) {
    withTransform({ scale(size.width / 320f, size.height / 220f, pivot = Offset.Zero) }) {
        when (celebration) {
            Celebration.DOLPHINS, Celebration.WHALES, Celebration.ANCHOVIES -> drawOcean(celebration, progress)
            Celebration.PARTY -> drawParty(progress)
            Celebration.CANDY_SHOWER -> drawCandyShower(progress)
            Celebration.PIKACHU -> drawPikachuCelebration(progress)
            Celebration.SQUIRTLE -> drawSquirtleCelebration(progress)
            Celebration.BULBASAUR -> drawBulbasaurCelebration(progress)
            Celebration.CHARMANDER -> drawCharmanderCelebration(progress)
            Celebration.JIGGLYPUFF -> drawJigglypuffCelebration(progress)
            Celebration.PALAFIN -> drawPalafinCelebration(progress)
            Celebration.FINIZEN -> drawFinizenCelebration(progress)
            Celebration.WAILMER -> drawWailmerCelebration(progress)
            Celebration.WAILORD -> drawWailordCelebration(progress)
            Celebration.BOUFFALANT -> drawBouffalantCelebration(progress)
            Celebration.VELUZA -> drawVeluzaCelebration(progress)
            Celebration.MANTYKE -> drawMantykeCelebration(progress)
            Celebration.MANTINE -> drawMantineCelebration(progress)
            Celebration.IVYSAUR, Celebration.VENUSAUR, Celebration.CHARMELEON, Celebration.CHARIZARD,
            Celebration.WARTORTLE, Celebration.BLASTOISE, Celebration.CATERPIE, Celebration.METAPOD,
            Celebration.BUTTERFREE, Celebration.WEEDLE, Celebration.KAKUNA, Celebration.BEEDRILL,
            Celebration.PIDGEY, Celebration.PIDGEOTTO, Celebration.PIDGEOT, Celebration.RATTATA,
            Celebration.RATICATE, Celebration.SPEAROW, Celebration.FEAROW, Celebration.EKANS,
            Celebration.ARBOK, Celebration.RAICHU, Celebration.SANDSHREW, Celebration.SANDSLASH,
            Celebration.NIDORAN_FEMALE, Celebration.NIDORINA, Celebration.NIDOQUEEN,
            Celebration.NIDORAN_MALE, Celebration.NIDORINO, Celebration.NIDOKING,
            Celebration.CLEFAIRY, Celebration.CLEFABLE, Celebration.VULPIX, Celebration.NINETALES,
            Celebration.WIGGLYTUFF, Celebration.ZUBAT, Celebration.GOLBAT, Celebration.ODDISH,
            Celebration.GLOOM, Celebration.VILEPLUME, Celebration.PARAS, Celebration.PARASECT,
            Celebration.VENONAT, Celebration.VENOMOTH, Celebration.DIGLETT, Celebration.DUGTRIO,
            Celebration.MEOWTH, Celebration.PERSIAN, Celebration.PSYDUCK, Celebration.GOLDUCK,
            Celebration.MANKEY, Celebration.PRIMEAPE, Celebration.GROWLITHE, Celebration.ARCANINE,
            Celebration.POLIWAG, Celebration.POLIWHIRL, Celebration.POLIWRATH, Celebration.ABRA,
            Celebration.KADABRA, Celebration.ALAKAZAM, Celebration.MACHOP, Celebration.MACHOKE,
            Celebration.MACHAMP, Celebration.BELLSPROUT, Celebration.WEEPINBELL, Celebration.VICTREEBEL,
            Celebration.TENTACOOL, Celebration.TENTACRUEL_GEODUDE, Celebration.GOLEM,
            Celebration.PONYTA, Celebration.RAPIDASH, Celebration.SLOWPOKE, Celebration.SLOWBRO,
            Celebration.MAGNEMITE, Celebration.MAGNETON, Celebration.FARFETCHD, Celebration.DODUO,
            Celebration.DODRIO, Celebration.SEEL, Celebration.DEWGONG, Celebration.GRIMER,
            Celebration.MUK, Celebration.SHELLDER, Celebration.CLOYSTER, Celebration.GASTLY,
            Celebration.HAUNTER, Celebration.GENGAR, Celebration.ONIX, Celebration.DROWZEE,
            Celebration.HYPNO, Celebration.KRABBY, Celebration.KINGLER, Celebration.VOLTORB,
            Celebration.ELECTRODE, Celebration.EXEGGCUTE, Celebration.EXEGGUTOR, Celebration.CUBONE,
            Celebration.MAROWAK, Celebration.HITMONLEE, Celebration.HITMONCHAN, Celebration.LICKITUNG,
            Celebration.KOFFING, Celebration.WEEZING, Celebration.RHYHORN, Celebration.RHYDON,
            Celebration.CHANSEY, Celebration.TANGELA, Celebration.KANGASKHAN, Celebration.HORSEA,
            Celebration.SEADRA, Celebration.GOLDEEN, Celebration.SEAKING, Celebration.STARYU,
            Celebration.STARMIE, Celebration.MR_MIME, Celebration.SCYTHER, Celebration.JYNX,
            Celebration.ELECTABUZZ, Celebration.MAGMAR, Celebration.PINSIR, Celebration.TAUROS,
            Celebration.MAGIKARP, Celebration.GYARADOS, Celebration.LAPRAS, Celebration.DITTO,
            Celebration.EEVEE, Celebration.VAPOREON, Celebration.JOLTEON, Celebration.FLAREON,
            Celebration.PORYGON, Celebration.OMANYTE, Celebration.OMASTAR, Celebration.KABUTO,
            Celebration.KABUTOPS, Celebration.AERODACTYL, Celebration.SNORLAX, Celebration.ARTICUNO,
            Celebration.ZAPDOS, Celebration.MOLTRES, Celebration.DRATINI, Celebration.DRAGONAIR,
            Celebration.DRAGONITE, Celebration.MEWTWO, Celebration.MEW -> drawKantoCelebration(celebration, progress)
            else -> drawMegaCelebration(celebration, progress)
        }
    }
}

private fun DrawScope.drawOcean(celebration: Celebration, progress: Float) {
    drawRect(Color(0xFFDDF5FF), size = Size(320f, 220f))
    drawCircle(Color(0xFFFFDA70), 19f, Offset(281f, 31f))
    drawRect(Color(0xFF58BEE0), topLeft = Offset(0f, 165f), size = Size(320f, 55f))
    val count = when (celebration) {
        Celebration.WHALES -> 2
        Celebration.ANCHOVIES -> 7
        else -> 3
    }
    repeat(count) { index ->
        val phase = (progress * 3f + index * 0.16f) % 1f
        val jump = sin(phase * PI).toFloat()
        val x = 320f * (index + 1) / (count + 1)
        val y = 201f - jump * (if (celebration == Celebration.ANCHOVIES) 105f else 108f)
        withTransform({
            translate(x, y)
            rotate(-42f + phase * 84f, pivot = Offset.Zero)
        }) {
            when (celebration) {
                Celebration.DOLPHINS -> drawDolphin()
                Celebration.WHALES -> drawWhale()
                Celebration.ANCHOVIES -> drawAnchovy()
                else -> Unit
            }
        }
        if (phase < 0.2f || phase > 0.8f) {
            repeat(5) { drop ->
                val angle = PI * (drop + 1) / 6
                val spread = 14f + jump * 24f
                drawCircle(Color.White.copy(alpha = 0.8f), 2f,
                    Offset(x + cos(angle).toFloat() * spread, 168f - sin(angle).toFloat() * spread))
            }
        }
    }
    val wave = Path().apply {
        moveTo(0f, 176f)
        for (x in 0..320 step 8) {
            lineTo(x.toFloat(), 176f + sin(x * 0.045f + progress * 6f * PI.toFloat()) * 4f)
        }
    }
    drawPath(wave, Color(0xFFCCF5FF), style = Stroke(3f))
    wave.lineTo(320f, 220f)
    wave.lineTo(0f, 220f)
    wave.close()
    drawPath(wave, Brush.verticalGradient(listOf(Color(0xFF199FD0), Color(0xFF086AA6)), 176f, 220f))
}

private fun DrawScope.drawDolphin() {
    val body = Color(0xFF4E8EB2)
    drawPath(Path().apply {
        moveTo(-30f, 2f); lineTo(-47f, -10f); lineTo(-42f, 4f); lineTo(-48f, 15f); close()
    }, body)
    drawPath(Path().apply {
        moveTo(-12f, -12f); quadraticTo(-11f, -30f, -5f, -28f); lineTo(6f, -11f); close()
    }, body)
    drawPath(Path().apply {
        moveTo(-34f, 3f)
        cubicTo(-22f, -20f, 4f, -21f, 23f, -8f)
        quadraticTo(30f, -6f, 41f, -4f)
        quadraticTo(46f, 0f, 39f, 3f)
        lineTo(23f, 3f)
        cubicTo(12f, 18f, -10f, 16f, -34f, 3f)
        close()
    }, body)
    drawOval(Color(0xFFBDE6F0), Offset(-18f, 2f), Size(38f, 8f))
    drawPath(Path().apply {
        moveTo(3f, 7f); quadraticTo(-4f, 23f, 2f, 21f); lineTo(18f, 6f); close()
    }, Color(0xFF316989))
    drawCircle(Color(0xFF123C61), 2.5f, Offset(22f, -4f))
    drawLine(Color(0xFF123C61), Offset(25f, 4f), Offset(36f, 2f), strokeWidth = 1.5f)
}

private fun DrawScope.drawWhale() {
    val body = Color(0xFF426BB0)
    drawPath(Path().apply {
        moveTo(-36f, 5f); lineTo(-55f, -9f); lineTo(-51f, 6f)
        lineTo(-62f, 13f); quadraticTo(-45f, 20f, -32f, 12f); close()
    }, body)
    drawOval(body, Offset(-42f, -23f), Size(84f, 46f))
    drawOval(Color(0xFFBEDCF4), Offset(-23f, 8f), Size(60f, 14f))
    drawPath(Path().apply {
        moveTo(-5f, 9f); quadraticTo(-3f, 35f, 6f, 26f); lineTo(21f, 10f); close()
    }, Color(0xFF2F5090))
    drawCircle(Color(0xFF102951), 3f, Offset(26f, -2f))
    drawCircle(Color.White, 1f, Offset(27f, -3f))
    drawArc(Color(0xFF102951), 0f, 100f, false, Offset(18f, 0f), Size(17f, 12f), style = Stroke(1.5f))
    drawPath(Path().apply {
        moveTo(9f, -24f); quadraticTo(9f, -44f, -3f, -39f)
        moveTo(9f, -24f); quadraticTo(12f, -46f, 23f, -38f)
    }, Color(0xFF73CDEA), style = Stroke(3f))
}

private fun DrawScope.drawAnchovy() {
    drawPath(Path().apply {
        moveTo(-14f, 0f); lineTo(-25f, -9f); lineTo(-22f, 0f); lineTo(-25f, 9f); close()
    }, Color(0xFF7799AF))
    drawOval(Color(0xFFD6E8EF), Offset(-18f, -6f), Size(36f, 12f))
    drawLine(Color(0xFF527F9F), Offset(-15f, -2f), Offset(10f, -2f), strokeWidth = 3f)
    drawLine(Color(0xFF7799AF), Offset(7f, -4f), Offset(7f, 4f), strokeWidth = 1f)
    drawCircle(Color(0xFF123C61), 2f, Offset(12f, -1f))
}

private val partyColors = listOf(Color(0xFFFFCA55), Color(0xFFFF78AC), Color(0xFF66E0D2),
    Color(0xFFB495FF), Color(0xFF77CFFF))

private fun DrawScope.drawParty(progress: Float) {
    drawRect(Color(0xFF293563), size = Size(320f, 220f))
    repeat(5) { index ->
        val x = 34f + index * 63f
        val y = 116f + sin(progress * 4f * PI.toFloat() + index) * 20f
        drawPath(Path().apply {
            moveTo(x, y + 20f)
            cubicTo(x - 12f, y + 40f, x + 12f, y + 50f, x, y + 78f)
        }, Color.White.copy(alpha = 0.7f), style = Stroke(1.5f))
        drawOval(partyColors[index], Offset(x - 19f, y - 25f), Size(38f, 48f))
        drawOval(Color.White.copy(alpha = 0.4f), Offset(x - 11f, y - 18f), Size(8f, 14f))
        drawPath(Path().apply {
            moveTo(x, y + 21f); lineTo(x - 4f, y + 27f); lineTo(x + 4f, y + 27f); close()
        }, partyColors[index])
    }
    repeat(48) { index ->
        val phase = (progress * 3f + index * 0.073f) % 1f
        val x = ((index * 71) % 320).toFloat() + sin(phase * 6f + index) * 10f
        val y = -15f + phase * 250f
        rotate(phase * 360f + index * 19f, pivot = Offset(x, y)) {
            drawRect(partyColors[index % partyColors.size], Offset(x, y), Size(5f, 9f))
        }
    }
}
