package kz.ibrazaim.catalog.model;

import java.util.Locale;

public enum ProductAudience {
    MEN("men", "Мужское", "Мужская коллекция"),
    WOMEN("women", "Женское", "Женская коллекция"),
    KIDS("kids", "Детское", "Детская коллекция"),
    UNISEX("unisex", "Унисекс", "Унисекс");

    private final String slug;
    private final String displayName;
    private final String collectionTitle;

    ProductAudience(String slug, String displayName, String collectionTitle) {
        this.slug = slug;
        this.displayName = displayName;
        this.collectionTitle = collectionTitle;
    }

    public String getSlug() {
        return slug;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCollectionTitle() {
        return collectionTitle;
    }

    public static ProductAudience fromSlugOrName(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String clean = raw.trim().toLowerCase(Locale.ROOT);
        return switch (clean) {
            case "men", "man", "male", "мужское", "мужская" -> MEN;
            case "women", "woman", "female", "женское", "женская" -> WOMEN;
            case "kids", "kid", "child", "children", "детское", "детская" -> KIDS;
            case "unisex", "унисекс" -> UNISEX;
            default -> null;
        };
    }
}
