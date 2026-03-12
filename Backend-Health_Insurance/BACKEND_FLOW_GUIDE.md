# 🏥 Health Insurance Backend - Complete Flow Guide

## 📚 Table of Contents
1. [Project Overview](#project-overview)
2. [Technology Stack](#technology-stack)
3. [Architecture Layers](#architecture-layers)
4. [Database Design](#database-design)
5. [Complete User Flows](#complete-user-flows)
6. [Security & Authentication](#security--authentication)
7. [API Endpoints Summary](#api-endpoints-summary)

---

## 🎯 Project Overview

**HealthShield** is a Corporate Health Insurance Management System that handles:
- Customer registration and policy purchases
- Premium calculation and quotes
- Claims filing and processing
- Multi-role user management (Admin, Customer, Underwriter, Claims Officer)

---

## 🛠️ Technology Stack

### Core Technologies
- **Spring Boot 3.4.5** - Main framework
- **Java 21** - Programming language
- **H2 Database** - In-memory database (for development)
- **MySQL** - Production database support
- **Spring Security** - Authentication & Authorization
- **JWT (JSON Web Tokens)** - Stateless authentication
- **Lombok** - Reduces boilerplate code
- **Spring Data JPA** - Database operations
- **Swagger/OpenAPI** - API documentation

### Key Dependencies
```xml
- spring-boot-starter-web (REST APIs)
- spring-boot-starter-data-jpa (Database)
- spring-boot-starter-security (Security)
- spring-boot-starter-validation (Input validation)
- jjwt (JWT tokens)
- springdoc-openapi (API docs)
```

---

## 🏗️ Architecture Layers

The application follows **Layered Architecture**:

```
┌─────────────────────────────────────┐
│   CONTROLLER LAYER (REST APIs)      │  ← Handles HTTP requests
├─────────────────────────────────────┤
│   SERVICE LAYER (Business Logic)    │  ← Core business rules
├─────────────────────────────────────┤
│   REPOSITORY LAYER (Data Access)    │  ← Database operations
├─────────────────────────────────────┤
│   ENTITY LAYER (Database Models)    │  ← JPA entities
└─────────────────────────────────────┘
```

### 1️⃣ **Entity Layer** (Database Models)
Location: `com.healthshield.entity`

**Core Entities:**
- **User** (Parent class) - Base user with authentication
  - Customer (extends User)
  - Admin (extends User)
  - Underwriter (extends User)
  - ClaimsOfficer (extends User)
  - Agent (extends User) - Currently commented out

- **InsurancePlan** - Available insurance plans
- **Policy** - Purchased policies
- **PolicyMember** - Family members covered
- **Claim** - Insurance claims
- **ClaimDocument** - Claim supporting documents
- **Payment** - Payment records
- **PremiumQuote** - Premium calculations
- **NetworkHospital** - Approved hospitals
- **AuditLog** - System audit trail

**Key Concept: Single Table Inheritance**
```java
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "user_type")
public class User { ... }

@Entity
@DiscriminatorValue("CUSTOMER")
public class Customer extends User { ... }
```
All user types stored in ONE table with `user_type` column distinguishing them.

### 2️⃣ **Repository Layer** (Data Access)
Location: `com.healthshield.repository`

Uses **Spring Data JPA** - no need to write SQL!

```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

Spring automatically implements these methods!

### 3️⃣ **Service Layer** (Business Logic)
Location: `com.healthshield.service`

Contains all business rules:
- **AuthService** - Registration, Login
- **PolicyService** - Policy purchase, renewal, cancellation
- **ClaimService** - Claim filing, processing
- **AdminService** - User management, assignments
- **UnderwriterService** - Premium quotes
- **ClaimsOfficerService** - Claim reviews
- **PaymentService** - Payment processing
- **InsurancePlanService** - Plan management

### 4️⃣ **Controller Layer** (REST APIs)
Location: `com.healthshield.controller`

Exposes HTTP endpoints:
- **AuthController** - `/api/auth/*`
- **PolicyController** - `/api/policies/*`
- **ClaimController** - `/api/claims/*`
- **AdminController** - `/api/admin/*`
- **UnderwriterController** - `/api/underwriter/*`
- **ClaimsOfficerController** - `/api/claims-officer/*`

### 5️⃣ **DTO Layer** (Data Transfer Objects)
Location: `com.healthshield.dto`

**Request DTOs** - Data coming FROM frontend
**Response DTOs** - Data going TO frontend

Why? Separates internal entities from external API contracts.

### 6️⃣ **Config Layer** (Configuration)
Location: `com.healthshield.config`

- **SecurityConfig** - Security rules, CORS, JWT filter
- **JwtAuthFilter** - Intercepts requests, validates JWT
- **OpenApiConfig** - Swagger documentation setup
- **DataInitializer** - Seeds initial data on startup

---

## 🗄️ Database Design

### User Hierarchy (Single Table)
```
users table
├── user_id (PK)
├── user_type (CUSTOMER, ADMIN, UNDERWRITER, CLAIMS_OFFICER)
├── email (unique)
├── password (encrypted)
├── first_name, last_name, phone
├── is_active
└── created_at

Customer-specific columns:
├── date_of_birth
├── gender
├── address, city, state, pincode

Underwriter-specific columns:
├── license_number
├── specialization
├── commission_rate

ClaimsOfficer-specific columns:
├── employee_id
├── department
```

### Key Relationships

**Policy → User (Customer)**
- One customer can have many policies
- `@ManyToOne` relationship

**Policy → InsurancePlan**
- Each policy is based on one plan
- `@ManyToOne` relationship

**Policy → PolicyMember**
- One policy can cover multiple family members
- `@OneToMany` relationship

**Policy → Claim**
- One policy can have multiple claims
- `@OneToMany` relationship

**Claim → ClaimDocument**
- One claim can have multiple documents
- `@OneToMany` relationship

**Policy → Underwriter (assigned)**
- Admin assigns underwriter to calculate premium
- `@ManyToOne` relationship

**Claim → ClaimsOfficer (assigned)**
- Admin assigns officer to review claim
- `@ManyToOne` relationship

---

## 🔄 Complete User Flows

### Flow 1: Customer Registration & Login

**Step 1: Registration**
```
Frontend → POST /api/auth/register
         ↓
AuthController.register()
         ↓
AuthService.register()
         ↓
1. Check if email exists
2. Create Customer entity
3. Encrypt password (BCrypt)
4. Save to database
5. Generate JWT token
         ↓
Return: AuthResponse (token, userId, role)
```

**Step 2: Login**
```
Frontend → POST /api/auth/login
         ↓
AuthController.login()
         ↓
AuthService.login()
         ↓
1. Find user by email
2. Verify password
3. Check if account is active
4. Generate JWT token
         ↓
Return: AuthResponse (token, userId, role)
```

**JWT Token Structure:**
```
Header: { "alg": "HS256", "typ": "JWT" }
Payload: { "sub": "user@email.com", "role": "CUSTOMER", "exp": 1234567890 }
Signature: HMACSHA256(header + payload, secret)
```

---

### Flow 2: Policy Purchase Journey

**Step 1: Browse Insurance Plans**
```
Frontend → GET /api/plans
         ↓
InsurancePlanController.getAllPlans()
         ↓
InsurancePlanService.getAllActivePlans()
         ↓
Return: List of available plans
```

**Step 2: Calculate Premium Quote**
```
Frontend → POST /api/premium/calculate
         ↓
PremiumQuoteController.calculatePremium()
         ↓
PremiumQuoteService.calculatePremium()
         ↓
1. Get plan details
2. Calculate base premium
3. Apply age factors
4. Apply member count factors
5. Apply pre-existing disease factors
         ↓
Return: PremiumQuoteResponse (estimated premium)
```

**Premium Calculation Logic:**
```java
Base Premium = Plan's base premium
+ (Age factor × base premium)
+ (Number of members × member rate)
+ (Pre-existing diseases × risk premium)
= Total Premium
```

**Step 3: Purchase Policy**
```
Frontend → POST /api/policies
         ↓
PolicyController.purchasePolicy()
         ↓
PolicyService.purchasePolicy()
         ↓
1. Validate plan exists and is active
2. Create Policy entity (status = PENDING)
3. Generate unique policy number
4. Add policy members
5. Save to database
         ↓
Policy Status: PENDING (awaiting admin assignment)
         ↓
Return: PolicyResponse
```

**Step 4: Admin Assigns Underwriter**
```
Admin → POST /api/admin/policies/{id}/assign-underwriter
         ↓
AdminController.assignUnderwriter()
         ↓
AdminService.assignUnderwriter()
         ↓
1. Find policy (must be PENDING)
2. Find underwriter
3. Assign underwriter to policy
4. Update policy status to UNDER_REVIEW
5. Log audit trail
         ↓
Policy Status: UNDER_REVIEW
```

**Step 5: Underwriter Calculates Quote**
```
Underwriter → POST /api/underwriter/quotes/{policyId}
         ↓
UnderwriterController.submitQuote()
         ↓
UnderwriterService.submitQuote()
         ↓
1. Validate policy is assigned to this underwriter
2. Calculate detailed premium
3. Create PremiumQuote entity
4. Update policy with quote amount
5. Update policy status to QUOTED
         ↓
Policy Status: QUOTED (customer can now pay)
```

**Step 6: Customer Makes Payment**
```
Customer → POST /api/payments
         ↓
PaymentController.makePayment()
         ↓
PaymentService.processPayment()
         ↓
1. Validate policy exists and is QUOTED
2. Create Payment entity
3. Process payment (integration point)
4. Update policy status to ACTIVE
5. Set policy start/end dates
         ↓
Policy Status: ACTIVE ✅
```

---

### Flow 3: Claims Processing Journey

**Step 1: Customer Files Claim**
```
Frontend → POST /api/claims (multipart/form-data)
         ↓
ClaimController.fileClaim()
         ↓
ClaimService.fileClaim()
         ↓
1. Validate policy is ACTIVE
2. Check remaining coverage
3. Create Claim entity (status = SUBMITTED)
4. Generate unique claim number
5. Upload documents to file system
6. Save ClaimDocument entities
         ↓
Claim Status: SUBMITTED (awaiting admin assignment)
         ↓
Return: ClaimResponse
```

**Document Upload:**
```
Files saved to: uploads/claims/{claimNumber}/filename.pdf
Database stores: file path, type, upload date
```

**Step 2: Admin Assigns Claims Officer**
```
Admin → POST /api/admin/claims/{id}/assign-officer
         ↓
AdminController.assignClaimsOfficer()
         ↓
AdminService.assignClaimsOfficer()
         ↓
1. Find claim (must be SUBMITTED)
2. Find claims officer
3. Assign officer to claim
4. Update claim status to UNDER_REVIEW
5. Set reviewStartedAt timestamp
         ↓
Claim Status: UNDER_REVIEW
```

**Step 3: Claims Officer Reviews Claim**
```
Officer → POST /api/claims-officer/claims/{id}/review
         ↓
ClaimsOfficerController.reviewClaim()
         ↓
ClaimsOfficerService.reviewClaim()
         ↓
1. Validate claim is assigned to this officer
2. Review documents
3. Validate claim amount
4. Make decision: APPROVED or REJECTED
5. Set approved amount (if approved)
6. Add reviewer remarks
7. Update claim status
8. Set reviewedAt timestamp
         ↓
Claim Status: APPROVED or REJECTED
```

**Approval Logic:**
```java
if (claimAmount <= policy.remainingCoverage) {
    if (documentsValid && diagnosisValid) {
        status = APPROVED
        approvedAmount = claimAmount (or adjusted)
    }
} else {
    status = REJECTED
    reason = "Insufficient coverage"
}
```

**Step 4: Settlement (if approved)**
```
System/Admin → Settle Claim
         ↓
ClaimService.settleClaim()
         ↓
1. Calculate settlement amount
2. Apply co-pay/deductibles
3. Update claim status to SETTLED
4. Update policy.totalClaimedAmount
5. Update policy.remainingCoverage
6. Set settlementDate
         ↓
Claim Status: SETTLED ✅
Policy Coverage: REDUCED
```

**Settlement Calculation:**
```java
settlementAmount = approvedAmount - coPay - deductible
policy.remainingCoverage -= settlementAmount
policy.totalClaimedAmount += settlementAmount
```

---

### Flow 4: Policy Renewal

**When Policy Expires:**
```
Policy Status: EXPIRED (when endDate < today)
```

**Customer Initiates Renewal:**
```
Customer → POST /api/policies/{id}/renew
         ↓
PolicyController.renewPolicy()
         ↓
PolicyService.renewPolicy()
         ↓
1. Validate original policy exists
2. Check if eligible for renewal
3. Calculate No-Claim Bonus (if no claims)
4. Create NEW policy (linked to original)
5. Apply discount based on NCB
6. Increment renewalCount
7. Set status to PENDING
         ↓
New Policy Created (goes through same flow)
```

**No-Claim Bonus Logic:**
```java
if (policy.claims.isEmpty() || allClaimsRejected) {
    noClaimBonus = 5% to 20% (increases each year)
    newPremium = oldPremium × (1 - noClaimBonus)
}
```

---

## 🔐 Security & Authentication

### JWT Authentication Flow

**1. User Logs In:**
```
POST /api/auth/login
→ Validates credentials
→ Generates JWT token
→ Returns token to client
```

**2. Client Stores Token:**
```javascript
localStorage.setItem('token', response.token)
```

**3. Subsequent Requests:**
```
Every API call includes:
Header: Authorization: Bearer <JWT_TOKEN>
```

**4. Server Validates Token:**
```
Request → JwtAuthFilter
         ↓
1. Extract token from header
2. Validate token signature
3. Check expiration
4. Extract user email
5. Load user from database
6. Set authentication in SecurityContext
         ↓
Request proceeds to controller
```

### Security Configuration

**Public Endpoints (No Auth Required):**
- `/api/auth/**` - Login, Register
- `/api/plans/**` (GET) - Browse plans
- `/api/premium/calculate` - Calculate quotes
- `/h2-console/**` - Database console
- `/swagger-ui/**` - API docs

**Protected Endpoints (Auth Required):**
- `/api/admin/**` - ROLE_ADMIN only
- `/api/underwriter/**` - ROLE_UNDERWRITER only
- `/api/claims-officer/**` - ROLE_CLAIMS_OFFICER only
- `/api/policies/**` - ROLE_CUSTOMER (mostly)
- `/api/claims/**` - ROLE_CUSTOMER (mostly)

**Role-Based Access Control:**
```java
@PreAuthorize("hasRole('CUSTOMER')")
public ResponseEntity<PolicyResponse> purchasePolicy() { ... }

@PreAuthorize("hasAnyRole('ADMIN','UNDERWRITER')")
public ResponseEntity<List<PolicyResponse>> getAllPolicies() { ... }
```

### Password Security
```java
// Registration: Encrypt password
String encrypted = passwordEncoder.encode(plainPassword);
// Uses BCrypt with salt

// Login: Verify password
boolean matches = passwordEncoder.matches(plainPassword, encryptedPassword);
```

---

## 📡 API Endpoints Summary

### Authentication APIs
```
POST   /api/auth/register          - Customer registration
POST   /api/auth/login             - User login
```

### Insurance Plan APIs
```
GET    /api/plans                  - Get all active plans
GET    /api/plans/{id}             - Get plan by ID
POST   /api/admin/plans            - Create plan (Admin)
PUT    /api/admin/plans/{id}       - Update plan (Admin)
PATCH  /api/admin/plans/{id}/deactivate - Deactivate plan
```

### Premium Quote APIs
```
POST   /api/premium/calculate      - Calculate premium quote
```

### Policy APIs
```
POST   /api/policies               - Purchase policy (Customer)
GET    /api/policies/my-policies   - Get my policies (Customer)
GET    /api/policies/{id}          - Get policy details
GET    /api/policies/{id}/members  - Get policy members
POST   /api/policies/{id}/renew    - Renew policy
PATCH  /api/policies/{id}/cancel   - Cancel policy
GET    /api/policies               - Get all policies (Admin)
```

### Claim APIs
```
POST   /api/claims                 - File claim (Customer)
GET    /api/claims/my-claims       - Get my claims (Customer)
GET    /api/claims/{id}            - Get claim details
GET    /api/claims                 - Get all claims (Admin)
GET    /api/claims/pending         - Get pending claims
GET    /api/claims/status/{status} - Get claims by status
GET    /api/claims/{id}/documents  - Get claim documents
PATCH  /api/claims/{id}/status     - Update claim status (Admin)
```

### Admin APIs
```
GET    /api/admin/dashboard        - Dashboard statistics
POST   /api/admin/create-underwriter - Create underwriter
POST   /api/admin/create-claims-officer - Create claims officer
GET    /api/admin/customers        - Get all customers
GET    /api/admin/underwriters     - Get all underwriters
GET    /api/admin/claims-officers  - Get all claims officers
PATCH  /api/admin/users/{id}/deactivate - Deactivate user
PATCH  /api/admin/users/{id}/activate - Activate user

POST   /api/admin/policies/{id}/assign-underwriter - Assign underwriter
GET    /api/admin/pending-applications - Get pending policies

POST   /api/admin/claims/{id}/assign-officer - Assign claims officer
GET    /api/admin/submitted-claims - Get submitted claims
```

### Underwriter APIs
```
GET    /api/underwriter/dashboard  - Dashboard stats
GET    /api/underwriter/assigned-policies - Get assigned policies
POST   /api/underwriter/quotes/{policyId} - Submit quote
GET    /api/underwriter/quotes     - Get all quotes
```

### Claims Officer APIs
```
GET    /api/claims-officer/dashboard - Dashboard stats
GET    /api/claims-officer/assigned-claims - Get assigned claims
POST   /api/claims-officer/claims/{id}/review - Review claim
GET    /api/claims-officer/claims/{id} - Get claim details
```

### Payment APIs
```
POST   /api/payments               - Make payment
GET    /api/payments/my-payments   - Get my payments
GET    /api/payments/{id}          - Get payment details
```

---

## 🎯 Key Concepts Explained

### 1. **Dependency Injection**
Spring automatically creates and injects objects:
```java
@Service
@RequiredArgsConstructor  // Lombok generates constructor
public class PolicyService {
    private final PolicyRepository policyRepository;  // Auto-injected
    private final UserRepository userRepository;      // Auto-injected
}
```

### 2. **JPA Relationships**
```java
// One policy has many members
@OneToMany(mappedBy = "policy", cascade = CascadeType.ALL)
private List<PolicyMember> members;

// Many members belong to one policy
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "policy_id")
private Policy policy;
```

### 3. **Enums for Type Safety**
```java
public enum PolicyStatus {
    PENDING,        // Awaiting underwriter assignment
    UNDER_REVIEW,   // Underwriter reviewing
    QUOTED,         // Quote provided, awaiting payment
    ACTIVE,         // Policy active
    EXPIRED,        // Policy expired
    CANCELLED       // Policy cancelled
}
```

### 4. **DTOs vs Entities**
```java
// Entity (Database model)
@Entity
public class Policy {
    private Long policyId;
    private User user;  // Full user object
    // ... many fields
}

// Response DTO (API response)
public class PolicyResponse {
    private Long policyId;
    private String customerName;  // Only name, not full user
    // ... only needed fields
}
```

### 5. **Exception Handling**
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(404).body(new ErrorResponse(ex.getMessage()));
    }
}
```

---

## 🚀 Application Startup Flow

**1. Spring Boot Starts:**
```
HealthInsuranceApplication.main()
→ SpringApplication.run()
→ Scans @Component, @Service, @Repository, @Controller
→ Creates beans (objects)
→ Injects dependencies
```

**2. Database Initialization:**
```
application.yaml: ddl-auto: create-drop
→ Drops existing tables
→ Creates tables from @Entity classes
→ DataInitializer runs
→ Seeds initial data (admin, plans, etc.)
```

**3. Security Setup:**
```
SecurityConfig loads
→ Configures JWT filter
→ Sets up authentication
→ Defines access rules
```

**4. Server Ready:**
```
Tomcat started on port 8080
Application ready to accept requests
```

---

## 📊 Data Flow Example: Filing a Claim

```
┌─────────────┐
│  Frontend   │
│  (React)    │
└──────┬──────┘
       │ POST /api/claims
       │ Headers: Authorization: Bearer <token>
       │ Body: { policyId, claimAmount, diagnosis, ... }
       │ Files: [medical_bill.pdf, prescription.pdf]
       ↓
┌──────────────────────────────────────────────┐
│  JwtAuthFilter                               │
│  1. Extract token                            │
│  2. Validate token                           │
│  3. Load user from database                  │
│  4. Set SecurityContext                      │
└──────┬───────────────────────────────────────┘
       ↓
┌──────────────────────────────────────────────┐
│  ClaimController                             │
│  @PostMapping                                │
│  @PreAuthorize("hasRole('CUSTOMER')")        │
│  fileClaim(@AuthenticationPrincipal User)    │
└──────┬───────────────────────────────────────┘
       ↓
┌──────────────────────────────────────────────┐
│  ClaimService                                │
│  1. Validate policy exists                   │
│  2. Check policy is ACTIVE                   │
│  3. Check remaining coverage                 │
│  4. Generate claim number (CLM-2024-123456)  │
│  5. Create Claim entity                      │
│  6. Save claim to database                   │
│  7. Upload documents to file system          │
│  8. Create ClaimDocument entities            │
│  9. Save documents to database               │
└──────┬───────────────────────────────────────┘
       ↓
┌──────────────────────────────────────────────┐
│  ClaimRepository                             │
│  save(claim) → INSERT INTO claims ...        │
└──────┬───────────────────────────────────────┘
       ↓
┌──────────────────────────────────────────────┐
│  Database (H2)                               │
│  claims table: New row inserted              │
│  claim_documents table: New rows inserted    │
└──────┬───────────────────────────────────────┘
       ↓
┌──────────────────────────────────────────────┐
│  ClaimService                                │
│  Convert Claim entity → ClaimResponse DTO    │
└──────┬───────────────────────────────────────┘
       ↓
┌──────────────────────────────────────────────┐
│  ClaimController                             │
│  return ResponseEntity.ok(claimResponse)     │
└──────┬───────────────────────────────────────┘
       ↓
┌─────────────┐
│  Frontend   │
│  Receives:  │
│  {          │
│    claimId, │
│    claimNumber,│
│    status,  │
│    ...      │
│  }          │
└─────────────┘
```

---

## 🎓 Learning Tips

### Start Here:
1. **Understand Entities** - Look at `entity` package
2. **Follow One Flow** - Pick "Customer Registration" and trace it
3. **Read Controllers** - See what APIs are available
4. **Check Services** - Understand business logic
5. **Test APIs** - Use Swagger UI at `http://localhost:8080/swagger-ui.html`

### Debugging Tips:
```yaml
# application.yaml
spring:
  jpa:
    show-sql: true  # See SQL queries in console
    properties:
      hibernate:
        format_sql: true  # Pretty print SQL
```

### Common Patterns:
```java
// 1. Find or throw exception
Policy policy = policyRepository.findById(id)
    .orElseThrow(() -> new ResourceNotFoundException("Policy not found"));

// 2. Check authorization
if (!policy.getUser().getUserId().equals(user.getUserId())) {
    throw new UnauthorizedException("Not your policy");
}

// 3. Update and save
policy.setPolicyStatus(PolicyStatus.ACTIVE);
policyRepository.save(policy);

// 4. Convert entity to DTO
return PolicyResponse.builder()
    .policyId(policy.getPolicyId())
    .policyNumber(policy.getPolicyNumber())
    .build();
```

---

## 🎉 Summary

**This backend handles:**
✅ User authentication with JWT
✅ Role-based access control
✅ Policy lifecycle management
✅ Claims processing workflow
✅ File uploads
✅ Premium calculations
✅ Payment processing
✅ Audit logging
✅ Multi-user workflows (Admin assigns → Underwriter quotes → Customer pays)

**Key Strengths:**
- Clean layered architecture
- Secure authentication
- Comprehensive business logic
- Well-structured database design
- RESTful API design

**Next Steps:**
1. Run the application
2. Access Swagger UI: `http://localhost:8080/swagger-ui.html`
3. Test APIs using Swagger
4. Check H2 Console: `http://localhost:8080/h2-console`
5. Trace one complete flow end-to-end

---

**Happy Learning! 🚀**
