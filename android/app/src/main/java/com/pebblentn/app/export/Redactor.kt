package com.pebblentn.app.export

import com.pebblentn.app.notification.NotificationSnapshot

/**
 * Structurally redacts notification text for privacy-safe diagnostic export (spec/300-data
 * ExportFormat.md). Digits, units and punctuation are preserved, and a whitelist of maneuver
 * keywords — in several languages — is kept so a rule maintainer can still see the structure; every
 * other word (road names, destination labels, free-form text) is replaced with a fixed placeholder.
 *
 * The keyword list is multilingual on purpose: a privacy-safe log from a non-English user is only
 * useful for authoring that language's rules if the maneuver words survive. Direction and maneuver
 * verbs ("destra", "tournez", "abbiegen", "rotonde", …) carry no personal information; the private
 * parts (road/place names) are exactly the words NOT on this list, so they are still redacted.
 */
class Redactor(private val placeholder: String = PLACEHOLDER) {

    private val word = Regex("\\p{L}+")

    fun redact(snapshot: NotificationSnapshot): NotificationSnapshot = snapshot.copy(
        title = redactText(snapshot.title),
        text = redactText(snapshot.text),
        subText = redactText(snapshot.subText),
        bigText = redactText(snapshot.bigText),
        summaryText = redactText(snapshot.summaryText),
        infoText = redactText(snapshot.infoText),
    )

    fun redactText(value: String?): String? {
        if (value == null) return null
        return word.replace(value) { match ->
            if (match.value.lowercase() in KEEP) match.value else placeholder
        }
    }

    companion object {
        const val PLACEHOLDER = "▮" // ▮

        /** English maneuver/structural keywords + units, kept in privacy-safe exports. */
        private val EN = setOf(
            "turn", "left", "right", "slight", "slightly", "sharp", "keep", "roundabout",
            "exit", "continue", "straight", "uturn", "u", "merge", "head", "go", "proceed",
            "north", "south", "east", "west", "arrive", "arriving", "arrived", "destination",
            "onto", "on", "in", "at", "take", "the", "toward", "towards", "now",
            "km", "m", "mi", "ft", "yd", "min", "mins", "minute", "minutes", "sec", "secs", "s", "h", "hr",
            "st", "nd", "rd", "th",
        )

        // Maneuver/direction words per language (Google Maps phrasing). Kept so a non-English
        // privacy-safe log still shows the maneuver; road/place names are not here, so they redact.
        private val IT = setOf(
            "destra", "sinistra", "svolta", "svoltare", "gira", "girare", "rotonda", "rotatoria",
            "inversione", "inverti", "marcia", "leggermente", "tieni", "mantieni", "mantenere",
            "prosegui", "proseguire", "continua", "continuare", "dritto", "diritto", "sempre", "vai",
            "arrivo", "arrivato", "arrivata", "arrivati", "giunto", "giunta", "destinazione", "uscita",
        )
        private val FR = setOf(
            "droite", "gauche", "tournez", "tourner", "rond", "point", "giratoire", "demi", "tour",
            "faites", "serré", "serrée", "franchement", "prononcé", "prononcée", "légèrement",
            "serrez", "restez", "maintenez", "continuez", "tout", "droit", "poursuivez", "dirigez",
            "direction", "prenez", "arrivé", "arrivée", "arrivez", "sortie",
        )
        private val ES = setOf(
            "derecha", "izquierda", "gira", "girar", "rotonda", "glorieta", "cambio", "sentido",
            "media", "vuelta", "bruscamente", "cerrada", "cerrado", "pronunciada", "pronunciado",
            "ligeramente", "leve", "mantente", "mantén", "manten", "sigue", "seguir", "todo", "recto",
            "continúa", "continuar", "dirígete", "hacia", "llegado", "llegada", "llegaste", "destino", "salida", "toma",
        )
        private val DE = setOf(
            "rechts", "links", "abbiegen", "biegen", "kreisverkehr", "kreisel", "wenden", "umkehren",
            "scharf", "leicht", "halten", "halte", "geradeaus", "weiter", "richtung", "immer",
            "ziel", "angekommen", "erreicht", "ausfahrt", "nehmen",
        )
        private val NL = setOf(
            "rechtsaf", "linksaf", "sla", "rotonde", "keer", "om", "omkeren", "bocht", "scherpe",
            "scherp", "flauwe", "licht", "aanhouden", "houd", "rechtdoor", "vervolg", "volg",
            "richting", "rijd", "door", "neem", "afslag", "aangekomen", "bestemming",
        )

        /** Maneuver/structural keywords kept in privacy-safe exports, across supported languages. */
        val KEEP: Set<String> = EN + IT + FR + ES + DE + NL
    }
}
