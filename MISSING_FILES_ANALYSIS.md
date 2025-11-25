# Missing Files Analysis - GitHub vs Local Repository

This document lists all files that exist in the GitHub repository but are missing from your local machine.

## Summary

Your local repository is missing files from the backend (Spring Boot) part of the project. The GitHub repository contains these files across all three branches.

---

## Branch 1: `main` (Default Branch)
**Total Missing Files: 29**

### Configuration Files
1. `.dockerignore` - Docker ignore file
2. `Dockerfile` - Docker container configuration
3. `README.md` - Project documentation

### AOP (Aspect-Oriented Programming) Files
4. `src/main/java/com/parking/app/aop/LoggingAspect.java`
5. `src/main/java/com/parking/app/aop/PerformanceMonitoringAspect.java`
6. `src/main/java/com/parking/app/aop/RequestTracingFilter.java`
7. `src/main/java/com/parking/app/aop/SecurityAuditAspect.java`

### Configuration Classes
8. `src/main/java/com/parking/app/config/OAuth2LoginSuccessHandler.java`
9. `src/main/java/com/parking/app/config/OpenApiConfig.java`
10. `src/main/java/com/parking/app/config/TracingConfig.java`
11. `src/main/java/com/parking/app/config/logging/ApiLoggingInterceptor.java`
12. `src/main/java/com/parking/app/config/logging/BusinessEventsLogger.java`
13. `src/main/java/com/parking/app/config/logging/RequestResponseBodyLoggingFilter.java`

### Constants
14. `src/main/java/com/parking/app/constants/Role.java`
15. `src/main/java/com/parking/app/constants/SecurityConstants.java`

### DTOs (Data Transfer Objects)
16. `src/main/java/com/parking/app/dto/AuthResponseDto.java`
17. `src/main/java/com/parking/app/dto/LoginRequestDto.java`
18. `src/main/java/com/parking/app/dto/UserRequestDto.java`
19. `src/main/java/com/parking/app/dto/UserResponseDto.java`

### Scheduler
20. `src/main/java/com/parking/app/scheduler/BookingScheduler.java`

### Services
21. `src/main/java/com/parking/app/service/TokenBlacklistService.java`
22. `src/main/java/com/parking/app/service/booking/BookingWalletService.java`

### Utilities
23. `src/main/java/com/parking/app/util/BookingUtility.java`

### Resources
24. `src/main/resources/logback-spring.xml` - Logging configuration
25. `src/main/resources/static/css/styles.css`
26. `src/main/resources/static/html/index.html`
27. `src/main/resources/static/js/script.js`
28. `src/main/resources/static/logo.jpeg`
29. `src/main/resources/static/oauth.html`

---

## Branch 2: `security_concerns`
**Total Missing Files: 33**

### All files from `main` branch PLUS these additional files:

30. `src/main/java/com/parking/app/controller/AdMobController.java` - AdMob integration controller
31. `src/main/java/com/parking/app/model/AdImpressions.java` - Ad impressions model
32. `src/main/java/com/parking/app/repository/AdImpressionsRepository.java` - Ad impressions repository
33. `src/main/java/com/parking/app/service/AdMobService.java` - AdMob service

**Note:** This branch includes AdMob (Google Ads) integration features that are not in the main branch.

---

## Branch 3: `feature/transaction`
**Total Missing Files: 29**

This branch has the **same missing files** as the `main` branch (no additional unique files).

---

## Key Findings

### Missing Components by Category:

1. **Docker Configuration** (2 files)
   - .dockerignore
   - Dockerfile

2. **Documentation** (1 file)
   - README.md

3. **Backend Security & Auth** (11 files)
   - OAuth2 handlers
   - Security constants
   - Auth DTOs
   - Token blacklist service

4. **Logging & Monitoring** (7 files)
   - Logging aspects
   - Performance monitoring
   - Request tracing
   - Business events logger

5. **Booking Features** (3 files)
   - Booking scheduler
   - Booking wallet service
   - Booking utility

6. **Static Web Resources** (5 files)
   - HTML/CSS/JS files
   - Logo
   - OAuth page

7. **AdMob Integration** (4 files - only in `security_concerns`)
   - AdMob controller, service, model, and repository

---

## Recommendations

1. **If you need the complete backend**: Pull files from the `main` branch
2. **If you need AdMob features**: Pull files from the `security_concerns` branch
3. **If you only work on Android**: These missing files are backend (Spring Boot) components and may not be needed for Android development

---

## Commands to Get Missing Files

### To get all files from main branch:
```bash
cd /Users/yashchauhan/Gridee
git checkout origin/main -- <filename>
```

### To get all files from security_concerns branch:
```bash
cd /Users/yashchauhan/Gridee
git checkout origin/security_concerns -- <filename>
```

### To merge the entire main branch into your current branch:
```bash
git merge origin/main
```

---

*Generated on: November 16, 2025*
