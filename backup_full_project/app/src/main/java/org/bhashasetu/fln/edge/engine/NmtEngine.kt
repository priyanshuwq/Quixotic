package org.bhashasetu.fln.edge.engine

import android.content.Context
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

data class TranslationResult(
    val olChiki: String,
    val devanagari: String
)

class NmtEngine(private val context: Context) {

    private var dictionary: MutableMap<String, Pair<String, String>> = mutableMapOf()
    private var phrasebook: MutableMap<String, Pair<String, String>> = mutableMapOf()
    private var initialized = false

    fun initialize() {
        if (initialized) return

        loadDictionary()
        loadPhrasebook()
        initialized = true
    }

    private fun loadDictionary() {
        try {
            val inputStream = context.assets.open("data/hindi_santali_dict.json")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonStr = reader.readText()
            val json = JSONObject(jsonStr)

            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val obj = json.getJSONObject(key)
                val olChiki = obj.getString("olChiki")
                val devanagari = obj.getString("devanagari")
                dictionary[key] = Pair(olChiki, devanagari)
            }
            reader.close()
            println("Dictionary loaded: ${dictionary.size} words")
        } catch (e: Exception) {
            println("Dictionary not found, using built-in")
            loadBuiltinDictionary()
        }
    }

    private fun loadPhrasebook() {
        try {
            val inputStream = context.assets.open("data/phrasebook.json")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonStr = reader.readText()
            val json = JSONObject(jsonStr)

            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val obj = json.getJSONObject(key)
                val olChiki = obj.getString("olChiki")
                val devanagari = obj.getString("devanagari")
                phrasebook[key] = Pair(olChiki, devanagari)
            }
            reader.close()
            println("Phrasebook loaded: ${phrasebook.size} phrases")
        } catch (e: Exception) {
            println("Phrasebook not found, using built-in")
            loadBuiltinPhrasebook()
        }
    }

    private fun loadBuiltinDictionary() {
        // Hindi -> Santali: Pair(Ol Chiki, Devanagari Santali)
        // Devanagari Santali is used for TTS pronunciation
        val words = mapOf(
            // Greetings
            "\u0928\u092e\u0938\u094d\u0924\u0947" to Pair("\u1c61\u1c76\u1c6e", "\u091c\u094b\u0939\u093e\u0930"), // namaste -> johar
            "\u0927\u0928\u094d\u092f\u0935\u093e\u0926" to Pair("\u1c65\u1c76\u1c6e\u1c61 \u1c65\u1c76\u1c6e\u1c61", "\u092c\u0938\u093e \u092c\u0938\u093e"), // dhanyavad -> basa basa

            // Numbers
            "\u090f\u0915" to Pair("\u1c65\u1c6f", "\u090f\u0915"), // ek -> ek
            "\u0926\u094b" to Pair("\u1c65\u1c70", "\u092c\u093e\u0930"), // do -> bar
            "\u0924\u0940\u0928" to Pair("\u1c65\u1c69\u1c81", "\u0905\u092a\u0947\u0924"), // teen -> apet
            "\u091a\u093e\u0930" to Pair("\u1c65\u1c61\u1c76\u1c81", "\u091a\u093e\u0930"), // chaar -> chaar
            "\u092a\u093e\u0902\u091a" to Pair("\u1c65\u1c76\u1c6e\u1c76", "\u092a\u091e\u091a"), // paanch -> panca
            "\u091b\u0939" to Pair("\u1c75\u1c66", "\u091b\u0947"), // chhah -> chhe
            "\u0938\u093e\u0924" to Pair("\u1c61\u1c65", "\u0938\u093e\u0924"), // saat -> saat
            "\u0906\u0920" to Pair("\u1c61\u1c65\u1c81", "\u0906\u0920"), // aath -> aath
            "\u0928\u0949" to Pair("\u1c65\u1c68", "\u0928\u0949"), // nau -> nau
            "\u0926\u0938" to Pair("\u1c61\u1c76", "\u0926\u0938"), // das -> das

            // School vocabulary
            "\u0935\u093f\u0926\u094d\u092f\u093e\u0932\u092f" to Pair("\u1c65\u1c76\u1c6e\u1c6e\u1c61", "\u0935\u093f\u0926\u094d\u092f\u093e\u0932\u092f"), // vidyalay -> vidyalay
            "\u0935\u093f\u0926\u094d\u092f\u093e\u0930\u094d\u0925\u0940" to Pair("\u1c65\u1c76\u1c6e\u1c6e\u1c65", "\u0935\u093f\u0926\u094d\u092f\u093e\u0930\u094d\u0925\u0940"), // vidyarthi -> vidyarthi
            "\u0936\u093f\u0915\u094d\u0937\u0915" to Pair("\u1c65\u1c76\u1c6e\u1c6e\u1c65", "\u0936\u093f\u0915\u094d\u0937\u0915"), // shikshak -> shikshak
            "\u092a\u0941\u0938\u094d\u0924\u093e\u0915" to Pair("\u1c61\u1c73\u1c66\u1c76", "\u092a\u0941\u0938\u094d\u0924\u0915"), // pustak -> pustak
            "\u0915\u0932\u092e" to Pair("\u1c4a\u1c66\u1c73\u1c76", "\u0915\u0932\u092e"), // kalam -> kalam
            "\u092e\u0947\u091c\u093c\u093e" to Pair("\u1c65\u1c72\u1c76\u1c71", "\u092e\u0947\u091c\u093c\u093e"), // meza -> meza
            "\u0915\u0941\u0930\u094d\u0938\u0940" to Pair("\u1c65\u1c6e\u1c66\u1c76", "\u0915\u0941\u0930\u094d\u0938\u0940"), // kursi -> kursi

            // Actions
            "\u092a\u0922\u093c\u0928\u093e" to Pair("\u1c61\u1c81\u1c6b\u1c76", "\u092a\u0922\u093c\u0928\u093e"), // padhna -> padhna
            "\u0932\u093f\u0916\u0932\u0928\u093e" to Pair("\u1c42\u1c6b\u1c76", "\u0932\u093f\u0916\u0928\u093e"), // likhna -> likhna
            "\u092c\u094b\u0932\u0928\u093e" to Pair("\u1c65\u1c73\u1c73\u1c61\u1c76", "\u092c\u094b\u0932\u0928\u093e"), // bolna -> bolna
            "\u0938\u0941\u0928\u0928\u093e" to Pair("\u1c65\u1c76\u1c76\u1c73\u1c76", "\u0938\u0941\u0928\u0928\u093e"), // sunna -> sunna
            "\u0926\u0947\u0916\u0928\u093e" to Pair("\u1c61\u1c81\u1c61\u1c73\u1c58\u1c76", "\u0926\u0947\u0916\u0928\u093e"), // dekhna -> dekhna
            "\u0938\u0940\u0916\u0928\u093e" to Pair("\u1c61\u1c81\u1c76\u1c61\u1c76", "\u0938\u0940\u0916\u0928\u093e"), // seekhna -> seekhna

            // Common words
            "\u0939\u093e\u0902" to Pair("\u1c65\u1c76", "\u0939\u093e\u0902"), // haan -> haan
            "\u0928\u0939\u0940\u0902" to Pair("\u1c6b\u1c63\u1c76", "\u0928\u0939\u0940\u0902"), // nahin -> nahin
            "\u0905\u091a\u094d\u091b\u093e" to Pair("\u1c61\u1c76\u1c6e\u1c61", "\u0905\u091a\u094d\u091b\u093e"), // accha -> accha
            "\u092c\u0941\u0930\u093e" to Pair("\u1c65\u1c66", "\u092c\u0941\u0930\u093e"), // bura -> bura
            "\u092e\u0947\u0930\u093e" to Pair("\u1c65\u1c72\u1c76", "\u092e\u0947\u0930\u093e"), // mera -> mera
            "\u0924\u0947\u0930\u093e" to Pair("\u1c65\u1c72\u1c76\u1c71", "\u0924\u0947\u0930\u093e"), // tera -> tera
            "\u092f\u0939" to Pair("\u1c68\u1c7d", "\u092f\u0939"), // yah -> yah
            "\u0935\u0939" to Pair("\u1c6e\u1c76\u1c6e\u1c7d", "\u0935\u0939"), // vah -> vah
            "\u0915\u094d\u092f\u093e" to Pair("\u1c4a\u1c76\u1c6e", "\u0915\u094d\u092f\u093e"), // kya -> kya
            "\u0915\u0939\u093e\u0901" to Pair("\u1c6e\u1c61\u1c76\u1c7d", "\u0915\u0939\u093e\u0901"), // kahan -> kahan
            "\u0915\u094d\u092f\u094b\u0902" to Pair("\u1c4a\u1c76\u1c6e\u1c81\u1c76", "\u0915\u094d\u092f\u094b\u0902"), // kyon -> kyon
            "\u0915\u0948\u0938\u0947" to Pair("\u1c4a\u1c61\u1c73\u1c76", "\u0915\u0948\u0938\u0947"), // kaise -> kaise
            "\u0915\u0935" to Pair("\u1c4a\u1c61", "\u0915\u0935"), // kab -> kab

            // Colors
            "\u0932\u093e\u0932" to Pair("\u1c42\u1c6b\u1c76\u1c61\u1c76", "\u0932\u093e\u0932"), // laal -> laal
            "\u0928\u0940\u0932\u093e" to Pair("\u1c66\u1c75\u1c6f", "\u0928\u0940\u0932\u093e"), // neela -> neela
            "\u0939\u0930\u093e" to Pair("\u1c61\u1c65\u1c72\u1c6f", "\u0939\u0930\u093e"), // hara -> hara
            "\u092a\u0940\u0932\u093e" to Pair("\u1c66\u1c76\u1c71", "\u092a\u0940\u0932\u093e"), // peela -> peela
            "\u0938\u0947\u0926\u093c" to Pair("\u1c66\u1c76\u1c71", "\u0938\u0947\u0926"), // safed -> safed
            "\u0915\u093e\u0932\u093e" to Pair("\u1c65\u1c66\u1c6e\u1c76", "\u0915\u093e\u0932\u093e"), // kaala -> kaala

            // Body parts
            "\u0939\u093e\u0925" to Pair("\u1c65\u1c76", "\u0939\u093e\u0925"), // haath -> haath
            "\u092a\u0948\u0930" to Pair("\u1c65\u1c72\u1c73\u1c76", "\u092a\u0948\u0930"), // pair -> pair
            "\u0906\u0916" to Pair("\u1c61\u1c76", "\u0906\u0916"), // aakh -> aakh
            "\u0915\u093e\u0928" to Pair("\u1c60\u1c76\u1c71", "\u0915\u093e\u0928"), // kaan -> kaan
            "\u0928\u093e\u0915" to Pair("\u1c65\u1c70\u1c6f", "\u0928\u093e\u0915"), // naak -> naak
            "\u092e\u0941\u0902\u0939" to Pair("\u1c65\u1c6e", "\u092e\u0941\u0902\u0939"), // muh -> muh
            "\u0938\u093f\u0930" to Pair("\u1c65\u1c72\u1c73\u1c76", "\u0938\u093f\u0930"), // sir -> sir
            "\u092a\u0947\u091f" to Pair("\u1c61\u1c73\u1c66\u1c76", "\u092a\u0947\u091f"), // pet -> pet

            // Nature
            "\u092a\u093e\u0928\u0940" to Pair("\u1c6e\u1c73\u1c66\u1c76", "\u092a\u093e\u0928\u0940"), // paani -> paani
            "\u092e\u093f\u091f\u094d\u091f\u0940" to Pair("\u1c60\u1c76\u1c71", "\u092e\u093f\u091f\u094d\u091f\u0940"), // mitti -> mitti
            "\u092a\u0947\u0921\u093c" to Pair("\u1c65\u1c76\u1c6f", "\u092a\u0947\u0921"), // ped -> ped
            "\u092b\u0942\u0932" to Pair("\u1c65\u1c76\u1c73\u1c76", "\u092b\u0942\u0932"), // phool -> phool
            "\u092a\u0924\u094d\u0924\u093e" to Pair("\u1c65\u1c76\u1c6e", "\u092a\u0924\u094d\u0924\u093e"), // pattha -> pattha
            "\u092a\u094d\u0939\u093e\u0921\u093c\u093e" to Pair("\u1c65\u1c76\u1c6e\u1c6e\u1c65", "\u092a\u0939\u093e\u0921\u093c\u093e"), // pahaaD -> pahaad
            "\u0926\u0947\u0935\u0938\u0932\u093e" to Pair("\u1c42\u1c6b\u1c76\u1c71\u1c61\u1c76", "\u0926\u0947\u0935\u0938\u0932\u093e"), // devsala -> devsala

            // Food
            "\u091a\u093c\u0935\u093e\u0932" to Pair("\u1c65\u1c76\u1c61\u1c76\u1c73\u1c76", "\u091a\u093c\u0935\u093e\u0932"), // chawal -> chawal
            "\u0930\u094b\u091f\u0940" to Pair("\u1c65\u1c76\u1c6f\u1c6e\u1c61", "\u0930\u094b\u091f\u0940"), // roti -> roti
            "\u0926\u093e\u0932" to Pair("\u1c61\u1c76\u1c61\u1c76\u1c61", "\u0926\u093e\u0932"), // daal -> daal
            "\u0926\u0942\u0927" to Pair("\u1c68\u1c76\u1c66\u1c6e", "\u0926\u0942\u0927"), // doodh -> doodh
            "\u092b\u0932" to Pair("\u1c65\u1c76\u1c73\u1c76", "\u092b\u0932"), // phal -> phal
            "\u0938\u092c\u093c\u0940\u091c\u093c\u0940" to Pair("\u1c65\u1c76\u1c73\u1c76", "\u0938\u092c\u093c\u0940\u091c\u093c\u0940"), // sabji -> sabji

            // Time
            "\u0938\u0941\u092c\u0939" to Pair("\u1c61\u1c6f\u1c76\u1c6e\u1c76", "\u0938\u0941\u092c\u0939"), // subah -> subah
            "\u0926\u094b\u092a\u0939\u0930" to Pair("\u1c65\u1c72\u1c73\u1c76\u1c6e\u1c76", "\u0926\u094b\u092a\u0939\u0930"), // dopahar -> dopahar
            "\u0936\u093e\u092e" to Pair("\u1c61\u1c7e\u1c66\u1c76", "\u0936\u093e\u092e"), // shaam -> shaam
            "\u0930\u093e\u0924" to Pair("\u1c6e\u1c61\u1c76\u1c76\u1c76", "\u0930\u093e\u0924"), // raat -> raat
            "\u0926\u093f\u0928" to Pair("\u1c61\u1c6f\u1c76\u1c6e\u1c76", "\u0926\u093f\u0928"), // din -> din
            "\u092e\u0939\u0940\u0928\u093e" to Pair("\u1c60\u1c61\u1c76\u1c6e", "\u092e\u0939\u0940\u0928\u093e"), // mahina -> mahina
            "\u0938\u093e\u0932" to Pair("\u1c65\u1c76", "\u0938\u093e\u0932"), // saal -> saal

            // Class room
            "\u0915\u0932\u093e\u0938" to Pair("\u1c60\u1c73\u1c66\u1c76", "\u0915\u0932\u093e\u0938"), // class -> class
            "\u092e\u0947\u0932" to Pair("\u1c65\u1c72\u1c76", "\u092e\u0947\u0932"), // mel -> mel
            "\u0915\u092e" to Pair("\u1c4a\u1c61\u1c73\u1c76", "\u0915\u092e"), // kam -> kam
            "\u092c\u0930\u093e\u092c\u0930" to Pair("\u1c65\u1c76\u1c61\u1c76", "\u092c\u0930\u093e\u092c\u0930"), // barabar -> barabar

            // Colors
            "\u0932\u093e\u0932" to Pair("\u1c42\u1c6b\u1c76\u1c61\u1c76", "\u0932\u093e\u0932"), // laal -> laal
            "\u0928\u0940\u0932\u093e" to Pair("\u1c66\u1c75\u1c6f", "\u0928\u0940\u0932\u093e"), // neela -> neela
            "\u0939\u0930\u093e" to Pair("\u1c61\u1c65\u1c72\u1c6f", "\u0939\u0930\u093e"), // hara -> hara
            "\u092a\u0940\u0932\u093e" to Pair("\u1c66\u1c76\u1c71", "\u092a\u0940\u0932\u093e"), // peela -> peela
            "\u0938\u0947\u0926\u093c" to Pair("\u1c66\u1c76\u1c71", "\u0938\u0947\u0926"), // safed -> safed
            "\u0915\u093e\u0932\u093e" to Pair("\u1c65\u1c66\u1c6e\u1c76", "\u0915\u093e\u0932\u093e"), // kaala -> kaala
        )
        dictionary.putAll(words)
    }

    private fun loadBuiltinPhrasebook() {
        // Hindi phrases -> Santali: Pair(Ol Chiki, Devanagari Santali)
        val phrases = mapOf(
            "\u0928\u092e\u0938\u094d\u0924\u0947 \u092c\u091a\u094d\u091a\u094b\u0902" to Pair("\u1c61\u1c76\u1c6e \u1c65\u1c76\u1c6e\u1c6e\u1c65", "\u091c\u094b\u0939\u093e\u0930 \u092c\u091a\u094d\u091a\u094b\u0902"), // namaste bachchon -> johar bachchon
            "\u0915\u0948\u0938\u0947 \u0939\u094b" to Pair("\u1c4a\u1c61\u1c73\u1c76 \u1c65\u1c76", "\u0915\u0948\u0938\u0947 \u0939\u094b"), // kaise ho -> kaise ho
            "\u092e\u0948\u0902 \u0905\u091a\u094d\u091b\u093e \u0939\u0942\u0902" to Pair("\u1c68 \u1c61\u1c76\u1c6e\u1c61 \u1c65\u1c76", "\u092e\u0948\u0902 \u0905\u091a\u094d\u091b\u093e \u0939\u0942\u0902"), // main accha hoon -> main accha hoon
            "\u092a\u0922\u093c\u094b \u092a\u0922\u093c\u094b" to Pair("\u1c61\u1c81\u1c6b\u1c76 \u1c61\u1c81\u1c6b\u1c76", "\u092a\u0922\u093c\u094b \u092a\u0922\u093c\u094b"), // padho padho -> padho padho
            "\u0932\u093f\u0916\u094b" to Pair("\u1c42\u1c6b\u1c76", "\u0932\u093f\u0916\u094b"), // likho -> likho
            "\u0927\u094d\u092f\u093e\u0928 \u092e\u0939\u094b\u0926\u092f" to Pair("\u1c6e\u1c61\u1c76\u1c76\u1c76 \u1c65\u1c72\u1c76\u1c71", "\u0927\u094d\u092f\u093e\u0928\u094b\u0926 \u0938\u093e\u0939\u093e\u092c"), // dhanyavad mahoday -> dhanyavad sahab
            "\u092f\u0939 \u0915\u094d\u092f\u093e \u0939\u0948" to Pair("\u1c68\u1c7d \u1c4a\u1c76\u1c6e \u1c65\u1c76", "\u092f\u0939 \u0915\u094d\u092f\u093e \u0939\u0948"), // yah kya hai -> yah kya hai
        )
        phrasebook.putAll(phrases)
    }

    fun translate(hindiText: String): TranslationResult {
        if (!initialized) initialize()

        val normalizedInput = hindiText.trim().lowercase()

        for ((phrase, translation) in phrasebook) {
            if (normalizedInput.contains(phrase.lowercase())) {
                return TranslationResult(translation.first, translation.second)
            }
        }

        val words = hindiText.split("\\s+".toRegex())
        val translatedOlChiki = mutableListOf<String>()
        val translatedDevanagari = mutableListOf<String>()

        for (word in words) {
            val result = translateWord(word)
            if (result != null) {
                translatedOlChiki.add(result.first)
                translatedDevanagari.add(result.second)
            } else {
                translatedOlChiki.add(word)
                translatedDevanagari.add(word)
            }
        }

        return TranslationResult(
            translatedOlChiki.joinToString(" "),
            translatedDevanagari.joinToString(" ")
        )
    }

    private fun translateWord(word: String): Pair<String, String>? {
        val normalized = word.lowercase()
            .replace("\u0964", "")
            .replace("\u0965", "")
            .replace("?", "")
            .replace("!", "")
            .replace(".", "")
            .replace(",", "")

        dictionary[normalized]?.let { return it }

        val suffixes = listOf(
            "\u0939\u093e", "\u0924\u093e", "\u0924\u0947", "\u0924\u0940",
            "\u0928\u093e", "\u0928\u0947", "\u0928\u0940",
            "\u0915\u093e", "\u0915\u0947", "\u0915\u0940",
            "\u0938\u0947", "\u092e\u0947", "\u092a\u0930"
        )

        for (suffix in suffixes) {
            if (normalized.endsWith(suffix) && normalized.length > suffix.length + 2) {
                val stem = normalized.dropLast(suffix.length)
                dictionary[stem]?.let { return it }
            }
        }

        return null
    }

    fun isInitialized(): Boolean = initialized
}
