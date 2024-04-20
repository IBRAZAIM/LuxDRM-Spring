package kz.ibrazaim.catalog.repository;

import kz.ibrazaim.catalog.model.Value;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ValueRepository extends JpaRepository<Value, Long> {
}
