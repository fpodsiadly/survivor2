package com.surv20.game.core.run

import com.surv20.game.core.Balance
import com.surv20.game.core.Vec2
import kotlin.math.max
import kotlin.math.min

// --- Map pickups (SPEC W18) ---

data class BombTarget(val enemyClass: EnemyClass, val maxHealth: Double, val health: Double, val onScreen: Boolean)

data class BombHit(val damage: Double, val killed: Boolean)

// Bomb pickup (also the revive blast of W33): kills normal and reinforced enemies on screen; elites and bosses on
// screen lose a share of their max HP (a boss below that share dies like from any other damage).
object Bomb {
    fun explode(targets: List<BombTarget>, eliteBossShare: Double = Balance.BOMB_ELITE_BOSS_SHARE): List<BombHit> =
        targets.map { t ->
            if (!t.onScreen || t.health <= 0) return@map BombHit(0.0, false)
            val damage = if (t.enemyClass == EnemyClass.ELITE || t.enemyClass == EnemyClass.BOSS) {
                min(t.health, t.maxHealth * eliteBossShare)
            } else {
                t.health
            }
            BombHit(damage, damage >= t.health)
        }
}

// A pickup disappears after 60 s spent continuously off screen (PLAN P13).
class OffscreenTimer(private val limitSeconds: Double = Balance.PICKUP_OFFSCREEN_SECONDS) {
    private var offscreenFor = 0.0
    var expired = false
        private set

    init {
        require(limitSeconds > 0)
    }

    // Returns true once the pickup should vanish.
    fun tick(deltaSeconds: Double, onScreen: Boolean): Boolean {
        if (expired) return true
        offscreenFor = if (onScreen) 0.0 else offscreenFor + deltaSeconds
        expired = offscreenFor >= limitSeconds
        return expired
    }
}

// --- Enemies and bosses (SPEC W12, W20) ---

enum class ChargerState { CHASE, TELEGRAPH, CHARGE, RECOVER }

// Charging enemy (SPEC W20 type 5; PLAN P17): walks toward the player; within range it stops, shows the direction
// for at least telegraphSeconds, then charges along that locked direction, then recovers.
class ChargerBrain(
    val walkSpeed: Float,
    val chargeSpeed: Float,
    val triggerDistance: Float,
    val telegraphSeconds: Double = Balance.MIN_TELEGRAPH_SECONDS,
    val chargeSeconds: Double = 0.8, // placeholder
    val recoverSeconds: Double = 1.0, // placeholder
) {
    var state = ChargerState.CHASE
        private set

    // Locked charge direction, shown to the player during the telegraph.
    var direction = Vec2.ZERO
        private set

    private var stateTime = 0.0

    init {
        require(telegraphSeconds >= Balance.MIN_TELEGRAPH_SECONDS) { "SPEC W20: at least 0.7 s." }
    }

    // Advances the state and returns the velocity for this frame.
    fun tick(deltaSeconds: Double, self: Vec2, player: Vec2): Vec2 {
        stateTime += deltaSeconds
        return when (state) {
            ChargerState.CHASE -> {
                val toPlayer = player - self
                if (toPlayer.length <= triggerDistance) {
                    enter(ChargerState.TELEGRAPH)
                    direction = toPlayer.normalized
                    Vec2.ZERO
                } else {
                    toPlayer.normalized * walkSpeed
                }
            }
            ChargerState.TELEGRAPH -> {
                if (stateTime >= telegraphSeconds) enter(ChargerState.CHARGE)
                Vec2.ZERO
            }
            ChargerState.CHARGE -> if (stateTime >= chargeSeconds) {
                enter(ChargerState.RECOVER)
                Vec2.ZERO
            } else {
                direction * chargeSpeed
            }
            ChargerState.RECOVER -> {
                if (stateTime >= recoverSeconds) enter(ChargerState.CHASE)
                Vec2.ZERO
            }
        }
    }

    private fun enter(next: ChargerState) {
        state = next
        stateTime = 0.0
    }
}

// One boss attack (SPEC W12): damage as a fraction of the player's base max HP.
data class BossAttack(val id: String, val damageFraction: Double, val telegraphSeconds: Double)

data class BossDefinition(val id: String, val attacks: List<BossAttack>, val contactDamageFraction: Double)

// Data check for bosses (SPEC W12; PLAN P18). Empty list = valid.
object BossValidator {
    fun validate(boss: BossDefinition): List<String> {
        val errors = mutableListOf<String>()
        if (boss.attacks.size < 2) errors += "${boss.id}: at least 2 attacks are required."
        boss.attacks
            .filter { it.damageFraction >= Balance.TELEGRAPH_DAMAGE_THRESHOLD && it.telegraphSeconds < Balance.MIN_TELEGRAPH_SECONDS }
            .forEach { errors += "${boss.id}/${it.id}: heavy attack needs a telegraph of at least ${Balance.MIN_TELEGRAPH_SECONDS} s." }
        if (boss.contactDamageFraction >= Balance.TELEGRAPH_DAMAGE_THRESHOLD) errors += "${boss.id}: contact damage must stay below 15%."
        return errors
    }
}

