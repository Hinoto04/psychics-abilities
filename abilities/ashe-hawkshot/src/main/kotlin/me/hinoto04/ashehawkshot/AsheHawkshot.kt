package me.hinoto04.ashehawkshot

import com.github.noonmaru.psychics.ActiveAbility
import com.github.noonmaru.psychics.AbilityConcept
import com.github.noonmaru.psychics.PsychicProjectile
import com.github.noonmaru.psychics.attribute.EsperStatistic
import com.github.noonmaru.psychics.item.isPsychicbound
import com.github.noonmaru.psychics.tooltip.TooltipBuilder
import com.github.noonmaru.psychics.util.TargetFilter
import com.github.noonmaru.tap.config.Config
import com.github.noonmaru.tap.effect.playFirework
import com.github.noonmaru.tap.fake.FakeEntity
import com.github.noonmaru.tap.fake.Movement
import com.github.noonmaru.tap.fake.Trail
import com.github.noonmaru.tap.fake.invisible
import com.github.noonmaru.tap.math.normalizeAndLength
import org.bukkit.*
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.LivingEntity
import org.bukkit.event.player.PlayerEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.BoundingBox
import org.bukkit.util.EulerAngle

class AsheHawkShotConcept : AbilityConcept() {

    @Config
    var radius: Double = 10.0

    @Config
    var glowDurationTicks: Int = 1 * 20

    @Config
    var explosionDurationTicks: Int = 5 * 20

    @Config
    var launchSpeed: Double = 2.0

    @Config
    var explosionRaySize: Double = 0.3

    private val wandItem: ItemStack = ItemStack(Material.PRISMARINE_SHARD).apply {
        val meta = itemMeta
        meta.setDisplayName("${ChatColor.AQUA}${ChatColor.BOLD}매 날리기")
        meta.isPsychicbound = true
        itemMeta = meta
    }

    init {
        displayName = "매 날리기"
        cooldownTicks = 50 * 20
        wand = wandItem
        supplyItems = listOf(
            wandItem
        )
        castingTicks = 5
        range = 128.0
        description = listOf(
            "주변 일대의 적에게 ${ChatColor.YELLOW}발광 ${ChatColor.GRAY}효과를 ",
            "${ChatColor.WHITE}<glowSeconds>초${ChatColor.GRAY} 동안 부여하는 매를 전방으로 날립니다.",
            "매는 부딫히면 그 자리에서 폭발하며,",
            "${ChatColor.YELLOW}발광 ${ChatColor.GRAY}효과를 ${ChatColor.WHITE}<explodeSeconds>초${ChatColor.GRAY} 동안 부여합니다.",
            " ",
            "매는 최대 ${ChatColor.WHITE}\${common.range}m${ChatColor.GRAY}까지 날아가며,",
            "그 이상 날아가면 매는 사라집니다."
        )
    }

    override fun onRenderTooltip(tooltip: TooltipBuilder, stats: (EsperStatistic) -> Double) {
        tooltip.addTemplates(
            "glowSeconds" to glowDurationTicks / 20,
            "explodeSeconds" to explosionDurationTicks / 20
        )
    }
}

class AsheHawkshot : ActiveAbility<AsheHawkShotConcept>() {

    override fun onCast(event: PlayerEvent, action: WandAction, target: Any?) {
        val eyeLocation = esper.player.eyeLocation
        val hawk = Hawk(eyeLocation)
        val projectile = HawkProjectile(hawk).apply {
            velocity = eyeLocation.direction.multiply(concept.launchSpeed)
        }
        psychic.launchProjectile(eyeLocation, projectile)
        eyeLocation.world.playSound(eyeLocation,Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0F, 1.5F)

        cooldownTicks = concept.cooldownTicks
    }

    inner class Hawk(private val location: Location) {
        private val entity: FakeEntity =
            psychic.spawnFakeEntity(location, ArmorStand::class.java).apply {
                updateMetadata<ArmorStand> {
                    rightArmPose = EulerAngle(0.0, Math.PI / 2.0, Math.PI)
                    invisible = true
                    isMarker = true
                    isSmall = true
                }
                updateEquipment {
                    setItemInMainHand(ItemStack(Material.PRISMARINE))
                }
            }

        fun update(newloc: Location = location) {
            entity.moveTo(newloc.clone().apply { y -= 0.81 })
        }

        fun remove() {
            entity.remove()
        }
    }

    private inner class HawkProjectile(private val hawk: Hawk) : PsychicProjectile(60*20, concept.range) {

        override fun onMove(movement: Movement) {
            movement.to = movement.from.clone().add(velocity)

            hawk.update(movement.to)

            val radius = concept.radius
            val box = BoundingBox.of(movement.from, radius, radius, radius)
            val filter = TargetFilter(esper.player)

            for(entity in movement.from.world.getNearbyEntities(box, filter)) {
                if(entity is LivingEntity) {
                    entity.addPotionEffect(PotionEffect(PotionEffectType.GLOWING, concept.glowDurationTicks, 0, false, false,true))
                }
            }
        }

        override fun onTrail(trail: Trail) {
            trail.velocity?.let { velocity ->
                val from = trail.from
                val world = from.world
                val length = velocity.normalizeAndLength()
                val filter = TargetFilter(esper.player)

                world.rayTrace(
                    from,
                    velocity,
                    length,
                    FluidCollisionMode.NEVER,
                    true,
                    concept.explosionRaySize,
                    filter
                )?.let { result ->
                    remove()

                    val hitPosition = result.hitPosition
                    val hitLocation = hitPosition.toLocation(world)

                    val firework =
                        FireworkEffect.builder().with(FireworkEffect.Type.STAR).withColor(Color.AQUA).build()
                    world.playFirework(hitLocation, firework)

                    val radius = concept.radius
                    val box = BoundingBox.of(hitPosition, radius, radius, radius)

                    for(entity in world.getNearbyEntities(box, filter)) {
                        if(entity is LivingEntity) {
                            entity.addPotionEffect(PotionEffect(PotionEffectType.GLOWING, concept.explosionDurationTicks, 0, false, false,true))
                        }
                    }
                }
            }
        }

        override fun onRemove() {
            hawk.remove()
        }
    }
}

