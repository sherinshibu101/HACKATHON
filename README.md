# 🏙️ Community Hero AI

An AI-powered hyperlocal civic platform that helps citizens **report, validate, track, and resolve community infrastructure issues** transparently.

![Status](https://img.shields.io/badge/Status-Deployed-brightgreen)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-green)
![React](https://img.shields.io/badge/React-18-blue)
![MySQL](https://img.shields.io/badge/MySQL-8-blue)
![Google Cloud](https://img.shields.io/badge/Google%20Cloud-Deployed-4285F4)
![Gemini](https://img.shields.io/badge/AI-Google%20Gemini-8E75B2)
![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)
---

> **Live Application:** [Community Hero AI](https://project-eb406bb8-8a69-4442-937.web.app)
>

---

## ✨ Features

### 👤 Citizen Portal
- Firebase Google authentication
- Image and video-based issue reporting
- GPS location, address search, and draggable map pin
- Google Cloud Vision evidence validation
- Duplicate and previously resolved issue detection
- Community verification
- Emergency authority escalation
- Public status history and resolution certificates
- Citizen help chatbot

### 🤖 AI and Automation
- Gemini-powered issue categorization
- Severity and impact scoring
- Department and resolution recommendations
- Complaint and escalation message generation
- Bounded Civic Case Manager Agent
- Multi-step evidence, duplicate, ward-health, and community analysis
- Human approval for consequential actions

### 🏛️ Admin Authority Portal
- Protected authority case queue
- AI investigation activity log
- Evidence review and moderation
- Controlled status workflow
- Authority email through Resend
- Resolution notes and evidence
- HMAC-SHA-256 civil ledger integrity check

### 📊 Civic Intelligence
- Ward health scoring
- High-impact issue dashboard
- Category and locality statistics
- Leaflet marker clustering and heatmaps
- Gamification, badges, and leaderboard

---

## 🚀 Quick Start

### Prerequisites

- Java 21+
- Maven 3.9+
- Node.js 20+
- MySQL 8+
- Firebase project with Google sign-in
- Google Cloud project

### 1. Clone Repository

```bash
git clone https://github.com/sherinshibu101/HACKATHON.git
cd HACKATHON
```

### 2. Create Database

Run inside MySQL:

```sql
CREATE DATABASE community_hero_ai;
```

### 3. Configure Environment

```bash
cp backend/.env.example backend/.env
cp frontend/.env.example frontend/.env
```

Add your database, Firebase, Gemini, Cloud Storage, Vision, and Resend configuration.

Never commit `.env` files or API keys.

### 4. Start Backend

```bash
cd backend
mvn spring-boot:run
```

### 5. Start Frontend

```bash
cd frontend
npm install
npm run dev
```


## 🏗️ Architecture

```text
┌──────────────────┐     ┌────────────────────┐     ┌──────────────────┐
│ React + Firebase │────►│ Spring Boot API    │────►│ Cloud SQL MySQL  │
│ Citizen / Admin  │     │ Google Cloud Run   │     │ Civic Data       │
└──────────────────┘     └─────────┬──────────┘     └──────────────────┘
                                  │
                 ┌────────────────┼────────────────┐
                 ▼                ▼                ▼
          Google Gemini    Cloud Vision     Cloud Storage
```

### Tech Stack

- **Frontend:** React, Vite, Tailwind CSS, Axios
- **Backend:** Java 21, Spring Boot, Spring Security, Spring Data JPA
- **Database:** MySQL and Google Cloud SQL
- **Authentication:** Firebase Google Authentication
- **AI:** Google Gemini and Google Cloud Vision
- **Maps:** Leaflet and OpenStreetMap
- **Storage:** Google Cloud Storage
- **Deployment:** Cloud Run and Firebase Hosting
- **Secrets:** Google Secret Manager
- **Email:** Resend API

---

## 🔒 Security

- Firebase ID-token verification
- Backend-enforced citizen and admin roles
- Google Secret Manager for production credentials
- Private Cloud Storage evidence
- Input validation and controlled status transitions
- HMAC-SHA-256 tamper-evident civil ledger
- Human approval for AI recommendations
- Secrets and generated files excluded from Git

---

## 📄 License

This project is licensed under the **Apache License 2.0** 
