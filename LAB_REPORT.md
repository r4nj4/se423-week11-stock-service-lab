# SE423 — Enterprise Software Architecture
## Lab Activity: Week 11 — Stock Inquiry Web Service

**Lecturer:** Dr. Hoger Mahmud
**Submission date:** 10 May 2026
**Student:** _[your name]_

---

## 1. Introduction

In this lab I built a basic Web Service using the three core technologies
covered in the lecture: **WSDL** for service description, **SOAP** for
the communication protocol, and **UDDI** for service discovery.

The scenario is a regional warehouse that wants to expose stock
availability to retail stores. A retail store sends an item name (for
example `Laptops`) and the warehouse service replies with the current
stock count.

The four lab steps are:

1. Define the service interface in WSDL.
2. Implement the service provider using SOAP.
3. Register the service in a (simulated) UDDI entry.
4. Test the service from a client (SoapUI / HTTP POST).

---

## 2. Tools used

| Tool | Version | What it was used for |
|------|---------|----------------------|
| JDK (Eclipse Temurin) | 17 | Language runtime |
| IntelliJ IDEA Ultimate | 2024.x | IDE |
| Apache Maven | 3.9 | Build / dependency management |
| JAX-WS Reference Implementation (Eclipse Metro) | 4.0.2 | Web Service framework |
| SoapUI Open Source | 5.7 | SOAP client for testing |
| GitHub | — | Version control |

The lab spec lists Apache Tomcat 9 *or* the built-in JAX-WS libraries as
acceptable frameworks. I went with **JAX-WS** because it is part of the
Java ecosystem, has a `jakarta.xml.ws.Endpoint.publish(...)` method that
makes it possible to host a SOAP endpoint without installing Tomcat,
and it auto-generates a fully compliant WSDL from the annotated Java
classes at runtime — which means I can compare the WSDL I wrote by
hand (Step 1) against the WSDL the framework generates.

---

## 3. SOAP Protocol — short refresher

SOAP (Simple Object Access Protocol) is an XML-based messaging protocol
for exchanging structured information between systems. Every SOAP
message has the same shape:

```
+-----------------------------------+
|   SOAP Envelope                   |
|   +---------------------------+   |
|   |   SOAP Header (optional)  |   |
|   +---------------------------+   |
|   +---------------------------+   |
|   |   SOAP Body               |   |
|   |   - operation payload     |   |
|   |   - or fault message      |   |
|   +---------------------------+   |
+-----------------------------------+
```

In this lab, SOAP messages travel over HTTP POST. The body of the
request carries the item name; the body of the response carries the
quantity. Section 6 below shows a full request/response capture.

---

## 4. Step 1 — Define the Service Interface (WSDL)

WSDL (Web Services Description Language) is the XML contract that
describes **what** the service does and **how** to call it. I wrote
the WSDL by hand first so I had a clear picture of the contract before
writing any Java.

The WSDL defines:

- **Two messages**: `getStockRequest` (carries the item name) and
  `getStockResponse` (carries the quantity).
- **One PortType** called `StockServicePortType` with a single
  operation `checkStock` that takes the request and returns the
  response.
- **A SOAP binding** over HTTP using document/literal style.
- **A service** that exposes a port at a chosen address.

File: `src/main/resources/StockService.wsdl`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<definitions name="StockService"
             targetNamespace="http://stock.example.org/"
             xmlns:tns="http://stock.example.org/"
             xmlns:xsd="http://www.w3.org/2001/XMLSchema"
             xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/"
             xmlns="http://schemas.xmlsoap.org/wsdl/">

    <types>
        <xsd:schema targetNamespace="http://stock.example.org/">
            <xsd:element name="getStockRequest">
                <xsd:complexType>
                    <xsd:sequence>
                        <xsd:element name="item" type="xsd:string"/>
                    </xsd:sequence>
                </xsd:complexType>
            </xsd:element>
            <xsd:element name="getStockResponse">
                <xsd:complexType>
                    <xsd:sequence>
                        <xsd:element name="quantity" type="xsd:int"/>
                    </xsd:sequence>
                </xsd:complexType>
            </xsd:element>
        </xsd:schema>
    </types>

    <message name="getStockRequest">
        <part name="parameters" element="tns:getStockRequest"/>
    </message>
    <message name="getStockResponse">
        <part name="parameters" element="tns:getStockResponse"/>
    </message>

    <portType name="StockServicePortType">
        <operation name="checkStock">
            <input  message="tns:getStockRequest"/>
            <output message="tns:getStockResponse"/>
        </operation>
    </portType>

    <binding name="StockServiceSoapBinding" type="tns:StockServicePortType">
        <soap:binding style="document"
                      transport="http://schemas.xmlsoap.org/soap/http"/>
        <operation name="checkStock">
            <soap:operation soapAction="http://stock.example.org/checkStock"/>
            <input> <soap:body use="literal"/> </input>
            <output><soap:body use="literal"/> </output>
        </operation>
    </binding>

    <service name="StockService">
        <port name="StockServicePort" binding="tns:StockServiceSoapBinding">
            <soap:address location="http://localhost:8080/StockService"/>
        </port>
    </service>
