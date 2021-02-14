# HealthBuddy

![](https://github.com/dschanoeh/HealthBuddy/workflows/build/badge.svg)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

HealthBuddy is a service that periodically queries health endpoints of one
or more services and generates alerts in case these queries fail.

It supports:
* Microsoft Teams channel webhooks
* Optional Basic Auth
* Operation through an HTTP proxy
* A configurable list of acceptable status codes

## Building

An execution of
```
./gradlew build  
```
will generate a fat jar located under build/libs/. 

## Configuration

Save the following as ```application.yaml``` next to the binary for execution:
```yaml
# The update interval (in ms)
updateInterval: 10000
# A list of services and their endpoints to be queried
services: 
  - name: my-service prod
    url: https://foo.bar/actuator/health
    allowedStatusCodes:
      - 200
  - name: other-service prod
    url: http://127.0.0.1
    allowedStatusCodes:
      - 200
    userName: basicAuthUser
    password: basicAuthPass
# The Teams webhook to be called for alerts
teams:
  webHookURL: http://127.0.0.1/hook
# Optional network configuration
network:
  httpProxyHost: 127.0.0.1
  httpProxyPort: 8080
  timeout: 5000
```

