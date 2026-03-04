# RevVault: Advanced Enterprise-Grade Password Manager

RevVault is a secure, specialized password management system designed to provide users with complete control over their digital credentials. Built on the robust Spring Boot framework and backed by Oracle Database, RevVault ensures that sensitive information remains protected, accessible, and well-managed through industry-standard encryption and multi-layered security.

---

## 📄 Abstract
In an era of increasing cyber threats and frequent data breaches, the need for secure and reliable credential management has never been more critical. **RevVault** address this challenge by providing a centralized, local-first repository for sensitive account information.

The system utilizes **AES-256 (Advanced Encryption Standard)** for data at rest and **BCrypt** for secure authentication. To mitigate risks of unauthorized access, RevVault implements a multi-path identity verification system involving **Email-based Two-Factor Authentication (2FA)** and encrypted **Security Questions**. Beyond simple storage, the application features an intelligent **Security Audit Engine** that actively monitors password health, identifying weak or aging credentials and empowering users with a customizable **Password Generator** to create high-entropy alternatives.

By combining enterprise-grade security with an intuitive user interface, RevVault bridges the gap between complex cryptographic requirements and user-friendly accessibility, serving as a definitive "digital safe" for modern personal and professional digital identities.

---

## 🌟 Key Features

*   **Secure Vault Storage**: Industry-standard AES-256 encryption ensures your passwords are safe even if the database is compromised.
*   **Multi-Factor Authentication (MFA)**: Enhanced login security via mandatory Email OTP and secondary Security Questions.
*   **Security Health Audit**: Automated analysis of password strength, age, and reuse to keep your digital life secure.
*   **High-Entropy Password Generator**: Create complex, customizable passwords to replace weak or compromised ones.
*   **Identity-Verified Recovery**: A robust recovery flow that ensures users never lose access while maintaining strict identity checks.
*   **Performance Tracking**: Integrated logging and auditing for all security-sensitive operations.

---

## 🛠️ Technology Stack

*   **Backend**: [Spring Boot 3.x](https://spring.io/projects/spring-boot) (JPA, Security, Mail, Validation)
*   **Database**: [Oracle Database 19c/XE](https://www.oracle.com/database/)
*   **Security Layer**: Spring Security, JWT (API), BCrypt, AES-256
*   **Frontend**: Thymeleaf, Vanilla CSS, JavaScript
*   **Build Tool**: Maven

---

## 📐 System Architecture

RevVault follows a modern N-Tier architecture to ensure scalability and separation of concerns:

1.  **Presentation Layer**: Responsive Thymeleaf templates and RESTful API endpoints.
2.  **Service Layer**: Core business logic including Encryption, Security Audit, and User Management.
3.  **Security Layer**: Managed by Spring Security, providing authentication and MFA challenges.
4.  **Data Layer**: Spring Data JPA interacting with a persistent Oracle Database.
