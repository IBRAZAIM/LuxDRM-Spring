package kz.ibrazaim.catalog.repository;

import kz.ibrazaim.catalog.model.Option;
import kz.ibrazaim.catalog.model.Product;
import kz.ibrazaim.catalog.model.Value;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ValueRepository extends JpaRepository<Value, Long> {
    @Transactional
    void deleteAllByProductId(long productId);

    @Transactional(readOnly = true)
    Optional<Value> findByProductAndOption(Product product, Option option);

    @EntityGraph(attributePaths = {"option"})
    List<Value> findByProduct(Product product);
}
