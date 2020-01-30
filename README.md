# bank-transfer-api

## Last build status:
[![CircleCI](https://circleci.com/gh/allanneves/money-transfer-api/tree/master.svg?style=svg)](https://circleci.com/gh/allanneves/money-transfer-api/tree/master)

## Tech Stack:
![Java](https://img.shields.io/badge/Java-1.8-red.svg?style=plastic)
![Play](https://img.shields.io/badge/Play%20Framework-2.5.9-green.svg?style=plastic)

Play is a solid framework with full support to reactive programming (asynchronous and non-blocking). Its hot reload feature makes the development easier and it plays well with both Java and Scala.

![H2](https://img.shields.io/badge/h2Database-1.4.192-blue.svg?style=plastic)

As we need an in-memory database my first choice would be Redis. However, Play offers support to H2 out of the box and its libraries and implementations for H2 are more mature compared to Redis.

Play has a solid integration with Java Persistence API (JPA) for the Java ORM standard for storing, accessing, and managing Java objects in a relational database

![Lombok](https://img.shields.io/badge/lombok-1.18.2-blue.svg?style=plastic)

Lombok reduces the code ceremony required by Java to do simple tasks and offers top-notch implementations of some of the most common patterns and tasks that we implement over and over such as Builders, Logs and HashCode.

## Test coverage
Integration tests are added to cover controllers and unit tests are added to cover other classes.

To run test cases, just do -
<kbd>>sbt clean</kbd>
<kbd>>sbt compile</kbd>
<kbd>>sbt test</kbd>
<kbd>>sbt jacoco</kbd>

A jacoco report would be generated in the target folder.

```
[info] ------- Jacoco Coverage Report -------
[info] 
[info] Lines: 100% (>= required 0.0%) covered, 0 of 209 missed, OK
[info] Instructions: 95.78% (>= required 0.0%) covered, 53 of 1255 missed, OK
[info] Branches: 100% (>= required 0.0%) covered, 0 of 38 missed, OK
[info] Methods: 89.66% (>= required 0.0%) covered, 12 of 116 missed, OK
[info] Complexity: 91.11% (>= required 0.0%) covered, 12 of 135 missed, OK
[info] Class: 100% (>= required 0.0%) covered, 0 of 26 missed, OK
[info] 
```

#### The current line, branch and class coverage is 100%


## Challenges in the task

1. To avoid deadlock while making transfers between User A to User B and User B to User A at the same time. 
	- handled by taking locks always in the same order using account id
2. Handling edge cases like negative amount transfer, double payment, thread synchronisation to get account details


## Endpoints:
### ![POST](https://img.shields.io/badge/POST-red.svg?style=plastic) - Transfers money between two accounts

```
http://localhost:9000/transfer
```
```json
curl --location --request POST 'http://localhost:9000/transfer' \
--header 'Content-Type: application/json' \
--data-raw '{
    "fromAccountId": 19283751,
    "toAccountId": 19283752,
    "amount": 1000,
    "currency": "SGD"
}'
```


### ![GET](https://img.shields.io/badge/GET-red.svg?style=plastic) - Get Transaction logs for an account

```
http://localhost:9000/customer/:customerId/logs/:accountId
```
```json
curl --location --request GET 'http://localhost:9000/customer/5/logs/19283751' \
--header 'Content-Type: application/json'
```

### ![GET](https://img.shields.io/badge/GET-red.svg?style=plastic) - Get account details for a customer

```
http://localhost:9000/customer/:customerId/accounts
```
```json
curl --location --request GET 'http://localhost:9000/customer/5/accounts' \
--header 'Content-Type: application/json'
```

### ![POST](https://img.shields.io/badge/POST-red.svg?style=plastic) - Onboard a new customer

```
http://localhost:9000/customer/onboard
```
```json
curl --location --request POST 'http://localhost:9000/customer/onboard' \
--header 'Content-Type: application/json' \
--data-raw '{
	"firstName": "Justin",
	"lastName": "Bieber",
	"accounts": [{
		"balance": 1000,
		"currency": "EUR"
	}]
}'
```

## Running:
The server runs on port 9000.
### Standalone server:
1) Download the bank-transfer-api-1.0.zip file available in the root of the project. Alternatively, the zip file will be downloaded automatically when cloning the repo.
2) Extract the zip file in a folder of your preference. The Unix command line should look like this: <kbd>> unzip bank-transfer-api-1.0.zip</kbd>
3) You can either navigate to the extracted folder or execute the app from the folder you are. If you choose the latter, the command line for a Unix system will be similar to this: <kbd>> bank-transfer-api-1.0/bin/bank-transfer-api -Dplay.http.secret.key=ankur</kbd>

If you are running on Unix it is possible that you need to set the scripts permission to execute it. Otherwise, the system will block it and a "permission denied" message will be shown.

In memory db will be initialised with following tables -

```
                    ACCOUNTS
ID  	      BALANCE  	CURRENCY  	CUSTOMER_ID
19283746	  10000.00	   SGD	        1
19283747	  20000.00	   USD	        2
19283748	  30000.00	   EUR	        3
19283749	  40000.00	   USD	        4
19283750	  22000.00	   SGD	        4
19283751	  50000.00	   SGD	        5
19283752	  67000.00	   SGD	        5
```

```
                                  CUSTOMERS
ID  	CREATED_AT  	      UPDATED_AT  	FIRSTNAME  	    LASTNAME  
1	2020-01-30 12:05:22.695	  null	        Christopher   Williams
2	2020-01-30 12:05:22.723	  null	        Joseph	      Taylor
3	2020-01-30 12:05:22.724	  null	        Daniel	      Brown
4	2020-01-30 12:05:22.724	  null	        Joshua	      Johnson
5	2020-01-30 12:05:22.725	  null	        Matthew	      Miller
```
