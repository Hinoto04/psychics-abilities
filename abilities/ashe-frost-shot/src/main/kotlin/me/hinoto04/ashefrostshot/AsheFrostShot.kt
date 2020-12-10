package me.hinoto04.ashefrostshot

import com.github.noonmaru.psychics.Ability
import com.github.noonmaru.psychics.AbilityConcept
import com.github.noonmaru.psychics.attribute.EsperAttribute
import com.github.noonmaru.psychics.attribute.EsperStatistic
import com.github.noonmaru.psychics.damage.Damage
import com.github.noonmaru.psychics.damage.DamageType
import com.github.noonmaru.psychics.damage.psychicDamage
import com.github.noonmaru.psychics.item.isPsychicbound
import com.github.noonmaru.psychics.tooltip.TooltipBuilder
import com.github.noonmaru.tap.config.Config
import com.github.noonmaru.tap.event.EntityProvider
import com.github.noonmaru.tap.event.TargetEntity
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

class AsheFrostShotConcept : AbilityConcept() {

    @Config
    var slowValue: Int = 2

    @Config
    var slowTicks: Int = 40

    @Config
    var addDamage: Double = 0.5

    init {
        displayName = "서리 화살"
        supplyItems = listOf(
            ItemStack(Material.BOW).apply {
                val meta = itemMeta
                meta.setDisplayName("${ChatColor.AQUA}${ChatColor.BOLD}애쉬의 활")
                meta.addEnchant(Enchantment.ARROW_INFINITE, 1, false)
                meta.isUnbreakable = true
                meta.isPsychicbound = true
                meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE)
                itemMeta = meta
            }
        )
        description = listOf(
            "스킬 사용 및 공격 시",
            "${ChatColor.BLUE}<SlowSeconds>초${ChatColor.GRAY}간 대상의 이동속도가 " +
                    "${ChatColor.BLUE}<SlowLevels>%${ChatColor.GRAY}느려집니다.",
            " ",
            "또한, 애쉬는 화살이 공격력의 ${ChatColor.RED}<DamagePercent>%${ChatColor.STRIKETHROUGH}${ChatColor.GRAY}의 ",
            "추가 피해를 입힙니다."
        )
    }

    override fun onRenderTooltip(tooltip: TooltipBuilder, stats: (EsperStatistic) -> Double) {
        tooltip.addTemplates(
            "DamagePercent" to addDamage*100,
            "SlowSeconds" to slowTicks/20,
            "SlowLevels" to slowValue*15
        )
    }
}

class AsheFrostShot : Ability<AsheFrostShotConcept>() {
    override fun onEnable() {
        psychic.registerEvents(EventListener())
    }

    inner class EventListener : Listener {
        @EventHandler
        fun onShootArrow(event: EntityShootBowEvent) {
            val projectile = event.projectile
            projectile.customName = "AsheFrostShot"
        }

        @EventHandler
        @TargetEntity(TargetProvider::class)
        fun onHitArrow(event: EntityDamageByEntityEvent) {
            val arrow: Entity = event.damager
            val hitE: Entity = event.entity
            if(hitE is LivingEntity){
                if("Ashe" in arrow.customName.toString()) {
                    hitE.addPotionEffect(PotionEffect(PotionEffectType.SLOW,concept.slowTicks, concept.slowValue))
                }
                if(arrow.customName.toString() == "AsheFrostShot") {
                    val damage = Damage(DamageType.RANGED, EsperStatistic.Companion.of(EsperAttribute.ATTACK_DAMAGE to concept.addDamage))
                    hitE.psychicDamage(damage, esper.player.eyeLocation, 1.0)
                }
            }
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



