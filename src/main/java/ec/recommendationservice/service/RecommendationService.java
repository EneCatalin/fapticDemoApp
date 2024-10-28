package ec.recommendationservice.service;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import ec.recommendationservice.dto.CryptoRecord;
import ec.recommendationservice.entity.Coin;
import ec.recommendationservice.repository.CoinRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {
    private static final Logger logger = LoggerFactory.getLogger(RecommendationService.class);
    @Value("${coin.data.directory-path}")
    private String DIRECTORY_PATH;


    @Autowired
    private CoinRepository coinRepository;


    public void readData() {
        logger.info("Data read started");

        File directory = new File(DIRECTORY_PATH);

        CsvSchema schema = CsvSchema.emptySchema().withHeader();
        CsvMapper csvMapper = new CsvMapper();

        File[] csvFiles = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));
        if (csvFiles == null || csvFiles.length == 0) {
            logger.warn("No CSV files found in directory: {}", DIRECTORY_PATH);
            return;
        }

        Arrays.stream(csvFiles).forEach(file -> {
            logger.info("Reading file: {}", file.getName());
            try (MappingIterator<CryptoRecord> iterator = csvMapper.readerFor(CryptoRecord.class)
                    .with(schema)
                    .<CryptoRecord>readValues(file)) {

                List<CryptoRecord> records = iterator.readAll();
                records.forEach(record -> {
                    Coin coin = new Coin();
                    coin.setSymbol(record.symbol());
                    coin.setTimestamp(LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(record.timestamp())), ZoneId.systemDefault()));
                    coin.setPrice(Double.parseDouble(record.price()));

                    // Save each coin to the database
                    coinRepository.save(coin);
                });

            } catch (IOException e) {
                logger.error("Error reading CSV file: {}", file.getName(), e);
            }
        });

        logger.info("Data read completed for all files");
    }

    public Optional<Double> getHighestPriceBySymbol(String symbol) {
        return coinRepository.findHighestPriceBySymbol(symbol);
    }

    public Map<String, Double> getHighestPriceForEachCoin() {
        List<Object[]> results = coinRepository.findHighestPriceForEachCoin();
        Map<String, Double> highestPrices = new HashMap<>();

        for (Object[] result : results) {
            String symbol = (String) result[0];
            Double price = (Double) result[1];
            highestPrices.put(symbol, price);
        }

        return highestPrices;
    }


    public List<Map<String, Object>> getNormalizedRangeForAllCoins() {
        List<Object[]> results = coinRepository.findMaxAndMinPricesForAllSymbols();

        // Calculate normalized range and prepare the results
        List<Map<String, Object>> normalizedRanges = new ArrayList<>();
        for (Object[] result : results) {
            String symbol = (String) result[0];
            Double maxPrice = (Double) result[1];
            Double minPrice = (Double) result[2];

            if (minPrice != null && minPrice > 0) {
                double normalizedRange = (maxPrice - minPrice) / minPrice;

                normalizedRanges.add(Map.of(
                        "symbol", symbol,
                        "normalizedRange", normalizedRange
                ));
            }
        }

        // Sort by normalized range in descending order
        return normalizedRanges.stream()
                .sorted((o1, o2) -> Double.compare((Double) o2.get("normalizedRange"), (Double) o1.get("normalizedRange")))
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getMonthlySummaryForAllCoins() {
        List<Object[]> results = coinRepository.findOldestNewestMinMaxPricesForAllSymbols();
        List<Map<String, Object>> summaryList = new ArrayList<>();

        for (Object[] result : results) {
            String symbol = (String) result[0];
            LocalDateTime oldestTimestamp = (LocalDateTime) result[1];
            LocalDateTime newestTimestamp = (LocalDateTime) result[2];
            Double minPrice = (Double) result[3];
            Double maxPrice = (Double) result[4];

            // Format response for each crypto
            Map<String, Object> summary = new HashMap<>();
            summary.put("symbol", symbol);
            summary.put("oldest", oldestTimestamp);
            summary.put("newest", newestTimestamp);
            summary.put("min", minPrice);
            summary.put("max", maxPrice);

            summaryList.add(summary);
        }

        return summaryList;
    }

    public Optional<Map<String, Object>> getCryptoWithHighestNormalizedRangeForDate(LocalDate date) {
        List<Object[]> results = coinRepository.findMaxAndMinPricesBySymbolForDate(date);

        Map<String, Object> highestNormalizedRangeCrypto = null;
        double highestNormalizedRange = -1;

        for (Object[] result : results) {
            String symbol = (String) result[0];
            Double maxPrice = (Double) result[1];
            Double minPrice = (Double) result[2];

            if (minPrice != null && minPrice > 0) {
                double normalizedRange = (maxPrice - minPrice) / minPrice;

                if (normalizedRange > highestNormalizedRange) {
                    highestNormalizedRange = normalizedRange;
                    highestNormalizedRangeCrypto = new HashMap<>();
                    highestNormalizedRangeCrypto.put("symbol", symbol);
                    highestNormalizedRangeCrypto.put("normalizedRange", normalizedRange);
                }
            }
        }

        return Optional.ofNullable(highestNormalizedRangeCrypto);
    }

}