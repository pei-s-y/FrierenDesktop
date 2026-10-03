package xyz.p050501.frierendesktop.service;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
public final class QuoteService {
    private static final List<String> QUOTES = List.of("今天也一起收集一点微小的快乐吧。", "这是搜集民间魔法的重要一环。",
        "辛美尔也曾夸奖过这个日程安排。", "偶尔停下来看看风景，也很好。", "我正在寻找一种能让铜像变干净的魔法。");
    public String next(LocalTime time) {
        if (time.getHour() >= 5 && time.getHour() < 9) return "早安。今天的旅途，从一杯水开始吧。";
        if (time.getHour() >= 22 || time.getHour() < 5) return "夜已经深了。剩下的魔法，明天再学吧。";
        return QUOTES.get(ThreadLocalRandom.current().nextInt(QUOTES.size()));
    }
}
