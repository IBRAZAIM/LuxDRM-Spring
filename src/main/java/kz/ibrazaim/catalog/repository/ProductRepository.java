package kz.ibrazaim.catalog.repository;


import kz.ibrazaim.catalog.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;


@Component
public interface ProductRepository extends JpaRepository<Product, Long> {
}

