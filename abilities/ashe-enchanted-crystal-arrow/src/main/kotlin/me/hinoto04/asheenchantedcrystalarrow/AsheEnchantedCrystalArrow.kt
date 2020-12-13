package me.hinoto04.asheenchantedcrystalarrow


import com.github.noonmaru.psychics.ActiveAbility
import com.github.noonmaru.psychics.AbilityConcept
import com.github.noonmaru.psychics.PsychicProjectile
import com.github.noonmaru.psychics.attribute.EsperAttribute
import com.github.noonmaru.psychics.attribute.EsperStatistic
import com.github.noonmaru.psychics.damage.Damage
import com.github.noonmaru.psychics.damage.DamageType
import com.github.noonmaru.psychics.item.isPsychicbound
import com.github.noonmaru.psychics.tooltip.TooltipBuilder
import com.github.noonmaru.psychics.util.TargetFilter
import com.github.noonmaru.tap.config.Config
import com.github.noonmaru.tap.effect.playFirework
import com.github.noonmaru.tap.fake.FakeEntity
import com.github.noonmaru.tap.fake.Movement
import com.github.noonmaru.tap.fake.Trail
import com.github.noonmaru.tap.math.normalizeAndLength
import com.github.noonmaru.tap.math.toRadians
import com.github.noonmaru.tap.trail.trail
import org.bukkit.*
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.event.player.PlayerEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.BoundingBox
import org.bukkit.util.EulerAngle

class AsheEnchantedCrystalArrowConcept : AbilityConcept() {

    private val wandItem: ItemStack = ItemStack(Material.HEART_OF_THE_SEA).apply {
        val meta = itemMeta
        meta.setDisplayName("${ChatColor.AQUA}${ChatColor.BOLD}마법의 수정화살")
        meta.isPsychicbound = true
        itemMeta = meta
    }

    @Config
    var stunDurationTicks: Int = 3 * 20

    @Config
    var arrowSize: Double = 0.5

    @Config
    var launchSpeed: Double = 1.5

    @Config
    var splashRadius: Double = 3.0

    @Config
    var splashDamage: Double = 3.0

    @Config
    var splashSlowValue: Int = 4

    @Config
    var splashSlowTicks: Long = 3 * 20

    @Config
    var glowRadius: Double = 5.0

    @Config
    var glowDurationTicks: Int = 1 * 20

    init {
        displayName = "마법의 수정화살"
        cooldownTicks = 80 * 20
        cost = 100.0
        range = 256.0
        damage = Damage(DamageType.RANGED, EsperStatistic.of(EsperAttribute.ATTACK_DAMAGE to 5.0))
        wand = wandItem
        supplyItems = listOf(
            wandItem
        )
        description = listOf(
            "얼음 수정 화살을 발사하여 처음 맞히는 적에게",
            "원거리 피해를 입히고,",
            "${ChatColor.WHITE}<stunSeconds>초 ${ChatColor.GRAY}동안 기절시킵니다.",
            "주변 적들은 공격력의 ${ChatColor.RED}<splashPercent>%${ChatColor.GRAY}의",
            "폭발 피해를 입고,",
            "이동속도가 ${ChatColor.WHITE}<splashSlowSeconds>초 ${ChatColor.GRAY}동안 " +
                    "${ChatColor.BLUE}<splashSlowValue>% ${ChatColor.GRAY}느려집니다.",
            "",
            "마법의 수정 화살은 최대 ${ChatColor.WHITE}\${common.range}m${ChatColor.GRAY}까지 날아가며,",
            "벽에 부딫히면 폭발하고,",
            "사거리 이상 날아가면 사라집니다."
        )
    }

    override fun onRenderTooltip(tooltip: TooltipBuilder, stats: (EsperStatistic) -> Double) {
        tooltip.addTemplates(
            "stunSeconds" to stunDurationTicks / 20,
            "splashPercent" to splashDamage * 100,
            "splashSlowSeconds" to splashSlowTicks / 20,
            "splashSlowValue" to splashSlowValue * 15
        )
    }
}

class AsheEnchantedCrystalArrow : ActiveAbility<AsheEnchantedCrystalArrowConcept>() {
    companion object {
        internal val arrowItem = ItemStack(Material.ICE)
    }

