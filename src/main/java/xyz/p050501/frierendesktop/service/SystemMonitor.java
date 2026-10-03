package xyz.p050501.frierendesktop.service;
import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;
public final class SystemMonitor {
    public record Snapshot(double cpu, double memory) {}
    public Snapshot sample() {
        if (!(ManagementFactory.getOperatingSystemMXBean() instanceof OperatingSystemMXBean bean)) return new Snapshot(-1, -1);
        long total = bean.getTotalMemorySize();
        return new Snapshot(bean.getCpuLoad(), total > 0 ? 1.0 - (double) bean.getFreeMemorySize() / total : -1);
    }
}
