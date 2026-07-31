# FileVault - Secure Content Sharing and Access Management System

## Project Overview

**FileVault** is a comprehensive secure, role-based file sharing and access management platform that enables **admins** (content creators) to upload and manage files across multiple categories while **users** (viewers) can discover, purchase, and access content based on their permissions.

### Key Features

- **Dual Dashboard System**: Separate authenticated dashboards for admins and users
- **Flexible Access Control**: Public, Private, and Restricted file access types
- **Monetized Content**: Users can purchase access to restricted files
- **Manual Access Sharing**: Admins can grant/revoke access to users by ID
- **Category Organization**: Content organized by Education, Story, and Genres
- **Role-Based Security**: Unauthorized users prevented from accessing admin dashboards
- **JWT Authentication**: Stateless token-based security
- **Persistent Sessions**: File structures maintained across login/logout cycles
- **Local File Storage**: Files stored on local filesystem with organized directory structure
- **Payment Tracking**: Complete payment history and earnings tracking

---

## Tech Stack

### Backend
- **Framework**: Spring Boot 3.1.5
- **Java Version**: 17 or higher (tested with Java 17 and Java 23)
- **Build Tool**: Maven
- **Database**: MySQL 8.0+
- **Security**: Spring Security + JWT (jjwt 0.12.3)
- **ORM**: JPA/Hibernate

### Frontend
- **Framework**: React
- **State Management**: Redux or Context API
- **HTTP Client**: Axios
- **Authentication**: JWT Token Storage

### Storage
- **Type**: Local Filesystem
- **Location**: Configurable via `file.upload-dir` in `backend/src/main/resources/application.properties`.
  - Default (when not overridden) is the `uploads` folder inside the `backend` directory (e.g. `backend/uploads`).
  - You can set an absolute path using environment variables if needed.

---

## Project Structure

```
FileVault/
├── backend/
│   ├── pom.xml
│   └── src/main/java/com/filevault/
│       ├── FileVaultApplication.java
│       ├── config/
│       │   ├── SecurityConfig.java
│       │   └── CorsConfig.java
│       ├── controller/
│       │   ├── AuthController.java
│       │   ├── AdminController.java
│       │   ├── UserController.java
│       │   └── FileController.java
│       ├── service/
│       │   ├── AuthService.java
│       │   ├── AdminService.java
│       │   ├── UserService.java
│       │   ├── FileService.java
│       │   ├── AccessControlService.java
│       │   └── PaymentService.java
│       ├── entity/
│       │   ├── Admin.java
│       │   ├── User.java
│       │   ├── File.java
│       │   ├── Category.java
│       │   ├── AccessControl.java
│       │   ├── Payment.java
│       │   └── Enums (FileAccessType, AccessType, PaymentStatus)
│       ├── repository/
│       │   ├── AdminRepository.java
│       │   ├── UserRepository.java
│       │   ├── FileRepository.java
│       │   ├── CategoryRepository.java
│       │   ├── AccessControlRepository.java
│       │   └── PaymentRepository.java
│       ├── dto/
│       │   ├── LoginRequest.java
│       │   ├── RegisterRequest.java
│       │   ├── JwtResponse.java
│       │   ├── FileUploadRequest.java
│       │   ├── AccessGrantRequest.java
│       │   ├── PaymentRequest.java
│       │   └── FileResponse.java
│       ├── security/
│       │   ├── JwtProvider.java
│       │   ├── CustomUserDetailsService.java
│       │   └── JwtAuthenticationFilter.java
│       └── exception/
│           ├── GlobalExceptionHandler.java
│           ├── ResourceNotFoundException.java
│           └── UnauthorizedAccessException.java
├── frontend/
│   ├── package.json
│   └── src/
│       ├── components/
│       ├── pages/
│       └── App.jsx
├── database/
│   └── schema.sql
└── README.md
```

---

## Database Setup

### 1. Create Database and Tables

```bash
# Open MySQL command line
mysql -u root -p

# Execute schema.sql
source /path/to/database/schema.sql
```

### 2. Default Categories
The following categories are automatically created:
- Education
- Story
- Genres

