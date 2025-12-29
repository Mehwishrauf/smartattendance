# Smart Attendance System

A mobile-based Android application designed to manage attendance efficiently
for administrators, teachers, and students using role-based access.

##  Technologies Used:
- Java
- Android
- Android Studio
- XML (User Interface Design)

## ✨ Features:

### Admin Module
- Admin dashboard
- Manage teachers
- Manage classes
- View attendance reports
- Attendance analytics
- Database management

### Teacher Module
- Teacher dashboard
- Manage classes
- View class roster
- View session details

### Student Module
- Student dashboard
- View attendance history
- View session details

### Common Features
- Secure login system
- Role-based access control

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
