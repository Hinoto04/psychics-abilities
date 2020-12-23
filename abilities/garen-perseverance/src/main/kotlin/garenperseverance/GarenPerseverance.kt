package garenperseverance

import com.github.noonmaru.psychics.Ability
import com.github.noonmaru.psychics.AbilityConcept
import com.github.noonmaru.psychics.attribute.EsperStatistic
import com.github.noonmaru.psychics.item.isPsychicbound
import com.github.noonmaru.psychics.tooltip.TooltipBuilder
import com.github.noonmaru.tap.config.Config
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.ItemStack

class GarenPerseveranceConcept : AbilityConcept() {
    private val wandItem = ItemStack(Material.LIME_DYE).apply {
        val meta = itemMeta
        meta.setDisplayName("인내심")
        meta.isPsychicbound = true
        itemMeta = meta
    }

    @Config
    var waitTicks: Long = 8 * 20

    @Config
    var healCoeff: Double = 2.0

    @Config
    var healDelayTicks: Long = 5 * 20

    @Config
    var stopMinDamage: Double = 1.0

    init {
        displayName = "인내심"
        wand = wandItem
        description = listOf(
            "${ChatColor.WHITE}<waitSeconds>초 ${ChatColor.GRAY}동안",
            "${ChatColor.WHITE}<minDamage> ${ChatColor.GRAY}이상의 피해를 입지 않으면,",
            "${ChatColor.WHITE}<delaySeconds>초${ChatColor.GRAY}마다 레벨의 ${ChatColor.LIGHT_PURPLE}<healCoeff>%${ChatColor.GRAY}의 체력을 회복합니다."
        )
    }

    override fun onRenderTooltip(tooltip: TooltipBuilder, stats: (EsperStatistic) -> Double) {
        tooltip.addTemplates(
            "waitSeconds" to waitTicks / 20,
            "minDamage" to stopMinDamage,
            "delaySeconds" to healDelayTicks / 20,
            "healCoeff" to healCoeff * 100
        )
    }
}

class GarenPerseverance : Ability<GarenPerseveranceConcept>() {

    override fun onEnable() {
        //psychic.registerEvents(EventListener())
        //psychic.runTaskTimer(CheckHeal(), 1L, 1L)
    }

    //var waitTimer: Long = concept.waitTicks
    //var healTimer: Long = concept.healDelayTicks

    inner class EventListener : Listener {
        @EventHandler
        fun onDamaged(event: EntityDamageByEntityEvent) {
//            if(event.damage > concept.stopMinDamage) {
//                waitTimer = concept.waitTicks
//                healTimer = concept.healDelayTicks
//            }
        }
    }

    inner class CheckHeal : Runnable {
        override fun run() {
            /*if(waitTimer > 0) {
                waitTimer--
            } else if(healTimer > 0) {
                healTimer--
            } else {
                healTimer = concept.healDelayTicks
                esper.player.health += esper.getAttribute(EsperAttribute.LEVEL) * concept.healCoeff
            }*/
            Bukkit.broadcastMessage("태스크")
        }
    }
}