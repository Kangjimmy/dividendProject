package com.dayone.service;

import com.dayone.exception.impl.NoCompanyException;
import com.dayone.model.Company;
import com.dayone.model.Dividend;
import com.dayone.model.ScrapedResult;
import com.dayone.model.constants.CacheKey;
import com.dayone.persist.entity.CompanyEntity;
import com.dayone.persist.entity.CompanyRepository;
import com.dayone.persist.entity.DividendEntity;
import com.dayone.persist.entity.DividendRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class FinanceService {

    private final CompanyRepository companyRepository;
    private final DividendRepository dividendRepository;

    //요청이 자주 들어오는가?
    //자주 변경되는 데이터 인가?
    @Cacheable(key="#companyName", value= CacheKey.KEY_FINANCE)
    public ScrapedResult getDividendByCompanyName(String companyName) {
        log.info("search company -> " + companyName);
        //1. 회사명을 기준으로 회사 정보를 조회
        CompanyEntity companyEntity = companyRepository.findByName(companyName)
                .orElseThrow(() -> new NoCompanyException());
        
        //2. 조회된 회사 ID로 배당금 정보 조회
        List<DividendEntity> dividendEntityList = dividendRepository
                .findAllByCompanyId(companyEntity.getId());
        
        //3. 결과 조합 후 반환
        ScrapedResult scrapedResult = new ScrapedResult();
        List<Dividend> dividends = dividendEntityList.stream()
                .map((m) -> Dividend.builder()
                        .date(m.getDate())
                        .dividend(m.getDividend()).build()
                ).collect(Collectors.toList());

        Company company = Company.builder()
                .name(companyEntity.getName())
                .ticker(companyEntity.getTicker())
                .build();

        scrapedResult.setDividends(dividends);
        scrapedResult.setCompany(company);

        return scrapedResult;
    }
}