---

## Backend Setup

### Prerequisites
- JDK 17 or higher
- MySQL 8.0 or higher
- Maven 3.6 or higher

### Installation Steps

1. **Clone/Extract the project**
```bash
cd backend
```

2. **Update application.properties**
```properties
# backend/src/main/resources/application.properties
spring.datasource.url=jdbc:mysql://localhost:3306/filevault_db
spring.datasource.username=root
spring.datasource.password=your_password

# Update JWT secret (minimum 32 characters)
jwt.secret=your-very-long-secret-key-change-this-in-production

# Configure file upload directory (default: uploads inside backend)
file.upload-dir=uploads
```

3. **Install Dependencies**
```bash
mvn clean install
```

4. **Build the Project**
```bash
mvn clean build
```

5. **Run the Application**
```bash
mvn spring-boot:run
```

The backend will start on `http://localhost:8080`
If you run locally with the frontend dev server, the frontend uses port `3000` by default and proxies API requests to `http://localhost:8080`.

Quick admin registration & login (useful for local testing):

PowerShell example:

```powershell
# Register an admin
Invoke-RestMethod -Uri http://localhost:8080/api/auth/admin/register -Method Post -ContentType 'application/json' -Body '{"email":"admin@example.com","password":"Password123!","firstName":"Admin","lastName":"User","phoneNumber":"1234567890"}'

# Login as admin
(Invoke-RestMethod -Uri http://localhost:8080/api/auth/admin/login -Method Post -ContentType 'application/json' -Body '{"email":"admin@example.com","password":"Password123!"}')
```

Notes:
- Ensure `jwt.secret` is set to a secure random string in production (minimum 32 characters).
- The application reads configuration from environment variables when present; see `application.properties` for defaults.

---

## API Endpoints

### Authentication
```
POST   /api/auth/admin/register      - Register admin account
POST   /api/auth/admin/login         - Admin login
POST   /api/auth/user/register       - Register user account
POST   /api/auth/user/login          - User login
```

### Admin Endpoints
```
GET    /api/admin/profile/{adminId}          - Get admin profile
PUT    /api/admin/profile/{adminId}          - Update admin profile
GET    /api/admin/{adminId}/files            - Get admin's files
GET    /api/admin/{adminId}/dashboard        - Get admin dashboard
GET    /api/admin/{adminId}/earnings         - Get total earnings
POST   /api/admin/{adminId}/access/grant     - Grant file access to user
POST   /api/admin/{adminId}/access/revoke    - Revoke file access from user
GET    /api/admin/{adminId}/file/{fileId}/access - Get file access information
```

### User Endpoints
```
GET    /api/user/profile/{userId}            - Get user profile
PUT    /api/user/profile/{userId}            - Update user profile
GET    /api/user/{userId}/files              - Get available files
GET    /api/user/{userId}/dashboard          - Get user dashboard
GET    /api/user/{userId}/wallet             - Get wallet balance
POST   /api/user/{userId}/wallet/fund        - Fund wallet
POST   /api/user/{userId}/payment/purchase/{fileId} - Purchase file
GET    /api/user/{userId}/purchases          - Get user purchases
GET    /api/user/{userId}/access             - Get user access information
```

### File Endpoints
```
POST   /api/files/upload                     - Upload file (Admin only)
GET    /api/files/{fileId}                   - Get file details
GET    /api/files/public                     - Get all public files
GET    /api/files/category/{categoryId}      - Get files by category
DELETE /api/files/{fileId}                   - Delete file (Admin only)
GET    /api/files/download/{fileId}          - Download file
```

---

## Sample API Requests

### Admin Registration
```bash
curl -X POST http://localhost:8080/api/auth/admin/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@example.com",
    "password": "password123",
    "firstName": "John",
    "lastName": "Doe",
    "phoneNumber": "+1234567890"
  }'
```

### User Registration
```bash
curl -X POST http://localhost:8080/api/auth/user/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123",
    "firstName": "Jane",
    "lastName": "Smith"
  }'
```

