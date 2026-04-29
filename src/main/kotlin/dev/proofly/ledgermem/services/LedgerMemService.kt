package dev.proofly.getmnemo.services

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import dev.proofly.getmnemo.MnemoPlugin
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

data class Memory(
    val id: String,
    val content: String,
    val createdAt: String,
    val score: Double? = null,
)

data class MnemoConfig(
    val apiKey: String,
    val workspaceId: String,
    val endpoint: String,
    val defaultLimit: Int,
)

@Service(Service.Level.APP)
class MnemoService {
    private val log: Logger = Logger.getInstance(MnemoService::class.java)
    private val props get() = PropertiesComponent.getInstance()

    private fun apiKeyAttributes(): CredentialAttributes =
        CredentialAttributes(generateServiceName("Mnemo", KEY_API_KEY))

    private fun loadApiKey(): String {
        val safe = PasswordSafe.instance.get(apiKeyAttributes())?.getPasswordAsString().orEmpty()
        if (safe.isNotEmpty()) return safe
        // One-time migration: previous versions stored the API key in PropertiesComponent
        // (plaintext on disk). Move it into PasswordSafe and clear the legacy entry.
        val legacy = props.getValue(KEY_API_KEY, "")
        if (legacy.isNotEmpty()) {
            PasswordSafe.instance.set(apiKeyAttributes(), Credentials("getmnemo", legacy))
            props.unsetValue(KEY_API_KEY)
            return legacy
        }
        return ""
    }

    fun config(): MnemoConfig = MnemoConfig(
        apiKey = loadApiKey(),
        workspaceId = props.getValue(KEY_WORKSPACE, ""),
        endpoint = props.getValue(KEY_ENDPOINT, "https://api.getmnemo.dev"),
        defaultLimit = props.getInt(KEY_LIMIT, 10),
    )

    fun setApiKey(value: String) {
        if (value.isBlank()) {
            PasswordSafe.instance.set(apiKeyAttributes(), null)
        } else {
            PasswordSafe.instance.set(apiKeyAttributes(), Credentials("getmnemo", value))
        }
        // Make sure no plaintext copy lingers in PropertiesComponent.
        props.unsetValue(KEY_API_KEY)
    }
    fun setWorkspace(value: String) = props.setValue(KEY_WORKSPACE, value)
    fun setEndpoint(value: String) = props.setValue(KEY_ENDPOINT, value)
    fun setDefaultLimit(value: Int) = props.setInt(KEY_LIMIT, value, 10)

    @Throws(IllegalStateException::class)
    fun search(query: String, limit: Int? = null): List<Memory> {
        val cfg = ensureConfigured()
        val body = """{"query":${esc(query)},"workspaceId":${esc(cfg.workspaceId)},"limit":${limit ?: cfg.defaultLimit}}"""
        val raw = post(cfg, "/v1/search", body)
        return parseList(raw)
    }

    @Throws(IllegalStateException::class)
    fun add(content: String, metadata: Map<String, String> = emptyMap()): Memory {
        val cfg = ensureConfigured()
        val meta = metadata.entries.joinToString(",", "{", "}") { "${esc(it.key)}:${esc(it.value)}" }
        val body = """{"content":${esc(content)},"workspaceId":${esc(cfg.workspaceId)},"metadata":$meta}"""
        val raw = post(cfg, "/v1/memories", body)
        return parseSingle(raw)
    }

    @Throws(IllegalStateException::class)
    fun recent(limit: Int? = null): List<Memory> {
        val cfg = ensureConfigured()
        val n = limit ?: cfg.defaultLimit
        val raw = get(cfg, "/v1/memories?workspaceId=${enc(cfg.workspaceId)}&limit=$n&order=desc")
        return parseList(raw)
    }

    @Throws(IllegalStateException::class)
    fun delete(id: String) {
        val cfg = ensureConfigured()
        delete(cfg, "/v1/memories/${enc(id)}?workspaceId=${enc(cfg.workspaceId)}")
    }

    private fun ensureConfigured(): MnemoConfig {
        val cfg = config()
        check(cfg.apiKey.isNotBlank()) { "${MnemoPlugin.DISPLAY_NAME}: API key is not set." }
        check(cfg.workspaceId.isNotBlank()) { "${MnemoPlugin.DISPLAY_NAME}: workspace ID is not set." }
        return cfg
    }

    private fun post(cfg: MnemoConfig, path: String, body: String): String =
        request(cfg, path, "POST", body)

    private fun get(cfg: MnemoConfig, path: String): String =
        request(cfg, path, "GET", null)

    private fun delete(cfg: MnemoConfig, path: String) {
        request(cfg, path, "DELETE", null)
    }

    private fun request(cfg: MnemoConfig, path: String, method: String, body: String?): String {
        val url = URL(cfg.endpoint.trimEnd('/') + path)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10_000
            readTimeout = 30_000
            setRequestProperty("Authorization", "Bearer ${cfg.apiKey}")
            setRequestProperty("Accept", "application/json")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
        }
        try {
            if (body != null) {
                conn.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.let { BufferedReader(InputStreamReader(it, StandardCharsets.UTF_8)).readText() } ?: ""
            if (code !in 200..299) {
                log.warn("Mnemo $method $path -> $code: $text")
                throw IllegalStateException("Mnemo $method $path failed: HTTP $code")
            }
            return text
        } finally {
            conn.disconnect()
        }
    }

    private fun parseList(json: String): List<Memory> {
        // Lightweight parse — extract objects between top-level brackets while
        // ignoring braces and brackets that appear inside JSON string literals.
        val items = mutableListOf<Memory>()
        var depth = 0
        var inString = false
        var escaped = false
        val buf = StringBuilder()
        for (ch in json) {
            if (depth > 0) buf.append(ch)
            if (inString) {
                if (escaped) {
                    escaped = false
                } else if (ch == '\\') {
                    escaped = true
                } else if (ch == '"') {
                    inString = false
                }
                continue
            }
            when (ch) {
                '"' -> inString = true
                '{' -> {
                    if (depth == 0) {
                        buf.clear()
                        buf.append(ch)
                    }
                    depth++
                }
                '}' -> {
                    depth--
                    if (depth == 0) items.add(parseSingle(buf.toString()))
                }
                else -> { /* no-op */ }
            }
        }
        return items
    }

    private fun parseSingle(json: String): Memory = Memory(
        id = field(json, "id") ?: "",
        content = field(json, "content") ?: "",
        createdAt = field(json, "createdAt") ?: "",
        score = field(json, "score")?.toDoubleOrNull(),
    )

    private fun field(json: String, key: String): String? {
        val regexQuoted = Regex("\"$key\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"")
        regexQuoted.find(json)?.let { return it.groupValues[1].replace("\\\"", "\"") }
        val regexBare = Regex("\"$key\"\\s*:\\s*([0-9.eE+-]+)")
        return regexBare.find(json)?.groupValues?.get(1)
    }

    private fun esc(s: String): String =
        "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""

    private fun enc(s: String): String = java.net.URLEncoder.encode(s, "UTF-8")

    companion object {
        const val KEY_API_KEY = "getmnemo.apiKey"
        const val KEY_WORKSPACE = "getmnemo.workspaceId"
        const val KEY_ENDPOINT = "getmnemo.endpoint"
        const val KEY_LIMIT = "getmnemo.defaultLimit"
    }
}
