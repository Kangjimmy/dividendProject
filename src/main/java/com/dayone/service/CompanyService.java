package com.dayone.service;

import com.dayone.exception.impl.NoCompanyException;
import com.dayone.model.Company;
import com.dayone.model.ScrapedResult;
import com.dayone.persist.entity.CompanyEntity;
import com.dayone.persist.entity.CompanyRepository;
import com.dayone.persist.entity.DividendEntity;
import com.dayone.persist.entity.DividendRepository;
import com.dayone.scraper.Scraper;
import lombok.AllArgsConstructor;
import org.apache.commons.collections4.Trie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CompanyService {

    private final Trie trie;
    private final Scraper yahooFinanceScraper;
    private final CompanyRepository companyRepository;
    private final DividendRepository dividendRepository;

    public Company save(String ticker) {

        if (companyRepository.existsByTicker(ticker)) {
            throw new RuntimeException("already exists ticker -> " + ticker);
        }
        return storeCompanyAndDividend(ticker);
    }
    private Company storeCompanyAndDividend(String ticker) {
        //ticker 를 기준으로 회사를 스크래핑
        Company company = yahooFinanceScraper.scrapCompanyByTicker(ticker);

        if (ObjectUtils.isEmpty(company)) {
            throw new RuntimeException("failed to scrap ticker -> " + ticker);
        }

        ScrapedResult scrapedResult = yahooFinanceScraper.scrap(company);

        CompanyEntity companyEntity = companyRepository.save(new CompanyEntity(company));
        List<DividendEntity> dividendEntityList = scrapedResult.getDividends().stream()
                .map(e -> new DividendEntity(companyEntity.getId(), e))
                .collect(Collectors.toList());

        this.dividendRepository.saveAll(dividendEntityList);

        return company;
    }

    public List<String> getCompanyNamesByKeyword(String keyword) {
        Pageable limit = PageRequest.of(0, 10);
        Page<CompanyEntity> companyEntities = companyRepository.findByNameStartingWithIgnoreCase(keyword, limit);

        return companyEntities
                .stream()
                .map((m)-> m.getName()).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<CompanyEntity> getAllCompany(Pageable pageable) {
        return companyRepository.findAll(pageable);
    }

    public void addAutoCompleteKeyword(String keyword) {
        trie.put(keyword, null);
    }

    public List<String> autoComplete(String keyword) {
        return (List<String>) this.trie.prefixMap(keyword).keySet()
                .stream().collect(Collectors.toList());
    }

    public void deleteAutoCompleteKeyword(String keyword) {
        trie.remove(keyword);
    }

    public String deleteCompany(String ticker) {

        CompanyEntity companyEntity = companyRepository.findByTicker(ticker)
                .orElseThrow(() -> new NoCompanyException());

        dividendRepository.deleteAllByCompanyId(companyEntity.getId());
        companyRepository.delete(companyEntity);

        deleteAutoCompleteKeyword(companyEntity.getName());

        return companyEntity.getName();
    }
}
