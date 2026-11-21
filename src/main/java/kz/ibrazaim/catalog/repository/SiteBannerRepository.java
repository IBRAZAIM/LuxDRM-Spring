package kz.ibrazaim.catalog.repository;

import kz.ibrazaim.catalog.model.SiteBanner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SiteBannerRepository extends JpaRepository<SiteBanner, String> {
    List<SiteBanner> findAllByOrderBySortOrderAscSlotKeyAsc();
}
