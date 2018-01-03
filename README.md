# Streaming transaction statistics

This project implements REST API to aggregate in memory statistics for last time window about real time transactions.

## POST /transactions

```json
{
  "amount": 12.3,
  "timestamp": 1478192204000
}
```

Where:
- amount - transaction amount
- timestamp - transaction time in epoch in millis in UTC time zone (this is not current
timestamp)
Returns: Empty body with either 201 or 204.
- 201 - in case of success
- 204 - if transaction is older than 60 seconds

## GET /statistics

Returns:

```json
{
  "sum": 1000,
  "avg": 100,
  "max": 200,
  "min": 50,
  "count": 10
}
```

Where:
- sum is a double specifying the total sum of transaction value in the last 60 seconds
- avg is a double specifying the average amount of transaction value in the last 60
seconds
- max is a double specifying single highest transaction value in the last 60 seconds
- min is a double specifying single lowest transaction value in the last 60 seconds
- count is a long specifying the total number of transactions happened in the last 60
seconds
