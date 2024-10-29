package ec.recommendationservice.service;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import ec.recommendationservice.dto.CryptoRecord;
import ec.recommendationservice.entity.Coin;
import ec.recommendationservice.repository.CoinRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {
    private static final Logger logger = LoggerFactory.getLogger(RecommendationService.class);
    private final String directoryPath;
    private final CoinRepository coinRepository;
    private final Set<String> whitelist;

    public RecommendationService(@Value("${coin.data.directory-path}") String directoryPath,
                                 CoinRepository coinRepository,
                                 @Value("${coin.whitelist}") Set<String> whitelist) {
        this.directoryPath = directoryPath;
        this.coinRepository = coinRepository;
        this.whitelist = whitelist;
    }

    void parseCsvFile(File file) {
        logger.info("Reading file: {}", file.getName());
        CsvMapper csvMapper = new CsvMapper();
        CsvSchema schema = CsvSchema.emptySchema().withHeader();

        try (MappingIterator<CryptoRecord> iterator = csvMapper.readerFor(CryptoRecord.class)
                .with(schema)
                .<CryptoRecord>readValues(file)) {

            List<CryptoRecord> records = iterator.readAll();
            records.forEach(record -> {
                if (whitelist.contains(record.symbol())) {
                    coinRepository.save(transformToCoin(record));
                } else {
                    logger.warn("Skipping unsupported crypto symbol: {}", record.symbol());
                }
            });

        } catch (IOException e) {
            logger.error("Error reading CSV file: {}", file.getName(), e);
            throw new RuntimeException("Error reading CSV file: " + file.getName(), e);
        }
    }


    public void readData() {
        logger.info("Data read started");

        File directory = new File(directoryPath);
        File[] csvFiles = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));
        if (csvFiles == null || csvFiles.length == 0) {
            logger.warn("No CSV files found in directory: {}", directoryPath);
            throw new IllegalArgumentException("No CSV files found in directory: " + directoryPath);
        }

        Arrays.stream(csvFiles).forEach(this::parseCsvFile);

        logger.info("Data read completed for all files");
    }


    Coin transformToCoin(CryptoRecord record) {
        if (record.symbol().isEmpty() || record.timestamp().isEmpty() || record.price().isEmpty()) {
            logger.error("Invalid record: {}", record);
            throw new IllegalArgumentException("Invalid record: " + record);
        }

        Coin coin = new Coin();
        coin.setSymbol(record.symbol());
        coin.setTimestamp(LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(record.timestamp())), ZoneId.systemDefault()));
        coin.setPrice(Double.parseDouble(record.price()));
        return coin;
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

    private double calculateNormalizedRange(Double maxPrice, Double minPrice) {
        return (maxPrice - minPrice) / minPrice;
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

    private Map<String, Object> convertToNormalizedRangeMap(Object[] result) {
        String symbol = (String) result[0];
        Double maxPrice = (Double) result[1];
        Double minPrice = (Double) result[2];
        double normalizedRange = calculateNormalizedRange(maxPrice, minPrice);

        Map<String, Object> normalizedRangeMap = new HashMap<>();
        normalizedRangeMap.put("symbol", symbol);
        normalizedRangeMap.put("normalizedRange", normalizedRange);
        return normalizedRangeMap;
    }


    public Optional<Map<String, Object>> getCryptoWithHighestNormalizedRangeForDate(LocalDate date) {
        return coinRepository.findMaxAndMinPricesBySymbolForDate(date).stream()
                .filter(result -> result[2] != null && (Double) result[2] > 0) // Filter out null or zero min prices
                .map(this::convertToNormalizedRangeMap)
                .max(Comparator.comparingDouble(result -> (Double) result.get("normalizedRange"))); // Find max normalized range
    }

    public List<Map<String, Object>> getNormalizedRangeForDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<Object[]> results = coinRepository.findMaxAndMinPricesBySymbolInDateRange(startDateTime, endDateTime);

        return results.stream()
                .filter(result -> result[2] != null && (Double) result[2] > 0)
                .map(this::convertToNormalizedRangeMap)
                .sorted((o1, o2) -> Double.compare((Double) o2.get("normalizedRange"), (Double) o1.get("normalizedRange")))
                .collect(Collectors.toList());
    }

}