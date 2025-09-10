# StockInfoApi

## Overview

**Note: This project has been built for educational purposes (non-commercial)**

This project fetches daily stock records from an Api (Alpha Vantage) and builds a time series of stock metric data.

*Any symbol that is accepted by Alpha Vantage will work in this API, eg: `IBM`*

```json
{
	"Meta Data": {
		"1. Information": "Daily Time Series with custom metrics",
		"2. Symbol": "IBM",
		"3. Last Refreshed": "2025-09-09",
		"4. Time Zone": "US/Eastern",
		"5. Record Count": 25
	},
	"Time Series (Daily)": {
		"2025-09-09": {
			"1. close": "259.11",
			"2. previousCloseChange": "3.02",
			"3. movingAverage(30d)": "245.95",
			"4. volatility(7d%)": "1.26",
			"5. momentum(14d%)": "7.39"
		},
		"2025-09-08": {
			"1. close": "256.09",
			"2. previousCloseChange": "7.56",
			"3. movingAverage(30d)": "246.09",
			"4. volatility(7d%)": "1.25",
			"5. momentum(14d%)": "6.95"
		},
```
- movingAverage(30d) -> the average price of the stock over the past 30 days
- volatility(7d%) -> the percentage volatility (standard deviation of daily returns) over the past 7 days
- momentum(14d%) -> the percentage price change over the past 7 days

## Running Locally

**Prerequisite: Docker + AWS CLI must be installed**

### 1. Obtain an API key
1. Follow the steps on site https://www.alphavantage.co/support/#api-key to gain access to a free API key (25 requests per day)
2. Copy your key into the `token` section of the `application.yml` file in this project (this should be the only change)

### 2. Start the docker containers
Use the helper script to start the spring app and dynamo db locally using docker:

```bash
./scripts/start-locally.sh
```

* Spring app runs in Docker on http://localhost:8080
* Dynamo instance runs in Docker on http://localhost:8000

### 3. Select a stock to analyse

Default API path (returns latest 25 metric records):

```
http://localhost:8080/stocks/{STOCK_SYMBOL}
```
Optional query parameter (returns latest 100 records):
```
http://localhost:8080/stocks/{STOCK_SYMBOL}?outputSize=full
```
