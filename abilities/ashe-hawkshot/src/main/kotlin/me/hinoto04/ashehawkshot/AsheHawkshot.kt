package me.hinoto04.ashehawkshot

import com.github.noonmaru.psychics.ActiveAbility
import com.github.noonmaru.psychics.AbilityConcept
import com.github.noonmaru.tap.config.Config

class AsheHawkShotConcept : AbilityConcept() {

    @Config
    var radius: Double = 10.0

    @Config
    var glowDurationTicks: Int = 20

    init {
        displayName = "매 날리기"
        cooldownTicks = 60 * 20

   }
}