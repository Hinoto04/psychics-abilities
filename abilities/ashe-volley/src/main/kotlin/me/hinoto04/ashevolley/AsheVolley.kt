package me.hinoto04.ashevolley

import com.github.noonmaru.psychics.ActiveAbility
import com.github.noonmaru.psychics.AbilityConcept
import com.github.noonmaru.psychics.TestResult
import com.github.noonmaru.psychics.attribute.EsperAttribute
import com.github.noonmaru.psychics.attribute.EsperStatistic
import com.github.noonmaru.psychics.damage.Damage
import com.github.noonmaru.psychics.damage.DamageType
import com.github.noonmaru.psychics.item.isPsychicbound
import com.github.noonmaru.tap.event.EntityProvider
import com.github.noonmaru.tap.event.TargetEntity
import com.github.noonmaru.tap.math.toRadians
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerEvent
import org.bukkit.inventory.ItemStack
import java.lang.IllegalArgumentException
import kotlin.math.cos
import kotlin.math.sin

class AsheVolleyConcept : AbilityConcept() {

    private val wandItem: ItemStack = ItemStack(Material.ARROW).apply {
        val meta = itemMeta
        meta.setDisplayName("${ChatColor.AQUA}${ChatColor.BOLD}일제 사격")
        meta.isPsychicbound = true
        itemMeta = meta
    }

    init {
        displayName = "일제 사격"
        cooldownTicks = 9 * 20
        wand = wandItem
        cost = 70.0
        damage = Damage(DamageType.RANGED, EsperStatistic.Companion.of(EsperAttribute.ATTACK_DAMAGE to 1.5))
        supplyItems = listOf(
            wandItem
        )
        castingTicks = 5
        description = listOf(
            "원뿔 모양으로 화살을 7개 발사해",
            "맞은 적에게 원거리 피해를 줍니다."
        )
    }
}

class AsheVolley : ActiveAbility<AsheVolleyConcept>() {
    override fun onCast(event: PlayerEvent, action: WandAction, target: Any?) {
        val testResult = test()
        if(testResult == TestResult.SUCCESS) {

            cooldownTicks = concept.cooldownTicks
            psychic.consumeMana(concept.cost)

            val player: Player = event.player
            val eyeLocation: Location = player.eyeLocation

            var angle: Double = eyeLocation.yaw - 40.0
            for(i in 1..7) {
                angle += 10.0
                val arrow: Arrow = player.launchProjectile(Arrow::class.java)
                arrow.velocity = player.eyeLocation.direction.apply {
                    x = -sin(angle.toRadians())
                    z = cos(angle.toRadians())
                }.multiply(2)
                arrow.customName = "AsheVolley"
                arrow.pickupStatus = AbstractArrow.PickupStatus.CREATIVE_ONLY
            }
            player.world.playSound(eyeLocation, Sound.ENTITY_ARROW_SHOOT, 1.5F, 0.5F)
        }
    }

    override fun onEnable() {
        psychic.registerEvents(EventListener())
    }

    inner class EventListener : Listener {
        @EventHandler
        @TargetEntity(TargetProvider::class)
        fun onHitArrow(event: EntityDamageByEntityEvent) {
            val arrow: Entity = event.damager
            val hitE: Entity = event.entity
            if(arrow.customName.toString() == "AsheVolley") {
                try {
                    val damage = requireNotNull(concept.damage)
                    if (hitE is LivingEntity) {
                        hitE.psychicDamage(damage, esper.player.eyeLocation, 1.0)
                        event.isCancelled = true
                        arrow.remove()
                    }
                } catch (e: IllegalArgumentException) {
                    arrow.remove()
                }
            }
        }
    }

    class TargetProvider : EntityProvider<EntityDamageByEntityEvent> {
        override fun getFrom(event: EntityDamageByEntityEvent): Entity? {
            if(event.damager is Arrow){
                val arrow: Arrow = event.damager as Arrow
                if(arrow.shooter is Entity) {
                    if(event.entity == arrow.shooter as Entity){
                        return event.entity
                    }
                    return arrow.shooter as Entity
                }
            }
            return event.damager
        }
    }
}