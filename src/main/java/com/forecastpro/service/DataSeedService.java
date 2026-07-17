package com.forecastpro.service;

import com.forecastpro.entity.Category;
import com.forecastpro.entity.Product;
import com.forecastpro.entity.Sale;
import com.forecastpro.entity.User;
import com.forecastpro.entity.UserRole;
import com.forecastpro.repository.CategoryRepository;
import com.forecastpro.repository.ProductRepository;
import com.forecastpro.repository.SaleRepository;
import com.forecastpro.repository.UserRepository;
import com.forecastpro.util.PasswordUtil;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class DataSeedService {

    private static final Logger LOG = Logger.getLogger(DataSeedService.class.getName());

    @Inject
    private UserRepository userRepository;

    @Inject
    private CategoryRepository categoryRepository;

    @Inject
    private ProductRepository productRepository;

    @Inject
    private SaleRepository saleRepository;

    /**
     * Idempotent: seeds only when {@code users} table has zero rows.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void seedIfEmpty() {
        long userCount = userRepository.count();
        LOG.info(() -> "ForecastPro DataSeedService: user count = " + userCount);
        if (userCount > 0) {
            LOG.info("ForecastPro DataSeedService: skip (users already present).");
            return;
        }

        LOG.info("ForecastPro DataSeedService: seeding users, categories, products, sales...");

        persistUsers();
        List<Category> cats = persistCategories();
        Random rnd = new Random(42L);

        String[][] furniture = {
                {"Oak Desk", "1299.00"},
                {"Office Chair", "349.50"},
                {"Bookshelf", "199.99"},
                {"Dining Table", "599.00"}
        };
        String[][] appliances = {
                {"Refrigerator", "899.00"},
                {"Microwave", "159.00"},
                {"Washing Machine", "649.00"},
                {"Air Fryer", "129.00"}
        };
        String[][] electronics = {
                {"Laptop 15\"", "1199.00"},
                {"Tablet", "399.00"},
                {"Smartphone", "799.00"},
                {"Wireless Headphones", "149.00"}
        };

        createProductsAndSales(cats.get(0), furniture, rnd);
        createProductsAndSales(cats.get(1), appliances, rnd);
        createProductsAndSales(cats.get(2), electronics, rnd);

        LOG.info("ForecastPro DataSeedService: seeding completed successfully.");
    }

    private void persistUsers() {
        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(PasswordUtil.hash("Admin@123"));
        admin.setRole(UserRole.ADMIN);
        admin.setEnabled(true);
        userRepository.save(admin);

        User manager = new User();
        manager.setUsername("manager");
        manager.setPassword(PasswordUtil.hash("Manager@123"));
        manager.setRole(UserRole.SALES_MANAGER);
        manager.setEnabled(true);
        userRepository.save(manager);

        User employee = new User();
        employee.setUsername("employee");
        employee.setPassword(PasswordUtil.hash("Employee@123"));
        employee.setRole(UserRole.EMPLOYEE);
        employee.setEnabled(true);
        userRepository.save(employee);
    }

    private List<Category> persistCategories() {
        Category furniture = new Category();
        furniture.setName("Furniture");
        categoryRepository.save(furniture);

        Category appliances = new Category();
        appliances.setName("Home Appliances");
        categoryRepository.save(appliances);

        Category electronics = new Category();
        electronics.setName("Electronics");
        categoryRepository.save(electronics);

        List<Category> list = new ArrayList<>(3);
        list.add(furniture);
        list.add(appliances);
        list.add(electronics);
        return list;
    }

    private void createProductsAndSales(Category category, String[][] defs, Random rnd) {
        LocalDate baseMonth = LocalDate.of(2025, 10, 1);
        for (String[] def : defs) {
            int months = 3 + rnd.nextInt(4);
            List<Integer> qtyPerMonth = new ArrayList<>(months);
            int totalSold = 0;
            for (int m = 0; m < months; m++) {
                int q = 2 + rnd.nextInt(14);
                qtyPerMonth.add(q);
                totalSold += q;
            }
            int initialStock = totalSold + 100 + rnd.nextInt(150);
            Product p = new Product();
            p.setName(def[0]);
            p.setCategory(category);
            p.setPrice(new BigDecimal(def[1]));
            p.setStockQuantity(initialStock);
            productRepository.save(p);

            for (int m = 0; m < months; m++) {
                LocalDate saleDate = baseMonth.plusMonths(m).withDayOfMonth(15);
                Sale s = new Sale();
                s.setProduct(p);
                s.setQuantitySold(qtyPerMonth.get(m));
                s.setSaleDate(saleDate);
                saleRepository.save(s);
                p.setStockQuantity(p.getStockQuantity() - qtyPerMonth.get(m));
            }
            productRepository.save(p);
            if (p.getStockQuantity() < 0) {
                LOG.log(Level.SEVERE, "Stock negative for product {0}, clamping", p.getName());
                p.setStockQuantity(0);
                productRepository.save(p);
            }
        }
    }
}
