# 🏦 Java SDK for Enzona API

## Description
The Java SDK for Enzona API 🚀 offers developers a comprehensive toolset to interact seamlessly with the Enzona API, 🇨🇺 Cuba's sole payment platform. This SDK is designed to facilitate and automate financial transactions specifically within Java applications, providing a unique gateway 🌐 to advanced payment, refund, and financial management capabilities. By integrating with Enzona, developers can streamline complex processes inherent to the financial industry, leveraging the exclusive features of Cuba's premier financial transaction service.

## 🌟 Features
- **Payment Management**: Streamline the creation, confirmation, and cancellation of payments.
- **Refund Handling**: Efficiently manage the lifecycle of refunds.
- **Secure Authentication**: Leverage secure protocols for authentication and token management.
- **Comprehensive Documentation**: Access in-depth documentation for all modules and functionalities, ensuring a smooth integration experience.

## Useful Links
- [Maven Repository](https://central.sonatype.com/artifact/io.github.alejo2075/enzona-sdk)
- [API Documentation](https://alejo2075.github.io/enzona-sdk-java/)

## 🔧 Installation

### Prerequisites
Ensure you have Java 17 or higher 📥 installed on your system to utilize this SDK effectively.

### Adding the Dependency
Integrate this SDK into your Maven project by adding the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.alejo2075</groupId>
    <artifactId>enzona-sdk</artifactId>
    <version>0.9.5</version>
</dependency>
```


## 📚 Basic Usage
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

## 🤝 Contributing
We welcome contributions from the community to make this SDK even better! Here’s how you can contribute:

- Fork the repository.
- Create a feature branch: git checkout -b feature/fooBar
- Commit your changes after making sure tests pass.
- Push to the branch: git push origin feature/fooBar
- Submit a pull request.

### 📄 License
This project is licensed under the Apache License 2.0 - see the LICENSE file for more details.

### 💬 Support
Encounter any issues or have questions? Please open an issue in the GitHub repository issue tracker. Our team is dedicated to providing support and addressing your queries promptly.
