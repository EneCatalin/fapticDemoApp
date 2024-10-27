package ec.recommendationservice.controller;


import ec.recommendationservice.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/")
@Tag(name = "Recommendation Controller", description = "APIs for Recommendation Service")
@ControllerAdvice
public class RecController {

    @Autowired
    private RecommendationService recommendationService;


    @Operation(summary = "Check service health", description = "Returns the status of the user service")
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> checkServiceHealth() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    //TESTING ROUTE
    //TODO: decide what to do with the data
    @Operation(summary = "Get Data From CSV ", description = "Reads the data inside the csv files")
    @PostMapping("/seed")
    public ResponseEntity<Map<String, String>> seedWithData() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");

        recommendationService.readData();
        return new ResponseEntity<>(response, HttpStatus.NO_CONTENT);
    }

    //TODO: TEST
    @Operation(summary = "Get lowest price for each coin ", description = "Returns the lowest price for each coin")
    @GetMapping("/coins/highest-prices")
    public ResponseEntity<Map<String, Double>> getHighestPriceForEachCoin() {
        Map<String, Double> highestPrices = recommendationService.getHighestPriceForEachCoin();
        return new ResponseEntity<>(highestPrices, HttpStatus.OK);
    }

    //TODO: TEST
    @Operation(summary = "Get highest price for given coin ", description = "Returns the highest price for a given coin")
    @GetMapping("/coins/{symbol}/highest-price")
    public ResponseEntity<Double> getHighestPriceBySymbol(@PathVariable String symbol) {
        Optional<Double> highestPrice = recommendationService.getHighestPriceBySymbol(symbol);

        return highestPrice
                .map(price -> new ResponseEntity<>(price, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }


}
