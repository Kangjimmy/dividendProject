package com.dayone.service;

import com.dayone.model.Company;
import com.dayone.model.ScrapedResult;
import com.dayone.persist.entity.CompanyEntity;
import com.dayone.persist.entity.CompanyRepository;
import com.dayone.persist.entity.DividendEntity;
import com.dayone.persist.entity.DividendRepository;
import com.dayone.scraper.Scraper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CompanyService {

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
}
