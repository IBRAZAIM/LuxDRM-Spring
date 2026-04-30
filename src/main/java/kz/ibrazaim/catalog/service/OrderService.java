package kz.ibrazaim.catalog.service;

import kz.ibrazaim.catalog.model.CartItem;
import kz.ibrazaim.catalog.model.Order;
import kz.ibrazaim.catalog.model.OrderProduct;
import kz.ibrazaim.catalog.model.OrderStatus;
import kz.ibrazaim.catalog.model.User;
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
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {
    public record BucketStats(long orders, double sales) {}

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OrderProductRepository orderProductRepository;
    private final CartService cartService;

    @Transactional
    public Order create(User user, String email, String phone, String fullName, String country, String city, String address, String postalCode, String deliveryMethod, String paymentMethod, List<CartItem> cartItems) {
        try {
            List<CartItem> safeCartItems = cartItems == null ? List.of() : cartItems;
            if (safeCartItems.isEmpty()) {
                throw new IllegalArgumentException("Cart is empty");
            }

            Order order = new Order();
            order.setUser(user);
            order.setStatus(OrderStatus.PENDING);
            order.setEmail(email);
            order.setPhone(phone);
            order.setFullName(fullName);
            order.setCountry(country);
            order.setCity(city);
            order.setAddress(address);
            order.setPostalCode(postalCode);
            order.setDeliveryMethod(deliveryMethod);
            order.setPaymentMethod(paymentMethod);
            order.setDate(LocalDateTime.now());
            order.setTotalPrice(cartService.finalPrice(safeCartItems));
            order.setSerialNumber(generateOrderSerialNumber());

            List<OrderProduct> orderProducts = new ArrayList<>();
            for (CartItem cartItem : safeCartItems) {
                OrderProduct orderProduct = new OrderProduct();
                orderProduct.setOrder(order);
                orderProduct.setProduct(cartItem.getProduct());
                orderProduct.setSize(cartItem.getSize());
                orderProduct.setUnitPrice(cartItem.getProduct().getPrice());
                orderProduct.setDiscountPercent(Math.max(0, Math.min(cartItem.getProduct().getDiscount(), 100)));
                orderProduct.setQuantity(cartItem.getQuantity());
                orderProduct.setLineTotal(cartService.lineFinalPrice(cartItem));
                orderProducts.add(orderProduct);
            }

            order.setOrderProducts(orderProducts);
            orderRepository.save(order);
            orderProductRepository.saveAll(orderProducts);

            return order;
        } catch (Exception ex) {
            logger.error("Ошибка при создании заказа: " + ex.getMessage(), ex);
            throw ex;
        }
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public List<Order> getOrdersForUser(User user) {
        return orderRepository.findByUser(user);
    }

    public Order getOrderById(long id) {
        return orderRepository.getOrderById(id);
    }

    public void updateOrderStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
        order.setStatus(OrderStatus.fromValue(status));
        orderRepository.save(order);
    }

    private String generateOrderSerialNumber() {
        return "ORD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
    }

    public Long getOrdersCount(LocalDateTime start, LocalDateTime end) {
        return orderRepository.countOrdersInPeriod(start, end);
    }

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
