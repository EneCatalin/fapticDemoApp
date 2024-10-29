package ec.recommendationservice.service;

import ec.recommendationservice.dto.CryptoRecord;
import ec.recommendationservice.repository.CoinRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RecommendationServiceTest {

    @Mock
    private CoinRepository coinRepository;

    @InjectMocks
    private RecommendationService recommendationService;

    private final Set<String> whitelist = Set.of("BTC", "ETH");

    @BeforeEach
    void setUp() throws IOException {
        // Set up a temporary directory with test CSV files
        File tempDir = new File("tempTestDir");
        tempDir.mkdir();
        File csvFile = new File(tempDir, "test.csv");
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("symbol,timestamp,price\n");
            writer.write("BTC,1640995200000,50000\n");
            writer.write("ETH,1640995200000,4000\n");
            writer.write("XRP,1640995200000,0.8\n"); // Not in whitelist, should be skipped
        }
        // Initialize RecommendationService with temp directory path and whitelist
        recommendationService = new RecommendationService(tempDir.getAbsolutePath(), coinRepository, whitelist);
    }

    @Test
    void testReadDataPositiveScenario() {
        // Call readData, which should process files in the temporary directory
        recommendationService.readData();

        // Verify that only whitelisted symbols are saved to the repository
        verify(coinRepository, times(2)).save(any()); // Only BTC and ETH records should be saved
        verify(coinRepository, never()).save(argThat(record -> "XRP".equals(record.getSymbol())));
    }

    @Test
    void testTransformToCoinThrowsIllegalArgumentExceptionForInvalidRecord() {
        // Create an invalid CryptoRecord with empty fields
        CryptoRecord invalidRecord = new CryptoRecord("", "", "");

        // Assert that transformToCoin throws an IllegalArgumentException
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> recommendationService.transformToCoin(invalidRecord));

        // Optionally, verify the exception message
        assertTrue(exception.getMessage().contains("Invalid record"));
    }

    @Test
    void testParseCsvFileThrowsRuntimeException() {
        // Create a non-existent file to simulate an IOException during reading
        File nonExistentFile = new File("nonExistentFile.csv");

        // Assert that a RuntimeException is thrown when readData tries to parse the non-existent file
        RuntimeException exception = assertThrows(RuntimeException.class, () -> recommendationService.parseCsvFile(nonExistentFile));

        // Check if the exception message is correct
        assertTrue(exception.getMessage().contains("Error reading CSV file"));
    }

    @Test
    void testReadDataNoCsvFiles() {
        // Create and use a different empty directory path specifically for this test
        File emptyDirectory = new File("emptyTestDirPath");
        emptyDirectory.mkdir(); // Ensure the directory exists but has no files

        // Re-initialize RecommendationService with the empty directory path
        recommendationService = new RecommendationService(emptyDirectory.getAbsolutePath(), coinRepository, whitelist);

        // Verify that the exception is thrown when no CSV files are present
        Exception exception = assertThrows(IllegalArgumentException.class, () -> recommendationService.readData());

        // Check the exception message to ensure it's correct
        String expectedMessage = "No CSV files found in directory: emptyTestDirPath";
        System.out.println("Actual exception message: " + exception.getMessage());

        assertTrue(exception.getMessage().contains("No CSV files found in directory"));
    }


    //----------------------------------------------------
    @Test
    void testGetHighestPriceBySymbolPositiveCase() {
        String symbol = "BTC";
        double expectedPrice = 50000.0;

        // Mock the repository to return the expected price for the symbol
        when(coinRepository.findHighestPriceBySymbol(symbol)).thenReturn(Optional.of(expectedPrice));

        // Call the method
        Optional<Double> result = recommendationService.getHighestPriceBySymbol(symbol);

        // Assert that the result matches the expected price
        assertEquals(Optional.of(expectedPrice), result);
    }

    @Test
    void testGetHighestPriceBySymbolSymbolNotFound() {
        String symbol = "BTC";

        // Mock the repository to return an empty Optional when the symbol is not found
        when(coinRepository.findHighestPriceBySymbol(symbol)).thenReturn(Optional.empty());

        // Call the method
        Optional<Double> result = recommendationService.getHighestPriceBySymbol(symbol);

        // Assert that the result is an empty Optional
        assertTrue(result.isEmpty());
    }

    //--------------------------------

    @Test
    void testGetHighestPriceForEachCoinPositiveCase() {
        // Mock data for the repository response
        List<Object[]> mockResults = Arrays.asList(
                new Object[]{"BTC", 50000.0},
                new Object[]{"ETH", 4000.0}
        );

        // Mock the repository to return this data
        when(coinRepository.findHighestPriceForEachCoin()).thenReturn(mockResults);

        // Call the method
        Map<String, Double> result = recommendationService.getHighestPriceForEachCoin();

        // Expected results
        Map<String, Double> expected = new HashMap<>();
        expected.put("BTC", 50000.0);
        expected.put("ETH", 4000.0);

        // Assert that the returned map matches the expected map
        assertEquals(expected, result);
    }

    @Test
    void testGetHighestPriceForEachCoinEmptyCase() {
        // Mock the repository to return an empty list
        when(coinRepository.findHighestPriceForEachCoin()).thenReturn(Collections.emptyList());

        // Call the method
        Map<String, Double> result = recommendationService.getHighestPriceForEachCoin();

        // Assert that the result is an empty map
        assertTrue(result.isEmpty());
    }

    //--------------------------------
    @Test
    void testGetMonthlySummaryForAllCoinsPositiveCase() {
        // Prepare mock data for the repository response
        List<Object[]> mockResults = Arrays.asList(
                new Object[]{"BTC", LocalDateTime.of(2023, 1, 1, 0, 0), LocalDateTime.of(2023, 1, 31, 23, 59), 30000.0, 50000.0},
                new Object[]{"ETH", LocalDateTime.of(2023, 1, 1, 0, 0), LocalDateTime.of(2023, 1, 31, 23, 59), 2000.0, 4000.0}
        );

        // Mock the repository to return this data
        when(coinRepository.findOldestNewestMinMaxPricesForAllSymbols()).thenReturn(mockResults);

        // Call the method
        List<Map<String, Object>> result = recommendationService.getMonthlySummaryForAllCoins();

        // Expected results
        List<Map<String, Object>> expected = new ArrayList<>();

        Map<String, Object> btcSummary = new HashMap<>();
        btcSummary.put("symbol", "BTC");
        btcSummary.put("oldest", LocalDateTime.of(2023, 1, 1, 0, 0));
        btcSummary.put("newest", LocalDateTime.of(2023, 1, 31, 23, 59));
        btcSummary.put("min", 30000.0);
        btcSummary.put("max", 50000.0);

        Map<String, Object> ethSummary = new HashMap<>();
        ethSummary.put("symbol", "ETH");
        ethSummary.put("oldest", LocalDateTime.of(2023, 1, 1, 0, 0));
        ethSummary.put("newest", LocalDateTime.of(2023, 1, 31, 23, 59));
        ethSummary.put("min", 2000.0);
        ethSummary.put("max", 4000.0);

        expected.add(btcSummary);
        expected.add(ethSummary);

        // Assert that the result matches the expected output
        assertEquals(expected, result);
    }

    @Test
    void testGetMonthlySummaryForAllCoinsEmptyCase() {
        // Mock the repository to return an empty list
        when(coinRepository.findOldestNewestMinMaxPricesForAllSymbols()).thenReturn(Collections.emptyList());

        // Call the method
        List<Map<String, Object>> result = recommendationService.getMonthlySummaryForAllCoins();

        // Assert that the result is an empty list
        assertTrue(result.isEmpty());
    }

    //--------------------------------

    @Test
    void testGetCryptoWithHighestNormalizedRangeForDateNormalCase() {
        // Define a sample date
        LocalDate date = LocalDate.of(2023, 1, 1);

        // Mock repository response
        List<Object[]> mockResults = Arrays.asList(
                new Object[]{"BTC", 60000.0, 30000.0},
                new Object[]{"ETH", 4000.0, 2000.0}
        );

        when(coinRepository.findMaxAndMinPricesBySymbolForDate(date)).thenReturn(mockResults);

        // Call the method
        Optional<Map<String, Object>> result = recommendationService.getCryptoWithHighestNormalizedRangeForDate(date);

        // Expected map with highest normalized range
        Map<String, Object> expected = new HashMap<>();
        expected.put("symbol", "BTC");
        expected.put("normalizedRange", 1.0); // (60000 - 30000) / 30000 = 1.0

        // Verify the result
        assertTrue(result.isPresent());
        assertEquals(expected, result.get());
    }

    @Test
    void testGetCryptoWithHighestNormalizedRangeForDateNoData() {
        // Define a sample date
        LocalDate date = LocalDate.of(2023, 1, 1);

        // Mock repository to return an empty list
        when(coinRepository.findMaxAndMinPricesBySymbolForDate(date)).thenReturn(Collections.emptyList());

        // Call the method
        Optional<Map<String, Object>> result = recommendationService.getCryptoWithHighestNormalizedRangeForDate(date);

        // Verify that the result is empty
        assertTrue(result.isEmpty());
    }
    //--------------------------------

    @Test
    void testGetNormalizedRangeForDateRangeNormalCase() {
        // Define a date range
        LocalDate startDate = LocalDate.of(2023, 1, 1);
        LocalDate endDate = LocalDate.of(2023, 1, 31);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // Mock repository response
        List<Object[]> mockResults = Arrays.asList(
                new Object[]{"BTC", 60000.0, 30000.0},
                new Object[]{"ETH", 4000.0, 2000.0}
        );

        when(coinRepository.findMaxAndMinPricesBySymbolInDateRange(startDateTime, endDateTime)).thenReturn(mockResults);

        // Call the method
        List<Map<String, Object>> result = recommendationService.getNormalizedRangeForDateRange(startDate, endDate);

        // Expected results in sorted order by normalized range
        List<Map<String, Object>> expected = new ArrayList<>();
        Map<String, Object> btcMap = new HashMap<>();
        btcMap.put("symbol", "BTC");
        btcMap.put("normalizedRange", 1.0); // (60000 - 30000) / 30000 = 1.0
        Map<String, Object> ethMap = new HashMap<>();
        ethMap.put("symbol", "ETH");
        ethMap.put("normalizedRange", 1.0); // (4000 - 2000) / 2000 = 1.0
        expected.add(btcMap);
        expected.add(ethMap);

        // Assert that the results match expected output
        assertEquals(expected, result);
    }

    @Test
    void testGetNormalizedRangeForDateRangeEmptyCase() {
        // Define a date range
        LocalDate startDate = LocalDate.of(2023, 1, 1);
        LocalDate endDate = LocalDate.of(2023, 1, 31);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // Mock the repository to return an empty list
        when(coinRepository.findMaxAndMinPricesBySymbolInDateRange(startDateTime, endDateTime)).thenReturn(Collections.emptyList());

        // Call the method
        List<Map<String, Object>> result = recommendationService.getNormalizedRangeForDateRange(startDate, endDate);

        // Verify that the result is an empty list
        assertTrue(result.isEmpty());
    }


}