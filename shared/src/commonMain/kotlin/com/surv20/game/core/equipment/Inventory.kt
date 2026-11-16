package com.surv20.game.core.equipment

// Owned items, the equipped item per slot, and the gold and plans used for upgrades.
class Inventory(val catalog: ItemCatalog) {
    private val itemsByUid = LinkedHashMap<Int, Item>()
    private val equippedBySlot = LinkedHashMap<Slot, Int>()
    private val plans = LinkedHashMap<Slot, Long>()
    private var nextUid = 1

    var gold: Long = 0
        private set

    val items: Collection<Item> get() = itemsByUid.values
    val equipped: Map<Slot, Int> get() = equippedBySlot

    fun add(typeId: String, tier: Tier, level: Int = 1): Item {
        catalog.get(typeId)
        val item = Item(nextUid++, typeId, tier, level)
        itemsByUid[item.uid] = item
        return item
    }

    // Restores an item with a known uid (used by the save loader).
    fun restore(item: Item) {
        catalog.get(item.typeId)
        require(item.uid !in itemsByUid) { "Duplicate uid ${item.uid}." }
        itemsByUid[item.uid] = item
        nextUid = maxOf(nextUid, item.uid + 1)
    }

    fun find(uid: Int): Item? = itemsByUid[uid]
    fun get(uid: Int): Item = itemsByUid[uid] ?: throw NoSuchElementException("No item with uid $uid.")
    fun typeOf(item: Item): ItemType = catalog.get(item.typeId)
    fun slotOf(item: Item): Slot = typeOf(item).slot

    fun equip(uid: Int) {
        equippedBySlot[slotOf(get(uid))] = uid
    }

    fun unequip(slot: Slot) {
        equippedBySlot.remove(slot)
    }

    fun isEquipped(uid: Int): Boolean = equippedBySlot.containsValue(uid)
    fun equippedIn(slot: Slot): Item? = equippedBySlot[slot]?.let { itemsByUid[it] }

    internal fun replace(item: Item) {
        require(item.uid in itemsByUid) { "No item with uid ${item.uid}." }
        itemsByUid[item.uid] = item
    }

    internal fun remove(uid: Int) {
        check(!isEquipped(uid)) { "Item $uid is equipped and cannot be removed." }
        itemsByUid.remove(uid)
    }

    fun plansFor(slot: Slot): Long = plans[slot] ?: 0L

    fun addGold(amount: Long) {
        require(amount >= 0)
        gold += amount
    }

    fun addPlans(slot: Slot, amount: Long) {
        require(amount >= 0)
        plans[slot] = plansFor(slot) + amount
    }

    fun canAfford(gold: Long, slot: Slot, plans: Long): Boolean = this.gold >= gold && plansFor(slot) >= plans

    // Spends gold if there is enough (talents, SPEC W30).
    fun trySpendGold(amount: Long): Boolean {
        require(amount >= 0)
        if (gold < amount) return false
        gold -= amount
        return true
    }

    internal fun spend(gold: Long, slot: Slot, plans: Long) {
        check(canAfford(gold, slot, plans)) { "Not enough gold or plans." }
        this.gold -= gold
        this.plans[slot] = plansFor(slot) - plans
    }
}
