package com.nfcplatform.product.controller;

import com.nfcplatform.common.response.ApiResponse;
import com.nfcplatform.product.dto.ProductRequest;
import com.nfcplatform.product.dto.ProductResponse;
import com.nfcplatform.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminProductController {

    private final ProductService productService;

    @GetMapping
    public ApiResponse<List<ProductResponse>> list() {
        return ApiResponse.ok(productService.listAllForAdmin());
    }

    @PostMapping
    public ApiResponse<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        return ApiResponse.ok(productService.create(request), "Product created");
    }

    @PutMapping("/{uuid}")
    public ApiResponse<ProductResponse> update(@PathVariable String uuid, @Valid @RequestBody ProductRequest request) {
        return ApiResponse.ok(productService.update(uuid, request), "Product updated");
    }
}
