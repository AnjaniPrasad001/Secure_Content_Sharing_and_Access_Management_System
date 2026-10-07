# FileVault - Secure Content Sharing & AI Analytics Management System

## Project Overview

**FileVault** is a comprehensive, secure, role-based file sharing, access management, and AI-powered analytics platform. It enables **admins** (content creators) to upload and manage files across categories while **users** (viewers) can discover, search, purchase, request, and access content based on their permissions.

FileVault features **two fully integrated LLM-powered chatbots**:
1. **Public Content Discovery Assistant**: Integrated into the Discover Public Content page for natural-language content discovery, category search, recent upload filtering, recommendations, and Retrieval-Augmented Generation (RAG) over public documents.
2. **Admin AI Analytics Assistant**: Integrated into the Admin Dashboard for secure, role-isolated analytics, real-time metrics, historical trend graphs (using Recharts), AI performance insights, and RAG document summaries over the admin's own uploaded content.

---

## Key Features

- **Dual LLM Chatbots**:
  - **Public Discovery Chatbot**: Natural language search, recent upload filters, grounded RAG document QA, recommendation engine, structured clickable file cards.
  - **Admin Analytics Chatbot**: Strict server-side `hasRole('ADMIN')` RBAC, time-series visualization (Recharts), category breakdown, month-over-month engagement comparison, AI performance suggestions, admin-scoped document RAG.
- **RAG & In-Memory Vector Storage**: Document text extractor (`DocumentExtractorService`) supporting TXT, PDF, DOCX, MD, CSV, Code, and SQL. Chunking and term/embedding similarity search (`VectorStoreService`) auto-indexed on app start and upload/update/delete events.
- **Dual Dashboard System**: Dedicated, role-authenticated dashboards for Admins and Users.
- **Flexible Access Control**: Public, Private, and Restricted file access levels with server-enforced visibility rules.
- **Monetized Content & Wallet**: Users can fund their wallet and purchase access to restricted files.
- **Access Requests & Approval**: Users can request access to files; Admins can approve/reject with custom reasons.
- **OTP Password Reset & Phone Recovery**: Phone number validation and OTP-based recovery.
- **Role-Based Security**: Spring Security + JWT authentication enforcing strict role separation.

---

## Tech Stack

### Backend
- **Framework**: Spring Boot 3.1.5 (Java 17+)
- **Build Tool**: Maven
- **Database**: H2 (embedded zero-config fallback) / MySQL 8.0+ / PostgreSQL
- **Security**: Spring Security + JWT (`jjwt 0.12.3`) & BCrypt Password Encoding
- **AI / LLM Integration**: Google Gemini API (`gemini-1.5-flash` / configurable model) with backend API key security and graceful DB/RAG fallback.
- **Vector Search / RAG**: In-memory vector store with document chunking and metadata scoping.

### Frontend
- **Framework**: React 18 with React Router v6
- **Styling**: Vanilla CSS & Tailwind CSS
- **Visualization**: Recharts (Line & Bar charts)
- **Icons**: Lucide React
- **HTTP Client**: Axios with JWT interceptors
- **State & Notifications**: Context API & React Hot Toast

---

## AI Architecture & Data Flow

```
[ User / Guest ] ---> [ Public Discovery Chatbot UI ]
                               |
                               v
                     POST /api/chat/public
                               |
            +------------------+------------------+
            |                                     |
            v                                     v
  [ FileRepository DB Search ]          [ VectorStoreService RAG ]
            |                                     |
            +------------------+------------------+
                               |
                               v
                  [ LlmService (Gemini API) ] ---> Grounded AI Reply + File Cards

-----------------------------------------------------------------------------------

[ Authenticated Admin ] ---> [ Admin AI Analytics Assistant UI ]
                                    |
                                    v
                         POST /api/chat/admin  (Server RBAC check)
                                    |
            +-----------------------+-----------------------+
            |                       |                       |
            v                       v                       v
[ DB Aggregations & Trends ] [ Recharts ChartData ] [ Admin VectorStore RAG ]
            |                       |                       |
            +-----------------------+-----------------------+
                                    |
                                    v
                       [ LlmService (Gemini API) ] ---> AI Insights + Interactive Charts
```

