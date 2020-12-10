package me.hinoto04.status

import com.github.noonmaru.psychics.ActiveAbility
import com.github.noonmaru.psychics.AbilityConcept
import com.github.noonmaru.psychics.AbilityType
import com.github.noonmaru.psychics.attribute.EsperAttribute
import com.github.noonmaru.psychics.item.isPsychicbound
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.event.player.PlayerEvent
import org.bukkit.inventory.ItemStack
import kotlin.math.roundToInt

class Status : ActiveAbility<MyAbilityConcept>() {
    override fun onCast(event: PlayerEvent, action: WandAction, target: Any?) {
        var attackDamage = esper.getAttribute(EsperAttribute.ATTACK_DAMAGE)*100
        attackDamage = attackDamage.roundToInt().toDouble()
        attackDamage /= 100
        esper.player.sendMessage("공격력 : $attackDamage")
        val defense = esper.getAttribute(EsperAttribute.DEFENSE)
        esper.player.sendMessage("방어력 : $defense")

        cooldownTicks = concept.cooldownTicks
    }
}

class MyAbilityConcept : AbilityConcept() {

    private val wandItem: ItemStack = ItemStack(Material.WRITTEN_BOOK).apply {
        val meta = itemMeta
        meta.setDisplayName("${ChatColor.BLUE}${ChatColor.BOLD}스테이터스")
        meta.isPsychicbound = true
        itemMeta = meta
    }

    init {
        type = AbilityType.ACTIVE
        displayName = "스테이터스"
        cooldownTicks = 1 * 20
        wand = wandItem
        supplyItems = listOf(
            wandItem
        )
        description = listOf(
            "자신의 공격력과 방어력을",
            "확인할 수 있습니다."
        )
    }
}