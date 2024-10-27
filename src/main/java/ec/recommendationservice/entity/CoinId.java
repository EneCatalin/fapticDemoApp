package ec.recommendationservice.entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

public class CoinId implements Serializable {
    private String symbol;
    private LocalDateTime timestamp;

    public CoinId() {
    }

    public CoinId(String symbol, LocalDateTime timestamp) {
        this.symbol = symbol;
        this.timestamp = timestamp;
    }

    // Getters, setters, equals, and hashCode
    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CoinId coinId = (CoinId) o;
        return Objects.equals(symbol, coinId.symbol) && Objects.equals(timestamp, coinId.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, timestamp);
    }
}
