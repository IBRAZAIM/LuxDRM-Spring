package kz.ibrazaim.catalog.repository;

import kz.ibrazaim.catalog.model.Category;

import java.util.List;

public interface SimpleRepository<T> {
    void create(T noun);
    T findById(long id);
    List<T> findAll();
}
