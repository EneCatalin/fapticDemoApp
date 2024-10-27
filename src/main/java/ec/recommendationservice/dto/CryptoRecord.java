package ec.recommendationservice.dto;

public record CryptoRecord(String timestamp, String symbol, String price) {
    @Override
    public String timestamp() {
        return timestamp;
    }

    @Override
    public String symbol() {
        return symbol;
    }

    @Override
    public String price() {
        return price;
    }
}
