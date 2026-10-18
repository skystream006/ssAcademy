package com.ssmath.app

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class MegaCatalogTest {
    private data class Expected(val ndex: String, val id: String, val name: String, val types: List<String>)

    // Independent transcription of the requested display identifiers, including its unusual z suffixes.
    private val expected = """
        0154|MEGA_MEGANIUM|Meganium|Grass/Fairy
        0500|MEGA_EMBOAR|Emboar|Fire/Fighting
        0160|MEGA_FERALIGATR|Feraligatr|Water/Dragon
        0015|MEGA_BEEDRILL|Beedrill|Bug/Poison
        0018|MEGA_PIDGEOT|Pidgeot|Normal/Flying
        0181|MEGA_AMPHAROS|Ampharos|Electric/Dragon
        0130|MEGA_GYARADOS|Gyarados|Water/Dark
        0689|MEGA_BARBARACLE|Barbaracle|Rock/Fighting
        0121|MEGA_STARMIE|Starmie|Water/Psychic
        0670|MEGA_FLOETTE|Floette|Fairy
        0678|MEGA_MEOWSTIC|Meowstic|Psychic
        0668|MEGA_PYROAR|Pyroar|Fire/Normal
        0026x|MEGA_RAICHU_X|Raichu X|Electric
        0026y|MEGA_RAICHU_Y|Raichu Y|Electric
        0036|MEGA_CLEFABLE|Clefable|Fairy/Flying
        0065|MEGA_ALAKAZAM|Alakazam|Psychic
        0094|MEGA_GENGAR|Gengar|Ghost/Poison
        0545|MEGA_SCOLIPEDE|Scolipede|Bug/Poison
        0071|MEGA_VICTREEBEL|Victreebel|Grass/Poison
        0308|MEGA_MEDICHAM|Medicham|Fighting/Psychic
        0310|MEGA_MANECTRIC|Manectric|Electric
        0282|MEGA_GARDEVOIR|Gardevoir|Psychic/Fairy
        0475|MEGA_GALLADE|Gallade|Psychic/Fighting
        0229|MEGA_HOUNDOOM|Houndoom|Dark/Fire
        0334|MEGA_ALTARIA|Altaria|Dragon/Fairy
        0531|MEGA_AUDINO|Audino|Normal/Fairy
        0428|MEGA_LOPUNNY|Lopunny|Normal/Fighting
        0354|MEGA_BANETTE|Banette|Ghost
        0323|MEGA_CAMERUPT|Camerupt|Fire/Ground
        0530|MEGA_EXCADRILL|Excadrill|Ground/Steel
        0445|MEGA_GARCHOMP|Garchomp|Dragon/Ground
        0445z|MEGA_GARCHOMP_Z|Garchomp Z|Dragon
        0302|MEGA_SABLEYE|Sableye|Dark/Ghost
        0303|MEGA_MAWILE|Mawile|Steel/Fairy
        0359|MEGA_ABSOL|Absol|Dark
        0359z|MEGA_ABSOL_Z|Absol Z|Dark/Ghost
        0448|MEGA_LUCARIO|Lucario|Fighting/Steel
        0448z|MEGA_LUCARIO_Z|Lucario Z|Fighting/Steel
        0080|MEGA_SLOWBRO|Slowbro|Water/Psychic
        0319|MEGA_SHARPEDO|Sharpedo|Water/Dark
        0604|MEGA_EELEKTROSS|Eelektross|Electric
        0149|MEGA_DRAGONITE|Dragonite|Dragon/Flying
        0003|MEGA_VENUSAUR|Venusaur|Grass/Poison
        0006|MEGA_CHARIZARD_X|Charizard X|Fire/Dragon
        0006z|MEGA_CHARIZARD_Y|Charizard Y|Fire/Flying
        0009|MEGA_BLASTOISE|Blastoise|Water
        0687|MEGA_MALAMAR|Malamar|Dark/Psychic
        0691|MEGA_DRAGALGE|Dragalge|Poison/Dragon
        0362|MEGA_GLALIE|Glalie|Ice
        0478|MEGA_FROSLASS|Froslass|Ice/Ghost
        0460|MEGA_ABOMASNOW|Abomasnow|Grass/Ice
        0212|MEGA_SCIZOR|Scizor|Bug/Steel
        0127|MEGA_PINSIR|Pinsir|Bug/Flying
        0214|MEGA_HERACROSS|Heracross|Bug/Fighting
        0701|MEGA_HAWLUCHA|Hawlucha|Fighting/Flying
        0560|MEGA_SCRAFTY|Scrafty|Dark/Fighting
        0609|MEGA_CHANDELURE|Chandelure|Ghost/Fire
        0142|MEGA_AERODACTYL|Aerodactyl|Rock/Flying
        0208|MEGA_STEELIX|Steelix|Steel/Ground
        0306|MEGA_AGGRON|Aggron|Steel
        0248|MEGA_TYRANITAR|Tyranitar|Rock/Dark
        0658|MEGA_GRENINJA|Greninja|Water/Dark
        0870|MEGA_FALINKS|Falinks|Fighting
        0652|MEGA_CHESNAUGHT|Chesnaught|Grass/Fighting
        0227|MEGA_SKARMORY|Skarmory|Steel/Flying
        0655|MEGA_DELPHOX|Delphox|Fire/Psychic
        0373|MEGA_SALAMENCE|Salamence|Dragon/Flying
        0115|MEGA_KANGASKHAN|Kangaskhan|Normal
        0780|MEGA_DRAMPA|Drampa|Normal/Dragon
        0376|MEGA_METAGROSS|Metagross|Steel/Psychic
        0718|MEGA_ZYGARDE|Zygarde|Dragon/Ground
        0719|MEGA_DIANCIE|Diancie|Rock/Fairy
        0150|MEGA_MEWTWO_X|Mewtwo X|Psychic/Fighting
        0150z|MEGA_MEWTWO_Y|Mewtwo Y|Psychic
        0952|MEGA_SCOVILLAIN|Scovillain|Grass/Fire
        0970|MEGA_GLIMMORA|Glimmora|Rock/Poison
        0978|MEGA_TATSUGIRI|Tatsugiri|Dragon/Water
        0978c|MEGA_TATSUGIRI_C|Tatsugiri|Dragon/Water
        0978d|MEGA_TATSUGIRI_D|Tatsugiri|Dragon/Water
        0998|MEGA_BAXCALIBUR|Baxcalibur|Dragon/Ice
        0254|MEGA_SCEPTILE|Sceptile|Grass/Dragon
        0257|MEGA_BLAZIKEN|Blaziken|Fire/Fighting
        0260|MEGA_SWAMPERT|Swampert|Water/Ground
        0358|MEGA_CHIMECHO|Chimecho|Psychic/Steel
        0768|MEGA_GOLISOPOD|Golisopod|Bug/Steel
        0623|MEGA_GOLURK|Golurk|Ground/Ghost
        0398|MEGA_STARAPTOR|Staraptor|Fighting/Flying
        0740|MEGA_CRABOMINABLE|Crabominable|Fighting/Ice
        0485|MEGA_HEATRAN|Heatran|Fire/Steel
        0491|MEGA_DARKRAI|Darkrai|Dark
        0380|MEGA_LATIAS|Latias|Dragon/Psychic
        0381|MEGA_LATIOS|Latios|Dragon/Psychic
        0384|MEGA_RAYQUAZA|Rayquaza|Dragon/Flying
        0801|MEGA_MAGEARNA|Magearna|Steel/Fairy
        0807|MEGA_ZERAORA|Zeraora|Electric
    """.trimIndent().lines().map { line ->
        val fields = line.split('|')
        Expected(fields[0], fields[1], fields[2], fields[3].split('/'))
    }

    @Test fun completeRequestedRosterRetainsOrderLabelsAndExactSuffixes() {
        assertEquals(95, expected.size)
        assertEquals(expected.map { it.id }, Celebration.megaPokemons.map { it.name })
        expected.zip(Celebration.megaPokemons).forEach { (row, pokemon) ->
            assertEquals(row.ndex.take(4).toInt(), pokemon.ndex)
            assertEquals(row.ndex.drop(4), pokemon.ndexSuffix)
            assertEquals("Mega ${row.name}", pokemon.label)
            assertEquals("#${row.ndex} Mega ${row.name}", pokemon.collectionLabel)
            assertEquals(CelebrationCategory.MEGA_POKEMONS, pokemon.category)
            assertTrue(pokemon.description.startsWith("Mega Pokémon — "))
        }
        assertEquals(95, Celebration.megaPokemons.map { it.collectionLabel }.toSet().size)
    }

    @Test fun everyFormHasItsVerifiedTypesAndSpeciesImageReference() {
        assertEquals(Celebration.megaPokemons.toSet(), megaPokemonProfiles.keys)
        expected.forEach { row ->
            val pokemon = Celebration.valueOf(row.id)
            val profile = megaPokemonProfile(pokemon)
            assertEquals(row.id, row.types, profile.types)
            val species = row.name.substringBeforeLast(' ', row.name)
            assertEquals("https://bulbapedia.bulbagarden.net/wiki/${species}_(Pok%C3%A9mon)", profile.imageSource)
            assertTrue(row.id, profile.visualSignature.startsWith(pokemon.label))
            assertTrue(row.id, profile.visualSignature.length > pokemon.label.length + 25)
        }
        assertEquals(95, megaPokemonProfiles.values.map { it.visualSignature }.toSet().size)
        assertThrows(IllegalArgumentException::class.java) { megaPokemonProfile(Celebration.PIKACHU) }
    }

    @Test fun allEnumNamesRoundTripWithoutCollidingWithStandardPokemon() {
        Celebration.entries.forEach { pokemon ->
            assertEquals("\"${pokemon.name}\"", Json.encodeToString(pokemon))
            assertSame(pokemon, Json.decodeFromString<Celebration>(Json.encodeToString(pokemon)))
        }
        val mixed = setOf(Celebration.VENUSAUR, Celebration.MEGA_VENUSAUR,
            Celebration.MEGA_TATSUGIRI, Celebration.MEGA_TATSUGIRI_C, Celebration.MEGA_TATSUGIRI_D)
        assertEquals(mixed, Json.decodeFromString<Set<Celebration>>(Json.encodeToString(mixed)))
        assertEquals(CelebrationCategory.POKEMONS, CelebrationCategory.entries[0])
        assertEquals(CelebrationCategory.OTHER, CelebrationCategory.entries[1])
        assertEquals("Mega Pokémons", CelebrationCategory.entries[2].label)
        assertTrue(Celebration.pokemons.none { it in Celebration.megaPokemons })
        assertEquals(Celebration.entries.toSet(), (Celebration.standardCelebrations + Celebration.megaPokemons).toSet())
        assertEquals("#0003 Venusaur", Celebration.VENUSAUR.collectionLabel)
    }

    @Test fun typeChangingFormsUseDifferentEnvironmentsOrMotion() {
        assertEquals(listOf("Fire", "Dragon"), megaPokemonProfile(Celebration.MEGA_CHARIZARD_X).types)
        assertEquals(listOf("Fire", "Flying"), megaPokemonProfile(Celebration.MEGA_CHARIZARD_Y).types)
        assertNotEquals(megaPokemonProfile(Celebration.MEGA_CHARIZARD_X).habitat,
            megaPokemonProfile(Celebration.MEGA_CHARIZARD_Y).habitat)
        assertNotEquals(megaPokemonProfile(Celebration.MEGA_GARCHOMP).motion,
            megaPokemonProfile(Celebration.MEGA_GARCHOMP_Z).motion)
        assertNotEquals(megaPokemonProfile(Celebration.MEGA_MEWTWO_X).motion,
            megaPokemonProfile(Celebration.MEGA_MEWTWO_Y).motion)
        assertEquals(MegaHabitat.SNOW, megaPokemonProfile(Celebration.MEGA_BAXCALIBUR).habitat)
        assertEquals(MegaHabitat.CAVE, megaPokemonProfile(Celebration.MEGA_DIANCIE).habitat)
    }
}