### File Upload
```bash
curl -X POST http://localhost:8080/api/files/upload \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@/path/to/file.pdf" \
  -F "categoryId=1" \
  -F "accessType=RESTRICTED" \
  -F "price=9.99" \
  -F "description=Premium Content"
```

---

## Frontend Setup

### Prerequisites
- Node.js 14+
- npm or yarn

### Installation Steps

1. **Navigate to frontend directory**
```bash
cd frontend
npm install
```

2. **Configure API Base URL**
- Create `.env` file in frontend root:
```
REACT_APP_API_URL=http://localhost:8080/api
```

3. **Run Development Server**
```bash
npm start
```

Frontend will start on `http://localhost:3000`

---

## Key Database Entities

### Admin (Content Creators)
- Email & Password authentication
- Profile information
- Can upload files
- Can grant/revoke access
- Earns from file sales

### User (Viewers)
- Email authentication
- Profile information
- Can browse files
- Can purchase access
- Wallet for payments
- Access history

### File
- Metadata storage
- Access type control
- Category assignment
- Pricing for restricted content

### AccessControl
- Tracks file access permissions
- Two types: SHARED_BY_ADMIN, PURCHASED
- Can be revoked by admin

### Payment
- Transaction history
- Track earnings
- Support multiple statuses

---

## Security Features

1. **JWT Authentication**: Stateless token-based authentication
2. **Password Encryption**: BCrypt password hashing
3. **Role-Based Access**: ADMIN and USER roles
4. **CORS Configuration**: Restricted cross-origin requests
5. **Exception Handling**: Centralized error management
6. **Input Validation**: DTO-based request validation

---

## File Upload Configuration

### Upload Directory Structure
```
D:/filevault-uploads/
├── admin_1/
│   ├── 1234567890_abc123.pdf
│   ├── 1234567891_def456.docx
│   └── ...
├── admin_2/
│   └── ...
```

### Supported Features
- Multiple file format support
- File size validation (configurable)
- Unique filename generation
- Organized by admin ID

---

## Troubleshooting

### Database Connection Issues
```
Check MySQL is running:
- Windows: Services > MySQL
- Linux: sudo service mysql status
- macOS: brew services list
```

### JWT Token Expiration
- Default expiration: 24 hours
- Configure in `application.properties`: `jwt.expiration=86400000`

### File Upload Failures
- Ensure upload directory exists and has write permissions
- Check file size limits in `application.properties`
- Verify MIME type support

### CORS Issues
- Check CorsConfig.java for allowed origins
- Default: all origins allowed (change in production)

---

## Future Enhancements

1. **Payment Gateway Integration** (Stripe, PayPal)
2. **Video Streaming Support**
3. **Advanced Analytics Dashboard**
4. **File Encryption**
5. **User Comments/Reviews**
6. **Subscription Model**
7. **Admin Analytics**
8. **Mobile App**
9. **CDN Integration** for faster downloads
10. **Cloud Storage Support** (AWS S3, Azure Blob)

---

## Environment Variables

```properties
# Database
spring.datasource.url=jdbc:mysql://localhost:3306/filevault_db
spring.datasource.username=root
spring.datasource.password=password

# JWT
jwt.secret=your-secure-secret-key-minimum-32-chars
jwt.expiration=86400000

# File Upload
file.upload-dir=D:/filevault-uploads
spring.servlet.multipart.max-file-size=100MB
spring.servlet.multipart.max-request-size=100MB

# Server
server.port=8080

# Logging
logging.level.root=INFO
logging.level.com.filevault=DEBUG
```

---

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit changes
4. Push to branch
5. Create Pull Request

---

## License

This project is licensed under the MIT License - see LICENSE file for details.

---

## Support

For support, email support@filevault.com or create an issue on GitHub.

---

## Authors

- **Development Team**: FileVault Team
- **Last Updated**: February 2026

---

## Additional Notes

- Always change JWT secret in production
- Configure upload directory with adequate storage
- Set up regular database backups
- Implement rate limiting for production
- Use HTTPS in production
- Configure environment-specific properties
- Monitor file storage usage
- Implement logging and monitoring

- PR: remove debug panel & add JWT logging
