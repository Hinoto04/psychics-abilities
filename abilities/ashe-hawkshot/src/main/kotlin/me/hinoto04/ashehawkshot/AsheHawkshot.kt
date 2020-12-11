package me.hinoto04.ashehawkshot

import com.github.noonmaru.psychics.ActiveAbility
import com.github.noonmaru.psychics.AbilityConcept
import com.github.noonmaru.psychics.PsychicProjectile
import com.github.noonmaru.psychics.attribute.EsperStatistic
import com.github.noonmaru.psychics.item.isPsychicbound
import com.github.noonmaru.psychics.tooltip.TooltipBuilder
import com.github.noonmaru.tap.config.Config
import com.github.noonmaru.tap.fake.Movement
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.event.player.PlayerEvent
import org.bukkit.inventory.ItemStack

class AsheHawkShotConcept : AbilityConcept() {

    @Config
    var radius: Double = 10.0

    @Config
    var glowDurationTicks: Int = 1 * 20

    @Config
    var hawkDurationTicks: Int = 5 * 20

    @Config
    var launchSpeed: Double = 2.0

    private val wandItem: ItemStack = ItemStack(Material.PRISMARINE_SHARD).apply {
        val meta = itemMeta
        meta.setDisplayName("${ChatColor.AQUA}${ChatColor.BOLD}매 날리기")
        meta.isPsychicbound = true
        itemMeta = meta
    }

    init {
        displayName = "매 날리기"
        cooldownTicks = 60 * 20
        wand = wandItem
        supplyItems = listOf(
            wandItem
        )
        castingTicks = 5
        range = 128.0
        description = listOf(
            "주변 일대의 적에게 ${ChatColor.YELLOW}발광 ${ChatColor.GRAY}효과를 ",
            "${ChatColor.WHITE}<glowSeconds>초${ChatColor.GRAY}간 부여하는 매를 전방으로 날립니다.",
            "매는 벽에 부딫히면 그자리에 멈추며,",
            "${ChatColor.WHITE}<hawkSeconds>초${ChatColor.GRAY}간 유지됩니다.",
            " ",
            "매는 최대 ${ChatColor.WHITE}\${common.range}m${ChatColor.GRAY}까지 날아갑니다."
        )
    }

    override fun onRenderTooltip(tooltip: TooltipBuilder, stats: (EsperStatistic) -> Double) {
        tooltip.addTemplates(
            "glowSeconds" to glowDurationTicks / 20,
            "hawkSeconds" to hawkDurationTicks / 20
        )
    }
}

class AsheHawkshot : ActiveAbility<AsheHawkShotConcept>() {
    override fun onCast(event: PlayerEvent, action: WandAction, target: Any?) {
        val eyeLocation = esper.player.eyeLocation
        val projectile = HawkProjectile()
        psychic.launchProjectile(eyeLocation, projectile)
        projectile.velocity = eyeLocation.direction.multiply(concept.launchSpeed)
    }

    private inner class HawkProjectile : PsychicProjectile(60*20, concept.range) {
        override fun onMove(movement: Movement) {

        }
    }
}