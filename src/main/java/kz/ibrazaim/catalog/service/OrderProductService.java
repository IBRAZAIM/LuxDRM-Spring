package kz.ibrazaim.catalog.service;

import kz.ibrazaim.catalog.model.OrderProduct;
import kz.ibrazaim.catalog.repository.OrderProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderProductService {
    private final OrderProductRepository orderProductRepository;

    public List<OrderProduct> findAllOrderProduct(){
        return orderProductRepository.findAll();
    }
}
