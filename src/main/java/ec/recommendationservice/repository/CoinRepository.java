package ec.recommendationservice.repository;

import ec.recommendationservice.entity.Coin;
import ec.recommendationservice.entity.CoinId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


public interface CoinRepository extends JpaRepository<Coin, CoinId> {

    @Query("""
                SELECT c.symbol, MAX(c.price), MIN(c.price)
                FROM Coin c
                WHERE c.timestamp BETWEEN :startDate AND :endDate
                GROUP BY c.symbol
            """)
    List<Object[]> findMaxAndMinPricesBySymbolInDateRange(@Param("startDate") LocalDateTime startDate,
                                                          @Param("endDate") LocalDateTime endDate);

    @Query("""
                SELECT c.symbol, MAX(c.price), MIN(c.price)
                FROM Coin c 
                WHERE DATE(c.timestamp) = :date
                GROUP BY c.symbol
            """)
    List<Object[]> findMaxAndMinPricesBySymbolForDate(@Param("date") LocalDate date);

    @Query("""
                SELECT c.symbol, 
                       MIN(c.timestamp), MAX(c.timestamp), 
                       MIN(c.price), MAX(c.price)
                FROM Coin c 
                GROUP BY c.symbol
            """)
    List<Object[]> findOldestNewestMinMaxPricesForAllSymbols();

    @Query("SELECT c.symbol, MAX(c.price), MIN(c.price) FROM Coin c GROUP BY c.symbol")
    List<Object[]> findMaxAndMinPricesForAllSymbols();

    // 1. Find the highest price for a given coin (symbol)
    @Query("SELECT MAX(c.price) FROM Coin c WHERE c.symbol = :symbol")
    Optional<Double> findHighestPriceBySymbol(@Param("symbol") String symbol);

    // 2. Find the highest price for each coin
    @Query("SELECT c.symbol, MAX(c.price) FROM Coin c GROUP BY c.symbol")
    List<Object[]> findHighestPriceForEachCoin();
}
