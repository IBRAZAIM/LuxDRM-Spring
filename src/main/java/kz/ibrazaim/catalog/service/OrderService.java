package kz.ibrazaim.catalog.service;

import kz.ibrazaim.catalog.model.Order;
import kz.ibrazaim.catalog.model.OrderProduct;
import kz.ibrazaim.catalog.model.Product;
import kz.ibrazaim.catalog.model.User;
import kz.ibrazaim.catalog.repository.OrderProductRepository;
import kz.ibrazaim.catalog.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderProductRepository orderProductRepository;

    @Transactional
    public Order createOrder(User user, String address, int quantity, List<Product> products) {
        Order order = new Order();
        order.setUser(user);
        order.setStatus("Оформлено");
        order.setAddress(address);
        order.setOrderDate(LocalDateTime.now());
        orderRepository.save(order);

        for (Product product : products) {
            OrderProduct orderProduct = new OrderProduct();
            orderProduct.setOrder(order);
            orderProduct.setProduct(product);
            orderProduct.setQuantity(quantity);
            orderProductRepository.save(orderProduct);
        }

        return order;
    }
}

