package xyz.p050501.frierendesktop.model;
import java.time.Duration;
import java.time.Instant;
/** Pure companion state, independent of JavaFX and timers. */
public final class CompanionState {
    private int affection;
    private boolean resting;
    private Instant lastTreat, focusEnd;
    public int affection() { return affection; }
    public boolean resting() { return resting; }
    public void setResting(boolean value) { resting = value; }
    public String pet() {
        if (resting) return "嘘……让我再睡一会儿。";
        affection = Math.min(100, affection + 1);
        return "嗯……这样的魔法，也不坏。";
    }
    public String feed(Instant now) {
        if (resting) return "点心留到醒来再吃吧。";
        if (lastTreat != null && now.isBefore(lastTreat.plusSeconds(30))) return "刚刚才吃过呢，过一会儿再来吧。";
        lastTreat = now;
        affection = Math.min(100, affection + 5);
        return "甜甜的。下次旅行，也带上这个吧。";
    }
    public void startFocus(Instant now, Duration duration) {
        if (duration.isNegative() || duration.isZero()) throw new IllegalArgumentException("Positive duration required");
        focusEnd = now.plus(duration);
    }
    public void cancelFocus() { focusEnd = null; }
    public boolean focusing() { return focusEnd != null; }
    public long remainingSeconds(Instant now) {
        return focusEnd == null ? 0 : Math.max(0, (long) Math.ceil(Duration.between(now, focusEnd).toMillis() / 1000.0));
    }
    public boolean finishFocus(Instant now) {
        if (focusEnd == null || now.isBefore(focusEnd)) return false;
        focusEnd = null;
        affection = Math.min(100, affection + 10);
        return true;
    }
}
