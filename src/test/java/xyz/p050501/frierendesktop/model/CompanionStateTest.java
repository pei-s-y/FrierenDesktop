package xyz.p050501.frierendesktop.model;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
class CompanionStateTest {
    private final Instant now = Instant.parse("2026-10-03T00:00:00Z");
    @Test void treatsHaveCooldownAndAffectionIsBounded() {
        CompanionState state = new CompanionState();
        state.feed(now); state.feed(now.plusSeconds(29)); assertEquals(5, state.affection());
        state.feed(now.plusSeconds(30)); assertEquals(10, state.affection());
        for (int i = 0; i < 200; i++) state.pet();
        assertEquals(100, state.affection());
    }
    @Test void restingBlocksAffectionButNotFocusCompletion() {
        CompanionState state = new CompanionState(); state.setResting(true);
        state.pet(); state.feed(now); assertEquals(0, state.affection());
        state.startFocus(now, Duration.ofMinutes(25));
        assertFalse(state.finishFocus(now.plusSeconds(1499)));
        assertTrue(state.finishFocus(now.plusSeconds(1500)));
        assertFalse(state.finishFocus(now.plusSeconds(1501))); assertEquals(10, state.affection());
    }
    @Test void countdownUsesElapsedTimeAndCanBeCancelled() {
        CompanionState state = new CompanionState(); state.startFocus(now, Duration.ofMinutes(15));
        assertEquals(900, state.remainingSeconds(now.plusMillis(1)));
        assertEquals(30, state.remainingSeconds(now.plusSeconds(870)));
        state.cancelFocus(); assertFalse(state.focusing()); assertFalse(state.finishFocus(now.plusSeconds(9999)));
        assertThrows(IllegalArgumentException.class, () -> state.startFocus(now, Duration.ZERO));
    }
}
