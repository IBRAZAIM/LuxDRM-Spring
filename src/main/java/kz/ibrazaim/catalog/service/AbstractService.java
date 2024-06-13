package kz.ibrazaim.catalog.service;

import kz.ibrazaim.catalog.model.Category;

import java.util.List;

public interface AbstractService<T> {
    void deleteProductById(long productId);

    List<T> findAll();

    T findById(long id);

    void create(T entity);

    void deleteById(long id);

}