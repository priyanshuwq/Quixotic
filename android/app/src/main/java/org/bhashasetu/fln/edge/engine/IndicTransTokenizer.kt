package org.bhashasetu.fln.edge.engine

import android.content.Context
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * BPE Tokenizer for IndicTrans2 NMT model.
 * Loads vocabulary from assets and performs byte-pair encoding.
 */
class IndicTransTokenizer(context: Context, language: String = "sat") {
    private val vocab: Map<String, Int>
    private val invVocab: Map<Int, String>
    private val merges: List<Pair<String, String>>
    private val unkTokenId: Int
    private val bosTokenId: Int
    private val eosTokenId: Int
    private val padTokenId: Int

    init {
        val assets = context.assets

        // Load vocabulary
        val vocabJson = loadAssetJson(assets, "models/nmt/dict.TGT.json")
        vocab = mutableMapOf<String, Int>().apply {
            vocabJson.keys().forEach { key ->
                put(key, vocabJson.getInt(key))
            }
        }
        invVocab = vocab.entries.associate { (k, v) -> v to k }

        // Special tokens
        unkTokenId = vocab["<unk>"] ?: 0
        bosTokenId = vocab["<s>"] ?: 1
        eosTokenId = vocab["</s>"] ?: 2
        padTokenId = vocab["<pad>"] ?: vocab["<unk>"] ?: 0

        // Load merges if available
        merges = try {
            val mergeStr = loadAssetText(assets, "models/nmt/merges.txt")
            mergeStr.lines().filter { it.isNotBlank() && !it.startsWith("#") }.map { line ->
                val parts = line.split(" ")
                if (parts.size >= 2) parts[0] to parts[1] else "" to ""
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun encode(text: String, addSpecialTokens: Boolean = true, maxLength: Int = 128): IntArray {
        val tokens = mutableListOf<String>()
        if (addSpecialTokens) tokens.add("<s>")

        val words = text.trim().split("\\s+".toRegex())
        for (word in words) {
            if (word.isEmpty()) continue
            if (vocab.containsKey(word)) {
                tokens.add(word)
            } else {
                for (ch in word) {
                    tokens.add(ch.toString())
                }
            }
        }

        if (addSpecialTokens) tokens.add("</s>")

        val ids = tokens.map { vocab[it] ?: unkTokenId }.toIntArray()
        return if (ids.size > maxLength) ids.copyOf(maxLength) else ids
    }

    fun decode(tokenIds: IntArray): String {
        val sb = StringBuilder()
        for (id in tokenIds) {
            val token = invVocab[id] ?: continue
            when (token) {
                "<s>", "</s>", "<pad>", "<unk>" -> continue
                else -> {
                    if (token.startsWith("\u2581")) {
                        if (sb.isNotEmpty()) sb.append(" ")
                        sb.append(token.removePrefix("\u2581"))
                    } else {
                        sb.append(token)
                    }
                }
            }
        }
        return sb.toString().trim()
    }

    fun encodeTarget(text: String, maxLength: Int = 128): IntArray {
        return encode(text, addSpecialTokens = true, maxLength = maxLength)
    }

    fun getLanguageTag(language: String): String {
        return when (language) {
            "hi" -> "hin_Deva"
            "sat" -> "sat_Olck"
            "ho" -> "ho_Latn"
            "mnj" -> "mnj_Latn"
            else -> language
        }
    }

    private fun loadAssetJson(assets: android.content.res.AssetManager, path: String): JSONObject {
        val inputStream = assets.open(path)
        val reader = BufferedReader(InputStreamReader(inputStream))
        val text = reader.readText()
        reader.close()
        return JSONObject(text)
    }

    private fun loadAssetText(assets: android.content.res.AssetManager, path: String): String {
        val inputStream = assets.open(path)
        val reader = BufferedReader(InputStreamReader(inputStream))
        val text = reader.readText()
        reader.close()
        return text
    }
}
