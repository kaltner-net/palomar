package net.kaltner.palomar

import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

const val PROVIDER_CODEX = "codex"
const val PROVIDER_CLAUDE_CODE = "claude-code"

internal fun supportedProvider(provider: String): Boolean =
    provider == PROVIDER_CODEX || provider == PROVIDER_CLAUDE_CODE

@Serializable
data class ProviderInfo(
    val id: String,
    val displayName: String,
    val installed: Boolean? = null,
    val enabled: Boolean = true,
    val available: Boolean,
    val version: String? = null,
    val cliVersion: String? = null,
    val sdkVersion: String? = null,
    val nodeVersion: String? = null,
    val capabilities: List<String> = emptyList(),
    val limitations: List<String> = emptyList(),
    val unavailableReason: String? = null,
)

internal fun providerEnabled(provider: ProviderInfo): Boolean = provider.enabled

/** A provider can back task operations only when configuration and host runtime agree. */
internal fun providerUsableForTasks(provider: ProviderInfo): Boolean =
    providerEnabled(provider) && provider.available

internal fun usableTaskProviders(providers: List<ProviderInfo>): List<ProviderInfo> =
    providers.filter(::providerUsableForTasks)

internal fun providerUsableForTasks(
    providers: List<ProviderInfo>,
    provider: String,
): Boolean = providers.any { it.id == provider && providerUsableForTasks(it) }

internal fun providerMustRemainEnabled(
    provider: ProviderInfo,
    providers: List<ProviderInfo>,
): Boolean {
    if (!provider.enabled) return false
    val enabled = providers.filter(::providerEnabled)
    val usableEnabled = enabled.count { it.available }
    return enabled.size == 1 || (provider.available && usableEnabled == 1)
}

internal fun providerConfigurationStatus(provider: ProviderInfo): String {
    val configured = if (provider.enabled) "Enabled" else "Disabled"
    val installed = when (provider.installed) {
        true -> "Installed"
        false -> "Not installed"
        null -> "Installation unknown"
    }
    val availability = if (provider.available) "Available" else "Unavailable"
    return "$configured · $installed · $availability"
}

internal fun soleUsableTaskProvider(
    providers: List<ProviderInfo>,
    catalogLoaded: Boolean,
): ProviderInfo? {
    if (!catalogLoaded) return null
    return usableTaskProviders(providers).singleOrNull()
}

internal fun shouldShowProviderIdentity(
    providers: List<ProviderInfo>,
    catalogLoaded: Boolean,
): Boolean = soleUsableTaskProvider(providers, catalogLoaded) == null

internal fun newSessionProviderSelection(
    providers: List<ProviderInfo>,
    catalogLoaded: Boolean,
    preferredProvider: String,
): String? {
    if (!catalogLoaded) return null
    val usable = usableTaskProviders(providers)
    return usable.singleOrNull()?.id
        ?: usable.firstOrNull { it.id == preferredProvider }?.id
        ?: usable.firstOrNull()?.id
}

internal fun providerCatalogResponseIsCurrent(
    requestHostId: String?,
    activeHostId: String?,
    requestRevision: Long,
    currentRevision: Long,
): Boolean = requestHostId == activeHostId && requestRevision == currentRevision

@Serializable
data class PermissionModeInfo(
    val id: String,
    val displayName: String,
    val description: String = "",
    val highRisk: Boolean = false,
)

@Serializable
data class RateLimitWindow(
    val id: String? = null,
    val label: String? = null,
    val usedPercent: Double,
    val windowDurationMins: Long? = null,
    val resetsAt: Long? = null,
)

@Serializable
data class RateLimitSnapshot(
    val limitId: String? = null,
    val limitName: String? = null,
    val primary: RateLimitWindow? = null,
    val secondary: RateLimitWindow? = null,
    val windows: List<RateLimitWindow> = emptyList(),
    val planType: String? = null,
    val rateLimitReachedType: String? = null,
)

@Serializable
data class ProviderAccountUsage(
    val available: Boolean,
    val rateLimits: RateLimitSnapshot? = null,
    val experimental: Boolean = false,
    val observedAt: Long? = null,
    val availabilityReason: String? = null,
    val stale: Boolean = false,
)

