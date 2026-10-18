package uz.haksenguz.ertaklar.data.models

/** The library's category tabs, in display order. */
enum class Category(val id: String, val label: String) {
    All("all", "Barchasi"),
    Alifbo("alifbo", "🔤 Alifbo"),
    Folk("folk", "Xalq Ertaklari"),
    Classic("classic", "Klassik Asarlar"),
    Navoiy("navoiy", "Alisher Navoiy"),
    Modern("modern", "Zamonaviy Sirlar"),
    Twins("twins", "🇰🇷 Ikki xalq — bir ertak"),
    Korea("korea", "🏙️ Koreyadagi hayotim"),
    Holiday("holiday", "🎉 Bayramlar"),
    Kichik("kichik", "🧩 Qisqa va qiziq");

    fun matches(book: Book) = this == All || book.category == id
}