</definitions>
```

> **Screenshot to attach:** open `StockService.wsdl` in IntelliJ and
> take a screenshot showing the file contents.

---

## 5. Step 2 — Implement the Service Provider (SOAP)

### 5.1  The service interface (the “skeleton”)

Following the lab guidance to “generate the SOAP skeleton from the
WSDL”, I created a Java interface that mirrors the operations defined
in the WSDL. The JAX-WS annotations are how the framework knows to
expose this interface as a SOAP service.

File: `src/main/java/org/example/stock/StockService.java`

```java
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
@SOAPBinding(style = SOAPBinding.Style.DOCUMENT,
             use   = SOAPBinding.Use.LITERAL)
public interface StockService {

    @WebMethod(operationName = "checkStock")
    @RequestWrapper(
            localName = "getStockRequest",
            targetNamespace = "http://stock.example.org/",
            className = "org.example.stock.jaxws.GetStockRequest")
    @ResponseWrapper(
            localName = "getStockResponse",
            targetNamespace = "http://stock.example.org/",
            className = "org.example.stock.jaxws.GetStockResponse")
    @WebResult(name = "quantity")
    int checkStock(@WebParam(name = "item") String item);
}
```

The wrapper annotations make sure the live SOAP messages use the
exact element names (`getStockRequest` / `getStockResponse`) defined
in the hand-written WSDL.

### 5.2  The implementation — HashMap of stock data

File: `src/main/java/org/example/stock/StockServiceImpl.java`

```java
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
        STOCK.put("Laptops",     50);
        STOCK.put("Smartphones", 120);
        STOCK.put("Tablets",     35);
        STOCK.put("Monitors",    18);
        STOCK.put("Keyboards",   200);
    }

    @Override
    public int checkStock(String item) {
        if (item == null) return 0;
        return STOCK.getOrDefault(item, 0);
    }
}
```

The HashMap satisfies the lab requirement *“Use a simple HashMap to
store dummy data”*. Unknown items return `0`, which is a sensible
default for a stock query.

### 5.3  Publishing the endpoint

The lab text says “deploy to Apache Tomcat”. JAX-WS provides an
equivalent built-in publisher via `Endpoint.publish(...)` that does
the same job (it starts an HTTP listener and binds the service to a
URI), so I used that to keep the lab self-contained.

File: `src/main/java/org/example/Main.java`

```java
package org.example;

import jakarta.xml.ws.Endpoint;
import org.example.stock.StockServiceImpl;

public class Main {
    public static void main(String[] args) {
        String portEnv = System.getenv("PORT");
        int port = (portEnv != null && !portEnv.isBlank())
                       ? Integer.parseInt(portEnv) : 8080;

        String address = "http://0.0.0.0:" + port + "/StockService";
        Endpoint.publish(address, new StockServiceImpl());

        System.out.println("Stock Inquiry SOAP Service is running");
        System.out.println("Endpoint : " + address);
        System.out.println("WSDL     : " + address + "?wsdl");
    }
}
```

To start the service:

```bash
$ mvn clean package -DskipTests
$ java -jar target/stock-service.jar

