package me.hinoto04.asherangersfocus

import com.github.noonmaru.psychics.Ability
import com.github.noonmaru.psychics.AbilityConcept
import com.github.noonmaru.psychics.TestResult
import com.github.noonmaru.psychics.attribute.EsperAttribute
import com.github.noonmaru.psychics.attribute.EsperStatistic
import com.github.noonmaru.psychics.damage.Damage
import com.github.noonmaru.psychics.damage.DamageType
import com.github.noonmaru.psychics.item.isPsychicbound
import com.github.noonmaru.psychics.tooltip.TooltipBuilder
import com.github.noonmaru.tap.config.Config
import com.github.noonmaru.tap.event.EntityProvider
import com.github.noonmaru.tap.event.TargetEntity
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Arrow
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector

class AsheRangersFocusConcept : AbilityConcept() {

    private val wandItem : ItemStack = ItemStack(Material.PRISMARINE_CRYSTALS).apply {
        val meta = itemMeta
        meta.setDisplayName("${ChatColor.AQUA}${ChatColor.BOLD}궁사의 집중")
        meta.isPsychicbound = true
        itemMeta = meta
    }

    @Config
    var skillDurationTicks: Int = 4 * 20

    @Config
    var onesDamage: Double = 0.3

    @Config
    var arrowCount: Int = 3

    @Config
    var arrowDelay: Int = 1

    init {
        displayName = "궁사의 집중"
        cooldownTicks = 6 * 20
        cost = 50.0
        wand = wandItem
        supplyItems = listOf(wandItem)
        description = listOf(
            "${ChatColor.WHITE}<durationSeconds>초${ChatColor.GRAY} 동안 쏘는 화살이 다발 화살로 변경됩니다.",
            "다발 화살은 ${ChatColor.WHITE}<arrowCounts>연발${ChatColor.GRAY}로 발사되며,",
            "각각 공격력의 ${ChatColor.RED}<addDMGPercent>%${ChatColor.GRAY}의 추가 원거리 피해를 입힙니다."
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

class AsheRangersFocus : Ability<AsheRangersFocusConcept>() {

    override fun onEnable() {
        psychic.registerEvents(MyListener())
    }

    var isOn: Boolean = false
    var velocity: Vector? = null

    inner class MyListener : Listener {

        @EventHandler
        fun onPlayerInteract(event: PlayerInteractEvent) {
            val player: Player = event.player
            if(player.inventory.itemInMainHand == concept.wand) {
                val result = test()
                if(result == TestResult.SUCCESS) {
                    isOn = true

                    psychic.runTask(DurationEnd(), concept.skillDurationTicks.toLong())

                    psychic.consumeMana(concept.cost)
                    cooldownTicks = concept.cooldownTicks
                } else {
                    player.sendActionBar(result.getMessage(this@AsheRangersFocus))
                }
            }
        }

        @EventHandler
        fun onShootArrow(event: EntityShootBowEvent) {
            if(isOn) {
                val projectile = event.projectile
                projectile.customName = "AsheRangersFocus"
                velocity = projectile.velocity
                for(i in 1 until concept.arrowCount) {
                    psychic.runTask(ShootArrow(), (i * concept.arrowDelay).toLong())
                }
            }
        }

        @EventHandler
        @TargetEntity(TargetProvider::class)
        fun onHitArrow(event: EntityDamageByEntityEvent) {
            val arrow: Entity = event.damager
            val hitE: Entity = event.entity
            if(hitE is LivingEntity){
                if(arrow.customName.toString() == "AsheRangersFocus") {
                    val damage = Damage(DamageType.RANGED, EsperStatistic.of(EsperAttribute.ATTACK_DAMAGE to concept.onesDamage))
                    hitE.psychicDamage(damage, esper.player.eyeLocation, 1.0)
                    Bukkit.broadcastMessage(hitE.health.toString())
                    event.isCancelled = true
                    arrow.remove()
                }
            }
        }
    }

    inner class DurationEnd : Runnable {
        override fun run() {
            isOn = false
            esper.player.sendActionBar("${ChatColor.AQUA}${ChatColor.BOLD}궁사의 집중 ${ChatColor.WHITE}지속시간 종료")
        }
    }

    inner class ShootArrow : Runnable {
        override fun run() {
            val projectile = esper.player.launchProjectile(Arrow::class.java, velocity)
            esper.player.world.playSound(esper.player.eyeLocation, Sound.ENTITY_ARROW_SHOOT, 1.0F, 1.0F)
            projectile.customName = "AsheRangersFocus"
        }
    }

    class TargetProvider : EntityProvider<EntityDamageByEntityEvent> {
        override fun getFrom(event: EntityDamageByEntityEvent): Entity? {
            if(event.damager is Arrow){
                val arrow: Arrow = event.damager as Arrow
                if(arrow.shooter is Entity) {
                    return arrow.shooter as Entity
                }
            }
            return event.damager
        }
    }
}