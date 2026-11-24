package com.ucak.tprest.services;

import com.ucak.tprest.dto.request.ProductRequest;
import com.ucak.tprest.dto.response.ProductResponse;
import com.ucak.tprest.entities.Product;
import com.ucak.tprest.exceptions.ResourceNotFoundException;
import com.ucak.tprest.repositories.ProductRepository;
import com.ucak.tprest.validation.DataValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final DataValidator validator;

    public ProductService(ProductRepository productRepository, DataValidator validator) {
        this.productRepository = productRepository;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(p -> new ProductResponse(p.getId(), p.getName(), p.getDescription(), p.getPrice()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable avec l'identifiant " + id));
        return new ProductResponse(product.getId(), product.getName(), product.getDescription(), product.getPrice());
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Map<String, String> errors = new HashMap<>();
        String cleanName = validator.validateAndNormalizeName(request.getName(), "name", errors);
        String cleanDesc = validator.validateDescription(request.getDescription(), errors);
        validator.validatePrice(request.getPrice(), errors);
        validator.throwIfErrors(errors);

        Product product = new Product(cleanName, cleanDesc, request.getPrice());
        Product saved = productRepository.save(product);

        return new ProductResponse(saved.getId(), saved.getName(), saved.getDescription(), saved.getPrice());
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable avec l'identifiant " + id));

        Map<String, String> errors = new HashMap<>();
        String cleanName = validator.validateAndNormalizeName(request.getName(), "name", errors);
        String cleanDesc = validator.validateDescription(request.getDescription(), errors);
        validator.validatePrice(request.getPrice(), errors);
        validator.throwIfErrors(errors);

        product.setName(cleanName);
        product.setDescription(cleanDesc);
        product.setPrice(request.getPrice());

        Product updated = productRepository.save(product);
        return new ProductResponse(updated.getId(), updated.getName(), updated.getDescription(), updated.getPrice());
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable avec l'identifiant " + id));
        productRepository.delete(product);
    }
}