Stock Inquiry SOAP Service is running
Endpoint : http://0.0.0.0:8080/StockService
WSDL     : http://0.0.0.0:8080/StockService?wsdl
```

Once running, the framework auto-generates the runtime WSDL at
`/StockService?wsdl`. Visiting that URL in a browser confirms the
service is alive — sample contents:

```xml
<definitions targetNamespace="http://stock.example.org/" name="StockService">
  <types>
    <xsd:schema>
      <xsd:import namespace="http://stock.example.org/"
                  schemaLocation="http://localhost:8080/StockService?xsd=1"/>
    </xsd:schema>
  </types>
  <message name="checkStock">
    <part name="parameters" element="tns:getStockRequest"/>
  </message>
  <message name="checkStockResponse">
    <part name="parameters" element="tns:getStockResponse"/>
  </message>
  <portType name="StockServicePortType">
    <operation name="checkStock">
      <input  message="tns:checkStock"/>
      <output message="tns:checkStockResponse"/>
    </operation>
  </portType>
  <binding name="StockServicePortBinding" type="tns:StockServicePortType">
    <soap:binding transport="http://schemas.xmlsoap.org/soap/http"
                  style="document"/>
    <operation name="checkStock">
      <soap:operation soapAction=""/>
      <input> <soap:body use="literal"/> </input>
      <output><soap:body use="literal"/> </output>
    </operation>
  </binding>
  ...
</definitions>
```

> **Screenshots to attach:**
> 1. IntelliJ run console showing the `Endpoint : ...` and `WSDL : ...` lines.
> 2. Browser tab open at `http://localhost:8080/StockService?wsdl` showing the live WSDL.

---

## 6. Step 3 — Register the Service (UDDI Model)

UDDI (Universal Description, Discovery and Integration) is the
registry where service providers publish their service so requestors
can look it up. A real registry would be a server such as Apache
jUDDI; for this lab the entry is captured as XML.

File: `uddi-registration.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<uddi:businessEntity xmlns:uddi="urn:uddi-org:api_v3"
                     businessKey="uddi:auis.edu.iq:warehouse-systems">

    <!-- Business Entity — who publishes the service -->
    <uddi:name xml:lang="en">AUIS Warehouse Systems</uddi:name>
    <uddi:description xml:lang="en">
        Regional warehouse operated by AUIS providing stock-availability
        inquiry services to retail-store partners.
    </uddi:description>

    <uddi:businessServices>

        <!-- Business Service — the offering itself -->
        <uddi:businessService
                serviceKey="uddi:auis.edu.iq:stock-inquiry"
                businessKey="uddi:auis.edu.iq:warehouse-systems">
            <uddi:name xml:lang="en">Stock Inquiry</uddi:name>
            <uddi:description xml:lang="en">
                Returns the current quantity in stock for a given item name.
            </uddi:description>

            <uddi:bindingTemplates>
                <!-- Binding Template — the access point (live URL) -->
                <uddi:bindingTemplate
                        bindingKey="uddi:auis.edu.iq:stock-inquiry:soap-binding"
                        serviceKey="uddi:auis.edu.iq:stock-inquiry">

                    <uddi:accessPoint useType="endPoint">
                        http://localhost:8080/StockService
                    </uddi:accessPoint>

                    <uddi:tModelInstanceDetails>
                        <uddi:tModelInstanceInfo tModelKey="uddi:uddi.org:wsdl:types">
                            <uddi:instanceDetails>
                                <uddi:overviewDoc>
                                    <uddi:overviewURL useType="wsdlInterface">
                                        http://localhost:8080/StockService?wsdl
                                    </uddi:overviewURL>
                                </uddi:overviewDoc>
                            </uddi:instanceDetails>
                        </uddi:tModelInstanceInfo>
                    </uddi:tModelInstanceDetails>
                </uddi:bindingTemplate>
            </uddi:bindingTemplates>
        </uddi:businessService>

    </uddi:businessServices>
</uddi:businessEntity>
```

This XML covers the three structures the lab asks for:

| Lab requirement | Where it lives in the XML |
|------------------|---------------------------|
| 3.1 Business Entity | `<uddi:businessEntity>` element with name *AUIS Warehouse Systems* |
| 3.2 Business Service | `<uddi:businessService>` element with name *Stock Inquiry* |
| 3.3 Binding Template — Access Point | `<uddi:accessPoint>` → service URL |

