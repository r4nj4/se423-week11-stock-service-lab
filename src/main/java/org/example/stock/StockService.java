package org.example.stock;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;
import jakarta.xml.ws.RequestWrapper;
import jakarta.xml.ws.ResponseWrapper;

@WebService(
        name = "StockServicePortType",
        targetNamespace = "http://stock.example.org/"
)
@SOAPBinding(style = SOAPBinding.Style.DOCUMENT, use = SOAPBinding.Use.LITERAL)
public interface StockService {

    @WebMethod(operationName = "checkStock")
    @RequestWrapper(
            localName = "getStockRequest",
            targetNamespace = "http://stock.example.org/",
            className = "org.example.stock.jaxws.GetStockRequest"
    )
    @ResponseWrapper(
            localName = "getStockResponse",
            targetNamespace = "http://stock.example.org/",
            className = "org.example.stock.jaxws.GetStockResponse"
    )
    @WebResult(name = "quantity")
    int checkStock(@WebParam(name = "item") String item);
}