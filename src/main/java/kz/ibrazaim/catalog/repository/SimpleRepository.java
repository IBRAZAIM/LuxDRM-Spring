package kz.ibrazaim.catalog.repository;

import java.util.List;

public interface SimpleRepository<T>{
    List<T> findAll();
    void create(T t);
}
