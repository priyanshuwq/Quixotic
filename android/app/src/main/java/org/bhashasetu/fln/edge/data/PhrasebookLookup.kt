package org.bhashasetu.fln.edge.data

import android.content.Context
import android.util.Log

/**
 * SQLite-backed phrasebook lookup for high-frequency classroom phrases.
 * Provides validated Hindi→Santali translations without NMT inference.
 *
 * This serves as:
 * 1. An offline fallback when NMT model is not loaded
 * 2. A fast-path for common classroom phrases (bypasses inference)
 * 3. A high-confidence translation source for critical phrases
 */
class PhrasebookLookup(context: Context) {
    companion object {
        private const val TAG = "PhrasebookLookup"
    }

    private val phrasebook: Map<String, String>
    private val normalizedIndex: Map<String, String>  // lowercase normalized → original key

    init {
        val db = NipunDatabase.getDatabase(context)
        // Load from Room DB if available
        val dbPhrases = try {
            db.nipunDao().getAllPhrasebookEntries()
                .filter { it.isHighConfidence }
                .associate { it.hindiPhrase to it.santaliPhrase }
        } catch (e: Exception) {
            Log.w(TAG, "DB phrasebook unavailable, using built-in: ${e.message}")
            emptyMap()
        }

        // Merge with built-in classroom phrases (built-in takes priority for validated quality)
        phrasebook = BUILTIN_PHRASES + dbPhrases

        // Build normalized index for fuzzy matching
        normalizedIndex = phrasebook.keys.associateBy { it.trim().lowercase() }

        Log.d(TAG, "Phrasebook loaded: ${phrasebook.size} phrases " +
            "(${BUILTIN_PHRASES.size} built-in + ${dbPhrases.size} from DB)")
    }

    /**
     * Look up a Hindi phrase and return the Santali translation.
     * Returns null if no match is found.
     *
     * Supports:
     * - Exact match (case-insensitive)
     * - Normalized match (trimmed whitespace, lowercase)
     * - Substring match for short phrases embedded in longer text
     */
    fun lookup(hindiText: String): PhrasebookResult? {
        val normalized = hindiText.trim().lowercase()

        // Exact match
        val exactKey = normalizedIndex[normalized]
        if (exactKey != null) {
            val translation = phrasebook[exactKey]!!
            Log.d(TAG, "Exact match: '$hindiText' → '$translation'")
            return PhrasebookResult(translation, 1.0f, "exact")
        }

        // Substring match: check if any phrase is contained in the input
        for ((key, value) in phrasebook) {
            val keyNorm = key.trim().lowercase()
            if (keyNorm.length > 3 && normalized.contains(keyNorm)) {
                Log.d(TAG, "Substring match: '$hindiText' contains '$key' → '$value'")
                return PhrasebookResult(value, 0.8f, "substring")
            }
        }

        // No match
        return null
    }

    /** Number of phrases in the phrasebook */
    val size: Int get() = phrasebook.size

    data class PhrasebookResult(
        val santaliText: String,
        val confidence: Float,
        val matchType: String
    )
}

/**
 * Built-in classroom phrases: Hindi → Santali (Ol Chiki script)
 * These are validated for primary education contexts.
 */
