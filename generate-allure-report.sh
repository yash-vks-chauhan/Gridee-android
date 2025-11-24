#!/bin/bash

echo "=================================================="
echo "📊 Allure Report Generation Script"
echo "=================================================="
echo ""

# Step 1: Clean previous reports
echo "🧹 Cleaning previous reports..."
./gradlew clean

# Step 2: Run tests and generate Allure results
echo ""
echo "🧪 Running integration tests..."
./gradlew test --tests "com.parking.app.integration.BookingConcurrencyIntegrationTest"

# Step 3: Generate Allure report
echo ""
echo "📈 Generating Allure report..."
./gradlew allureReport

# Step 4: Serve the report
echo ""
echo "🚀 Opening Allure report in browser..."
echo "Report will be available at: http://localhost:45678"
echo ""
./gradlew allureServe

echo ""
echo "✅ Done! Press Ctrl+C to stop the server."

