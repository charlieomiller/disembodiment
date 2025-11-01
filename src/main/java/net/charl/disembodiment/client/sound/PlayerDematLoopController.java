// net/charl/disembodiment/client/sound/DematActiveHumController.java
package net.charl.disembodiment.client.sound;

import net.charl.disembodiment.config.ModClientConfigs;
import net.charl.disembodiment.sound.ModSounds; // your sound registry
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;

public final class PlayerDematLoopController {
    private static PlayerDematLoop current;

    public static void start(SoundEvent event, float volume, int fadeInTicks) {
        var mc = Minecraft.getInstance();
        if (!ModClientConfigs.CLIENT.enableAmbience.get()) return;

        stop(20); // gently stop any previous instance (1s by default)
        current = new PlayerDematLoop(event, volume, fadeInTicks);
        mc.getSoundManager().play(current);
    }

    public static void stop(int fadeOutTicks) {
        if (current != null) {
            current.beginFadeOut(fadeOutTicks);
            current = null; // let SoundManager own the rest
        }
    }

    private PlayerDematLoopController() {}
}