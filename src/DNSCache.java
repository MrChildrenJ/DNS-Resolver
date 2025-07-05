import java.util.*;
import static java.lang.System.*;

public class DNSCache {
    private final Map<DNSQuestion, DNSRecord> cache;

    public DNSCache() {
        this.cache = new HashMap<>();
    }

    public synchronized DNSRecord query(DNSQuestion question) {
        DNSRecord record = cache.get(question);
        if (record != null && record.isExpired()) {
            cache.remove(question);
            return null;
        }
        return record;
    }

    public synchronized void insert(DNSQuestion question, DNSRecord record) {
        cache.put(question, record);
    }
}
