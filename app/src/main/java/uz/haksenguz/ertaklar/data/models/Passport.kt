package uz.haksenguz.ertaklar.data.models

import kotlinx.serialization.Serializable

/** assets/data/passport.json: the places books take the child to, and their stickers. */
@Serializable
data class PassportFile(
    /** Points per sticker. */
    val price: Int = 50,
    val regions: List<Region>,
    /** The neighbours' names on the map: id -> [Uzbek, Korean]. */
    val names: Map<String, List<String>> = emptyMap()
)

/** One of Uzbekistan's 14 regions, or Korea. */
@Serializable
data class Region(
    val id: String,
    val name: String,
    val ko: String,
    /** The name on the map when it needs two lines. */
    val label: String? = null,
    /** Its centre (or capital); null for Tashkent, the country's capital itself. */
    val city: String? = null,
    val koCity: String? = null,
    /** The centre's [latitude, longitude]. */
    val at: List<Double>? = null,
    /** The stamp's colour. */
    val ink: String,
    /** The region's colour on the map once stamped. */
    val fill: String,
    val fact: String,
    val koFact: String,
    /** How the stamp is tilted, in degrees. */
    val rot: Int = 0,
    /** Korea: another country, so a square stamp with its capital. */
    val capital: Boolean = false,
    /** The stamp without a date. */
    val stamp: String,
    val stickers: List<Sticker>
)

@Serializable
data class Sticker(val id: String, val name: String, val ko: String, val image: String)

/** assets/data/map.json: the map of Uzbekistan as SVG paths in a [width] × [height] box. */
@Serializable
data class MapFile(
    val width: Int,
    val height: Int,
    val regions: Map<String, MapRegion>,
    val neighbours: Map<String, String>,
    /** Where the neighbours' names go: id -> [x, y]. */
    val names: Map<String, List<Int>>,
    /** The country's border. */
    val outline: String,
    /** Tashkent's star: [x, y]. */
    val capital: List<Int>,
    val korea: KoreaInset,
    val seoulKm: Int
)

@Serializable
data class MapRegion(val d: String, val label: List<Int>)

/** The small map of Korea in the corner. */
@Serializable
data class KoreaInset(
    /** [x, y, width, height]. */
    val box: List<Int>,
    val south: String,
    val north: String,
    val seoul: List<Int>
)

/** assets/data/holidays.json. */
@Serializable
data class HolidaysFile(val days: Map<String, HolidayDef>, val months: List<String>, val soon: Int = 21)

/**
 * A holiday: [date] is [month, day] in the calendar [cal] ("dangi" for the
 * Korean lunar one, "islamic-umalqura"; Gregorian when null). [long]: days
 * after it that are still the holiday.
 */
@Serializable
data class HolidayDef(val name: String, val date: List<Int>, val cal: String? = null, val long: Int = 0)
