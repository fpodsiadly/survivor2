package com.surv20.game.core.save

import com.surv20.game.core.KeyValueStore
import com.surv20.game.core.equipment.Inventory
import com.surv20.game.core.equipment.Item
import com.surv20.game.core.equipment.ItemCatalog
import com.surv20.game.core.equipment.Slot
import com.surv20.game.core.equipment.Tier
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// Persistent meta progress (SPEC W42). The in-run state is never saved: killing the app mid-run loses the run.
@Serializable
data class SaveData(
    var version: Int = CURRENT_VERSION,
    var gold: Long = 0,
    // Plans per slot name (Slot.name).
    var plans: MutableMap<String, Long> = mutableMapOf(),
    var items: MutableList<SavedItem> = mutableListOf(),
    // Equipped item uid per slot name.
    var equipped: MutableMap<String, Int> = mutableMapOf(),
    var accountLevel: Int = 1,
    var accountXp: Long = 0,
    var talents: MutableMap<String, Int> = mutableMapOf(),
    // Highest chapter that can be played (1-based).
    var unlockedChapter: Int = 1,
    var chapters: MutableMap<Int, ChapterRecord> = mutableMapOf(),
    var tutorialDone: Boolean = false,
    var settings: SettingsData = SettingsData(),
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}

@Serializable
data class SavedItem(val uid: Int, val typeId: String, val tier: String, val level: Int)

@Serializable
data class ChapterRecord(
    var won: Boolean = false,
    // Shortest winning time, null before the first win.
    var bestWinSeconds: Double? = null,
    // Longest survival (shown before the first win, SPEC W26).
    var bestSurvivalSeconds: Double = 0.0,
)

@Serializable
data class SettingsData(
    var musicVolume: Float = 0.8f,
    var effectsVolume: Float = 1f,
    var vibration: Boolean = true,
    var damageNumbers: Boolean = true,
    // "pl", "en" or null to follow the system language (SPEC W44).
    var language: String? = null,
)

enum class SaveSource { MAIN, BACKUP, NEW }

data class LoadResult(val data: SaveData, val source: SaveSource)

// Save with a backup copy (SPEC W42, PLAN K6): the previous save is copied to the backup key before the main key is
// overwritten, so an interrupted save leaves either the old main or the backup readable. Each key write is atomic
// (SharedPreferences commit / NSUserDefaults). Older formats are upgraded by migrations keyed by their version.
class SaveStore(
    private val store: KeyValueStore,
    private val migrations: Map<Int, (JsonObject) -> JsonObject> = emptyMap(),
) {
    fun save(data: SaveData) {
        data.version = SaveData.CURRENT_VERSION
        val json = JSON.encodeToString(SaveData.serializer(), data)
        store.getString(MAIN_KEY)?.let { store.putString(BACKUP_KEY, it) }
        store.putString(MAIN_KEY, json)
    }

    fun load(): LoadResult {
        tryRead(MAIN_KEY)?.let { return LoadResult(it, SaveSource.MAIN) }
        tryRead(BACKUP_KEY)?.let { return LoadResult(it, SaveSource.BACKUP) }
        return LoadResult(SaveData(), SaveSource.NEW)
    }

    private fun tryRead(key: String): SaveData? {
        val text = store.getString(key) ?: return null
        return try {
            val migrated = migrate(JSON.parseToJsonElement(text).jsonObject) ?: return null
            JSON.decodeFromJsonElement(SaveData.serializer(), migrated)
        } catch (e: SerializationException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private fun migrate(source: JsonObject): JsonObject? {
        var json = source
        var version = json["version"]?.jsonPrimitive?.int ?: 0
        if (version > SaveData.CURRENT_VERSION) return null
        while (version < SaveData.CURRENT_VERSION) {
            val step = migrations[version] ?: return null
            version++
            json = JsonObject(step(json) + ("version" to JsonPrimitive(version)))
        }
        return json
    }

    companion object {
        const val MAIN_KEY = "save.main"
        const val BACKUP_KEY = "save.backup"
        private val JSON = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}

// Copies equipment state between Inventory and SaveData.
object InventoryMapper {
    fun write(inventory: Inventory, save: SaveData) {
        save.gold = inventory.gold
        save.items = inventory.items.sortedBy { it.uid }.map { SavedItem(it.uid, it.typeId, it.tier.name, it.level) }.toMutableList()
        save.equipped = inventory.equipped.entries.associate { it.key.name to it.value }.toMutableMap()
        save.plans = Slot.entries.filter { inventory.plansFor(it) > 0 }.associate { it.name to inventory.plansFor(it) }.toMutableMap()
    }

    // Items whose type no longer exists in the catalog are dropped.
    fun read(save: SaveData, catalog: ItemCatalog): Inventory {
        val inventory = Inventory(catalog)
        inventory.addGold(save.gold)
        save.plans.forEach { (slot, amount) -> Slot.entries.firstOrNull { it.name == slot }?.let { inventory.addPlans(it, amount) } }
        save.items.forEach { saved ->
            val tier = Tier.entries.firstOrNull { it.name == saved.tier } ?: return@forEach
            if (catalog.contains(saved.typeId)) inventory.restore(Item(saved.uid, saved.typeId, tier, saved.level))
        }
        save.equipped.values.forEach { uid -> if (inventory.find(uid) != null) inventory.equip(uid) }
        return inventory
    }
}
