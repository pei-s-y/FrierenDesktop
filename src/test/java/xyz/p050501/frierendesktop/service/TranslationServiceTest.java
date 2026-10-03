package xyz.p050501.frierendesktop.service;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class TranslationServiceTest {
    @Test void joinsEveryTranslatedParagraph() {
        try (var service = new TranslationService()) {
            assertEquals("你好\n世界", service.parseResponse("{\"trans_result\":[{\"dst\":\"你好\"},{\"dst\":\"世界\"}]}"));
        }
    }
    @Test void handlesApiErrorsAndMalformedResults() {
        try (var service = new TranslationService()) {
            assertTrue(assertThrows(IllegalStateException.class, () -> service.parseResponse("{\"error_code\":\"54003\"}")).getMessage().contains("54003"));
            for (String body : new String[]{"null", "{}", "not json", "{\"trans_result\":[]}", "{\"trans_result\":[{}]}"})
                assertThrows(IllegalStateException.class, () -> service.parseResponse(body));
        }
    }
}
