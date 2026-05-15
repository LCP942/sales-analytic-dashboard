package com.lp.salesdashboard.seeder;

import com.lp.salesdashboard.entity.Customer;
import com.lp.salesdashboard.entity.Product;
import com.lp.salesdashboard.repository.CustomerRepository;
import com.lp.salesdashboard.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
@Order(1)
@RequiredArgsConstructor
public class CatalogSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CatalogSeeder.class);

    private final CustomerRepository customerRepository;
    private final ProductRepository  productRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedCustomers();
        seedProducts();
    }

    private void seedCustomers() {
        if (customerRepository.count() > 0) return;
        log.info("Seeding catalog customers...");
        customerRepository.saveAll(List.of(
            customer("Alice Martin",      "alice.martin@mail.fr",      "Paris"),
            customer("Bob Dupont",        "bob.dupont@mail.fr",        "Lyon"),
            customer("Claire Bernard",    "claire.bernard@mail.fr",    "Marseille"),
            customer("David Moreau",      "david.moreau@mail.fr",      "Bordeaux"),
            customer("Emma Petit",        "emma.petit@mail.fr",        "Toulouse"),
            customer("François Leroy",    "francois.leroy@mail.fr",    "Nantes"),
            customer("Gabrielle Simon",   "gabrielle.simon@mail.fr",   "Strasbourg"),
            customer("Hugo Laurent",      "hugo.laurent@mail.fr",      "Lille"),
            customer("Inès Thomas",       "ines.thomas@mail.fr",       "Rennes"),
            customer("Julien Roux",       "julien.roux@mail.fr",       "Nice"),
            customer("Karine Blanc",      "karine.blanc@mail.fr",      "Montpellier"),
            customer("Louis Garnier",     "louis.garnier@mail.fr",     "Paris"),
            customer("Marie Faure",       "marie.faure@mail.fr",       "Lyon"),
            customer("Nicolas Rousseau",  "nicolas.rousseau@mail.fr",  "Grenoble"),
            customer("Océane Mercier",    "oceane.mercier@mail.fr",    "Bordeaux"),
            customer("Paul Girard",       "paul.girard@mail.fr",       "Toulouse"),
            customer("Quentin Bonnet",    "quentin.bonnet@mail.fr",    "Nantes"),
            customer("Rachel Aubert",     "rachel.aubert@mail.fr",     "Paris"),
            customer("Sébastien Morel",   "sebastien.morel@mail.fr",   "Lyon"),
            customer("Theo Perrin",       "theo.perrin@mail.fr",       "Marseille")
        ));
    }

    private void seedProducts() {
        if (productRepository.count() > 0) return;
        log.info("Seeding catalog products...");
        productRepository.saveAll(List.of(
            product("Laptop Pro 15\"",      "Electronics", "1299.99"),
            product("Smartphone X12",       "Electronics",  "799.99"),
            product("Wireless Headphones",  "Electronics",  "149.99"),
            product("Mechanical Keyboard",  "Electronics",   "89.99"),
            product("Running Shoes",        "Clothing",      "119.99"),
            product("Winter Jacket",        "Clothing",      "189.99"),
            product("Yoga Pants",           "Clothing",       "59.99"),
            product("Cotton T-Shirt",       "Clothing",       "24.99"),
            product("Clean Code (book)",    "Books",          "34.99"),
            product("Design Patterns",      "Books",          "39.99"),
            product("Coffee Maker",         "Home",           "79.99"),
            product("Desk Lamp LED",        "Home",           "44.99")
        ));
    }

    private static Customer customer(String name, String email, String city) {
        Customer c = new Customer();
        c.setName(name);
        c.setEmail(email);
        c.setCity(city);
        return c;
    }

    private static Product product(String name, String category, String price) {
        Product p = new Product();
        p.setName(name);
        p.setCategory(category);
        p.setPrice(new BigDecimal(price));
        p.setUserCreated(false);
        return p;
    }
}
