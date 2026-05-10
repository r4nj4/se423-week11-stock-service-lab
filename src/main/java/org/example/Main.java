package org.example;

import jakarta.xml.ws.Endpoint;
import org.example.stock.StockServiceImpl;

public class Main {

    public static void main(String[] args) {
        String portEnv = System.getenv("PORT");
        int port = (portEnv != null && !portEnv.isBlank()) ? Integer.parseInt(portEnv) : 8080;

        String address = "http://0.0.0.0:" + port + "/StockService";
        Endpoint.publish(address, new StockServiceImpl());

        System.out.println("=========================================================");
        System.out.println(" Stock Inquiry SOAP Service is running");
        System.out.println(" Endpoint : " + address);
        System.out.println(" WSDL     : " + address + "?wsdl");
        System.out.println("=========================================================");
    }
}