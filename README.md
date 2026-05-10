# SE423 Week 11 — Stock Inquiry SOAP Service

A JAX-WS based SOAP web service that lets retail stores query the
stock count of items held by a regional warehouse.  Built for the
SE423 *Enterprise Software Architecture* lab (Dr. Hoger Mahmud).

| Layer | Technology |
|-------|-------------|
| Description | WSDL 1.1 — `src/main/resources/StockService.wsdl` |
| Communication | SOAP 1.1 over HTTP (JAX-WS RI 4.0.2 / Eclipse Metro) |
| Discovery | Simulated UDDI v3 entry — `uddi-registration.xml` |
| Runtime | Java 17 fat-JAR, hosted on Render.com |

---

## Project layout

```
src/main/java/org/example/Main.java                Endpoint.publish bootstrap
src/main/java/org/example/stock/StockService.java  @WebService interface
src/main/java/org/example/stock/StockServiceImpl   HashMap-backed implementation
src/main/resources/StockService.wsdl               Hand-written WSDL (Step 1)
uddi-registration.xml                              Simulated UDDI entry (Step 3)
pom.xml                                            JAX-WS + shade plugin
```

## Build

```bash
mvn clean package -DskipTests
```

Produces `target/stock-service.jar` (runnable fat JAR).

## Run locally

```bash
java -jar target/stock-service.jar
```

The service listens on `http://localhost:8080/StockService`.
The live WSDL is served at `http://localhost:8080/StockService?wsdl`.

Override the port with the `PORT` env var (this is what Render injects):

```bash
PORT=9000 java -jar target/stock-service.jar
```

## Deployment to Render.com

1. Push this repo to GitHub.
2. On Render → **New + → Web Service** → connect the repo.
3. Settings:
   - **Environment**: `Docker` *or* `Native` (Native works — Render auto-detects Java/Maven).
   - **Build Command**: `mvn clean package -DskipTests`
   - **Start Command**: `java -jar target/stock-service.jar`
4. Deploy.  Render assigns a public URL like
   `https://stock-service-xxxx.onrender.com`.
5. The live WSDL is then at
   `https://stock-service-xxxx.onrender.com/StockService?wsdl`.

After deployment, edit `uddi-registration.xml` and replace
`YOUR-RENDER-APP` with your actual Render subdomain.

## Sample data

| Item | Quantity |
|------|----------|
| Laptops | 50 |
| Smartphones | 120 |
| Tablets | 35 |
| Monitors | 18 |
| Keyboards | 200 |

Unknown items return `0`.

## Sample SOAP request

```xml
<soapenv:Envelope
    xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
    xmlns:stk="http://stock.example.org/">
  <soapenv:Body>
    <stk:getStockRequest>
      <item>Laptops</item>
    </stk:getStockRequest>
  </soapenv:Body>
</soapenv:Envelope>
```

Sample response:

```xml
<S:Envelope xmlns:S="http://schemas.xmlsoap.org/soap/envelope/">
  <S:Body>
    <ns2:getStockResponse xmlns:ns2="http://stock.example.org/">
      <quantity>50</quantity>
    </ns2:getStockResponse>
  </S:Body>
</S:Envelope>
```

## Testing with SoapUI (Step 4)

1. **File → New SOAP Project**.
2. **Initial WSDL**: `https://<your-render-subdomain>.onrender.com/StockService?wsdl`.
3. SoapUI auto-generates a request — replace `?` with an item name
   (e.g. `Laptops`) inside `<item>`.
4. Click the green ▶ button → inspect the response envelope.