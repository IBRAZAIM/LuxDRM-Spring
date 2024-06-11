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
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    private final OrderRepository orderRepository;
    private final OrderProductRepository orderProductRepository;

    @Transactional
    public Order createOrder(User user, String address , List<OrderProduct> orderProducts, List<CartItem> cartItems) {
        try {
            Order order = new Order();
            order.setUser(user);
            order.setStatus("Оформлено");
            order.setAddress(address);
            order.setOrderDate(LocalDateTime.now());
            for (int i = 0; i < orderProducts.size(); i++) {
                OrderProduct orderProduct = new OrderProduct();
                CartItem cartItem = cartItems.get(i);

                orderProduct.setOrder(order);
                orderProduct.setProduct(cartItem.getProduct());
                orderProduct.setQuantity(cartItem.getQuantity());
                //Сохраняем orderProduct в базе данных
                orderProductRepository.save(orderProduct);
            }
            order.setOrderProducts(orderProducts);
            //Сохраняем order в базе данных
            orderRepository.save(order);
            return order;
        } catch (Exception e) {
            // Запись информации об ошибке в логи
            logger.error("Ошибка при создании заказа: " + e.getMessage(), e);
            throw e; // Пробрасываем исключение выше для обработки
        }
    }
}


