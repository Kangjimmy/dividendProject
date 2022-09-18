package com.dayone.web;

import com.dayone.model.Company;
import com.dayone.persist.entity.CompanyEntity;
import com.dayone.service.CompanyService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/company")
@AllArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping("/autocomplete")
    public ResponseEntity<?> autocomplete(@RequestParam String keyword) {
        var result = companyService.getCompanyNamesByKeyword(keyword);
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<?> searchCompany(final Pageable pageable) {
        Page<CompanyEntity> companies = companyService.getAllCompany(pageable);

        return ResponseEntity.ok(companies);
    }

    @PostMapping
    public ResponseEntity<?> addCompany(@RequestBody Company request) {
        String ticker = request.getTicker().trim();
        if (ObjectUtils.isEmpty(ticker)) {
            throw new RuntimeException("ticker is empty");
        }
        Company company = companyService.save(ticker);
        companyService.addAutoCompleteKeyword(company.getName());

        return ResponseEntity.ok(company);
    }

    @DeleteMapping
    public ResponseEntity<?> deleteCompany() {
        return null;
    }
}
