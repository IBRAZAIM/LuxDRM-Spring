package kz.ibrazaim.catalog.controller;

import kz.ibrazaim.catalog.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;


@Controller
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartService cartService;


    @PostMapping("/delete")
    public String deleteItem(@RequestParam("id") Long itemId) {
        cartService.removeCartItem(itemId);
        return "redirect:/cart";
    }

//  TODO: добавить функцию удаления всех товаров
//    @PostMapping("clearAll")
//    public String clearCart(){
//        cartService.clearCart();
//        return "redirect:/cart";
//    }
}