---

## Environment Variables Configuration

Create a `.env` file or export environment variables:

| Variable Name | Description | Example / Default |
|---|---|---|
| `GEMINI_API_KEY` | Gemini API Key for LLM responses | `AIzaSy...` |
| `GEMINI_MODEL` | Gemini LLM Model Name | `gemini-1.5-flash` |
| `SPRING_DATASOURCE_URL` | DB Connection URL | `jdbc:h2:file:./data/filevault_db` |
| `SPRING_DATASOURCE_USERNAME` | DB Username | `sa` |
| `SPRING_DATASOURCE_PASSWORD` | DB Password | `` |
| `JWT_SECRET` | Secret key for signing JWT tokens | `your-secret-key-32-chars-minimum` |
| `PORT` | Backend server port | `8080` |

*Note:* If `GEMINI_API_KEY` is omitted, the chatbots automatically operate in **Grounded Local RAG & Database Fallback Mode**, ensuring 100% feature availability without crashing.

---

## API Endpoints

### AI & Analytics Endpoints
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/chat/public` | Process public discovery & RAG chatbot queries |
| POST | `/api/chat/admin` | Process admin analytics & RAG assistant queries (Admin only) |
| GET | `/api/analytics/overview` | Get overview analytics for authenticated admin |
| GET | `/api/analytics/content-performance` | Get content performance metrics |
| GET | `/api/analytics/time-series` | Get time-series chart data (`type=daily\|category\|30days`) |
| GET | `/api/analytics/engagement` | Get engagement comparison & category stats |

### File Engagement Endpoints
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/files/{id}/view-event` | Record file view event and increment counter |
| POST | `/api/files/{id}/like` | Toggle file like (User required) |
| GET | `/api/files/{id}/like/status` | Check if user liked file + get total likes |
| POST | `/api/admin/{adminId}/subscribe` | Toggle subscription to admin (User required) |
| GET | `/api/admin/{adminId}/subscribe/status` | Check subscription status + subscriber count |

---

## Setup & Running Locally

### Backend
1. Navigate to `backend`:
   ```bash
   cd backend
   ```
2. Run unit and integration tests:
   ```bash
   mvn test
   ```
3. Run Spring Boot dev server:
   ```bash
   mvn spring-boot:run
   ```
   Backend listens at `http://localhost:8080`.

### Frontend
1. Navigate to `frontend`:
   ```bash
   cd frontend
   ```
2. Install dependencies:
   ```bash
   npm install
   ```
3. Run React dev server:
   ```bash
   npm start
   ```
   Frontend runs at `http://localhost:3000`.

---

## Production Deployment Guide

### Deploying Frontend to Vercel
1. Push frontend code to GitHub.
2. Import project into Vercel dashboard with Root Directory set to `frontend`.
3. Set environment variable `REACT_APP_API_BASE_URL` to your deployed Render backend URL (e.g. `https://filevault-api.onrender.com`).
4. Build command: `npm run build`, Output directory: `build`.

### Deploying Backend to Render
1. Create a Web Service on Render pointing to `backend`.
2. Environment: `Docker` (using root/backend Dockerfile) or `Java`.
3. Configure Environment Variables on Render:
   - `GEMINI_API_KEY`
   - `SPRING_DATASOURCE_URL` (e.g. PostgreSQL or MySQL URL)
   - `SPRING_DATASOURCE_USERNAME`
   - `SPRING_DATASOURCE_PASSWORD`
   - `JWT_SECRET`
4. Deploy service and verify CORS headers match your Vercel origin.

---

## Automated Test Verification

Run Maven test suite to verify all backend services:
```bash
cd backend
mvn test
```
**Results**: 12/12 tests pass cleanly (Auth, Files, PasswordReset, VectorStore, Public Chatbot, Admin Analytics Chatbot).
