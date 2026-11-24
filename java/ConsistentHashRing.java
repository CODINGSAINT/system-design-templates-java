import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Consistent hashing ring with virtual nodes.
 *
 * Companion to sheet 07 (consistent hashing). Plain modulo (% nodeCount) remaps almost every key
 * when a node joins or leaves. A ring moves roughly 1/(N+1) of the keys when one node is added,
 * which is the property this class demonstrates in main().
 *
 * No dependencies. Run with: java ConsistentHashRing.java
 */
public final class ConsistentHashRing {

    private final NavigableMap<Long, String> ring = new TreeMap<>();
    private final int virtualNodes;

    public ConsistentHashRing(int virtualNodes) {
        if (virtualNodes < 1) throw new IllegalArgumentException("virtualNodes must be >= 1");
        this.virtualNodes = virtualNodes;
    }

    /** Adds a physical node as {@code virtualNodes} points on the ring. */
    public synchronized void addNode(String node) {
        for (int i = 0; i < virtualNodes; i++) {
            ring.put(hash(node + "#" + i), node);
        }
    }

    /** Removes a physical node and every point it owned. */
    public synchronized void removeNode(String node) {
        ring.values().removeIf(node::equals);
    }

    /** Returns the node that owns this key, or null when the ring is empty. */
    public synchronized String nodeFor(String key) {
        if (ring.isEmpty()) return null;
        Map.Entry<Long, String> entry = ring.ceilingEntry(hash(key));
        // Past the last point, wrap around to the first one.
        if (entry == null) entry = ring.firstEntry();
        return entry.getValue();
    }

    public synchronized int size() {
        return ring.size();
    }

    /** 64-bit hash point. SHA-256 keeps the ring uniform, unlike a bare String.hashCode(). */
    private static long hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            long h = 0;
            for (int i = 0; i < 8; i++) {
                h = (h << 8) | (digest[i] & 0xFFL);
            }
            return h;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by the JDK", e);
        }
    }

    // ------------------------------------------------------------------ demo

    private static ConsistentHashRing ringOf(int vnodes, String... nodes) {
        ConsistentHashRing ring = new ConsistentHashRing(vnodes);
        for (String node : nodes) ring.addNode(node);
        return ring;
    }

    public static void main(String[] args) {
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < 100_000; i++) keys.add("user:" + i);

        ConsistentHashRing three = ringOf(160, "cache-a", "cache-b", "cache-c");
        ConsistentHashRing four = ringOf(160, "cache-a", "cache-b", "cache-c", "cache-d");

        // 1. Load is spread evenly across the three nodes.
        Map<String, Integer> load = new TreeMap<>();
        int changed = 0;
        for (String key : keys) {
            load.merge(three.nodeFor(key), 1, Integer::sum);
            if (!three.nodeFor(key).equals(four.nodeFor(key))) changed++;
        }
        for (Map.Entry<String, Integer> e : load.entrySet()) {
            double share = 100.0 * e.getValue() / keys.size();
            System.out.printf("with 3 nodes: %-8s %5.1f%% of keys%n", e.getKey(), share);
            if (share < 25.0 || share > 42.0) throw new AssertionError("uneven distribution for " + e.getKey());
        }

        // 2. Adding a fourth node moves only its share, not the whole mapping.
        double movedPct = 100.0 * changed / keys.size();
        System.out.printf("adding a 4th node moved %.2f%% of keys (ideal ~25%%)%n", movedPct);
        if (movedPct < 20.0 || movedPct > 30.0) throw new AssertionError("ring is not behaving like consistent hashing");
        System.out.printf("plain modulo would have moved %.0f%% of keys%n", 100.0 * 3 / 4);

        // 3. Removing a node gives its keys back to the remaining nodes only.
        three.removeNode("cache-c");
        if (three.size() != 320) throw new AssertionError("virtual nodes not removed");
        for (String key : keys) {
            if ("cache-c".equals(three.nodeFor(key))) throw new AssertionError("removed node still owns keys");
        }

        System.out.println("ConsistentHashRing OK");
    }
}
