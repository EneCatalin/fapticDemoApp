package ec.recommendationservice.controller;


import ec.recommendationservice.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/")
@Tag(name = "Recommendation Controller", description = "APIs for Recommendation Service")
@ControllerAdvice
public class RecController {

    private final RecommendationService recommendationService;

    public RecController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }


    @Operation(summary = "Check service health", description = "Returns the status of the user service")
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> checkServiceHealth() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(summary = "Get Data From CSV ", description = "Reads the data inside the csv files")
    @PostMapping("/seed")
    public ResponseEntity<Map<String, String>> seedWithData() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");

        recommendationService.readData();
        return new ResponseEntity<>(response, HttpStatus.NO_CONTENT);
    }

    @Operation(summary = "Get lowest price for each coin ", description = "Returns the lowest price for each coin")
    @GetMapping("/coins/highest-prices")
    public ResponseEntity<Map<String, Double>> getHighestPriceForEachCoin() {
        Map<String, Double> highestPrices = recommendationService.getHighestPriceForEachCoin();
        return new ResponseEntity<>(highestPrices, HttpStatus.OK);
    }

    @Operation(summary = "Get highest price for given coin ", description = "Returns the highest price for a given coin")
    @GetMapping("/coins/{symbol}/highest-price")
    public ResponseEntity<Double> getHighestPriceBySymbol(@PathVariable String symbol) {
        Optional<Double> highestPrice = recommendationService.getHighestPriceBySymbol(symbol);

        return highestPrice
                .map(price -> new ResponseEntity<>(price, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }


    @Operation(summary = "Get highest oldest/newest/min/max ", description = "Returns the oldest/newest/min/max for each crypto for the whole month")
    @GetMapping("/coins/values")
    public ResponseEntity<List<Map<String, Object>>> getMonthlySummaryForAllCoins() {
        List<Map<String, Object>> monthlySummary = recommendationService.getMonthlySummaryForAllCoins();
        return ResponseEntity.ok(monthlySummary);
    }

    //! This could be cached/added to a db for each day
    @Operation(summary = "Get the highest normalized-range for given date ", description = "Returns the normalized range for a given date")
    @GetMapping("/coins/highest-normalized-range")
    public ResponseEntity<Map<String, Object>> getHighestNormalizedRangeForDate(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        Optional<Map<String, Object>> highestNormalizedRange =
                recommendationService.getCryptoWithHighestNormalizedRangeForDate(date);

        return highestNormalizedRange
                .map(ResponseEntity::ok)
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    //? made in response to this issue:
    /*For some cryptos it might be safe to invest, by just checking only one month's time
    frame. However, for some of them it might be more accurate to check six months or even
    a year. Will the recommendation service be able to handle this? */
    @Operation(summary = "Get the normalized-range for given date range ", description = "Returns the normalized range for a given date range")
    @GetMapping("/coins/normalized-range")
    public ResponseEntity<List<Map<String, Object>>> getNormalizedRangeForDateRange(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<Map<String, Object>> normalizedRanges = recommendationService.getNormalizedRangeForDateRange(startDate, endDate);
        return ResponseEntity.ok(normalizedRanges);
    }


}
