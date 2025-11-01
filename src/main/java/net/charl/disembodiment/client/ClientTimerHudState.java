package net.charl.disembodiment.client;

import net.charl.disembodiment.networking.TimerSyncS2C;
import org.jline.utils.Display;

public final class ClientTimerHudState {
    private static TimerSyncS2C.Phase phase = TimerSyncS2C.Phase.NONE;
    private static TimerSyncS2C.Phase prevPhase = TimerSyncS2C.Phase.NONE;
    private static int secRemaining = 0;
    private static int secTotal = 0;

    public static void applyServerSnapshot(TimerSyncS2C msg) {
        phase = msg.phase;
        secRemaining = Math.max(0, msg.ticksRemaining / 20);
        secTotal = Math.max(0, msg.ticksTotal / 20);
    }

    public static Display getDisplay() {
        return new Display(phase, secRemaining, secTotal);
    }

    public record Display(TimerSyncS2C.Phase phase, int secondsRemaining, int secondsTotal) {
        public static Display none()    { return new Display(TimerSyncS2C.Phase.NONE, 0, 0); }
        public boolean visible()        { return phase != TimerSyncS2C.Phase.NONE && secondsTotal > 0; }
        public String label()           {
            String p = (phase == TimerSyncS2C.Phase.BUFFER) ? "Dematerializing" : "Dematerialized";
            if (secondsRemaining == 1) { return p + " for " + secondsRemaining + " second"; }
            else { return p + " for " + secondsRemaining + " seconds"; }
        }
    }

    private ClientTimerHudState() {}
}