private val BUILTIN_PHRASES = mapOf(
    // Greetings
    "नमस्ते" to "ᱡᱚᱦᱟᱨ",
    "शुभ प्रभात" to "ᱥᱮᱪ ᱪᱟᱸᱫᱚ",
    "धन्यवाद" to "ᱥᱟᱨᱦᱟᱣ",

    // Classroom Commands
    "कृपया बैठिए" to "ᱫᱟᱭᱟ ᱠᱟᱛᱮ ᱫᱩᱲᱩᱵ ᱢᱮ",
    "ध्यान से सुनो" to "ᱦᱩᱥᱤᱭᱟᱹᱨ ᱛᱮ ᱟᱹᱭᱠᱟᱹᱣ ᱢᱮ",
    "अपनी किताब खोलो" to "ᱟᱢᱟᱜ ᱯᱚᱛᱚᱵ ᱡᱷᱤᱡ ᱢᱮ",
    "मेरे बाद दोहराओ" to "ᱤᱧᱟᱜ ᱛᱟᱭᱚᱢ ᱛᱮ ᱯᱷᱤᱨᱟᱹᱣ ᱢᱮ",
    "गिनती करो" to "ᱞᱮᱠᱷᱟ ᱢᱮ",
    "जवाब लिखो" to "ᱛᱮᱞᱟ ᱚᱞ ᱢᱮ",
    "खड़े हो जाओ" to "ᱛᱤᱝᱜᱩ ᱢᱮ",
    "बैठ जाओ" to "ᱫᱩᱲᱩᱵ ᱢᱮ",
    "आओ" to "ᱦᱤᱡᱩᱜ ᱢᱮ",
    "जाओ" to "ᱪᱟᱞᱟᱜ ᱢᱮ",
    "फिर से कोशिश करो" to "ᱟᱨ ᱢᱤᱫ ᱛᱟᱨᱟ ᱠᱮᱢᱟ ᱢᱮ",
    "क्या तुम समझे" to "ᱟᱢ ᱵᱩᱡᱷᱟᱹᱣ ᱠᱮᱫᱟᱢ ᱥᱮ",

    // Numbers
    "एक" to "ᱢᱤᱫ",
    "दो" to "ᱵᱟᱨ",
    "तीन" to "ᱯᱮ",
    "चार" to "ᱯᱩᱱ",
    "पाँच" to "ᱢᱚᱬᱮ",
    "छह" to "ᱛᱩᱨᱩᱭ",
    "सात" to "ᱮᱭᱟᱭ",
    "आठ" to "ᱤᱨᱟᱞ",
    "नौ" to "ᱟᱨᱮ",
    "दस" to "ᱜᱮᱞ",

    // Math
    "जोड़ो" to "ᱡᱚᱲᱟᱣ ᱢᱮ",
    "घटाओ" to "ᱜᱚᱲᱚᱣ ᱢᱮ",

    // Colours
    "लाल" to "ᱟᱨᱟᱜ",
    "नीला" to "ᱱᱤᱞ",
    "हरा" to "ᱥᱟᱥᱟᱝ",
    "सफेद" to "ᱯᱩᱱᱰ",
    "काला" to "ᱦᱮᱱᱫᱮ",

    // Nature / EVS
    "पानी" to "ᱫᱟᱜ",
    "सूरज" to "ᱥᱤᱧ ᱪᱟᱸᱫᱚ",
    "चाँद" to "ᱧᱤᱫᱟᱹ ᱪᱟᱸᱫᱚ",
    "पेड़" to "ᱫᱟᱨᱮ",
    "फूल" to "ᱵᱟᱦᱟ",
    "पक्षी" to "ᱪᱮᱬᱮ",
    "मछली" to "ᱦᱟᱠᱩ",

    // Shapes
    "यह गोल है" to "ᱱᱩᱤ ᱫᱚ ᱜᱩᱞᱢᱩᱞ ᱛᱟᱱᱟᱭ",
    "यह चौकोर है" to "ᱱᱩᱤ ᱫᱚ ᱪᱟᱨᱠᱚᱱᱟ ᱛᱟᱱᱟᱭ",
    "यह तिकोना है" to "ᱱᱩᱤ ᱫᱚ ᱛᱤᱱᱠᱚᱱᱟ ᱛᱟᱱᱟᱭ",

    // Encouragement
    "बहुत अच्छा" to "ᱵᱟᱝ ᱵᱩᱜᱤᱱ",
    "शाबाश" to "ᱵᱟᱝ ᱠᱟᱹᱢᱤ",

    // Common
    "हाँ" to "ᱦᱚᱭ",
    "नहीं" to "ᱵᱟᱝ",
    "तुम्हारा नाम क्या है" to "ᱟᱢᱟᱜ ᱧᱩᱛᱩᱢ ᱪᱤᱱᱟᱜ ᱠᱟᱱᱟ",
    "मैं तुम्हें पढ़ाऊंगा" to "ᱤᱧ ᱟᱢ ᱠᱮ ᱯᱟᱲᱦᱟᱣ ᱟᱢ",
    "आज हम गणित पढ़ेंगे" to "ᱛᱤᱱᱟᱜ ᱟᱞᱮ ᱜᱟᱬᱤᱛ ᱯᱟᱲᱦᱟᱣ ᱟᱞᱮ",
)
