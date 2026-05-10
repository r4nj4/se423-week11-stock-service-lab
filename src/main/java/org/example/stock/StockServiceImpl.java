package org.example.stock;

import jakarta.jws.WebService;

import java.util.HashMap;
import java.util.Map;

@WebService(
        endpointInterface = "org.example.stock.StockService",
        serviceName = "StockService",
        portName = "StockServicePort",
        targetNamespace = "http://stock.example.org/"
)
public class StockServiceImpl implements StockService {

    private static final Map<String, Integer> STOCK = new HashMap<>();

    static {
        STOCK.put("Laptops", 50);
        STOCK.put("Smartphones", 120);
        STOCK.put("Tablets", 35);
        STOCK.put("Monitors", 18);
        STOCK.put("Keyboards", 200);
    }

    @Override
    public int checkStock(String item) {
        if (item == null) {
            return 0;
        }
        return STOCK.getOrDefault(item, 0);
    }
}