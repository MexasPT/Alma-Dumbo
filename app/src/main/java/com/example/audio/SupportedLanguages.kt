package com.example.audio

data class LanguageMeta(
    val code: String,
    val namePt: String,
    val nativeName: String,
    val flag: String,
    val family: String,
    val script: String,
    val region: String,
    val sampleGreeting: String
)

object SupportedLanguages {
    val ALL: List<LanguageMeta> = listOf(
        LanguageMeta(
            code = "pt",
            namePt = "Português",
            nativeName = "Português",
            flag = "🇵🇹",
            family = "Românica",
            script = "Latino",
            region = "Portugal, Brasil, Angola, Moçambique, Cabo Verde, Guiné-Bissau",
            sampleGreeting = "Olá, como está?"
        ),
        LanguageMeta(
            code = "es",
            namePt = "Castelhano / Espanhol",
            nativeName = "Castellano / Español",
            flag = "🇪🇸",
            family = "Românica",
            script = "Latino",
            region = "Espanha, América Latina, Guiné Equatorial",
            sampleGreeting = "¡Hola! ¿Cómo estás hoy?"
        ),
        LanguageMeta(
            code = "fr",
            namePt = "Francês",
            nativeName = "Français",
            flag = "🇫🇷",
            family = "Românica",
            script = "Latino",
            region = "França, Bélgica, Canadá, Suíça, África Francófona",
            sampleGreeting = "Bonjour, comment allez-vous ?"
        ),
        LanguageMeta(
            code = "en",
            namePt = "Inglês",
            nativeName = "English",
            flag = "🇬🇧",
            family = "Germânica Ocidental",
            script = "Latino",
            region = "Reino Unido, EUA, Canadá, Austrália, Global",
            sampleGreeting = "Hello, how are you doing?"
        ),
        LanguageMeta(
            code = "it",
            namePt = "Italiano",
            nativeName = "Italiano",
            flag = "🇮🇹",
            family = "Românica",
            script = "Latino",
            region = "Itália, Suíça, San Marino, Vaticano",
            sampleGreeting = "Ciao, come stai?"
        ),
        LanguageMeta(
            code = "de",
            namePt = "Alemão",
            nativeName = "Deutsch",
            flag = "🇩🇪",
            family = "Germânica Ocidental",
            script = "Latino",
            region = "Alemanha, Áustria, Suíça, Luxemburgo, Liechtenstein",
            sampleGreeting = "Guten Tag, wie geht es Ihnen?"
        ),
        LanguageMeta(
            code = "nl",
            namePt = "Holandês / Neerlandês",
            nativeName = "Nederlands",
            flag = "🇳🇱",
            family = "Germânica Ocidental",
            script = "Latino",
            region = "Países Baixos, Bélgica (Flandres), Suriname",
            sampleGreeting = "Hallo, hoe gaat het met u?"
        ),
        LanguageMeta(
            code = "hr",
            namePt = "Croata",
            nativeName = "Hrvatski jezik",
            flag = "🇭🇷",
            family = "Eslava Meridional",
            script = "Latino",
            region = "Croácia, Bósnia e Herzegovina, Voivodina",
            sampleGreeting = "Dobar dan, kako ste?"
        ),
        LanguageMeta(
            code = "sq",
            namePt = "Albanês",
            nativeName = "Gjuha shqipe / Shqip",
            flag = "🇦🇱",
            family = "Indo-Europeia (Albanesa)",
            script = "Latino",
            region = "Albânia, Kosovo, Macedónia do Norte, Montenegro",
            sampleGreeting = "Përshëndetje, si jeni?"
        ),
        LanguageMeta(
            code = "da",
            namePt = "Dinamarquês",
            nativeName = "Dansk",
            flag = "🇩🇰",
            family = "Germânica Setentrional",
            script = "Latino",
            region = "Dinamarca, Ilhas Faroé, Gronelândia",
            sampleGreeting = "Hej, hvordan har du det?"
        ),
        LanguageMeta(
            code = "fi",
            namePt = "Finlandês",
            nativeName = "Suomen kieli / Suomi",
            flag = "🇫🇮",
            family = "Urálica (Fino-Úgrica)",
            script = "Latino",
            region = "Finlândia, Suécia, Noruega",
            sampleGreeting = "Hei, mitä kuuluu?"
        ),
        LanguageMeta(
            code = "ber",
            namePt = "Tamazight (Berbere)",
            nativeName = "Tamaziɣt / ⵜⴰⵎⴰⵣⵉⵖⵜ",
            flag = "ⵣ",
            family = "Afro-Asiática (Berbere)",
            script = "Tifinagh / Latino / Árabe",
            region = "Norte de África (Marrocos, Argélia, Tunísia, Líbia, Saara)",
            sampleGreeting = "Azul fell-awen / ⴰⵣⵓⵍ (Azul)"
        ),
        LanguageMeta(
            code = "ur",
            namePt = "Paquistanês (Urdu / Punjabi)",
            nativeName = "اردو / پنجابی",
            flag = "🇵🇰",
            family = "Indo-Ariana",
            script = "Perso-Árabe (Nasta'liq)",
            region = "Paquistão",
            sampleGreeting = "السلام علیکم (Assalam-o-Alaikum)"
        ),
        LanguageMeta(
            code = "hi",
            namePt = "Indiano (Hindi / Tamil / Telugu)",
            nativeName = "हिन्दी / தமிழ்",
            flag = "🇮🇳",
            family = "Indo-Ariana / Dravidiana",
            script = "Devanagari / Dravidiano",
            region = "Índia",
            sampleGreeting = "नमस्ते (Namaste)"
        ),
        LanguageMeta(
            code = "bn",
            namePt = "Bangladesh (Bengali / Bangla)",
            nativeName = "বাংলা",
            flag = "🇧🇩",
            family = "Indo-Ariana",
            script = "Alfabeto Bengali",
            region = "Bangladesh, Bengala Ocidental",
            sampleGreeting = "নমস্কার / আসসালামু আলাইকুম"
        ),
        LanguageMeta(
            code = "ar",
            namePt = "Árabe",
            nativeName = "العربية",
            flag = "🇸🇦",
            family = "Semítica",
            script = "Árabe",
            region = "Mundo Árabe, Médio Oriente, Norte de África",
            sampleGreeting = "مرحباً كيف حالك؟ (Marhaban)"
        ),
        LanguageMeta(
            code = "uk",
            namePt = "Ucraniano",
            nativeName = "Українська",
            flag = "🇺🇦",
            family = "Eslava Oriental",
            script = "Cirílico",
            region = "Ucrânia",
            sampleGreeting = "Привіт, як справи? (Pryvit)"
        ),
        LanguageMeta(
            code = "mo",
            namePt = "Moldavo",
            nativeName = "Limba moldovenească",
            flag = "🇲🇩",
            family = "Românica Oriental",
            script = "Latino",
            region = "Moldávia",
            sampleGreeting = "Bună ziua, ce mai faci?"
        ),
        LanguageMeta(
            code = "ro",
            namePt = "Romeno",
            nativeName = "Română",
            flag = "🇷🇴",
            family = "Românica Oriental",
            script = "Latino",
            region = "Roménia, Moldávia",
            sampleGreeting = "Salut! Cum ești?"
        ),
        LanguageMeta(
            code = "ko",
            namePt = "Coreano",
            nativeName = "한국어",
            flag = "🇰🇷",
            family = "Coreânica",
            script = "Hangul",
            region = "Coreia do Sul, Coreia do Norte",
            sampleGreeting = "안녕하세요 (Annyeonghaseyo)"
        ),
        LanguageMeta(
            code = "ja",
            namePt = "Japonês",
            nativeName = "日本語",
            flag = "🇯🇵",
            family = "Japônica",
            script = "Kanji, Hiragana, Katakana",
            region = "Japão",
            sampleGreeting = "こんにちは (Konnichiwa)"
        ),
        LanguageMeta(
            code = "th",
            namePt = "Tailandês",
            nativeName = "ภาษาไทย",
            flag = "🇹🇭",
            family = "Kra-Dai",
            script = "Alfabeto Tailandês",
            region = "Tailândia",
            sampleGreeting = "สวัสดีครับ / สวัสดีค่ะ (Sawasdee)"
        ),
        LanguageMeta(
            code = "vi",
            namePt = "Vietnamita",
            nativeName = "Tiếng Việt",
            flag = "🇻🇳",
            family = "Austro-asiática",
            script = "Chữ Quốc ngữ (Latino com diacríticos)",
            region = "Vietname",
            sampleGreeting = "Xin chào, bạn khỏe không?"
        ),
        LanguageMeta(
            code = "zh",
            namePt = "Chinês / Mandarim",
            nativeName = "普通话 / 中文",
            flag = "🇨🇳",
            family = "Sino-Tibetana",
            script = "Caracteres Chineses (Hanzi)",
            region = "China, Taiwan, Singapura",
            sampleGreeting = "你好，最近怎么样？ (Nǐ hǎo)"
        ),
        LanguageMeta(
            code = "ru",
            namePt = "Russo",
            nativeName = "Русский",
            flag = "🇷🇺",
            family = "Eslava Oriental",
            script = "Cirílico",
            region = "Rússia, Leste Europeu, Ásia Central",
            sampleGreeting = "Здравствуйте, как поживаете?"
        )
    )

    fun findByCode(code: String): LanguageMeta? {
        val clean = code.trim().lowercase()
        return ALL.firstOrNull { it.code == clean || it.code.startsWith(clean) }
    }

    fun findByNameOrCode(name: String): LanguageMeta? {
        val lower = name.lowercase().trim()
        return ALL.firstOrNull {
            it.namePt.lowercase().contains(lower) ||
            it.nativeName.lowercase().contains(lower) ||
            it.code.equals(lower, ignoreCase = true)
        }
    }
}
