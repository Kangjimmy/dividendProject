package com.dayone.scheduler;

import com.dayone.model.Company;
import com.dayone.model.ScrapedResult;
import com.dayone.model.constants.CacheKey;
import com.dayone.persist.entity.CompanyEntity;
import com.dayone.persist.entity.CompanyRepository;
import com.dayone.persist.entity.DividendEntity;
import com.dayone.persist.entity.DividendRepository;
import com.dayone.scraper.Scraper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@AllArgsConstructor
@Slf4j
@EnableCaching
public class ScraperScheduler {
    private final CompanyRepository companyRepository;
    private final DividendRepository dividendRepository;
    private final Scraper yahooScraper;

    //일정 주기마다 수행
    @CacheEvict(value = CacheKey.KEY_FINANCE, allEntries = true)
    @Scheduled(cron = "${scheduler.scrap.yahoo}")
    public void yahooFinanceScheduling() {
        log.info("scraping scheduler is started");
        //저장된 회사 목록을 조회
        List<CompanyEntity> companies = companyRepository.findAll();
        
        //회사마다 배당금 정보를 새로 스크래핑
        for (var company : companies) {
            log.info("scraping scheduler is started -> " + company.getName());
            ScrapedResult scrapedResult = yahooScraper.scrap(Company.builder()
                            .ticker(company.getTicker())
                            .name(company.getName())
                    .build());

            //스크래핑한 배당정보 중 DB에 없는 값을 저장

            scrapedResult.getDividends().stream()
                    .map((e) -> new DividendEntity(company.getId(), e))
                    .forEach( e -> {
                        boolean exists = dividendRepository.existsByCompanyIdAndDate(e.getCompanyId(), e.getDate());

                        if (!exists) {
                            dividendRepository.save(e);
                            log.info("insert new dividend" + e.toString());
                        }
                    });
            //연속적으로 스크래핑 대상 사이트에 요청을 날리지 않도록 일시 정지.
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        

    }
}
