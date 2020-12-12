package me.hinoto04.asherangersfocus

import com.github.noonmaru.psychics.ActiveAbility
import com.github.noonmaru.psychics.AbilityConcept
import com.github.noonmaru.psychics.attribute.EsperStatistic
import com.github.noonmaru.psychics.item.isPsychicbound
import com.github.noonmaru.psychics.tooltip.TooltipBuilder
import com.github.noonmaru.tap.config.Config
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Arrow
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.event.player.PlayerEvent
import org.bukkit.inventory.ItemStack

class AsheRangersFocusConcept : AbilityConcept() {

    private val wandItem : ItemStack = ItemStack(Material.PRISMARINE_CRYSTALS).apply {
        val meta = itemMeta
        meta.setDisplayName("${ChatColor.AQUA}${ChatColor.BOLD}궁사의 집중")
        meta.isPsychicbound = true
        itemMeta = meta
    }

    @Config
    var skillDurationTicks: Long = 4 * 20

    @Config
    var onesDamage: Double = 0.3

    @Config
    var arrowCount: Int = 5

    @Config
    var arrowDelay: Int = 2

    init {
        displayName = "궁사의 집중"
        cooldownTicks = 6 * 20
        cost = 50.0
        wand = wandItem
        supplyItems = listOf(wandItem)
        description = listOf(
            "${ChatColor.WHITE}<durationSeconds>초${ChatColor.GRAY} 동안 쏘는 화살이 다발 화살로 변경됩니다.",
            "다발 화살은 ${ChatColor.WHITE}<arrowCounts>연발${ChatColor.GRAY}로 발사되며,",
            "각각 공격력의 ${ChatColor.RED}<addDMGPercent>%${ChatColor.WHITE}의 원거리 피해를 입힙니다."
        )
    }

    override fun onRenderTooltip(tooltip: TooltipBuilder, stats: (EsperStatistic) -> Double) {
        tooltip.addTemplates(
            "durationSeconds" to skillDurationTicks / 20,
            "arrowCounts" to arrowCount,
            "addDMGPercent" to onesDamage
        )
    }
}

class AsheRangersFocus : ActiveAbility<AsheRangersFocusConcept>() {

    private var isOn = false
    private val player = esper.player
    private var velocity = esper.player.velocity

    override fun onCast(event: PlayerEvent, action: WandAction, target: Any?) {

        isOn = true

        psychic.runTask(DurationEnd(), concept.skillDurationTicks)

        psychic.consumeMana(concept.cost)
        cooldownTicks = concept.cooldownTicks

    }

    override fun onEnable() {
        psychic.registerEvents(MyListener())
    }

    inner class MyListener : Listener {
        @EventHandler
        fun onShootArrow(event: EntityShootBowEvent) {
            if(isOn) {
                velocity = event.projectile.velocity
                event.projectile.customName = "AsheRangersFocus"
                for(i in 1..concept.arrowCount) {
                    psychic.runTask(ShootArrow(), (i * concept.arrowDelay).toLong())
                }
            }
        }
    }

    inner class DurationEnd : Runnable {
        override fun run() {
            player.sendActionBar("${ChatColor.AQUA}${ChatColor.BOLD}궁사의 집중 ${ChatColor.WHITE}지속시간 종료")
        }
    }

    inner class ShootArrow : Runnable {
        override fun run() {
            val projectile = player.launchProjectile(Arrow::class.java, velocity)
            player.world.playSound(player.eyeLocation, Sound.ENTITY_ARROW_SHOOT, 1.0F, 1.0F)
            projectile.customName = "AsheRangersFocus"
        }
    }
}