package ec.recommendationservice.repository;

import ec.recommendationservice.entity.Coin;
import ec.recommendationservice.entity.CoinId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface CoinRepository extends JpaRepository<Coin, CoinId> {

    // 1. Find the highest price for a given coin (symbol)
    @Query("SELECT MAX(c.price) FROM Coin c WHERE c.symbol = :symbol")
    Optional<Double> findHighestPriceBySymbol(@Param("symbol") String symbol);

    // 2. Find the highest price for each coin
    @Query("SELECT c.symbol, MAX(c.price) FROM Coin c GROUP BY c.symbol")
    List<Object[]> findHighestPriceForEachCoin();
}
