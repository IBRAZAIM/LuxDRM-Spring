package kz.ibrazaim.catalog.service;

import kz.ibrazaim.catalog.model.Category;

import java.util.List;

public interface AbstractService<T> {
    List<T> findAll();

    T findById(long id);

    void create(T entity);


    void update(long id, Category updatedEntity);

    void deleteById(long id);
}