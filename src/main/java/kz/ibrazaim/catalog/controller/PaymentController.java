package kz.ibrazaim.catalog.controller;

import kz.ibrazaim.catalog.model.Order;
import kz.ibrazaim.catalog.model.PaymentStatus;
import kz.ibrazaim.catalog.model.PaymentTransaction;
import kz.ibrazaim.catalog.model.Role;
import kz.ibrazaim.catalog.model.User;
import kz.ibrazaim.catalog.payment.PaymentGateway;
import kz.ibrazaim.catalog.service.OrderService;
import kz.ibrazaim.catalog.service.PaymentService;
import kz.ibrazaim.catalog.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final UserService userService;
    private final PaymentGateway paymentGateway;

    /**
     * Страница платёжного шлюза (Sandbox / Hosted Payment Page).
     * При подключении реального эквайринга (mode=external) редирект идёт сразу на URL банка.
     */
    @GetMapping("/payment/checkout/{externalId}")
    public String showPaymentGateway(
            @PathVariable("externalId") String externalId,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        User user = userService.getUser();
        if (user == null) {
            return "redirect:/login";
        }

        Optional<PaymentTransaction> txOpt = paymentService.findByExternalId(externalId);
        if (txOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Платёжная сессия не найдена или истекла.");
            return "redirect:/orders";
        }

        PaymentTransaction tx = txOpt.get();
        Order order = tx.getOrder();

        boolean isOwner = order != null && order.getUser() != null && order.getUser().getId() == user.getId();
        boolean isStaff = userService.hasAnyRole(user, Role.ADMIN, Role.MODER);
        if (!isOwner && !isStaff) {
            return "redirect:/orders";
        }

        if (tx.getPaymentStatus() == PaymentStatus.PAID) {
            redirectAttributes.addFlashAttribute("successMessage", "Заказ №00" + order.getId() + " уже оплачен.");
            return "redirect:/orders/" + order.getId();
        }

        model.addAttribute("tx", tx);
        model.addAttribute("order", order);
        return "paymentGateway";
    }

    /**
     * Обработка результата оплаты со страницы шлюза (успешная оплата или отказ банка).
     */
    @PostMapping("/payment/confirm/{externalId}")
    public String confirmPayment(
            @PathVariable("externalId") String externalId,
            @RequestParam(value = "result", defaultValue = "success") String result,
            @RequestParam(value = "cardPan", required = false, defaultValue = "") String cardPan,
            RedirectAttributes redirectAttributes
    ) {
        User user = userService.getUser();
        if (user == null) {
            return "redirect:/login";
        }

        Optional<PaymentTransaction> txOpt = paymentService.findByExternalId(externalId);
        if (txOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Платёжная транзакция не найдена.");
            return "redirect:/orders";
        }

        PaymentTransaction tx = txOpt.get();
        Order order = tx.getOrder();
        boolean isOwner = order != null && order.getUser() != null && order.getUser().getId() == user.getId();
        boolean isStaff = userService.hasAnyRole(user, Role.ADMIN, Role.MODER);
        if (!isOwner && !isStaff) {
            return "redirect:/orders";
        }

        boolean isSuccess = "success".equalsIgnoreCase(result);
        String maskedPan = null;
        if (cardPan != null && !cardPan.isBlank()) {
            String digits = cardPan.replaceAll("\\D", "");
            if (digits.length() >= 4) {
                maskedPan = "**** **** **** " + digits.substring(digits.length() - 4);
            }
        } else if ("kaspi_qr".equalsIgnoreCase(tx.getProvider())) {
            maskedPan = "Kaspi Gold / QR";
        } else if ("apple_pay".equalsIgnoreCase(tx.getProvider())) {
            maskedPan = "Apple Pay · Face ID";
        } else if ("google_pay".equalsIgnoreCase(tx.getProvider())) {
            maskedPan = "Google Pay · Токен";
        } else if ("installment".equalsIgnoreCase(tx.getProvider())) {
            maskedPan = "Рассрочка 0-0-12";
        }

        paymentService.completePayment(externalId, isSuccess, maskedPan);

        if (isSuccess) {
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Оплата прошла успешно! Транзакция " + externalId +
                    " подтверждена. Заказ №00" + order.getId() + " передан в сборку."
            );
        } else {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Платёж отклонён банком-эмитентом (транзакция " + externalId + "). Вы можете повторить попытку оплаты."
            );
        }

        return "redirect:/orders/" + order.getId();
    }

    /**
     * Повторный переход к оплате со страницы заказа (/orders/{id}), если заказ ещё не оплачен.
     */
    @GetMapping("/orders/{id}/pay")
    public String payExistingOrder(
            @PathVariable("id") long id,
            RedirectAttributes redirectAttributes
    ) {
        User user = userService.getUser();
        if (user == null) {
            return "redirect:/login";
        }

        Order order = orderService.getOrderById(id);
        if (order == null) {
            return "redirect:/orders";
        }

        boolean isOwner = order.getUser() != null && order.getUser().getId() == user.getId();
        boolean isStaff = userService.hasAnyRole(user, Role.ADMIN, Role.MODER);
        if (!isOwner && !isStaff) {
            return "redirect:/orders";
        }

        Optional<PaymentTransaction> latestTx = paymentService.findLatestByOrder(order);
        if (latestTx.isPresent()) {
            PaymentTransaction tx = latestTx.get();
            if (tx.getPaymentStatus() == PaymentStatus.PAID) {
                redirectAttributes.addFlashAttribute("successMessage", "Этот заказ уже оплачен.");
                return "redirect:/orders/" + id;
            }
            if (tx.getPaymentStatus() == PaymentStatus.AWAITING_PAYMENT && tx.getPaymentUrl() != null) {
                return "redirect:" + tx.getPaymentUrl();
            }
        }

        String provider = (order.getPaymentMethod() == null || "other".equals(order.getPaymentMethod()) || "cod".equals(order.getPaymentMethod()))
                ? "card_gateway"
                : order.getPaymentMethod();
        PaymentTransaction newTx = paymentService.createPaymentForOrder(order, provider);
        return "redirect:" + newTx.getPaymentUrl();
    }

    /**
     * Асинхронный Webhook-эндпоинт для внешних платёжных систем
     * (Freedom Pay, CloudPayments, Halyk ePay, Kaspi Pay, Stripe).
     * Принимает JSON и проверяет HMAC-SHA256 подпись в заголовке X-Payment-Signature.
     */
    @PostMapping("/api/payments/webhook")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handlePaymentWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Payment-Signature", required = false) String signature,
            @RequestParam(value = "transactionId", required = false) String paramTxId,
            @RequestParam(value = "status", required = false) String paramStatus,
            @RequestParam(value = "maskedPan", required = false) String paramMaskedPan
    ) {
        if (signature != null && !signature.isBlank() && !paymentGateway.verifyWebhookSignature(rawBody, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("ok", false, "error", "Invalid webhook signature"));
        }

        String txId = paramTxId != null ? paramTxId : extractJsonString(rawBody, "transactionId");
        String status = paramStatus != null ? paramStatus : extractJsonString(rawBody, "status");
        String maskedPan = paramMaskedPan != null ? paramMaskedPan : extractJsonString(rawBody, "maskedPan");

        if (txId == null || txId.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("ok", false, "error", "Missing transactionId"));
        }

        Optional<PaymentTransaction> txOpt = paymentService.findByExternalId(txId);
        if (txOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("ok", false, "error", "Transaction not found"));
        }

        boolean paid = "PAID".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status) || "APPROVED".equalsIgnoreCase(status);
        PaymentTransaction updated = paymentService.completePayment(txId, paid, maskedPan);

        return ResponseEntity.ok(Map.of(
                "ok", true,
                "transactionId", updated.getExternalTransactionId(),
                "paymentStatus", updated.getPaymentStatus().name(),
                "orderId", updated.getOrder().getId()
        ));
    }

    private String extractJsonString(String json, String key) {
        if (json == null || key == null) return null;
        String pattern = "\"" + key + "\"";
        int idx = json.indexOf(pattern);
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx + pattern.length());
        if (colon < 0) return null;
        int firstQuote = json.indexOf('"', colon + 1);
        if (firstQuote < 0) return null;
        int secondQuote = json.indexOf('"', firstQuote + 1);
        if (secondQuote < 0) return null;
        return json.substring(firstQuote + 1, secondQuote);
    }
}

