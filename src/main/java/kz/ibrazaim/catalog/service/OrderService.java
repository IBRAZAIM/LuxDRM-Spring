package kz.ibrazaim.catalog.service;

import kz.ibrazaim.catalog.model.*;
import kz.ibrazaim.catalog.repository.OrderProductRepository;
import kz.ibrazaim.catalog.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    private final OrderRepository orderRepository;
    private final OrderProductRepository orderProductRepository;

    @Transactional
    public Order create(User user, String address, List<CartItem> cartItems) {
        try {
            Order order = new Order();
            order.setUser(user);
            order.setStatus("Оформлено");
            order.setAddress(address);
            order.setDate(LocalDateTime.now());

            List<OrderProduct> orderProducts = new ArrayList<>();
            for (CartItem cartItem : cartItems) {
                OrderProduct orderProduct = new OrderProduct();
                orderProduct.setOrder(order);
                orderProduct.setProduct(cartItem.getProduct());
                orderProduct.setQuantity(cartItem.getQuantity());
                orderProducts.add(orderProduct);
            }

            order.setOrderProducts(orderProducts);
            orderRepository.save(order); // Сохраняем заказ в базе данных

            // Сохраняем все позиции заказа в базе данных после сохранения заказа
            orderProductRepository.saveAll(orderProducts);

            return order;
        } catch (Exception e) {
            logger.error("Ошибка при создании заказа: " + e.getMessage(), e);
            throw e;
        }
    }
}



