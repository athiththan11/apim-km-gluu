# Integrate Gluu as a Third-Party Key Manager with WSO2 API Manager

[:construction:] Work in-progress

A third-party key manager implementation that allows to integrate the WSO2 API Developer portal with Gluu authorization server to manage OAuth clients and tokens required by WSO2 API Manager. This repo contains a sample key manager (client) implementation that consumes the APIs exposed by Gluu.

> Kindly note that this implementation & feature is supported from WSO2 API Manager v3.2.0 onwards

## Getting Started

To get started, please refer to [Integrate Gluu with WSO2 API Manager](docs/README.md).

## Build

The main (master) branch contains the code related to APIM 3.2.0 version. To build the project, execute the following command from the root directory of the project

```sh
mvn clean install
```

Follow [Build & Deploy](docs/README.md#build--deploy) to deploy the JAR artifact in WSO2 API Manager server.

## Contributing

To contribute and develop the Gluu key manager implementation, please fork the GitHub repository and send your Pull-Requests to [athiththan11/apim-km-gluu](https://github.com/athiththan11/apim-km-gluu)

## License

[Apache 2.0](LICENSE)
