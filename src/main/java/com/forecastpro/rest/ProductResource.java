package com.forecastpro.rest;

import com.forecastpro.entity.Product;
import com.forecastpro.repository.ProductRepository;
import jakarta.ejb.EJB;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/products")
public class ProductResource {

    @EJB
    private ProductRepository productRepository;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map<String, Object>> listAll() {
        return productRepository.findAllOrdered().stream()
                .map(this::toMap)
                .collect(Collectors.toList());
    }

    private Map<String, Object> toMap(Product p) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", p.getId());
        m.put("name", p.getName());
        m.put("categoryId", p.getCategory().getId());
        m.put("categoryName", p.getCategory().getName());
        m.put("price", p.getPrice());
        m.put("stockQuantity", p.getStockQuantity());
        return m;
    }
}
