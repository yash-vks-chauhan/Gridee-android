# Complete Testing Guide for Gridee App

## 🚀 Quick Testing Setup

### Prerequisites
1. **Java 21** ✅ (You have this)
2. **MongoDB** (Need to install)
3. **Android Studio** (For running the app)

### Step 1: MongoDB Setup

#### Option A: Using Docker (Easiest)
```bash
# Install Docker Desktop from https://www.docker.com/products/docker-desktop/
# Then run:
docker run -d -p 27017:27017 --name gridee-mongo mongo:latest
```

#### Option B: Using Homebrew
```bash
# First update Xcode Command Line Tools
sudo xcode-select --install

# Install MongoDB
brew tap mongodb/brew
brew install mongodb-community
brew services start mongodb-community
```

### Step 2: Start Backend Server
```bash
cd /Users/yashchauhan/Gridee/repo
./gradlew bootRun
```

**Expected Output:**
```
Started AppApplication in X.XXX seconds
```

**Backend will be running on:** http://localhost:8080

### Step 3: Verify Backend APIs
Test in browser or Postman:
- http://localhost:8080/swagger-index.html (API documentation)
- POST http://localhost:8080/api/users/register (for testing registration)

### Step 4: Run Android App

1. **Open Android Studio**
2. **Open Project:** `/Users/yashchauhan/Gridee/android-app`
3. **Wait for Gradle sync** to complete
4. **Run the app** on emulator or device

### Step 5: Test Complete Flow

1. **App Opens** → Login screen appears
2. **Click "Sign Up"** → Registration screen
3. **Fill registration form** → Create account
4. **Return to login** → Enter credentials
5. **Login success** → Ready for main features!

## 🔧 Troubleshooting

### If Backend Fails to Start:
```bash
# Check if port 8080 is free
lsof -i :8080

# Kill any process using port 8080
kill -9 <PID>
```

### If MongoDB Connection Fails:
```bash
# Check MongoDB status
brew services list | grep mongodb
# Or
pgrep -l mongod
```

### Android App Network Issues:
- **Emulator**: Uses `http://10.0.2.2:8080`
- **Physical Device**: Update `ApiClient.kt` with your computer's IP address

## ✅ What to Test

### Registration Flow:
- [ ] Form validation (empty fields, invalid email, password mismatch)
- [ ] Successful registration
- [ ] Navigation back to login

### Login Flow:
- [ ] Form validation (empty fields)
- [ ] Invalid credentials error
- [ ] Successful login
- [ ] Password visibility toggle

### Integration:
- [ ] User created in MongoDB
- [ ] Backend logs show API calls
- [ ] Android app receives proper responses

## 🎯 Success Criteria

**✅ Backend Running:** Swagger UI accessible at http://localhost:8080/swagger-index.html
**✅ Database Connected:** No MongoDB connection errors in backend logs
**✅ App Registration:** User successfully registers and data saves to MongoDB
**✅ App Login:** User can login with registered credentials

## 📱 Next Features to Test
Once basic auth works:
- Main Dashboard
- Parking Lot Discovery
- Booking System
- Payment Integration

## 🆘 Need Help?
If you encounter issues:
1. Check backend logs for errors
2. Check Android Studio logcat for app errors
3. Verify MongoDB is running and accessible
4. Ensure network connectivity between app and backend
