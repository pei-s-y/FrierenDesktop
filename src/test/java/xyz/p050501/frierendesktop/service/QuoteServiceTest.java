package xyz.p050501.frierendesktop.service;
import org.junit.jupiter.api.Test;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.*;
class QuoteServiceTest {
    @Test void greetingsRespectDayBoundaries() {
        var service = new QuoteService();
        assertTrue(service.next(LocalTime.of(5, 0)).startsWith("早安"));
        assertTrue(service.next(LocalTime.of(8, 59)).startsWith("早安"));
        assertTrue(service.next(LocalTime.of(22, 0)).startsWith("夜已经深了"));
        assertTrue(service.next(LocalTime.of(4, 59)).startsWith("夜已经深了"));
        assertFalse(service.next(LocalTime.NOON).isBlank());
    }
}
