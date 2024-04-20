package kz.ibrazaim.catalog.repository;

import kz.ibrazaim.catalog.model.Option;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OptionRepository extends JpaRepository<Option, Long> {
    List<Option> findAllByCategoryId(long categoryId);
}
