package net.charl.disembodiment.client.sound;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public class PlayerDematLoop extends AbstractTickableSoundInstance {
    private final float targetVolume;  // max volume to reach
    private int fadeIn;                // ticks remaining to fade in
    private int fadeOut;               // ticks remaining to fade out
    private boolean stopping;

    public PlayerDematLoop(SoundEvent event, float maxVolume, int fadeInTicks) {
        super(event, SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
        this.looping = true;        // keep playing until we stop it
        this.delay = 0;
        this.pitch = 1.0f;
        this.targetVolume = maxVolume;
        this.volume = 0.0f;         // start silent → fade in
        this.fadeIn = Math.max(0, fadeInTicks);
        this.fadeOut = 0;
        this.stopping = false;
        this.x = this.y = this.z = 0.0;  // ignored when relative = true
        this.relative = true;       // non-positional: plays “in your head”
        this.attenuation = Attenuation.NONE; // don’t dim with distance
    }

    public void beginFadeOut(int fadeOutTicks) {
        this.stopping = true;
        this.fadeOut = Math.max(1, fadeOutTicks);
    }

    @Override
    public boolean canStartSilent() {
        // allow the engine to start playing even if volume is 0 (so we can fade in)
        return true;
    }

    @Override
    public void tick() {
        if (stopping) {
            if (fadeOut > 0) {
                this.volume = Math.max(0f, this.volume - (targetVolume / fadeOut));
                fadeOut--;                           // <-- decrement!
            } else {
                this.stop();
            }
            return;
        }
        if (fadeIn > 0) {
            float step = targetVolume / fadeIn;
            this.volume = Math.min(targetVolume, this.volume + step);
            fadeIn--;
        } else {
            this.volume = targetVolume;
        }
    }
}




// HEY! HEY YOU! CHARLIE! PSSSSSSSSSST
// you left off trying to implement the noise that plays clientside while the player is in spectator.
// it is not functional right now, the noise has no fade in or out and it never stops!
// you also need to refactor a few files, especially ones relating to the crystal renderer!
// the crystal texture needs to be finalized
// loot tables, custom structure, jei integration
// then done! home stretch!