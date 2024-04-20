package kz.ibrazaim.catalog.repository;


import kz.ibrazaim.catalog.model.Category;
import kz.ibrazaim.catalog.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public interface ProductRepository extends JpaRepository<Product, Long> {
}

