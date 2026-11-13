package app.podium.desktop

import app.podium.core.common.Clock
import app.podium.core.common.Outcome
import app.podium.core.common.PodiumError
import app.podium.core.model.SourceId
import app.podium.sources.api.HealthOutcome
import app.podium.sources.api.SourceHealth
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Desktop implementation of SourceHealthMonitor.
 * Tracks health state of each music source for circuit breaker pattern.
 */
class DesktopSourceHealthMonitor(
    private val clock: Clock,
) {
    
    private val healthStates = ConcurrentHashMap<SourceId, app.podium.sources.api.SourceHealth>()
    private val mutex = Mutex()
    
    /**
     * Checks if a source can be attempted for a request.
     * Returns false if the source is rate limited or unreachable.
     */
    fun canAttempt(sourceId: SourceId): Boolean {
        val health = healthStates[sourceId] ?: return true
        return health.isAttemptAllowed
    }
    
    /**
     * Records a health outcome for a source.
     */
    fun record(sourceId: SourceId, outcome: app.podium.sources.api.HealthOutcome, retryAfterMillis: Long? = null) {
        mutex.withLock {
            val current = healthStates[sourceId] ?: app.podium.sources.api.SourceHealth.Healthy
            val updated = when {
                outcome == app.podium.sources.api.HealthOutcome.SUCCESS -> app.podium.sources.api.SourceHealth.Healthy
                outcome == app.podium.sources.api.HealthOutcome.NETWORK_FAILURE -> app.podium.sources.api.SourceHealth.Unreachable(
                    retryAtMillis = clock.nowMillis() + (retryAfterMillis ?: 30_000)
                )
                outcome == app.podium.sources.api.HealthOutcome.RATE_LIMIT -> app.podium.sources.api.SourceHealth.RateLimited(
                    untilMillis = clock.nowMillis() + (retryAfterMillis ?: 60_000)
                )
                outcome == app.podium.sources.api.HealthOutcome.MISS -> current // Miss doesn't change health
                else -> app.podium.sources.api.SourceHealth.Unreachable(
                    retryAtMillis = clock.nowMillis() + 30_000
                )
            }
            healthStates[sourceId] = updated
        }
    }
    
    /**
     * Gets the current health state of a source.
     */
    fun health(sourceId: SourceId): app.podium.sources.api.SourceHealth {
        return healthStates[sourceId] ?: app.podium.sources.api.SourceHealth.Healthy
    }
    
    /**
     * Resets health state for a source (e.g., after manual retry).
     */
    fun reset(sourceId: SourceId) {
        healthStates.remove(sourceId)
    }
}