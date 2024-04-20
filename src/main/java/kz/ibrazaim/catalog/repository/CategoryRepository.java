package kz.ibrazaim.catalog.repository;

import kz.ibrazaim.catalog.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Category findByName(String name);
}


