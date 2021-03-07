# HealthBuddy

![](https://github.com/dschanoeh/HealthBuddy/workflows/build/badge.svg)
![GitHub](https://img.shields.io/github/license/dschanoeh/HealthBuddy)

HealthBuddy is a service that periodically queries health endpoints of one
or more services and generates alerts in case these queries fail.

It supports:
* Microsoft Teams channel webhooks
* Optional Basic Auth
* Operation through an HTTP proxy
* A configurable list of acceptable status codes
* Spring boot actuator body evaluation based on a list of acceptable status

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
    allowedActuatorStatus:
      - UP
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

### Service Configuration
For each service, a name and a URL must be configured. Optionally, an environment can be
specified which will be included in alerts.

By default, an established connection to the health endpoint is deemed a success. Additionally,
lists of allowed HTTP status codes and lists of allowed actuator status can be specified that
will then also be evaluated.

The actuator status evaluation assumes the response body to be of the following form (following
the Spring Boot actuator schema):
```json
{"status":"UP"}
```

If user name and password (both or none must be present) are provided, HealthBuddy will perform
basic authentication when calling the health endpoint.

In addition to the config file, it is also possible to set parameters through the environment:
```shell
export TEAMS_WEBHOOKURL="http://127.0.0.1/hook"
```

### Proxy Configuration
A proxy can be provided through any of the following means which take precedence in the order
shown here:
1. Through the network section of the configuration file
2. Through Java system properties(`http.proxyHost`, `https.proxyHost` and corresponding 
   port, user and Password variables)
3. Through the environment variables HTTP_PROXY and HTTPS_PROXY