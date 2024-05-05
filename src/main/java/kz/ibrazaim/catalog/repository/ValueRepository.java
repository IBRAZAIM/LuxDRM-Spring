package kz.ibrazaim.catalog.repository;

import kz.ibrazaim.catalog.model.Value;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface ValueRepository extends JpaRepository<Value, Long> {
    @Transactional
    void  deleteAllByProductId(long productId);
}
