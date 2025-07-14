# Welcome to the Price Aggregator Project!

## Run Instructions

### Docker build/run (Optional)

If you wish to keep your local machine clean and you have docker installed:

- Spin up a docker container we can build and run in:

`docker run -p 8080:8080 --detach --name pricing-aggregator-build-run eclipse-temurin:21.0.7_6-jdk sh -c "tail -f /dev/null"`

- Access the running container:

`docker exec -it pricing-aggregator-build-run /bin/sh`

- Run the following commands:

`apt-get -y update && apt-get -y install git`

`git clone https://github.com/sam-utensity/price-aggregator-challenge.git`

`cd price-aggregator-challenge && ./gradlew bootJar`

`java -jar ./build/libs/aggregator.jar`

### Local Gradle/ Java

#### Prerequisites

- Java 21 installed on path
- If running local gradle then version required is 8.14.3
- This project cloned onto machine: `git clone https://github.com/sam-utensity/price-aggregator-challenge.git`
- gradlew has executable permissions (is already tracked as such, if something has gone wrong then use `chmod +x gradlew` )
- OPTIONAL: gradle installed as a path command

#### Instruction

- Open the terminal and navigate to project root
- Compile project: `./gradlew bootJar` (if gradle is pre-installed then `gradle bootjar`)
- Run executable jar: `java -jar ./build/libs/aggregator.jar`

# Configuration

To adjust the currency pairs tracked by this system please adjust the relevant property in `src/main/resources/application.yml`

Alternatively, pass in the required pairs via environment variable

# Calling the Aggregator API

NOTE: We will assume `localhost` is a safe local loopback address on your machine. Please adjust accordingly

NOTE 2: The service runs by default on port `8080`

## Example

`curl -H "Accept: application/json" http://localhost:8080/prices/BTC-USD`

## Detail

### Method

GET

### Slug

/prices/{symbol}

### Response Example

```json
{
  "price": 122107.00,
  "time": "2025-07-14T09:30:34.7493896Z"
}
```

### Response Missing Instrument Price

```json
{
  "status": 404,
  "error": "Missing",
  "message": "No symbol found for BTC-USZ",
  "path": "/prices/BTC-USZ"
}
```

### Response Error

```json
{
  "status": 400,
  "error": "Validation Failed",
  "message": "Path variable 'symbol' must be in the format 'AAA-BBB' using uppercase or lowercase letters.",
  "path": "/prices/BTC-USDd"
}
```

# Key Design Decisions

1) Scaffold extensible design from the get-go

    Adding additional base-quote pairs is trivial using the standardized properties injection as per application.yml
    
    Adding additional exchanges is relatively simple by following the same design as the Bitstamp websocket class and
    extending the abstract AbstractExchangeWebsocket class

    Use of standardised "Instrument" key ensures api and exchange services speak a unified language

    Rather than a complex adapter later the conversion of exchange-specific symbols is done via a lightweight mapping inside the websocket implementations.
    Mapping is non hardcoded and generated on application start

2) Use DI to share aggregator service allowing multiple exchanges to register prices.

3) Ensured decoupling of exchange domain specifics from application domain

4) Use thread safe in-memory object for pricing by way of ConcurrentHashMap. Spec did not call for a database layer

5) Added a little suger by way of a pre-loader for prices on subscription rather than forcing downstream services to wait for a price

# Considerations

## Scaling

See comment 1 in "Key Design Decisions" above. Design is build to allow adding additional pairs and exchanges with relative ease

Would need to add a new API path: `/prices/{exchange}/{symbol}` this would be trivial

Core throughput issues may well come from thread contention if too many pairs/ exchanges are added. We could increase the websocket thread pool or offload message processing to a seperate thread pool

When bandwidth or other bottle necks begin to become a concern we can explore horizontally scaling the application and relying on a centralised aggregation service backed by a message layer

NOTE: Pairs configured by way of application property are global. We may want to change this to per exchange incase an exchange does not support a desired pair

## Improvements

- Integration tests for thorough test coverage
- Github action scripts
- Push docker packages to github
- Add helm scripts using pushed packages
- Code style checks and dependency scanning checks
- Rollout the standardised APIError object to other spring mvc errors
- Observability and Auto documentation would also be nice


### Folder Structure

Chosen to do away with classic spring folder convention "controller", "service", "repository" etc.
Instead, going with a business driven/ domain structure