> **Screenshot to attach:** `uddi-registration.xml` opened in IntelliJ.

---

## 7. Step 4 — Test the Service (the Client)

I tested the service in two ways: with a quick HTTP call (to confirm
the SOAP envelopes flow correctly) and with SoapUI (to satisfy the
lab requirement of using a SOAP testing tool).

### 7.1  Confirming the SOAP request / response

The text below is exactly what was sent over HTTP. The first block is
the **request**, the second is the **response** that came back from
the running service (with the JVM started via
`java -jar target/stock-service.jar`).

**Request — checkStock("Laptops")**

```http
POST /StockService HTTP/1.1
Host: localhost:8080
Content-Type: text/xml; charset=UTF-8
SOAPAction: ""

<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:stk="http://stock.example.org/">
  <soapenv:Body>
    <stk:getStockRequest>
      <item>Laptops</item>
    </stk:getStockRequest>
  </soapenv:Body>
</soapenv:Envelope>
```

**Response**

```xml
<?xml version='1.0' encoding='UTF-8'?>
<S:Envelope xmlns:S="http://schemas.xmlsoap.org/soap/envelope/">
  <S:Body>
    <ns2:getStockResponse xmlns:ns2="http://stock.example.org/">
      <quantity>50</quantity>
    </ns2:getStockResponse>
  </S:Body>
</S:Envelope>
```

I repeated the test for two more items to make sure the HashMap lookup
worked correctly:

| Item                | `<quantity>` returned |
|---------------------|-----------------------|
| `Laptops`           | 50                    |
| `Smartphones`       | 120                   |
| `NonExistentItem`   | 0                     |

### 7.2  Using SoapUI

1. Opened SoapUI → **File → New SOAP Project**.
2. **Project Name:** `StockServiceLab`.
3. **Initial WSDL:** `http://localhost:8080/StockService?wsdl`
   *(if deployed publicly the URL would be replaced with the public address)*.
4. Clicked **OK**. SoapUI imported the WSDL and generated a sample
   request envelope under
   `StockServiceSoapBinding → checkStock → Request 1`.
5. Replaced the placeholder `<item>?</item>` with `<item>Laptops</item>`.
6. Pressed the green ▶ **Submit** button.
7. SoapUI sent the SOAP request via HTTP POST and displayed the
   response in the right-hand panel — `<quantity>50</quantity>`.

> **Screenshots to attach:**
> 1. SoapUI showing the SOAP project tree with the imported WSDL.
> 2. SoapUI showing the request envelope with `<item>Laptops</item>`.
> 3. SoapUI showing the response envelope with `<quantity>50</quantity>`.

---

## 8. Conclusion

Putting the four pieces together gave a complete picture of how
classic web services work:

- **WSDL** describes the contract — the operations and the message
  shapes.
- **SOAP** is the wire format — XML envelopes carrying request/response
  bodies over HTTP.
- **UDDI** is the discovery layer — a registry where the provider
  publishes the service and clients look it up by name.
- **The client** (here SoapUI) reads the WSDL, builds an envelope,
  POSTs it, and parses the response.

The actual work that the service performs (a HashMap lookup) is
trivial on purpose — the lab is about understanding the surrounding
WSDL/SOAP/UDDI machinery, and that is what this implementation
demonstrates.

---

## Appendix A — Project layout

```
untitled1/
├── pom.xml                                       Maven build file
├── Dockerfile                                    Optional containerised deploy
├── README.md                                     How to build and run
├── LAB_REPORT.md                                 (this report)
├── uddi-registration.xml                         Step 3 — UDDI entry
└── src/main/
    ├── resources/
    │   └── StockService.wsdl                     Step 1 — hand-written WSDL
    └── java/org/example/
        ├── Main.java                             Step 2 — Endpoint.publish
        └── stock/
            ├── StockService.java                 Step 2 — SOAP interface
            └── StockServiceImpl.java             Step 2 — HashMap data
```

## Appendix B — How to reproduce

```bash
git clone https://github.com/r4nj4/se423-week11-stock-service-lab.git
cd se423-week11-stock-service-lab
mvn clean package -DskipTests
java -jar target/stock-service.jar
# WSDL is now live at http://localhost:8080/StockService?wsdl
```