@Serializable
data class AccountUsage(
    val providers: Map<String, ProviderAccountUsage> = emptyMap(),
)

internal fun taskProviderAccountUsage(
    providers: List<ProviderInfo>,
    usage: AccountUsage,
): List<Pair<ProviderInfo, ProviderAccountUsage>> =
    usableTaskProviders(providers).mapNotNull { provider ->
        usage.providers[provider.id]?.let { provider to it }
    }

internal data class ContextUsageView(
    val usedTokens: Long,
    val remainingTokens: Long,
    val contextWindow: Long,
    val percentUsed: Int,
    val percentRemaining: Int,
)

internal fun contextUsageView(tokenUsage: ThreadTokenUsage?): ContextUsageView? {
    val used = tokenUsage?.last?.totalTokens ?: return null
    val window = tokenUsage.modelContextWindow ?: return null
    if (used < 0 || window <= 0) return null
    val safeUsed = used.coerceAtLeast(0)
    val percentUsed = ((safeUsed.toDouble() / window) * 100).roundToInt().coerceIn(0, 100)
    return ContextUsageView(
        usedTokens = safeUsed,
        remainingTokens = (window - safeUsed).coerceAtLeast(0),
        contextWindow = window,
        percentUsed = percentUsed,
        percentRemaining = 100 - percentUsed,
    )
}

internal fun formatTokenCount(value: Long): String =
    when {
        value >= 1_000_000 -> {
            val millions = value / 1_000_000.0
            if (millions >= 10) "${millions.toInt()}m"
            else "${String.format(Locale.US, "%.1f", millions).removeSuffix(".0")}m"
        }
        value >= 1_000 -> {
            val thousands = value / 1_000.0
            if (thousands >= 100) "${thousands.toInt()}k"
            else "${String.format(Locale.US, "%.1f", thousands).removeSuffix(".0")}k"
        }
        else -> value.toString()
    }

private const val MAX_USAGE_TIMESTAMP = 1_000_000_000_000L

private fun normalizedRateLimitWindow(
    window: RateLimitWindow?,
    fallbackId: String? = null,
): RateLimitWindow? {
    window ?: return null
    if (!window.usedPercent.isFinite()) return null
    val id = sequenceOf(window.id, fallbackId)
        .filterNotNull()
        .firstOrNull { it.matches(Regex("[A-Za-z0-9._:-]{1,100}")) }
        ?: return null
    val label = window.label?.replace(Regex("[\\p{Cc}]"), " ")?.trim()?.take(100)?.takeIf(String::isNotBlank)
    val duration = window.windowDurationMins?.takeIf { it in 1..525_600 }
    val resetsAt = window.resetsAt?.takeIf { it in 0..MAX_USAGE_TIMESTAMP }
    return RateLimitWindow(
        id = id,
        label = label,
        usedPercent = (window.usedPercent.coerceIn(0.0, 100.0) * 10).roundToInt() / 10.0,
        windowDurationMins = duration,
        resetsAt = resetsAt,
    )
}

internal fun normalizedRateLimitSnapshot(snapshot: RateLimitSnapshot?): RateLimitSnapshot? {
    snapshot ?: return null
    val windows = linkedMapOf<String, RateLimitWindow>()
    (snapshot.windows.take(32).map { it to null } + listOf(
        snapshot.primary to "primary",
        snapshot.secondary to "secondary",
    )).forEach { (raw, fallback) ->
        normalizedRateLimitWindow(raw, fallback)?.let { window ->
            if (windows.size < 32 && window.id !in windows) windows[window.id!!] = window
        }
    }
    if (windows.isEmpty()) return null
    val primary = normalizedRateLimitWindow(snapshot.primary, "primary")
        ?.let { windows[it.id] ?: it }
    val secondary = normalizedRateLimitWindow(snapshot.secondary, "secondary")
        ?.let { windows[it.id] ?: it }
    fun text(value: String?): String? = value?.replace(Regex("[\\p{Cc}]"), " ")
        ?.trim()?.take(100)?.takeIf(String::isNotBlank)
    return RateLimitSnapshot(
        limitId = text(snapshot.limitId),
        limitName = text(snapshot.limitName),
        primary = primary,
        secondary = secondary,
        windows = windows.values.toList(),
        planType = text(snapshot.planType),
        rateLimitReachedType = text(snapshot.rateLimitReachedType),
    )
}

