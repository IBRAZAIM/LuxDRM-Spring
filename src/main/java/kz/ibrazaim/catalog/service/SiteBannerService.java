package kz.ibrazaim.catalog.service;

import kz.ibrazaim.catalog.model.SiteBanner;
import kz.ibrazaim.catalog.repository.SiteBannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SiteBannerService {
    public static final String SLOT_HERO = "hero";
    public static final List<String> SECTION_SLOTS = List.of("section_men", "section_women", "section_kids");
    public static final List<String> CATEGORY_SLOTS = List.of("cat_pyjamas", "cat_home", "cat_kids", "cat_lounge");

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".webp", ".gif", ".avif"
    );

    private record BannerSeed(String slotKey, String title, String subtitle, String targetUrl, int sortOrder) {
        SiteBanner toEntity() {
            return new SiteBanner(slotKey, title, subtitle, targetUrl, null, sortOrder, LocalDateTime.now());
        }
    }

    private static final List<BannerSeed> DEFAULT_SEEDS = List.of(
            new BannerSeed(SLOT_HERO, "Домашний комфорт", "Новая коллекция · UXD", "/products", 1),
            new BannerSeed("section_men", "Мужское", "Коллекция для него", "/products?section=men", 10),
            new BannerSeed("section_women", "Женское", "Коллекция для неё", "/products?section=women", 11),
            new BannerSeed("section_kids", "Детское", "Комфорт для детей", "/products?section=kids", 12),
            new BannerSeed("cat_pyjamas", "Пижамы", "Коллекция", "/products?category=Пижамы", 20),
            new BannerSeed("cat_home", "Домашняя одежда", "Коллекция", "/products?category=Домашняя одежда", 21),
            new BannerSeed("cat_kids", "Детская", "Коллекция", "/products?category=Детская", 22),
            new BannerSeed("cat_lounge", "Lounge", "Коллекция", "/products?category=Lounge", 23)
    );

    private static final Map<String, BannerSeed> SEEDS_BY_KEY = buildSeedsMap();

    private static Map<String, BannerSeed> buildSeedsMap() {
        Map<String, BannerSeed> map = new LinkedHashMap<>();
        for (BannerSeed seed : DEFAULT_SEEDS) {
            map.put(seed.slotKey(), seed);
        }
        return Map.copyOf(map);
    }

    private final SiteBannerRepository siteBannerRepository;

    @Value("${app.upload.banners-dir:uploads/banners}")
    private String bannersUploadDir;

    private volatile Map<String, SiteBanner> cachedBannersMap;

    public Map<String, SiteBanner> getBannersMap() {
        Map<String, SiteBanner> snapshot = this.cachedBannersMap;
        if (snapshot != null) {
            return snapshot;
        }
        Map<String, SiteBanner> map = new LinkedHashMap<>();
        for (SiteBanner banner : siteBannerRepository.findAllByOrderBySortOrderAscSlotKeyAsc()) {
            map.put(banner.getSlotKey(), banner);
        }
        for (BannerSeed seed : DEFAULT_SEEDS) {
            map.putIfAbsent(seed.slotKey(), seed.toEntity());
        }
        Map<String, SiteBanner> immutable = Collections.unmodifiableMap(map);
        this.cachedBannersMap = immutable;
        return immutable;
    }

    public SiteBanner getHeroBanner() {
        return getBannersMap().get(SLOT_HERO);
    }

    public List<SiteBanner> getSectionBanners() {
        Map<String, SiteBanner> map = getBannersMap();
        return SECTION_SLOTS.stream().map(map::get).toList();
    }

    public List<SiteBanner> getCategoryBanners() {
        Map<String, SiteBanner> map = getBannersMap();
        return CATEGORY_SLOTS.stream().map(map::get).toList();
    }

    @Transactional
    public SiteBanner updateBanner(
            String slotKey,
            String title,
            String subtitle,
            String targetUrl,
            MultipartFile imageFile
    ) {
        String cleanKey = slotKey != null ? slotKey.trim() : "";
        SiteBanner banner = siteBannerRepository.findById(cleanKey).orElseGet(() -> {
            BannerSeed seed = SEEDS_BY_KEY.get(cleanKey);
            if (seed == null) {
                throw new IllegalArgumentException("Неизвестный блок оформления: " + slotKey);
            }
            return seed.toEntity();
        });

        if (title != null && !title.trim().isEmpty()) {
            String trimmedTitle = title.trim();
            banner.setTitle(trimmedTitle.length() > 160 ? trimmedTitle.substring(0, 160) : trimmedTitle);
        }

        if (subtitle != null) {
            String trimmedSub = subtitle.trim();
            banner.setSubtitle(trimmedSub.length() > 255 ? trimmedSub.substring(0, 255) : trimmedSub);
        }

        if (targetUrl != null && !targetUrl.trim().isEmpty()) {
            String trimmedUrl = targetUrl.trim();
            if (!trimmedUrl.startsWith("/") || trimmedUrl.startsWith("//")
                    || trimmedUrl.contains("\r") || trimmedUrl.contains("\n")) {
                throw new IllegalArgumentException("Ссылка должна быть внутренним путём, начинающимся с '/'");
            }
            banner.setTargetUrl(trimmedUrl.length() > 255 ? trimmedUrl.substring(0, 255) : trimmedUrl);
        }

        String oldImageUrl = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            String newImageUrl = storeBannerFile(imageFile);
            oldImageUrl = banner.getImageUrl();
            banner.setImageUrl(newImageUrl);
        }

        banner.setUpdatedAt(LocalDateTime.now());
        SiteBanner saved = siteBannerRepository.save(banner);
        this.cachedBannersMap = null;

        if (oldImageUrl != null && !oldImageUrl.equals(saved.getImageUrl())) {
            scheduleOldFileDeletion(oldImageUrl);
        }
        return saved;
    }

    @Transactional
    public SiteBanner resetBannerImage(String slotKey) {
        String cleanKey = slotKey != null ? slotKey.trim() : "";
        SiteBanner banner = siteBannerRepository.findById(cleanKey).orElseGet(() -> {
            BannerSeed seed = SEEDS_BY_KEY.get(cleanKey);
            if (seed == null) {
                throw new IllegalArgumentException("Неизвестный блок оформления: " + slotKey);
            }
            return seed.toEntity();
        });

        String oldImageUrl = banner.getImageUrl();
        banner.setImageUrl(null);
        banner.setUpdatedAt(LocalDateTime.now());
        SiteBanner saved = siteBannerRepository.save(banner);
        this.cachedBannersMap = null;

        if (oldImageUrl != null) {
            scheduleOldFileDeletion(oldImageUrl);
        }
        return saved;
    }

    private String storeBannerFile(MultipartFile file) {
        Path targetDir = Paths.get(bannersUploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(targetDir);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось создать директорию для загрузки фонов", e);
        }

        String originalName = file.getOriginalFilename();
        String ext = extractExtension(originalName, file.getContentType());
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException(
                    "Недопустимый формат файла: " + (originalName != null ? originalName : "") +
                    ". Разрешены: JPG, PNG, WEBP, GIF, AVIF"
            );
        }

        String filename = UUID.randomUUID() + ext;
        Path destination = targetDir.resolve(filename).normalize();
        if (!destination.startsWith(targetDir)) {
            throw new SecurityException("Некорректный путь сохранения файла");
        }

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/banners/" + filename;
        } catch (IOException e) {
            throw new IllegalStateException("Ошибка при сохранении изображения: " + originalName, e);
        }
    }

    private String extractExtension(String filename, String contentType) {
        if (filename != null) {
            int dotIndex = filename.lastIndexOf('.');
            if (dotIndex >= 0 && dotIndex < filename.length() - 1) {
                String ext = filename.substring(dotIndex).toLowerCase(Locale.ROOT).trim();
                if (ALLOWED_EXTENSIONS.contains(ext)) {
                    return ext;
                }
            }
        }
        if (contentType != null) {
            String ct = contentType.toLowerCase(Locale.ROOT);
            if (ct.contains("jpeg") || ct.contains("jpg")) return ".jpg";
            if (ct.contains("png")) return ".png";
            if (ct.contains("webp")) return ".webp";
            if (ct.contains("gif")) return ".gif";
            if (ct.contains("avif")) return ".avif";
        }
        return "";
    }

    private void scheduleOldFileDeletion(String oldImageUrl) {
        if (oldImageUrl == null || !oldImageUrl.startsWith("/uploads/banners/")) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteBannerFileOnDisk(oldImageUrl);
                }
            });
        } else {
            deleteBannerFileOnDisk(oldImageUrl);
        }
    }

    private void deleteBannerFileOnDisk(String webPath) {
        if (webPath == null || !webPath.startsWith("/uploads/banners/")) {
            return;
        }
        String filename = webPath.substring("/uploads/banners/".length());
        if (filename.isEmpty() || filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            return;
        }
        try {
            Path targetDir = Paths.get(bannersUploadDir).toAbsolutePath().normalize();
            Path filePath = targetDir.resolve(filename).normalize();
            if (filePath.startsWith(targetDir)) {
                Files.deleteIfExists(filePath);
            }
        } catch (IOException ignored) {
        }
    }
}
