package org.bhashasetu.fln.edge.engine

class OlckToDevaTransliterator {

    companion object {
        // Ol Chiki to Devanagari character mapping
        // Ol Chiki consonants (U+1C60-U+1C8B) map to Devanagari consonants
        // Ol Chiki matras (U+1C90-U+1C9C) map to Devanagari matras
        private val olckToDeva = mapOf(
            // Vowels
            '\u1c5a' to '\u0905', // ᱚ -> अ (a)
            '\u1c5b' to '\u0906', // ᱛ -> आ (aa)
            '\u1c5c' to '\u0907', // ᱜ -> इ (i)
            '\u1c5d' to '\u0908', // ᱝ -> ई (ii)
            '\u1c5e' to '\u0909', // ᱞ -> उ (u)
            '\u1c5f' to '\u090a', // ᱟ -> ऊ (uu)

            // Consonants
            '\u1c60' to '\u0915', // ᱠ -> क (ka)
            '\u1c61' to '\u0916', // ᱡ -> ख (kha)
            '\u1c62' to '\u0917', // ᱢ -> ग (ga)
            '\u1c63' to '\u0918', // ᱣ -> घ (gha)
            '\u1c64' to '\u0919', // ᱤ -> ङ (nga)
            '\u1c65' to '\u091a', // ᱥ -> च (cha)
            '\u1c66' to '\u091b', // ᱦ -> छ (chha)
            '\u1c67' to '\u091c', // ᱧ -> ज (ja)
            '\u1c68' to '\u091d', // ᱨ -> झ (jha)
            '\u1c69' to '\u091e', // ᱩ -> ञ (nya)
            '\u1c6a' to '\u091f', // ᱪ -> ट (ta)
            '\u1c6b' to '\u0920', // ᱫ -> ठ (tha)
            '\u1c6c' to '\u0921', // ᱬ -> ड (da)
            '\u1c6d' to '\u0922', // ᱭ -> ढ (dha)
            '\u1c6e' to '\u0923', // ᱮ -> ण (na)
            '\u1c6f' to '\u0924', // ᱯ -> त (ta)
            '\u1c70' to '\u0925', // ᱰ -> थ (tha)
            '\u1c71' to '\u0926', // ᱱ -> द (da)
            '\u1c72' to '\u0927', // ᱲ -> ध (dha)
            '\u1c73' to '\u0928', // ᱳ -> न (na)
            '\u1c74' to '\u092a', // ᱴ -> प (pa)
            '\u1c75' to '\u092b', // ᱵ -> फ (pha)
            '\u1c76' to '\u092c', // ᱶ -> ब (ba)
            '\u1c77' to '\u092d', // ᱷ -> भ (bha)
            '\u1c78' to '\u092e', // ᱸ -> म (ma)
            '\u1c79' to '\u092f', // ᱹ -> य (ya)
            '\u1c7a' to '\u0930', // ᱺ -> र (ra)
            '\u1c7b' to '\u0932', // ᱻ -> ल (la)
            '\u1c7c' to '\u0935', // ᱼ -> व (va)
            '\u1c7d' to '\u0936', // ᱽ -> श (sha)
            '\u1c7e' to '\u0937', // ᱾ -> ष (sha)
            '\u1c7f' to '\u0938', // ᱿ -> स (sa)
            '\u1c80' to '\u0939', // ᰀ -> ह (ha)

            // Additional consonants
            '\u1c81' to '\u0915', // ᰁ -> क (ka)
            '\u1c82' to '\u0916', // ᰂ -> ख (kha)
            '\u1c83' to '\u0917', // ᰃ -> ग (ga)
            '\u1c84' to '\u0918', // ᰄ -> घ (gha)
            '\u1c85' to '\u0919', // ᰅ -> ङ (nga)
            '\u1c86' to '\u091a', // ᰆ -> च (cha)
            '\u1c87' to '\u091b', // ᰇ -> छ (chha)
            '\u1c88' to '\u091c', // ᰈ -> ज (ja)
            '\u1c89' to '\u091d', // ᰉ -> झ (jha)
            '\u1c8a' to '\u091e', // ᰊ -> ञ (nya)

            // Matras (vowel signs)
            '\u1c90' to '\u093e', // ᰐ -> ा (aa matra)
            '\u1c91' to '\u093f', // ᰑ -> ि (i matra)
            '\u1c92' to '\u0940', // ᰒ -> ी (ii matra)
            '\u1c93' to '\u0941', // ᰓ -> ु (u matra)
            '\u1c94' to '\u0942', // ᰔ -> ू (uu matra)
            '\u1c95' to '\u0943', // ᰕ -> ृ (ri matra)
            '\u1c96' to '\u0947', // ᰖ -> े (e matra)
            '\u1c97' to '\u0948', // ᰗ -> ै (ai matra)
            '\u1c98' to '\u094b', // ᰘ -> ो (o matra)
            '\u1c99' to '\u094c', // ᰙ -> ौ (au matra)
            '\u1c9a' to '\u0902', // ᰚ -> ं (anusvara)
            '\u1c9b' to '\u0903', // ᰛ -> ः (visarga)
            '\u1c9c' to '\u094d', // ᰜ -> ् (virama)

            // Chandrabindu
            '\u1c9d' to '\u0901', // ᰝ -> ँ (chandrabindu)

            // Punctuation
            '\u1c9e' to '\u0964', // ᰞ -> । (danda)
            '\u1c9f' to '\u0965', // ᰟ -> ॥ (double danda)
        )
    }

    fun transliterate(olckText: String): String {
        val sb = StringBuilder()
        for (char in olckText) {
            val devaChar = olckToDeva[char]
            if (devaChar != null) {
                sb.append(devaChar)
            } else {
                sb.append(char)
            }
        }
        return sb.toString()
    }

    fun isOlChiki(text: String): Boolean {
        return text.any { it in '\u1c60'..'\u1c9f' }
    }
}