package kz.ibrazaim.catalog.controller;

import kz.ibrazaim.catalog.model.CartItem;
import kz.ibrazaim.catalog.repository.CartItemRepository;
import kz.ibrazaim.catalog.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;


@Controller
@RequiredArgsConstructor
@RequestMapping("/cart")
public class CartController {
    private final CartService cartService;
    private final CartItemRepository cartItemRepository;

    @PostMapping("/delete")
    public String deleteItem(@RequestParam("id") Long itemId) {
        cartService.removeCartItem(itemId);
        return "redirect:/cart";
    }

    @PostMapping("/update/{cartItemId}")
    public String updateCartItem(
            @PathVariable Long cartItemId,
            @RequestParam("quantity") int quantity
    ) {
        CartItem cartItem = cartItemRepository.findById(cartItemId).orElseThrow();
        cartItem.setQuantity(quantity);
        cartItemRepository.save(cartItem);

        return "redirect:/cart";
    }
    //  TODO: добавить функцию удаления всех товаров
}

