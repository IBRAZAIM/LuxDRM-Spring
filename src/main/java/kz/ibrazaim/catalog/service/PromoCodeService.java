package kz.ibrazaim.catalog.service;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpSession;
import kz.ibrazaim.catalog.model.PromoCode;
import kz.ibrazaim.catalog.repository.PromoCodeRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PromoCodeService {

    public static final String SESSION_KEY = "UXD_ACTIVE_PROMO";

    private static final Logger logger = LoggerFactory.getLogger(PromoCodeService.class);

    private final PromoCodeRepository promoCodeRepository;
    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensurePromoTableAndSeedDefaults() {
        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS promo_codes (
                    id BIGSERIAL PRIMARY KEY,
                    code VARCHAR(64) NOT NULL UNIQUE,
                    discount_percent INT NOT NULL,
                    description VARCHAR(255),
                    active BOOLEAN NOT NULL DEFAULT TRUE,
                    max_uses INT,
                    used_count INT NOT NULL DEFAULT 0,
                    min_order_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
                    expires_at TIMESTAMP,
                    created_at TIMESTAMP NOT NULL DEFAULT NOW()
                )
            """);
            jdbcTemplate.execute("""
                INSERT INTO promo_codes (code, discount_percent, description, active, max_uses, used_count, min_order_amount)
                VALUES
                    ('LUX10', 10, 'Скидка 10% по промокоду LUX10', TRUE, NULL, 0, 0),
                    ('WELCOME15', 15, 'Приветственная скидка 15%', TRUE, NULL, 0, 0),
                    ('VIP20', 20, 'VIP-скидка 20% на весь заказ', TRUE, NULL, 0, 0)
                ON CONFLICT (code) DO NOTHING
            """);
        } catch (Exception e) {
            logger.warn("Не удалось автоматически проверить таблицу promo_codes: {}", e.getMessage());
        }
    }

    public PromoInfo findPromo(String rawCode) {
        if (rawCode == null) {
            return null;
        }
        String normalized = rawCode.trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return null;
        }
        return promoCodeRepository.findByCodeIgnoreCase(normalized)
                .filter(this::isUsableNow)
                .map(p -> new PromoInfo(
                        p.getCode(),
                        p.getDiscountPercent(),
                        p.getDescription() != null && !p.getDescription().isBlank()
                                ? p.getDescription()
                                : "Скидка " + p.getDiscountPercent() + "% по промокоду " + p.getCode(),
                        p.getMinOrderAmount() != null ? p.getMinOrderAmount() : BigDecimal.ZERO
                ))
                .orElse(null);
    }

    private boolean isUsableNow(PromoCode promo) {
        if (promo == null || !promo.isActive() || promo.getDiscountPercent() <= 0) {
            return false;
        }
        if (promo.getExpiresAt() != null && promo.getExpiresAt().isBefore(LocalDateTime.now())) {
            return false;
        }
        if (promo.getMaxUses() != null && promo.getUsedCount() >= promo.getMaxUses()) {
            return false;
        }
        return true;
    }

    public PromoInfo getActivePromo(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object attr = session.getAttribute(SESSION_KEY);
        if (attr instanceof String code) {
            return findPromo(code);
        }
        return null;
    }

    public PromoInfo applyPromo(HttpSession session, String rawCode) {
        PromoInfo promo = findPromo(rawCode);
        if (promo != null && session != null) {
            session.setAttribute(SESSION_KEY, promo.getCode());
        }
        return promo;
    }

    public void clearPromo(HttpSession session) {
        if (session != null) {
            session.removeAttribute(SESSION_KEY);
        }
    }

    @Transactional
    public void consumePromo(HttpSession session) {
        if (session == null) {
            return;
        }
        Object attr = session.getAttribute(SESSION_KEY);
        if (attr instanceof String code && !code.isBlank()) {
            promoCodeRepository.findByCodeIgnoreCase(code.trim()).ifPresent(promo -> {
                promo.setUsedCount(promo.getUsedCount() + 1);
                promoCodeRepository.save(promo);
            });
        }
        clearPromo(session);
    }

    public BigDecimal calculatePromoDiscount(BigDecimal amountAfterProductDiscount, PromoInfo promo) {
        if (promo == null || amountAfterProductDiscount == null || amountAfterProductDiscount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (promo.getMinOrderAmount() != null && amountAfterProductDiscount.compareTo(promo.getMinOrderAmount()) < 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return amountAfterProductDiscount
                .multiply(BigDecimal.valueOf(promo.getPercent()))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                .min(amountAfterProductDiscount);
    }

    public BigDecimal calculatePromoDiscount(BigDecimal amountAfterProductDiscount, HttpSession session) {
        return calculatePromoDiscount(amountAfterProductDiscount, getActivePromo(session));
    }

    public List<PromoCode> findAllPromos() {
        return promoCodeRepository.findAllByOrderByIdDesc();
    }

    @Transactional
    public PromoCode createOrUpdatePromo(
            String rawCode,
            int discountPercent,
            String description,
            Integer maxUses,
            BigDecimal minOrderAmount,
            LocalDateTime expiresAt,
            boolean active
    ) {
        String code = rawCode == null ? "" : rawCode.trim().toUpperCase(Locale.ROOT);
        if (code.isBlank() || code.length() < 3) {
            throw new IllegalArgumentException("Код промокода должен содержать минимум 3 символа.");
        }
        if (discountPercent < 1 || discountPercent > 90) {
            throw new IllegalArgumentException("Процент скидки должен быть от 1 до 90%.");
        }
        if (maxUses != null && maxUses <= 0) {
            maxUses = null;
        }
        BigDecimal safeMinAmount = (minOrderAmount != null && minOrderAmount.compareTo(BigDecimal.ZERO) > 0)
                ? minOrderAmount
                : BigDecimal.ZERO;

        PromoCode promo = promoCodeRepository.findByCodeIgnoreCase(code).orElseGet(PromoCode::new);
        promo.setCode(code);
        promo.setDiscountPercent(discountPercent);
        promo.setDescription(description != null && !description.isBlank()
                ? description.trim()
                : "Скидка " + discountPercent + "% по промокоду " + code);
        promo.setMaxUses(maxUses);
        promo.setMinOrderAmount(safeMinAmount);
        promo.setExpiresAt(expiresAt);
        promo.setActive(active);
        if (promo.getCreatedAt() == null) {
            promo.setCreatedAt(LocalDateTime.now());
        }
        return promoCodeRepository.save(promo);
    }

    @Transactional
    public void togglePromo(Long id) {
        PromoCode promo = promoCodeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Промокод не найден."));
        promo.setActive(!promo.isActive());
        promoCodeRepository.save(promo);
    }

    @Transactional
    public void deletePromo(Long id) {
        if (promoCodeRepository.existsById(id)) {
            promoCodeRepository.deleteById(id);
        }
    }

    @Getter
    @AllArgsConstructor
    public static class PromoInfo {
        private final String code;
        private final int percent;
        private final String description;
        private final BigDecimal minOrderAmount;

        public PromoInfo(String code, int percent, String description) {
            this(code, percent, description, BigDecimal.ZERO);
        }
    }
}
