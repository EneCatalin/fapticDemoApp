package ec.recommendationservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "coins")
@IdClass(CoinId.class)
public class Coin implements Serializable {
    @Id
    private String symbol;

    @Id
    private LocalDateTime timestamp;

    private double price;

    // Getters and setters
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

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }
}