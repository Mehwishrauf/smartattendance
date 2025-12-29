# Smart Attendance System

## 📌 Project Overview
The **Smart Attendance System** is an Android application designed to automate classroom
attendance using **QR codes**, **role-based access**, and **real-time cloud storage**.
The system supports three user roles: **Admin**, **Teacher**, and **Student**.
Each role has clearly defined permissions to ensure secure and accurate attendance management.

## 🛠 Technologies Used

| Technology                    | Purpose |
|---------------------- --------|--------|
| Android Studio (Java)         |   Android application development |
| Firebase Authentication       | User login and registration |
| Firebase Realtime Database    |Store attendance, users, and sessions |
| ZXing Library                 | QR code generation and scanning |
| CSV Export                    |Attendance reporting |
| SharedPreferences             |Session persistence |
| Git & GitHub                  |Version control |

---

## 👥 System Roles & Responsibilities

### 🔹 Admin
The **Admin** is the highest authority in the system.

**Admin can:**
- Add and manage teachers (via pending approval system)
- View system analytics (users, classes, sessions)
- Manage database records
- View attendance reports
- Monitor overall system usage

**Note:** Admin does **not** mark attendance.

---

### 🔹 Teacher
Teachers manage classes and attendance sessions.

**Teacher can:**
- Create classes
- Add students to class roster
- Create attendance sessions
- Generate QR codes
- View live attendance
- View present and absent students
- Export attendance as CSV
- View monthly attendance reports

---

### 🔹 Student
Students can mark and track their attendance.

**Student can:**
- Register and login
- Join classes assigned by teachers
- Scan QR codes to mark attendance
- View attendance history
- View present records (read-only)

---

## 🔐 Authentication & Role Management

### Firebase Authentication
- Handles user login and registration
- Email and Password based authentication

## 📁 Project Structure
```text
app/
│
├── manifests/
│   └── AndroidManifest.xml
│
├── java/
│   └── com.example.smartattendancesystem/
│       ├── LoginActivity.java
│       ├── MainActivity.java
│       │
│       ├── AdminHomeActivity.java
│       ├── AdminManageTeachersActivity.java
│       ├── AdminDatabaseManagerActivity.java
│       ├── AdminReportsActivity.java
│       ├── AdminAnalyticsActivity.java
│       │
│       ├── TeacherHomeActivity.java
│       ├── ManageClassesActivity.java
│       ├── ClassRosterActivity.java
│       ├── SessionDetailsActivity.java
│       │
│       ├── StudentHomeActivity.java
│       └── StudentHistoryActivity.java
│
├── res/
│   ├── layout/
│   │   ├── activity_login.xml
│   │   ├── activity_main.xml
│   │   ├── activity_admin_home.xml
│   │   ├── activity_admin_manage_teachers.xml
│   │   ├── activity_admin_database_manager.xml
│   │   ├── activity_admin_reports.xml
│   │   ├── activity_admin_analytics.xml
│   │   ├── activity_teacher_home.xml
│   │   ├── activity_manage_classes.xml
│   │   ├── activity_class_roster.xml
│   │   ├── activity_session_details.xml
│   │   ├── activity_student_home.xml
│   │   └── activity_student_history.xml
│   │
│   └── drawable/
│
└── java (generated)
