package com.exhibitorreg.publicregistration;

import java.time.Duration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Case-insensitive company dedup, enforced here rather than at the DB level (H2 doesn't
 * support the functional index that would have enforced it — see Phase 2's handoff notes). */
@Service
public class CompanyService {

    private static final String LOCK_KEY_PREFIX = "companylock:";
    private static final Duration LOCK_TTL = Duration.ofMillis(5000);

    private final CompanyRepository companyRepository;
    private final RedisTemplate<String, String> redisTemplate;

    public CompanyService(CompanyRepository companyRepository, RedisTemplate<String, String> redisTemplate) {
        this.companyRepository = companyRepository;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public Company findOrCreate(String name) {
        String normalized = name.trim();
        return companyRepository.findByNameIgnoreCase(normalized).orElseGet(() -> createWithLock(normalized));
    }

    /** Best-effort race protection: two near-simultaneous submissions of a brand-new company
     * name could otherwise both pass the initial lookup and create a duplicate. Not a hard
     * requirement given the low collision odds for this workload — cheap insurance since
     * Redis is already available, not a blocking/retrying lock. */
    private Company createWithLock(String normalized) {
        String lockKey = LOCK_KEY_PREFIX + normalized.toLowerCase();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", LOCK_TTL);
        try {
            return companyRepository.findByNameIgnoreCase(normalized).orElseGet(() -> {
                Company company = new Company();
                company.setName(normalized);
                companyRepository.save(company);
                return company;
            });
        } finally {
            if (Boolean.TRUE.equals(acquired)) {
                redisTemplate.delete(lockKey);
            }
        }
    }
}
