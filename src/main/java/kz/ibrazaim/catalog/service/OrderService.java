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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class OrderService {
    public record BucketStats(long orders, double sales) {}

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    private final OrderRepository orderRepository;
    private final OrderProductRepository orderProductRepository;

    @Transactional
    public Order create(User user, String email, String phone, String fullName, String country, String city, String address, String postalCode, List<CartItem> cartItems, int totalPrice) {
        try {
            Order order = new Order();
            order.setUser(user);
            order.setStatus("В ожидании");
            order.setEmail(email);
            order.setPhone(phone);
            order.setFullName(fullName);
            order.setCountry(country);
            order.setCity(city);
            order.setAddress(address);
            order.setPostalCode(postalCode);
            order.setDate(LocalDateTime.now());
            order.setTotalPrice(totalPrice);
            // Генерация уникального серийного номера для заказа
            String serialNumber = generateOrderSerialNumber(user.getId());
            order.setSerialNumber(serialNumber);  // Устанавливаем серийный номер в заказ

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
        } catch (Exception ex) {
            logger.error("Ошибка при создании заказа: " + ex.getMessage(), ex);
            throw ex;
        }
    }

    public List<Order> getAllOrders() {
        List<Order> orders = orderRepository.findAll();
        calculateTotalPriceForOrders(orders);
        return orders;
    }

    public List<Order> getOrdersForUser(User user) {
        List<Order> orders = orderRepository.findByUser(user);
        calculateTotalPriceForOrders(orders);
        return orderRepository.findByUser(user);
    }

    public Order getOrderById(long id){
        return orderRepository.getOrderById(id);
    }

    public void updateOrderStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
        order.setStatus(status);
        orderRepository.save(order);
    }

    // Метод для создания уникального числового серийного номера
    private String generateOrderSerialNumber(long userId) {
        // Генерация случайного числа для повышения уникальности
        Random random = new Random();
        int randomSuffix = random.nextInt(100000);  // Генерация случайного числа от 0 до 99999

        // Создаем числовой серийный номер, комбинируя ID пользователя и случайное число
        return String.format("%d%05d", userId, randomSuffix);  // Форматируем как <userId><randomSuffix>
    }

    // Метод для расчета общей стоимости
    // Метод для расчета общей стоимости для каждого заказа
    private void calculateTotalPriceForOrders(List<Order> orders) {
        for (Order order : orders) {
            int totalPrice = 0;
            for (OrderProduct orderProduct : order.getOrderProducts()) {
                totalPrice += orderProduct.getProduct().getPrice() * orderProduct.getQuantity();
            }
            order.setTotalPrice(totalPrice); // Устанавливаем рассчитанную стоимость
        }
    }

    // Количество заказов за период
    public Long getOrdersCount(LocalDateTime start, LocalDateTime end) {
        return orderRepository.countOrdersInPeriod(start, end);
    }

    // Сумма всех заказов за период
    public Double getTotalSales(LocalDateTime start, LocalDateTime end) {
        Double total = orderRepository.sumTotalInPeriod(start, end);
        return total != null ? total : 0.0;
    }

    public Map<String, BucketStats> aggregateByPeriod(LocalDateTime start, LocalDateTime end, VisitService.Aggregation aggregation) {
        List<Object[]> rows = switch (aggregation) {
            case DAY -> orderRepository.aggregateByDay(start, end);
            case WEEK -> orderRepository.aggregateByWeek(start, end);
            case MONTH -> orderRepository.aggregateByMonth(start, end);
        };

        Map<String, BucketStats> stats = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String label = row[0].toString();
            long orders = ((Number) row[1]).longValue();
            double sales = ((Number) row[2]).doubleValue();
            stats.put(label, new BucketStats(orders, sales));
        }
        return stats;
    }
}



