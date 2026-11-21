package kz.ibrazaim.catalog.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "site_banners")
public class SiteBanner {

    @Id
    @Column(name = "slot_key", length = 64)
    String slotKey;

    @Column(name = "title", nullable = false, length = 160)
    String title;

    @Column(name = "subtitle", length = 255)
    String subtitle;

    @Column(name = "target_url", nullable = false, length = 255)
    String targetUrl;

    @Column(name = "image_url", length = 512)
    String imageUrl;

    @Column(name = "sort_order", nullable = false)
    int sortOrder = 0;

    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt = LocalDateTime.now();

    public boolean hasImage() {
        return imageUrl != null && !imageUrl.trim().isEmpty();
    }
}
