package com.delight.account.repository;

import com.delight.account.model.Product;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByCode(String code);
}