internal fun normalizedAccountUsage(usage: AccountUsage, stale: Boolean = false): AccountUsage =
    AccountUsage(
        providers = usage.providers.mapNotNull { (provider, raw) ->
            if (!supportedProvider(provider)) return@mapNotNull null
            val limits = normalizedRateLimitSnapshot(raw.rateLimits)
            provider to raw.copy(
                available = limits != null || raw.available,
                rateLimits = limits,
                observedAt = raw.observedAt?.takeIf { it in 0..MAX_USAGE_TIMESTAMP },
                availabilityReason = raw.availabilityReason?.replace(Regex("[\\p{Cc}]"), " ")
                    ?.trim()?.take(100)?.takeIf(String::isNotBlank),
                stale = limits != null && (stale || raw.stale),
            )
        }.toMap(),
    )

internal fun mergeAccountUsage(previous: AccountUsage, incoming: AccountUsage): AccountUsage {
    val normalized = normalizedAccountUsage(incoming)
    val providers = previous.providers.toMutableMap()
    normalized.providers.forEach { (provider, next) ->
        val cached = providers[provider]
        providers[provider] = if (
            accountUsageWindows(next).isEmpty() && accountUsageWindows(cached).isNotEmpty()
        ) cached!!.copy(stale = true) else next
    }
    return AccountUsage(providers)
}

internal fun accountUsageWindows(usage: ProviderAccountUsage?): List<RateLimitWindow> =
    normalizedRateLimitSnapshot(usage?.rateLimits)?.windows.orEmpty()

internal fun accountUsageRemaining(usage: ProviderAccountUsage?): String {
    val windows = accountUsageWindows(usage)
    if (windows.isEmpty()) return "unavailable"
    return "${(100 - windows.maxOf { it.usedPercent }).roundToInt().coerceIn(0, 100)}% left"
}

internal fun rateLimitLabel(window: RateLimitWindow): String {
    window.label?.trim()?.takeIf(String::isNotBlank)?.let { return it.take(100) }
    val duration = window.windowDurationMins
    return when {
        duration == 10_080L -> "Weekly limit"
        duration != null && duration > 0 && duration % 1_440 == 0L -> "${duration / 1_440}-day limit"
        duration != null && duration > 0 && duration % 60 == 0L -> "${duration / 60}-hour limit"
        duration != null && duration > 0 -> "$duration-minute limit"
        else -> "Usage limit"
    }
}

internal data class AccountUsageConstraint(
    val provider: ProviderInfo,
    val usage: ProviderAccountUsage,
    val window: RateLimitWindow,
    val additionalWindows: Int,
)

internal fun accountUsageConstraint(
    providers: List<Pair<ProviderInfo, ProviderAccountUsage>>,
): AccountUsageConstraint? {
    val candidates = providers.flatMap { (provider, usage) ->
        accountUsageWindows(usage).map { window -> Triple(provider, usage, window) }
    }
    val constrained = candidates.maxByOrNull { it.third.usedPercent } ?: return null
    return AccountUsageConstraint(
        provider = constrained.first,
        usage = constrained.second,
        window = constrained.third,
        additionalWindows = (candidates.size - 1).coerceAtLeast(0),
    )
}

internal fun accountUsageConstraintSummary(
    constraint: AccountUsageConstraint,
    showProviderIdentity: Boolean,
): String = buildString {
    if (showProviderIdentity) append(constraint.provider.displayName.removeSuffix(" Code")).append(' ')
    append(rateLimitLabel(constraint.window))
    append(" · ").append(accountUsageRemaining(constraint.usage))
}

data class SessionIdentity(
    val hostId: String,
    val provider: String,
    val sessionId: String,
)

internal fun providerSessionKey(provider: String, sessionId: String): String =
    "${provider.length}:$provider$sessionId"