    override fun onCast(event: PlayerEvent, action: WandAction, target: Any?) {
        val eyeLocation = esper.player.eyeLocation
        val arrow = CrystalArrow(eyeLocation)
        val projectile = CrystalArrowProjectile(arrow).apply {
            velocity = eyeLocation.direction.multiply(concept.launchSpeed)
        }
        psychic.launchProjectile(eyeLocation, projectile)
        eyeLocation.world.playSound(eyeLocation, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0F, 1.5F)

        cooldownTicks = concept.cooldownTicks
    }

    inner class CrystalArrow( private val location: Location ) {

        private val entity: FakeEntity = psychic.spawnFakeEntity(location, ArmorStand::class.java).apply {
            updateMetadata<ArmorStand> {
                isVisible = false
                isMarker = true
                headPose = EulerAngle(30.0.toRadians(), 40.0.toRadians(), 45.0.toRadians())
            }
            updateEquipment {
                helmet = arrowItem
            }
        }

        fun update(newLoc: Location = location) {
            entity.moveTo(newLoc.clone().apply { y -= 1.62 })
        }

        fun remove() {
            entity.remove()
        }

    }

    private inner class CrystalArrowProjectile(private val arrow: CrystalArrow) : PsychicProjectile(60*20, concept.range) {

        override fun onMove(movement: Movement) {
            movement.to = movement.from.clone().add(velocity)

            arrow.update(movement.to)

            val radius = concept.glowRadius

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
                val to = trail.to
                val world = from.world
                val length = velocity.normalizeAndLength()
                val filter = TargetFilter(esper.player)

                world.rayTrace(
                    from,
                    velocity,
                    length,
                    FluidCollisionMode.NEVER,
                    true,
                    concept.arrowSize,
                    filter
                )?.let { result ->
                    remove()

                    var targeted: Entity? = null

                    result.hitEntity?.let { target ->
                        if (target is LivingEntity) {
                            val damage = requireNotNull(concept.damage)

                            target.psychicDamage(damage, esper.player.eyeLocation, 0.0)

                            targeted = target

                            target.addPotionEffect(PotionEffect(PotionEffectType.SLOW, concept.stunDurationTicks, 9, false, false, true))
                            target.addPotionEffect(PotionEffect(PotionEffectType.GLOWING, concept.stunDurationTicks, 0, false, false, true))
                            target.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, concept.stunDurationTicks, 0, false, false, true))
                            target.addPotionEffect(PotionEffect(PotionEffectType.SLOW_DIGGING, concept.stunDurationTicks, 9, false, false, true))
                            target.addPotionEffect(PotionEffect(PotionEffectType.WEAKNESS, concept.stunDurationTicks, 9, false, false, true))
                        }
                    }

                    val hitPosition = result.hitPosition
                    val hitLocation = hitPosition.toLocation(world)

                    val firework =
                        FireworkEffect.builder().with(FireworkEffect.Type.STAR).withColor(Color.AQUA).build()
                    world.playFirework(hitLocation, firework)

                    val radius = concept.splashRadius
                    val box = BoundingBox.of(hitPosition, radius, radius, radius)

                    for(entity in world.getNearbyEntities(box, filter)) {
                        if(entity == targeted) {
                            continue
                        }
                        val damage = Damage(DamageType.BLAST, EsperStatistic.Companion.of(EsperAttribute.ATTACK_DAMAGE to concept.splashDamage))
                        if(entity is LivingEntity) {
                            entity.psychicDamage(damage, esper.player.eyeLocation, 0.0)
                        }
                    }
                }

                trail(from, to, 0.25) { w, x, y, z ->
                    w.spawnParticle(
                        Particle.CRIT_MAGIC,
                        x, y, z,
                        5,
                        0.1, 0.1, 0.1,
                        0.25, null, true
                    )
                    w.spawnParticle(
                        Particle.REDSTONE,
                        x, y, z,
                        3,
                        1.0, 1.0,1.0,
                        0.25, Particle.DustOptions(Color.fromRGB(0, 238, 245), 1F), true
                    )
                }
            }
        }

        override fun onRemove() {
            arrow.remove()
        }
    }
}