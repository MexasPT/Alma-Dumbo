package com.example.audio

import android.util.Log

data class DetectionResult(
    val languageCode: String,
    val languageName: String,
    val flag: String,
    val script: String,
    val confidence: Float,
    val isAutoDetected: Boolean = true
)

object LanguageAutoDetector {
    private const val TAG = "LanguageAutoDetector"

    fun detect(text: String): DetectionResult {
        val trimmed = text.trim()
        if (trimmed.isBlank()) {
            return DetectionResult(
                languageCode = "pt",
                languageName = "Português",
                flag = "🇵🇹",
                script = "Latino",
                confidence = 0.50f,
                isAutoDetected = false
            )
        }

        // 1. Check Non-Latin Scripts (High Confidence Direct Match)
        val nonLatinResult = detectByScript(trimmed)
        if (nonLatinResult != null) {
            return nonLatinResult
        }

        // 2. Lexical & Diacritical Scoring for Latin Scripts
        val latinResult = detectLatinLanguage(trimmed)
        return latinResult
    }

    private fun detectByScript(text: String): DetectionResult? {
        var arabicCount = 0
        var cyrillicCount = 0
        var devanagariCount = 0
        var bengaliCount = 0
        var hangulCount = 0
        var japaneseKanaCount = 0
        var thaiCount = 0
        var cjkCount = 0
        var tifinaghCount = 0

        for (ch in text) {
            when {
                // Arabic / Perso-Arabic
                ch in '\u0600'..'\u06FF' || ch in '\u0750'..'\u077F' || ch in '\u08A0'..'\u08FF' -> arabicCount++
                // Cyrillic
                ch in '\u0400'..'\u04FF' || ch in '\u0500'..'\u052F' -> cyrillicCount++
                // Devanagari (Hindi)
                ch in '\u0900'..'\u097F' -> devanagariCount++
                // Bengali
                ch in '\u0980'..'\u09FF' -> bengaliCount++
                // Hangul (Korean)
                ch in '\uAC00'..'\uD7AF' || ch in '\u1100'..'\u11FF' || ch in '\u3130'..'\u318F' -> hangulCount++
                // Japanese Kana
                ch in '\u3040'..'\u309F' || ch in '\u30A0'..'\u30FF' -> japaneseKanaCount++
                // CJK Ideographs (Chinese / Kanji)
                ch in '\u4E00'..'\u9FFF' -> cjkCount++
                // Thai
                ch in '\u0E00'..'\u0E7F' -> thaiCount++
                // Tifinagh (Tamazight)
                ch in '\u2D30'..'\u2D7F' -> tifinaghCount++
            }
        }

        val totalLen = text.length.coerceAtLeast(1)

        if (tifinaghCount > 0) {
            return findResult("ber", 0.99f)
        }
        if (hangulCount > 0) {
            return findResult("ko", 0.99f)
        }
        if (japaneseKanaCount > 0) {
            return findResult("ja", 0.99f)
        }
        if (cjkCount > 0) {
            return findResult("zh", 0.98f)
        }
        if (devanagariCount > 0) {
            return findResult("hi", 0.98f)
        }
        if (bengaliCount > 0) {
            return findResult("bn", 0.98f)
        }
        if (thaiCount > 0) {
            return findResult("th", 0.98f)
        }
        if (arabicCount > 0) {
            // Distinguish Urdu vs Arabic
            val hasUrduChars = text.any { it in listOf('ٹ', 'ڈ', 'ڑ', 'ں', 'ے', 'ہ', 'پ', 'چ', 'ژ', 'گ') }
            return if (hasUrduChars) findResult("ur", 0.96f) else findResult("ar", 0.96f)
        }
        if (cyrillicCount > 0) {
            // Distinguish Ukrainian vs Russian
            val hasUkrainianChars = text.any { it in listOf('і', 'І', 'ї', 'Ї', 'є', 'Є', 'ґ', 'Ґ') }
            val hasRussianChars = text.any { it in listOf('ы', 'Ы', 'э', 'Э', 'ъ', 'Ъ', 'ё', 'Ё') }
            return if (hasUkrainianChars && !hasRussianChars) {
                findResult("uk", 0.97f)
            } else {
                findResult("ru", 0.95f)
            }
        }

        return null
    }

