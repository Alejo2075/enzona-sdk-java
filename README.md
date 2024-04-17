# Enzona SDK for Java

## Description
The Enzona SDK for Java provides developers with a streamlined way to interact with the Enzona API, enabling the integration of payment functionalities into Java applications. This SDK simplifies the process of making API calls and provides tools for managing payments, refunds, and other financial transactions.

## Useful Links
- [Maven Repository](https://central.sonatype.com/artifact/io.github.alejo2075/enzona-sdk)
- [API Documentation](https://your-username.github.io/your-repository/)

## Features
- **Payment Management**: Easily create, confirm, and cancel payments.
- **Refund Handling**: Manage the creation and processing of refunds efficiently.
- **Secure Authentication**: Handles authentication and token management securely.
- **Comprehensive Documentation**: Detailed documentation is provided for each module and its functionalities.

## Installation

### Prerequisites
Make sure you have Java 17 or higher installed on your system to use this SDK.

### Adding the Dependency
To include this SDK in your Maven project, add the following dependency to your `pom.xml`:

``` xml
<dependency>
    <groupId>com.desoft</groupId>
    <artifactId>enzona-sdk</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Basic Usage
### Initial Setup
Configure the client with your credentials before making any API calls:

``` java
AuthClient authClient = new AuthClient("yourConsumerKey", "yourConsumerSecret");
PaymentApi paymentApi = new PaymentApi(authClient);
```

### Making a Payment
Example of initiating a payment:

``` java
CreatePaymentRequest request = new CreatePaymentRequest();
// Set up payment details
CreatePaymentResponse response = paymentApi.createPayment(request);
System.out.println("Payment Status: " + response.getStatus());
```

## Contributing
Contributions are welcome to improve this SDK. To contribute:

### Fork the repository.
Create a feature branch (git checkout -b feature/fooBar).
Make your changes and ensure tests pass.
Submit a pull request.

### License
This project is licensed under the Apache License 2.0 - see the LICENSE file for more details.

## Support
For any issues or questions regarding the SDK, please open an issue in the GitHub repository's issue tracker.