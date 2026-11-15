package kz.ibrazaim.catalog.service;

import kz.ibrazaim.catalog.model.CartItem;
import kz.ibrazaim.catalog.model.Order;
import kz.ibrazaim.catalog.model.OrderProduct;
import kz.ibrazaim.catalog.model.OrderStatus;
import kz.ibrazaim.catalog.model.Product;
import kz.ibrazaim.catalog.model.ProductSize;
import kz.ibrazaim.catalog.model.User;
import kz.ibrazaim.catalog.repository.OrderProductRepository;
import kz.ibrazaim.catalog.repository.OrderRepository;
import kz.ibrazaim.catalog.repository.ProductRepository;
import kz.ibrazaim.catalog.repository.ProductSizeRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final ProductRepository productRepository;
    private final ProductSizeRepository productSizeRepository;
    private final CartService cartService;
    private final NotificationService notificationService;

    @Transactional
    public Order create(User user, String email, String phone, String fullName, String country, String city, String address, String postalCode, String deliveryMethod, String paymentMethod, List<CartItem> cartItems) {
        return create(user, email, phone, fullName, country, city, address, postalCode, deliveryMethod, paymentMethod, cartItems, BigDecimal.ZERO, null);
    }

    @Transactional
    public Order create(User user, String email, String phone, String fullName, String country, String city, String address, String postalCode, String deliveryMethod, String paymentMethod, List<CartItem> cartItems, BigDecimal extraDiscount) {
        return create(user, email, phone, fullName, country, city, address, postalCode, deliveryMethod, paymentMethod, cartItems, extraDiscount, null);
    }

    @Transactional
    public Order create(User user, String email, String phone, String fullName, String country, String city, String address, String postalCode, String deliveryMethod, String paymentMethod, List<CartItem> cartItems, BigDecimal extraDiscount, String promoCode) {
        try {
            List<CartItem> safeCartItems = cartItems == null ? List.of() : cartItems;
            if (safeCartItems.isEmpty()) {
                throw new IllegalArgumentException("Cart is empty");
            }

            // Проверяем и списываем остатки на складе (по товару и размеру)
            for (CartItem cartItem : safeCartItems) {
                Product product = cartItem.getProduct();
                if (product != null) {
                    if (product.isDeleted()) {
                        throw new IllegalArgumentException("Товар '" + product.getName() + "' больше недоступен для заказа.");
                    }
                    if (product.getStock() < cartItem.getQuantity()) {
                        throw new IllegalArgumentException("Недостаточно товара '" + product.getName() + "' на складе. В наличии: " + product.getStock() + " шт.");
                    }
                    if (cartItem.getSize() != null && !cartItem.getSize().isBlank()) {
                        ProductSize sizeEntity = productSizeRepository
                                .findFirstByProductAndSizeIgnoreCase(product, cartItem.getSize().trim())
                                .orElse(null);
                        if (sizeEntity != null) {
                            if (sizeEntity.getStock() < cartItem.getQuantity()) {
                                throw new IllegalArgumentException("Недостаточно товара '" + product.getName() + "' (размер " + sizeEntity.getSize() + ") на складе. В наличии: " + sizeEntity.getStock() + " шт.");
                            }
                            sizeEntity.setStock(sizeEntity.getStock() - cartItem.getQuantity());
                            productSizeRepository.save(sizeEntity);
                        }
                    }
                    product.setStock(product.getStock() - cartItem.getQuantity());
                    productRepository.save(product);
                }
            }

            BigDecimal itemsTotal = cartService.finalPrice(safeCartItems);
            BigDecimal safeExtraDiscount = (extraDiscount != null && extraDiscount.compareTo(BigDecimal.ZERO) > 0)
                    ? extraDiscount.min(itemsTotal)
                    : BigDecimal.ZERO;
            BigDecimal discountedItemsTotal = itemsTotal.subtract(safeExtraDiscount).max(BigDecimal.ZERO);
            BigDecimal shippingCost = "premium".equalsIgnoreCase(deliveryMethod)
                    ? BigDecimal.valueOf(5600)
                    : BigDecimal.ZERO;

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
            order.setPromoCode(promoCode != null && !promoCode.isBlank() ? promoCode.trim().toUpperCase(Locale.ROOT) : null);
            order.setDate(LocalDateTime.now());
            order.setTotalPrice(discountedItemsTotal.add(shippingCost).setScale(2, RoundingMode.HALF_UP));
            order.setSerialNumber(generateOrderSerialNumber());

            List<OrderProduct> orderProducts = new ArrayList<>();
            for (CartItem cartItem : safeCartItems) {
                OrderProduct orderProduct = new OrderProduct();
                orderProduct.setOrder(order);
                orderProduct.setProduct(cartItem.getProduct());
                orderProduct.setSize(cartItem.getSize());
                orderProduct.setUnitPrice(cartItem.getProduct().getPrice());
                orderProduct.setDiscountPercent(cartItem.getProduct().getDiscount()
                        .max(BigDecimal.ZERO)
                        .min(BigDecimal.valueOf(100))
                        .setScale(0, RoundingMode.HALF_UP)
                        .intValue());
                orderProduct.setQuantity(cartItem.getQuantity());
                orderProduct.setLineTotal(cartService.lineFinalPrice(cartItem));
                orderProducts.add(orderProduct);
            }

            order.setOrderProducts(orderProducts);
            orderRepository.save(order);
            orderProductRepository.saveAll(orderProducts);

            notificationService.notifyOrderCreated(order);
            return order;
        } catch (Exception ex) {
            logger.error("Ошибка при создании заказа: " + ex.getMessage(), ex);
            throw ex;
        }
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByDateDescIdDesc();
    }

    public List<Order> getOrdersForUser(User user) {
        return orderRepository.findByUserOrderByDateDescIdDesc(user);
    }

    public Order getOrderById(long id) {
        return orderRepository.getOrderById(id);
    }

    @Transactional
    public void updateOrderStatus(Long orderId, String status) {
        updateOrderStatus(orderId, status, null);
    }

    @Transactional
    public void updateOrderStatus(Long orderId, String status, String trackingNumber) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
        OrderStatus newStatus = OrderStatus.fromValue(status);
        OrderStatus oldStatus = order.getStatus();

        // Возврат остатков на склад при отмене заказа
        if (oldStatus != OrderStatus.CANCELED && newStatus == OrderStatus.CANCELED) {
            if (order.getOrderProducts() != null) {
                for (OrderProduct op : order.getOrderProducts()) {
                    if (op.getProduct() != null) {
                        Product p = op.getProduct();
                        p.setStock(p.getStock() + op.getQuantity());
                        productRepository.save(p);
                        adjustSizeStock(p, op.getSize(), op.getQuantity());
                    }
                }
            }
        }
        // Повторное списание со склада, если заказ восстанавливают из отмененного
        else if (oldStatus == OrderStatus.CANCELED && newStatus != OrderStatus.CANCELED) {
            if (order.getOrderProducts() != null) {
                for (OrderProduct op : order.getOrderProducts()) {
                    if (op.getProduct() != null) {
                        Product p = op.getProduct();
                        p.setStock(Math.max(0, p.getStock() - op.getQuantity()));
                        productRepository.save(p);
                        adjustSizeStock(p, op.getSize(), -op.getQuantity());
                    }
                }
            }
        }

        order.setStatus(newStatus);
        if (trackingNumber != null) {
            String cleanTracking = trackingNumber.trim();
            order.setTrackingNumber(cleanTracking.isEmpty() ? null : cleanTracking);
        }
        orderRepository.save(order);
        notificationService.notifyOrderStatusChanged(order);
    }

    private void adjustSizeStock(Product product, String size, int delta) {
        if (product == null || size == null || size.isBlank()) {
            return;
        }
        productSizeRepository.findFirstByProductAndSizeIgnoreCase(product, size.trim())
                .ifPresent(ps -> {
                    ps.setStock(Math.max(0, ps.getStock() + delta));
                    productSizeRepository.save(ps);
                });
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
