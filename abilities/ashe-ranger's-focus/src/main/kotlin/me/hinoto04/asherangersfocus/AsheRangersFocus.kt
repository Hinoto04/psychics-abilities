package me.hinoto04.asherangersfocus

import com.github.noonmaru.psychics.Ability
import com.github.noonmaru.psychics.AbilityConcept
import com.github.noonmaru.psychics.AbilityType
import com.github.noonmaru.psychics.TestResult
import com.github.noonmaru.psychics.attribute.EsperStatistic
import com.github.noonmaru.psychics.item.isPsychicbound
import com.github.noonmaru.psychics.tooltip.TooltipBuilder
import com.github.noonmaru.tap.config.Config
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack

class AsheRangersFocusConcept : AbilityConcept() {

    val wandItem = ItemStack(Material.BOW).apply {
        val meta = itemMeta
        meta.setDisplayName("${ChatColor.AQUA}${ChatColor.BOLD}애쉬의 활")
        meta.addEnchant(Enchantment.ARROW_INFINITE, 1, false)
        meta.isUnbreakable = true
        meta.isPsychicbound = true
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE)
        itemMeta = meta
    }

    @Config
    var skillDurationTicks: Long = 4 * 20

    @Config
    var addDamage: Double = 0.3

    init {
        displayName = "궁사의 집중"
        type = AbilityType.ACTIVE
        cooldownTicks = 8 * 20
        cost = 50.0
        description = listOf(
            "${ChatColor.WHITE}<durationSeconds>초${ChatColor.GRAY} 동안 쏘는 화살이 다발 화살로 변경됩니다.",
            "다발 화살은 5연사로 발사되며,",
            "각각 공격력의 ${ChatColor.RED}<addDMGPercent>%${ChatColor.WHITE}의 원거리 피해를 입힙니다."
        )
        wand = wandItem
        supplyItems = listOf(
            wandItem
        )
    }

    override fun onRenderTooltip(tooltip: TooltipBuilder, stats: (EsperStatistic) -> Double) {
        tooltip.addTemplates(
            "durationSeconds" to skillDurationTicks / 20,
            "addDMGPercent" to addDamage * 100
        )
    }
}

class AsheRangersFocus : Ability<AsheRangersFocusConcept>() {

    var isOn: Boolean = false

    override fun onEnable() {
        psychic.registerEvents(EventListener())
    }

    inner class EventListener : Listener {
        @EventHandler
        fun onPlayerInteract(event: PlayerInteractEvent) {
            if(event.player.inventory.itemInMainHand == concept.wandItem) {
                if(event.action == Action.LEFT_CLICK_AIR || event.action == Action.LEFT_CLICK_BLOCK) {

                    val result = test()

                    if(result == TestResult.SUCCESS) {
                        isOn = true
                        cooldownTicks = concept.cooldownTicks
                        psychic.consumeMana(concept.cost)
                        psychic.runTask(DurationEnd(), concept.skillDurationTicks)
                    } else {
                        event.player.sendActionBar(result.getMessage(this@AsheRangersFocus))
                    }
                }
            }
        }
    }

    inner class DurationEnd : Runnable {
        override fun run() {
            isOn = false
        }
    }
}