// --- Stats (SPEC W16, W24, W30) ---

enum class Stat {
    ATTACK, MAX_HEALTH, MOVE_SPEED, COOLDOWN_REDUCTION, AREA, DURATION, PROJECTILE_SPEED, PICKUP_RADIUS,
    XP_GAIN, GOLD_GAIN, DAMAGE_REDUCTION,
    REGENERATION, // % of max HP healed every 5 s
    MEAT_HEALING, // extra meat healing as a fraction of the base 30% (talent, SPEC W30)
}

enum class ModifierSource { EQUIPMENT, TALENT, PASSIVE }

// A percentage bonus, e.g. 0.10 = +10%.
data class StatModifier(val stat: Stat, val value: Double, val source: ModifierSource)

// A passive skill's effect: level1 at level 1 to level5 at level 5, linear in between (PLAN P10).
data class PassiveEffect(val skillId: String, val stat: Stat, val level1: Double, val level5: Double) {
    fun valueAt(level: Int): Double {
        require(level in 1..SkillCatalog.MAX_LEVEL)
        return level1 + (level5 - level1) * (level - 1) / (SkillCatalog.MAX_LEVEL - 1)
    }
}

// Final stats: equipment bonuses add up among themselves; equipment, talent and passive groups multiply
// (SPEC W24, W30). Damage and cooldown reductions combine as 1 − Π(1 − r) so they never reach 100%.
object StatCalculator {
    private val REDUCTIONS = setOf(Stat.DAMAGE_REDUCTION, Stat.COOLDOWN_REDUCTION)

    fun factor(stat: Stat, modifiers: List<StatModifier>): Double {
        val bySource = modifiers.filter { it.stat == stat }.groupBy { it.source }.values.map { group -> group.sumOf { it.value } }
        return if (stat in REDUCTIONS) 1 - bySource.fold(1.0) { keep, r -> keep * (1 - r) }
        else bySource.fold(1.0) { f, bonus -> f * (1 + bonus) }
    }

    fun maxHealth(baseMaxHealth: Double, equipmentHealth: Double, modifiers: List<StatModifier>): Double =
        (baseMaxHealth + equipmentHealth) * factor(Stat.MAX_HEALTH, modifiers)

    fun attack(equipmentAttack: Double, modifiers: List<StatModifier>): Double = equipmentAttack * factor(Stat.ATTACK, modifiers)

    // Passive modifiers of the owned passive skills at their levels.
    fun fromPassives(loadout: Loadout, effects: List<PassiveEffect>): List<StatModifier> {
        val bySkill = effects.associateBy { it.skillId }
        return loadout.passives.mapNotNull { owned ->
            bySkill[owned.skillId]?.let { StatModifier(it.stat, it.valueAt(owned.level), ModifierSource.PASSIVE) }
        }
    }
}

// Player HP with contact damage and invulnerability after a hit (SPEC W13; PLAN P8).
class PlayerHealth(val maxHealth: Double, private val invulnerableSeconds: Double = Balance.INVULNERABLE_SECONDS) {
    var health = maxHealth
        private set
    private var invulnerableUntil = Double.NEGATIVE_INFINITY
    val isDead: Boolean get() = health <= 0

    init {
        require(maxHealth > 0)
    }

    fun isInvulnerable(now: Double): Boolean = now < invulnerableUntil

    // Applies a hit unless invulnerable; returns the damage taken.
    fun takeHit(now: Double, damage: Double, damageReduction: Double): Double {
        if (isDead || isInvulnerable(now) || damage <= 0) return 0.0
        val taken = damage * (1 - damageReduction)
        health = max(0.0, health - taken)
        invulnerableUntil = now + invulnerableSeconds
        return taken
    }

    fun heal(amount: Double) {
        if (isDead || amount <= 0) return
        health = min(maxHealth, health + amount)
    }

    companion object {
        // Damage per contact hit: rate (fraction of base HP per second) × invulnerability window × base HP.
        fun contactHit(ratePerSecond: Double, baseMaxHealth: Double, invulnerableSeconds: Double = Balance.INVULNERABLE_SECONDS) =
            ratePerSecond * invulnerableSeconds * baseMaxHealth

        // Meat heals 30% of max HP, raised by the meat talent (SPEC W18, W30).
        fun meatHeal(maxHealth: Double, meatHealingBonus: Double) = maxHealth * Balance.MEAT_HEAL_SHARE * (1 + meatHealingBonus)
    }
}