internal fun parseProviderSessionKey(key: String): Pair<String, String>? {
    val separator = key.indexOf(':')
    val providerLength = key.substring(0, separator.coerceAtLeast(0)).toIntOrNull() ?: return null
    val providerStart = separator + 1
    val providerEnd = providerStart + providerLength
    if (separator <= 0 || providerEnd > key.length) return null
    val provider = key.substring(providerStart, providerEnd)
    if (provider !in setOf(PROVIDER_CODEX, PROVIDER_CLAUDE_CODE)) return null
    val sessionId = key.substring(providerEnd)
    return if (sessionId.isBlank()) null else provider to sessionId
}

internal fun sessionIdentityKey(identity: SessionIdentity): String =
    "${identity.hostId.length}:${identity.hostId}" +
        providerSessionKey(identity.provider, identity.sessionId)

internal fun providerNotificationId(
    hostId: String,
    provider: String,
    sessionId: String,
    base: Int = 2_000,
): Int = base + (sessionIdentityKey(SessionIdentity(hostId, provider, sessionId)).hashCode() and 0x00ffffff)

internal fun legacySessionKey(value: String): String =
    if (value.startsWith("${PROVIDER_CODEX.length}:$PROVIDER_CODEX") ||
        value.startsWith("${PROVIDER_CLAUDE_CODE.length}:$PROVIDER_CLAUDE_CODE")
    ) value else providerSessionKey(PROVIDER_CODEX, value)

internal fun sessionProvider(session: SessionSummary): String =
    session.provider?.takeIf { it.isNotBlank() } ?: PROVIDER_CODEX

internal fun SessionSummary.providerKey(): String = providerSessionKey(sessionProvider(this), id)

internal fun SessionSummary.matches(provider: String, sessionId: String): Boolean =
    id == sessionId && sessionProvider(this) == provider

internal fun providerDisplayName(provider: String): String =
    when (provider) {
        PROVIDER_CODEX -> "Codex"
        PROVIDER_CLAUDE_CODE -> "Claude Code"
        else -> "Provider"
    }

internal fun sessionDisplayStatus(session: SessionSummary): String =
    when {
        sessionProvider(session) == PROVIDER_CLAUDE_CODE &&
            session.source == "external" && session.status == "working" -> "external active"
        else -> session.status
    }

internal fun providerUnavailableDescription(reason: String?): String =
    when (reason) {
        "cli-missing" -> "The Claude Code CLI is not installed."
        "node-missing" -> "Node.js 20 or newer is not installed."
        "sdk-missing" -> "The pinned Claude Agent SDK is not installed."
        "authentication-unavailable" -> "Claude authentication is unavailable on this host."
        else -> "The Claude Code adapter is unavailable."
    }

internal fun claudeInterruptEligible(session: SessionSummary): Boolean =
    sessionProvider(session) == PROVIDER_CLAUDE_CODE &&
        session.source == "managed" &&
        session.status in setOf("working", "waiting") &&
        !session.activeTurnId.isNullOrBlank()

internal fun providerInterruptEligible(session: SessionSummary): Boolean =
    if (sessionProvider(session) == PROVIDER_CLAUDE_CODE) {
        claudeInterruptEligible(session)
    } else {
        session.status in setOf("working", "waiting") && !session.activeTurnId.isNullOrBlank()
    }

internal fun providerPromptOperation(session: SessionSummary): String =
    if (sessionProvider(session) == PROVIDER_CLAUDE_CODE && session.source == "external") {
        "provider.session.resume"
    } else if (sessionProvider(session) == PROVIDER_CLAUDE_CODE) {
        "provider.turn.prompt"
    } else {
        "turn.prompt"
    }

internal fun claudePromptPayload(
    session: SessionSummary,
    text: String,
    model: String,
    permissionMode: String,
) = buildJsonObject {
    require(sessionProvider(session) == PROVIDER_CLAUDE_CODE)
    put("provider", PROVIDER_CLAUDE_CODE)
    put("sessionId", session.id)
    put("repositoryId", session.repositoryId ?: ".")
    put("text", text.trim())
    put("model", model)
    put("permissionMode", permissionMode)
}

internal fun defaultProviders(): List<ProviderInfo> =
    listOf(
        ProviderInfo(
            id = PROVIDER_CODEX,
            displayName = "Codex",
            available = true,
            capabilities = emptyList(),
            limitations = emptyList(),
        ),
    )
