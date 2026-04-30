package kz.ibrazaim.catalog.controller;

import kz.ibrazaim.catalog.model.Product;
import kz.ibrazaim.catalog.service.ProductService;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class PublicSeoController {
    private final ProductService productService;

    @GetMapping("/delivery")
    public String delivery() {
        return "delivery";
    }

    @GetMapping("/contact")
    public String contact() {
        return "contact";
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String robots(HttpServletRequest request) {
        String baseUrl = baseUrl(request);
        return String.join("\n",
                "User-agent: *",
                "Allow: /",
                "",
                "Disallow: /cart",
                "Disallow: /checkout",
                "Disallow: /login",
                "Disallow: /register",
                "Disallow: /orders",
                "Disallow: /sales",
                "Disallow: /reviews",
                "Disallow: /products/create",
                "Disallow: /products/update",
                "Disallow: /products/delete",
                "Disallow: /categories/create",
                "",
                "Sitemap: " + baseUrl + "/sitemap.xml",
                ""
        );
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public String sitemap(HttpServletRequest request) {
        String baseUrl = baseUrl(request);
        String lastMod = LocalDate.now().toString();
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        appendUrl(xml, baseUrl + "/", lastMod);
        appendUrl(xml, baseUrl + "/products", lastMod);
        appendUrl(xml, baseUrl + "/categories", lastMod);
        appendUrl(xml, baseUrl + "/delivery", lastMod);
        appendUrl(xml, baseUrl + "/contact", lastMod);

        List<Product> products = productService.findAll();
        for (Product product : products) {
            appendUrl(xml, baseUrl + "/products/" + product.getId(), lastMod);
        }

        xml.append("</urlset>");
        return xml.toString();
    }

    private static void appendUrl(StringBuilder xml, String url, String lastMod) {
        xml.append("  <url>\n");
        xml.append("    <loc>").append(url).append("</loc>\n");
        xml.append("    <lastmod>").append(lastMod).append("</lastmod>\n");
        xml.append("  </url>\n");
    }

    private static String baseUrl(HttpServletRequest request) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .build()
                .toUriString();
    }
}