    private fun detectLatinLanguage(text: String): DetectionResult {
        val lower = text.lowercase()
        val tokens = lower.split(Regex("[^\\p{L}]+")).filter { it.isNotBlank() }

        // Vietnamese specific diacritics
        val vietnameseChars = setOf('ơ', 'ư', 'ă', 'â', 'ê', 'ô', 'đ', 'ả', 'ã', 'ạ', 'ấ', 'ầ', 'ẩ', 'ẫ', 'ậ', 'ắ', 'ằ', 'ẳ', 'ẵ', 'ặ', 'ế', 'ề', 'ể', 'ễ', 'ệ', 'ố', 'ồ', 'ổ', 'ỗ', 'ộ', 'ớ', 'ờ', 'ở', 'ỡ', 'ợ', 'ứ', 'ừ', 'ử', 'ữ', 'ự')
        if (lower.any { it in vietnameseChars }) {
            return findResult("vi", 0.97f)
        }

        val scores = mutableMapOf<String, Float>()

        // Tamazight keywords in Latin
        val berWords = setOf("azul", "tanemmirt", "ameddakel", "tudert", "aman", "tamurt", "ihi", "uhu", "amek", "labas", "ansuf", "akud", "agellid")
        scores["ber"] = tokens.count { it in berWords } * 4.0f

        // Kimbundu keywords
        val kmbWords = setOf("kiambote", "sakidila", "ngana", "mona", "kimbundu", "wimbu", "ngaxikana", "mukua", "kudya")
        scores["kmb"] = tokens.count { it in kmbWords } * 4.0f

        // Portuguese
        val ptWords = setOf("olá", "obrigado", "obrigada", "bom", "dia", "tarde", "noite", "como", "está", "estou", "para", "com", "não", "sim", "por", "favor", "muito", "bem", "fazer", "mais", "tempo", "tudo", "você", "nós", "eles", "uma", "um", "das", "dos", "pão", "coração", "música")
        val ptChars = setOf('ã', 'õ')
        scores["pt"] = (tokens.count { it in ptWords } * 1.8f) + (lower.count { it in ptChars } * 2.5f)

        // Spanish
        val esWords = setOf("hola", "gracias", "por", "favor", "buenos", "días", "tardes", "noches", "cómo", "estás", "está", "amigo", "familia", "comida", "agua", "trabajo", "ciudad", "mañana", "corazón", "calle", "dinero", "viaje", "pero", "para", "con", "que", "es", "el", "la", "los", "las", "un", "una")
        val esChars = setOf('ñ', '¿', '¡')
        scores["es"] = (tokens.count { it in esWords } * 1.8f) + (lower.count { it in esChars } * 3.0f)

        // French
        val frWords = setOf("bonjour", "bonsoir", "merci", "plaît", "oui", "non", "ami", "famille", "eau", "pain", "temps", "travail", "ville", "nuit", "monde", "cœur", "soleil", "rue", "argent", "voyage", "paix", "avec", "pour", "dans", "vous", "nous", "est", "sont", "les", "des", "une", "le", "la")
        val frChars = setOf('œ', 'æ', 'è', 'é', 'ê', 'ë', 'à', 'ù')
        scores["fr"] = (tokens.count { it in frWords } * 1.8f) + (lower.count { it in frChars } * 1.2f)

        // English
        val enWords = setOf("hello", "hi", "thank", "you", "thanks", "please", "yes", "no", "friend", "family", "water", "food", "time", "work", "city", "night", "world", "heart", "sun", "street", "money", "journey", "peace", "the", "and", "is", "are", "have", "that", "this", "with", "from", "how", "what", "good", "morning")
        scores["en"] = tokens.count { it in enWords } * 1.7f

        // German
        val deWords = setOf("hallo", "guten", "tag", "morgen", "abend", "danke", "bitte", "ja", "nein", "freund", "familie", "wasser", "brot", "zeit", "arbeit", "stadt", "nacht", "welt", "herz", "sonne", "straße", "geld", "reise", "frieden", "der", "die", "das", "und", "ist", "nicht", "wir", "sie")
        val deChars = setOf('ä', 'ö', 'ü', 'ß')
        scores["de"] = (tokens.count { it in deWords } * 1.8f) + (lower.count { it in deChars } * 2.5f)

        // Italian
        val itWords = setOf("ciao", "grazie", "prego", "favore", "buongiorno", "buonasera", "buonanotte", "amico", "famiglia", "acqua", "pane", "tempo", "lavoro", "città", "notte", "mondo", "cuore", "sole", "strada", "denaro", "viaggio", "pace", "come", "stai", "sono", "perché", "molto", "bene")
        scores["it"] = tokens.count { it in itWords } * 1.8f

        // Dutch
        val nlWords = setOf("hallo", "goedemorgen", "goedenavond", "dank", "alstublieft", "ja", "nee", "vriend", "familie", "water", "brood", "tijd", "werk", "stad", "nacht", "wereld", "hart", "zon", "straat", "geld", "reis", "vrede", "het", "de", "een", "en", "van", "ik")
        scores["nl"] = tokens.count { it in nlWords } * 1.9f

        // Croatian
        val hrWords = setOf("dobar", "dan", "jutro", "večer", "hvala", "molim", "da", "ne", "prijatelj", "obitelj", "voda", "kruh", "vrijeme", "posao", "grad", "noć", "svijet", "srce", "sunce", "ulica", "novac", "putovanje", "mir", "kako", "ste")
        val hrChars = setOf('č', 'ć', 'ž', 'š', 'đ')
        scores["hr"] = (tokens.count { it in hrWords } * 2.0f) + (lower.count { it in hrChars } * 2.0f)

        // Albanian
        val sqWords = setOf("përshëndetje", "faleminderit", "ju", "lutem", "po", "jo", "mik", "familje", "ujë", "bukë", "kohë", "punë", "qytet", "natë", "botë", "zemër", "diell", "rrugë", "para", "udhëtim", "paqe", "mirë", "si", "jeni")
        scores["sq"] = (tokens.count { it in sqWords } * 2.2f) + (lower.count { it == 'ë' } * 2.5f)

        // Danish
        val daWords = setOf("hej", "godmorgen", "godaften", "tak", "vær", "venlig", "ja", "nej", "ven", "familie", "vand", "brød", "tid", "arbejde", "by", "nat", "verden", "hjerte", "sol", "gade", "penge", "rejse", "fred", "hvordan", "har")
        val daChars = setOf('æ', 'ø', 'å')
        scores["da"] = (tokens.count { it in daWords } * 2.0f) + (lower.count { it in daChars } * 2.5f)

        // Finnish
        val fiWords = setOf("hei", "terve", "hyvää", "huomenta", "iltaa", "kiitos", "ole", "hyvä", "kyllä", "ei", "ystävä", "perhe", "vesi", "leipä", "aika", "työ", "kaupunki", "yö", "maailma", "sydän", "aurinko", "katu", "raha", "matka", "rauha", "mitä", "kuuluu")
        scores["fi"] = tokens.count { it in fiWords } * 2.2f

        // Romanian / Moldavian
        val roWords = setOf("salut", "bună", "ziua", "dimineața", "seara", "mulțumesc", "vă", "rog", "da", "nu", "prieten", "familie", "apă", "pâine", "timp", "muncă", "oraș", "noapte", "lume", "inimă", "soare", "stradă", "bani", "călătorie", "pace", "cum", "ești")
        val roChars = setOf('ă', 'î', 'ș', 'ț')
        scores["ro"] = (tokens.count { it in roWords } * 2.0f) + (lower.count { it in roChars } * 2.5f)

        val bestEntry = scores.maxByOrNull { it.value }
        if (bestEntry != null && bestEntry.value > 1.2f) {
            val conf = (0.75f + (bestEntry.value * 0.05f)).coerceAtMost(0.99f)
            return findResult(bestEntry.key, conf)
        }

        // Default if Latin text has few matched stopwords
        return findResult("pt", 0.60f)
    }

    private fun findResult(code: String, confidence: Float): DetectionResult {
        val meta = SupportedLanguages.findByCode(code) ?: SupportedLanguages.ALL.first()
        return DetectionResult(
            languageCode = meta.code,
            languageName = meta.namePt,
            flag = meta.flag,
            script = meta.script,
            confidence = confidence,
            isAutoDetected = true
        )
    }
}
