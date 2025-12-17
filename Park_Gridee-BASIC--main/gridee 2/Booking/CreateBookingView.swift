//
//import SwiftUI
//
//struct CreateBookingView: View {
//    let selectedParking: ParkingSpot?
//    @Environment(\.dismiss) private var dismiss
//    @EnvironmentObject var homeViewModel: HomeViewModel
//    @EnvironmentObject var authViewModel: AuthViewModel
//    
//    @ObservedObject private var vehicleManager = SharedVehicleManager.shared
//
//    @State private var userId = ""
//    @State private var spotId = ""
//    @State private var lotId = ""
//    @State private var selectedVehicle = ""
//    @State private var vehicleOptions: [String] = []  // ✅ NEW: Store fetched vehicles
//    @State private var showingAddVehicle = false
//    @State private var newVehicleNumber = ""
//    @State private var checkInTime = Date()
//    @State private var checkOutTime = Date().addingTimeInterval(3600)
//    @State private var isLoading = false
//    @State private var isLoadingVehicles = false  // ✅ NEW: Loading state for vehicles
//    @State private var errorMessage = ""
//    @State private var showingError = false
//    @State private var checkInHourSelection: Int = 8
//    @State private var checkInMinuteSelection: Int = 0
//    @State private var checkOutHourSelection: Int = 9
//    @State private var checkOutMinuteSelection: Int = 0
//
//
//    
//    @State private var showingBookingSummary = false
//    @State private var bookingDetails: Bookings?
//
//    init(selectedParking: ParkingSpot? = nil) {
//        self.selectedParking = selectedParking
//    }
//    
//    // ✅ SMART BOOKING DATE
//    private var bookingDate: Date {
//        let calendar = Calendar.current
//        let now = Date()
//        let currentHour = calendar.component(.hour, from: now)
//        
//        if currentHour >= 20 {
//            return calendar.date(byAdding: .day, value: 1, to: now) ?? now
//        }
//        
//        return now
//    }
//    
//    // ✅ SMART TIME RANGE
//    private var allowedTimeRange: ClosedRange<Date> {
//        let calendar = Calendar.current
//        let now = Date()
//        let currentHour = calendar.component(.hour, from: now)
//        let bookingDay = bookingDate
//        
//        if currentHour >= 20 {
//            let startTime = calendar.date(bySettingHour: 8, minute: 0, second: 0, of: bookingDay)!
//            let endTime = calendar.date(bySettingHour: 20, minute: 0, second: 0, of: bookingDay)!
//            return startTime...endTime
//        }
//        
//        let currentTime = now
//        let endTime = calendar.date(bySettingHour: 20, minute: 0, second: 0, of: bookingDay)!
//        
//        return currentTime...endTime
//    }
//    
//    // ✅ SAFE CHECKOUT RANGE
//    private var checkOutTimeRange: ClosedRange<Date> {
//        let minCheckOut = checkInTime.addingTimeInterval(1800)
//        let maxCheckOut = allowedTimeRange.upperBound
//        
//        if minCheckOut > maxCheckOut {
//            return checkInTime...checkInTime
//        }
//        
//        return minCheckOut...maxCheckOut
//    }
//    
//    // ✅ DYNAMIC AVAILABILITY TEXT
//    private var availabilityTimeText: String {
//        let calendar = Calendar.current
//        let currentHour = calendar.component(.hour, from: Date())
//        
//        if currentHour >= 20 {
//            return "Available Hours: 8:00 AM - 8:00 PM (Tomorrow)"
//        } else {
//            return "Available Hours: Now - 8:00 PM (Today)"
//        }
//    }
//
//    var body: some View {
//        NavigationView {
//            formContent
//                .navigationTitle("Create Booking")
//                .navigationBarTitleDisplayMode(.inline)
//                .toolbar {
//                    toolbarContent
//                }
//        }
//        .sheet(isPresented: $showingAddVehicle) {
//            AddVehicleSheet(newVehicleNumber: $newVehicleNumber) { vehicle in
//                vehicleManager.addVehicleByNumber(vehicle)
//                vehicleOptions.append(vehicle)
//                selectedVehicle = vehicle
//                homeViewModel.userVehicles = vehicleManager.getVehicleNumbers()
//            }
//        }
//        .sheet(isPresented: $showingBookingSummary) {
//            if let details = bookingDetails {
//                BookingSummaryView(booking: details)
//                    .environmentObject(homeViewModel)
//            }
//        }
//        .alert("Error", isPresented: $showingError) {
//            Button("OK") {
//                errorMessage = ""
//                showingError = false
//            }
//        } message: {
//            Text(errorMessage)
//        }
//        .onAppear {
//            setupInitialData()
//            
//            // ✅ Initialize hour selections
//            let calendar = Calendar.current
//            checkInHourSelection = max(calendar.component(.hour, from: Date()), 8)
//            checkOutHourSelection = checkInHourSelection + 1
//            updateCheckInTime(hour: checkInHourSelection)
//            updateCheckOutTime(hour: checkOutHourSelection)
//            
//            // Load vehicles
//            if let currentUserId = getCurrentUserId() {
//                loadUserVehicles(userId: currentUserId)
//            }
//        }
//
//    }
//    
//    // ✅ NEW: Load vehicles from backend
//    private func loadUserVehicles(userId: String) {
//        print("🚗 Loading vehicles for user: \(userId)")
//        isLoadingVehicles = true
//        
//        APIService.shared.fetchUserVehicles(userId: userId) { result in
//            DispatchQueue.main.async {
//                self.isLoadingVehicles = false
//                
//                switch result {
//                case .success(let vehicles):
//                    print("✅ Loaded \(vehicles.count) vehicles: \(vehicles)")
//                    self.vehicleOptions = vehicles
//                    
//                    // Select first vehicle by default if available
//                    if !vehicles.isEmpty && self.selectedVehicle.isEmpty {
//                        self.selectedVehicle = vehicles[0]
//                    }
//                    
//                    // Update vehicle manager
//                    for vehicle in vehicles {
//                        self.vehicleManager.addVehicleByNumber(vehicle)
//                    }
//                    
//                case .failure(let error):
//                    print("❌ Failed to load vehicles: \(error)")
//                    // Fallback to vehicle manager
//                    let fallbackVehicles = self.vehicleManager.getVehicleNumbers()
//                    if !fallbackVehicles.isEmpty {
//                        self.vehicleOptions = fallbackVehicles
//                        self.selectedVehicle = fallbackVehicles[0]
//                    } else {
//                        // Create default vehicle
//                        let defaultVehicle = "USER_\(String(userId.prefix(6)))"
//                        self.vehicleOptions = [defaultVehicle]
//                        self.selectedVehicle = defaultVehicle
//                    }
//                }
//            }
//        }
//    }
//    
//    private func loadTimesFromViewModel() {
//        checkInTime = homeViewModel.selectedStartTime
//        checkOutTime = homeViewModel.selectedEndTime
//        
//        print("✅ Loaded times from HomeViewModel:")
//        print("   Start: \(formatDateTime(checkInTime))")
//        print("   End: \(formatDateTime(checkOutTime))")
//    }
//    
//    private func syncVehicles() {
//        vehicleManager.loadVehicles()
//        let vehicleNumbers = vehicleManager.getVehicleNumbers()
//        homeViewModel.userVehicles = vehicleNumbers
//        
//        if selectedVehicle.isEmpty {
//            selectedVehicle = vehicleManager.getPrimaryVehicle() ?? ""
//        }
//    }
//
//    @ViewBuilder
//    private var formContent: some View {
//        Form {
//            if let parking = selectedParking {
//                Section("Selected Parking") {
//                    selectedParkingView(parkingSpot: parking)
//                }
//            } else {
//                Section("Warning") {
//                    HStack {
//                        Image(systemName: "exclamationmark.triangle.fill")
//                            .foregroundColor(.orange)
//                        Text("No parking spot selected")
//                            .foregroundColor(.orange)
//                    }
//                }
//            }
//
//            vehicleSelectionSection
//            timingSection
//            bookingSummarySection
//            debugSection
//        }
//    }
//
//    @ViewBuilder
//    private var vehicleSelectionSection: some View {
//        Section("Vehicle Selection") {
//            if isLoadingVehicles {
//                HStack {
//                    ProgressView()
//                    Text("Loading vehicles...")
//                        .foregroundColor(.secondary)
//                }
//            } else if vehicleOptions.isEmpty {
//                HStack {
//                    Image(systemName: "car.fill")
//                        .foregroundColor(.gray)
//                    Text("No vehicles added")
//                        .foregroundColor(.gray)
//                }
//            } else {
//                vehiclePicker
//            }
//
//            Button("Add New Vehicle") {
//                showingAddVehicle = true
//            }
//            .foregroundColor(.blue)
//        }
//    }
//
//    @ViewBuilder
//    private var vehiclePicker: some View {
//        Picker("Select Vehicle", selection: $selectedVehicle) {
//            ForEach(vehicleOptions, id: \.self) { vehicle in
//                Text(vehicle)
//                    .tag(vehicle)
//            }
//        }
//        .pickerStyle(MenuPickerStyle())
//    }
//
//    @ViewBuilder
//    private var timingSection: some View {
//        Section {
//            VStack(spacing: 0) {
//                HourWheelPicker(
//                    checkInHour: $checkInHourSelection,
//                    checkInMinute: $checkInMinuteSelection,
//                    checkOutHour: $checkOutHourSelection,
//                    checkOutMinute: $checkOutMinuteSelection
//                )
//                .onChange(of: checkInHourSelection) { _, _ in
//                    updateCheckInTime(hour: <#Int#>)
//                }
//                .onChange(of: checkInMinuteSelection) { _, _ in
//                    updateCheckInTime(hour: <#Int#>)
//                }
//                .onChange(of: checkOutHourSelection) { _, _ in
//                    updateCheckOutTime(hour: <#Int#>)
//                }
//                .onChange(of: checkOutMinuteSelection) { _, _ in
//                    updateCheckOutTime(hour: <#Int#>)
//                }
//
//            }
//            .listRowInsets(EdgeInsets(top: 12, leading: 16, bottom: 12, trailing: 16))
//            .listRowBackground(Color.clear)
//        } header: {
//            Text("TIMING")
//                .font(.caption2)
//                .fontWeight(.heavy)
//                .tracking(1)
//        }
//    }
//
//
//
//    // ✅ NEW: Get available hours (8 AM to 8 PM)
//    private func getAvailableHours() -> [Int] {
//        let calendar = Calendar.current
//        let currentHour = calendar.component(.hour, from: Date())
//        let now = Date()
//        let today = calendar.startOfDay(for: now)
//        let selectedDay = calendar.startOfDay(for: bookingDate)
//        
//        // If booking for today and after 8 PM, no hours available today
//        if selectedDay == today && currentHour >= 20 {
//            return []
//        }
//        
//        // If booking for today, start from current hour or 8 AM (whichever is later)
//        if selectedDay == today {
//            let startHour = max(currentHour, 8)
//            return Array(startHour...19) // Up to 7 PM for check-in
//        }
//        
//        // For future dates, all hours from 8 AM to 7 PM
//        return Array(8...19)
//    }
//
//    // ✅ NEW: Get available checkout hours (must be after check-in)
//    private func getAvailableCheckoutHours() -> [Int] {
//        let calendar = Calendar.current
//        let checkInHour = calendar.component(.hour, from: checkInTime)
//        
//        // Checkout must be at least 1 hour after check-in, up to 8 PM
//        return Array((checkInHour + 1)...20)
//    }
//
//    // ✅ NEW: Create date with specific hour
//    private func createDate(hour: Int) -> Date {
//        let calendar = Calendar.current
//        return calendar.date(bySettingHour: hour, minute: 0, second: 0, of: bookingDate) ?? Date()
//    }
//
//    // ✅ NEW: Format hour for display
//    private func formatHour(_ hour: Int) -> String {
//        let period = hour < 12 ? "AM" : "PM"
//        let displayHour = hour == 0 ? 12 : (hour > 12 ? hour - 12 : hour)
//        return "\(displayHour):00 \(period)"
//    }
//    private func updateCheckInTime() {
//        let calendar = Calendar.current
//        checkInTime = calendar.date(bySettingHour: checkInHourSelection, minute: checkInMinuteSelection, second: 0, of: bookingDate) ?? checkInTime
//        homeViewModel.selectedStartTime = checkInTime
//    }
//
//    private func updateCheckOutTime() {
//        let calendar = Calendar.current
//        checkOutTime = calendar.date(bySettingHour: checkOutHourSelection, minute: checkOutMinuteSelection, second: 0, of: bookingDate) ?? checkOutTime
//        homeViewModel.selectedEndTime = checkOutTime
//    }
//
//
//
//
//    @ViewBuilder
//    private var timeWarning: some View {
//        HStack {
//            Image(systemName: "exclamationmark.triangle.fill")
//                .foregroundColor(.orange)
//            Text("Check-out time must be at least 30 minutes after check-in time")
//                .font(.caption)
//                .foregroundColor(.orange)
//        }
//    }
//
//    @ViewBuilder
//    private var bookingSummarySection: some View {
//        Section("Booking Summary") {
//            bookingSummaryView()
//        }
//    }
//    
//    @ViewBuilder
//    private var debugSection: some View {
//        Section("Debug Info") {
//            VStack(alignment: .leading, spacing: 4) {
//                Text("User ID: \(userId)")
//                Text("Spot ID: \(spotId)")
//                Text("Lot ID: \(lotId)")
//                Text("Vehicle: \(selectedVehicle)")
//                Text("Available Vehicles: \(vehicleOptions.count)")
//                Text("Check-in: \(formatDateTime(checkInTime))")
//                Text("Check-out: \(formatDateTime(checkOutTime))")
//                Text("Booking Date: \(formatDateOnly(bookingDate))")
//                Text("Form Valid: \(isFormValid() ? "Yes" : "No")")
//            }
//            .font(.caption)
//            .foregroundColor(.secondary)
//        }
//    }
//
//    @ToolbarContentBuilder
//    private var toolbarContent: some ToolbarContent {
//        ToolbarItem(placement: .navigationBarLeading) {
//            Button("Cancel") {
//                dismiss()
//            }
//            .foregroundColor(.red)
//        }
//
//        ToolbarItem(placement: .navigationBarTrailing) {
//            if isLoading {
//                ProgressView()
//                    .scaleEffect(0.8)
//            } else {
//                Button("Create") {
//                    createBooking()
//                }
//                .disabled(!isFormValid())
//                .foregroundColor(!isFormValid() ? .gray : .blue)
//                .fontWeight(.semibold)
//            }
//        }
//    }
//
//    @ViewBuilder
//    private func selectedParkingView(parkingSpot: ParkingSpot) -> some View {
//        VStack(alignment: .leading, spacing: 12) {
//            HStack {
//                Image(systemName: "location.circle.fill")
//                    .foregroundColor(.blue)
//                    .font(.title3)
//                
//                VStack(alignment: .leading, spacing: 4) {
//                    Text(displayZoneName(for: parkingSpot))
//                        .font(.headline)
//                        .fontWeight(.semibold)
//                    
//                    Text("Parking Location")
//                        .font(.caption)
//                        .foregroundColor(.secondary)
//                }
//                
//                Spacer()
//            }
//            
//            HStack {
//                Image(systemName: "car.circle.fill")
//                    .foregroundColor(.green)
//                    .font(.title3)
//                
//                VStack(alignment: .leading, spacing: 4) {
//                    Text("\(parkingSpot.available ?? 0) spots available")
//                        .font(.subheadline)
//                        .fontWeight(.medium)
//                    
//                    Text("Capacity: \(parkingSpot.capacity ?? 0)")
//                        .font(.caption)
//                        .foregroundColor(.secondary)
//                }
//                
//                Spacer()
//            }
//        }
//        .padding(.vertical, 4)
//    }
//    
//    private func displayZoneName(for parkingSpot: ParkingSpot) -> String {
//        if let zoneName = parkingSpot.zoneName,
//           !zoneName.isEmpty,
//           zoneName != "nil",
//           zoneName != "null" {
//            return zoneName
//        }
//        
//        switch parkingSpot.id {
//        case "ps1": return "TP Avenue Parking"
//        case "ps2": return "Medical College"
//        default:
//            if parkingSpot.id.hasPrefix("ps") {
//                let number = parkingSpot.id.replacingOccurrences(of: "ps", with: "")
//                return "Parking Zone \(number.uppercased())"
//            }
//            return "Parking Location \(parkingSpot.id.uppercased())"
//        }
//    }
//
//    private func setupInitialData() {
//        userId = authViewModel.getCurrentUserId() ?? ""
//
//        if let parking = selectedParking {
//            spotId = parking.id
//            lotId = parking.lotId ?? "default-lot"
//        }
//    }
//
//    private func isFormValid() -> Bool {
//        let hasUserId = !userId.isEmpty
//        let hasVehicle = !selectedVehicle.isEmpty
//        let hasSpotId = !spotId.isEmpty
//        let hasLotId = !lotId.isEmpty
//        let hasParking = selectedParking != nil
//        
//        // ✅ UPDATED: Check times are full hours and valid range
//        let calendar = Calendar.current
//        let checkInHour = calendar.component(.hour, from: checkInTime)
//        let checkInMinute = calendar.component(.minute, from: checkInTime)
//        let checkOutHour = calendar.component(.hour, from: checkOutTime)
//        let checkOutMinute = calendar.component(.minute, from: checkOutTime)
//        
//        let validTimeFormat = checkInMinute == 0 && checkOutMinute == 0
//        let validTimeRange = (checkInHour >= 8 && checkInHour < 20) &&
//                             (checkOutHour > 8 && checkOutHour <= 20)
//        let validDuration = checkOutTime > checkInTime.addingTimeInterval(3600)
//        let notInPast = checkInTime >= Date().addingTimeInterval(-300)
//        let validDate = isDateOnBookingDay(checkInTime) && isDateOnBookingDay(checkOutTime)
//
//        return hasUserId && hasVehicle && hasSpotId && hasLotId && hasParking &&
//               validTimeFormat && validTimeRange && validDuration && validDate && notInPast
//    }
//
//    
//    private func isTimeWithinAllowedRange(_ time: Date) -> Bool {
//        let calendar = Calendar.current
//        let hour = calendar.component(.hour, from: time)
//        return hour >= 8 && hour <= 20
//    }
//
//    private func isDateOnBookingDay(_ date: Date) -> Bool {
//        let calendar = Calendar.current
//        let today = calendar.startOfDay(for: Date())
//        let tomorrow = calendar.date(byAdding: .day, value: 1, to: today)!
//        let dateDay = calendar.startOfDay(for: date)
//        
//        return dateDay == today || dateDay == tomorrow
//    }
//    
//    // ✅ NEW: Get current user ID helper
//    private func getCurrentUserId() -> String? {
//        if let userId = UserDefaults.standard.string(forKey: "currentUserId") {
//            return userId
//        }
//        
//        if let userData = UserDefaults.standard.data(forKey: "userData"),
//           let user = try? JSONDecoder().decode(Users.self, from: userData) {
//            return user.id
//        }
//        
//        return nil
//    }
//
//    private func formatDateOnly(_ date: Date) -> String {
//        let formatter = DateFormatter()
//        formatter.dateStyle = .medium
//        formatter.timeStyle = .none
//        return formatter.string(from: date)
//    }
//    
//    private func formatDateTime(_ date: Date) -> String {
//        let formatter = DateFormatter()
//        formatter.dateStyle = .short
//        formatter.timeStyle = .short
//        return formatter.string(from: date)
//    }
//
//    private func createBooking() {
//        guard isFormValid() else {
//            showError("Please ensure all fields are filled correctly")
//            return
//        }
//        
//        guard let currentUserId = authViewModel.getCurrentUserId(),
//              userId == currentUserId else {
//            showError("Invalid user session. Please log in again.")
//            return
//        }
//        
//        guard selectedParking != nil else {
//            showError("Please select a parking spot first")
//            return
//        }
//        
//        isLoading = true
//        
//        let finalSpotId = selectedParking?.id ?? spotId
//        let finalLotId = lotId
//        
//        let isoFormatter = ISO8601DateFormatter()
//        isoFormatter.formatOptions = [.withInternetDateTime]
//        isoFormatter.timeZone = TimeZone(identifier: "UTC")
//        
//        let checkInString = isoFormatter.string(from: checkInTime)
//        let checkOutString = isoFormatter.string(from: checkOutTime)
//        
//        print("📋 Creating booking:")
//        print("   User: \(currentUserId)")
//        print("   Vehicle: \(selectedVehicle)")
//        print("   Booking Date: \(formatDateOnly(bookingDate))")
//        print("   Check-in: \(checkInString)")
//        print("   Check-out: \(checkOutString)")
//        
//        APIService.shared.createBooking(
//            spotId: finalSpotId,
//            userId: currentUserId,
//            lotId: finalLotId,
//            vehicleNumber: selectedVehicle,
//            checkInTime: checkInString,
//            checkOutTime: checkOutString
//        ) { result in
//            DispatchQueue.main.async {
//                self.isLoading = false
//                
//                switch result {
//                case .success(let booking):
//                    print("✅ Booking created successfully")
//                    self.bookingDetails = booking
//                    self.showingBookingSummary = true
//                    self.homeViewModel.fetchAllData()
//                    
//                case .failure(let error):
//                    print("❌ Booking failed: \(error)")
//                    self.showError("Failed to create booking: \(error.localizedDescription)")
//                }
//            }
//        }
//    }
//
//    private func showError(_ message: String) {
//        errorMessage = message
//        showingError = true
//    }
//
//    private func calculateTotal() -> Double {
//        let hours = checkOutTime.timeIntervalSince(checkInTime) / 3600.0
//        return hours * homeViewModel.parkingConfig.hourlyRate
//    }
//
//    @ViewBuilder
//    private func bookingSummaryView() -> some View {
//        VStack(alignment: .leading, spacing: 8) {
//            HStack {
//                Text("Duration:")
//                Spacer()
//                Text(formatDuration(checkOutTime.timeIntervalSince(checkInTime) / 3600.0))
//                    .fontWeight(.medium)
//            }
//            
//            HStack {
//                Text("Rate:")
//                Spacer()
//                Text("₹\(String(format: "%.2f", homeViewModel.parkingConfig.hourlyRate))/hr")
//                    .fontWeight(.medium)
//            }
//            
//            Divider()
//            
//            HStack {
//                Text("Estimated Total:")
//                    .fontWeight(.semibold)
//                Spacer()
//                Text("₹\(String(format: "%.2f", calculateTotal()))")
//                    .fontWeight(.bold)
//                    .foregroundColor(.green)
//            }
//        }
//    }
//
//    private func formatDuration(_ hours: Double) -> String {
//        let totalMinutes = Int(hours * 60)
//        let hrs = totalMinutes / 60
//        let mins = totalMinutes % 60
//        
//        if hrs > 0 && mins > 0 {
//            return "\(hrs)h \(mins)m"
//        } else if hrs > 0 {
//            return "\(hrs)h"
//        } else {
//            return "\(mins)m"
//        }
//    }
//}
//
//struct AddVehicleSheet: View {
//    @Binding var newVehicleNumber: String
//    let onAdd: (String) -> Void
//    @Environment(\.dismiss) private var dismiss
//
//    var body: some View {
//        NavigationView {
//            Form {
//                Section("Add New Vehicle") {
//                    TextField("Vehicle Number (e.g., KA01AB1234)", text: $newVehicleNumber)
//                        .textFieldStyle(RoundedBorderTextFieldStyle())
//                        .autocapitalization(.allCharacters)
//                        .autocorrectionDisabled()
//                }
//                
//                Section {
//                    Text("Enter your vehicle registration number. This will be used for parking bookings.")
//                        .font(.caption)
//                        .foregroundColor(.secondary)
//                }
//            }
//            .navigationTitle("Add Vehicle")
//            .navigationBarTitleDisplayMode(.inline)
//            .toolbar {
//                ToolbarItem(placement: .navigationBarLeading) {
//                    Button("Cancel") {
//                        dismiss()
//                    }
//                }
//                ToolbarItem(placement: .navigationBarTrailing) {
//                    Button("Add") {
//                        let trimmedVehicle = newVehicleNumber.trimmingCharacters(in: .whitespacesAndNewlines)
//                        onAdd(trimmedVehicle)
//                        dismiss()
//                    }
//                    .disabled(newVehicleNumber.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
//                }
//            }
//        }
//    }
//}


import SwiftUI

struct CreateBookingView: View {
    let selectedParking: ParkingSpot?
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject var homeViewModel: HomeViewModel
    @EnvironmentObject var authViewModel: AuthViewModel
    
    @ObservedObject private var vehicleManager = SharedVehicleManager.shared

    @State private var userId = ""
    @State private var spotId = ""
    @State private var lotId = ""
    @State private var selectedVehicle = ""
    @State private var vehicleOptions: [String] = []
    @State private var showingAddVehicle = false
    @State private var newVehicleNumber = ""
    @State private var checkInTime = Date()
    @State private var checkOutTime = Date().addingTimeInterval(3600)
    @State private var isLoading = false
    @State private var isLoadingVehicles = false
    @State private var errorMessage = ""
    @State private var showingError = false
    
    // ✅ UPDATED: Hour and minute selections for 30-min intervals
    @State private var checkInHourSelection: Int = 8
    @State private var checkInMinuteSelection: Int = 0
    @State private var checkOutHourSelection: Int = 9
    @State private var checkOutMinuteSelection: Int = 0
    
    @State private var showingBookingSummary = false
    @State private var bookingDetails: Bookings?

    init(selectedParking: ParkingSpot? = nil) {
        self.selectedParking = selectedParking
    }
    
    private var bookingDate: Date {
        let calendar = Calendar.current
        let now = Date()
        let currentHour = calendar.component(.hour, from: now)
        
        if currentHour >= 20 {
            return calendar.date(byAdding: .day, value: 1, to: now) ?? now
        }
        
        return now
    }

    var body: some View {
        NavigationView {
            formContent
                .navigationTitle("Create Booking")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    toolbarContent
                }
        }
        .sheet(isPresented: $showingAddVehicle) {
            AddVehicleSheet(newVehicleNumber: $newVehicleNumber) { vehicle in
                vehicleManager.addVehicleByNumber(vehicle)
                vehicleOptions.append(vehicle)
                selectedVehicle = vehicle
                homeViewModel.userVehicles = vehicleManager.getVehicleNumbers()
            }
        }
        .sheet(isPresented: $showingBookingSummary) {
            if let details = bookingDetails {
                BookingSummaryView(booking: details)
                    .environmentObject(homeViewModel)
            }
        }
        .alert("Error", isPresented: $showingError) {
            Button("OK") {
                errorMessage = ""
                showingError = false
            }
        } message: {
            Text(errorMessage)
        }
        .onAppear {
            setupInitialData()
            
            // ✅ Initialize hour selections
            let calendar = Calendar.current
            checkInHourSelection = max(calendar.component(.hour, from: Date()), 8)
            checkInMinuteSelection = 0
            checkOutHourSelection = checkInHourSelection + 1
            checkOutMinuteSelection = 0
            
            updateCheckInTime()  // ✅ FIXED: No arguments
            updateCheckOutTime()  // ✅ FIXED: No arguments
            
            // Load vehicles
            if let currentUserId = getCurrentUserId() {
                loadUserVehicles(userId: currentUserId)
            }
        }
    }
    
    private func loadUserVehicles(userId: String) {
        print("🚗 Loading vehicles for user: \(userId)")
        isLoadingVehicles = true
        
        APIService.shared.fetchUserVehicles(userId: userId) { result in
            DispatchQueue.main.async {
                self.isLoadingVehicles = false
                
                switch result {
                case .success(let vehicles):
                    print("✅ Loaded \(vehicles.count) vehicles: \(vehicles)")
                    self.vehicleOptions = vehicles
                    
                    if !vehicles.isEmpty && self.selectedVehicle.isEmpty {
                        self.selectedVehicle = vehicles[0]
                    }
                    
                    for vehicle in vehicles {
                        self.vehicleManager.addVehicleByNumber(vehicle)
                    }
                    
                case .failure(let error):
                    print("❌ Failed to load vehicles: \(error)")
                    let fallbackVehicles = self.vehicleManager.getVehicleNumbers()
                    if !fallbackVehicles.isEmpty {
                        self.vehicleOptions = fallbackVehicles
                        self.selectedVehicle = fallbackVehicles[0]
                    } else {
                        let defaultVehicle = "USER_\(String(userId.prefix(6)))"
                        self.vehicleOptions = [defaultVehicle]
                        self.selectedVehicle = defaultVehicle
                    }
                }
            }
        }
    }

    @ViewBuilder
    private var formContent: some View {
        Form {
            if let parking = selectedParking {
                Section("Selected Parking") {
                    selectedParkingView(parkingSpot: parking)
                }
            } else {
                Section("Warning") {
                    HStack {
                        Image(systemName: "exclamationmark.triangle.fill")
                            .foregroundColor(.orange)
                        Text("No parking spot selected")
                            .foregroundColor(.orange)
                    }
                }
            }

            vehicleSelectionSection
            timingSection
            bookingSummarySection
            debugSection
        }
    }

    @ViewBuilder
    private var vehicleSelectionSection: some View {
        Section("Vehicle Selection") {
            if isLoadingVehicles {
                HStack {
                    ProgressView()
                    Text("Loading vehicles...")
                        .foregroundColor(.secondary)
                }
            } else if vehicleOptions.isEmpty {
                HStack {
                    Image(systemName: "car.fill")
                        .foregroundColor(.gray)
                    Text("No vehicles added")
                        .foregroundColor(.gray)
                }
            } else {
                vehiclePicker
            }

            Button("Add New Vehicle") {
                showingAddVehicle = true
            }
            .foregroundColor(.blue)
        }
    }

    @ViewBuilder
    private var vehiclePicker: some View {
        Picker("Select Vehicle", selection: $selectedVehicle) {
            ForEach(vehicleOptions, id: \.self) { vehicle in
                Text(vehicle)
                    .tag(vehicle)
            }
        }
        .pickerStyle(MenuPickerStyle())
    }

    @ViewBuilder
    private var timingSection: some View {
        Section {
            VStack(spacing: 0) {
                HourWheelPicker(
                    checkInHour: $checkInHourSelection,
                    checkInMinute: $checkInMinuteSelection,
                    checkOutHour: $checkOutHourSelection,
                    checkOutMinute: $checkOutMinuteSelection
                )
                .onChange(of: checkInHourSelection) { _, _ in
                    updateCheckInTime()  // ✅ FIXED: No arguments
                }
                .onChange(of: checkInMinuteSelection) { _, _ in
                    updateCheckInTime()  // ✅ FIXED: No arguments
                }
                .onChange(of: checkOutHourSelection) { _, _ in
                    updateCheckOutTime()  // ✅ FIXED: No arguments
                }
                .onChange(of: checkOutMinuteSelection) { _, _ in
                    updateCheckOutTime()  // ✅ FIXED: No arguments
                }
            }
            .listRowInsets(EdgeInsets(top: 12, leading: 16, bottom: 12, trailing: 16))
            .listRowBackground(Color.clear)
        } header: {
            Text("TIMING")
                .font(.caption2)
                .fontWeight(.heavy)
                .tracking(1)
        }
    }

    // ✅ FIXED: No arguments needed
    private func updateCheckInTime() {
        let calendar = Calendar.current
        checkInTime = calendar.date(bySettingHour: checkInHourSelection, minute: checkInMinuteSelection, second: 0, of: bookingDate) ?? checkInTime
        homeViewModel.selectedStartTime = checkInTime
    }

    // ✅ FIXED: No arguments needed
    private func updateCheckOutTime() {
        let calendar = Calendar.current
        checkOutTime = calendar.date(bySettingHour: checkOutHourSelection, minute: checkOutMinuteSelection, second: 0, of: bookingDate) ?? checkOutTime
        homeViewModel.selectedEndTime = checkOutTime
    }

    @ViewBuilder
    private var bookingSummarySection: some View {
        Section("Booking Summary") {
            bookingSummaryView()
        }
    }
    
    @ViewBuilder
    private var debugSection: some View {
        Section("Debug Info") {
            VStack(alignment: .leading, spacing: 4) {
                Text("User ID: \(userId)")
                Text("Spot ID: \(spotId)")
                Text("Lot ID: \(lotId)")
                Text("Vehicle: \(selectedVehicle)")
                Text("Available Vehicles: \(vehicleOptions.count)")
                Text("Check-in: \(formatDateTime(checkInTime))")
                Text("Check-out: \(formatDateTime(checkOutTime))")
                Text("Booking Date: \(formatDateOnly(bookingDate))")
                Text("Form Valid: \(isFormValid() ? "Yes" : "No")")
            }
            .font(.caption)
            .foregroundColor(.secondary)
        }
    }

    @ToolbarContentBuilder
    private var toolbarContent: some ToolbarContent {
        ToolbarItem(placement: .navigationBarLeading) {
            Button("Cancel") {
                dismiss()
            }
            .foregroundColor(.red)
        }

        ToolbarItem(placement: .navigationBarTrailing) {
            if isLoading {
                ProgressView()
                    .scaleEffect(0.8)
            } else {
                Button("Create") {
                    createBooking()
                }
                .disabled(!isFormValid())
                .foregroundColor(!isFormValid() ? .gray : .blue)
                .fontWeight(.semibold)
            }
        }
    }

    @ViewBuilder
    private func selectedParkingView(parkingSpot: ParkingSpot) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Image(systemName: "location.circle.fill")
                    .foregroundColor(.blue)
                    .font(.title3)
                
                VStack(alignment: .leading, spacing: 4) {
                    Text(displayZoneName(for: parkingSpot))
                        .font(.headline)
                        .fontWeight(.semibold)
                    
                    Text("Parking Location")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
            }
            
            HStack {
                Image(systemName: "car.circle.fill")
                    .foregroundColor(.green)
                    .font(.title3)
                
                VStack(alignment: .leading, spacing: 4) {
                    Text("\(parkingSpot.available ?? 0) spots available")
                        .font(.subheadline)
                        .fontWeight(.medium)
                    
                    Text("Capacity: \(parkingSpot.capacity ?? 0)")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
            }
        }
        .padding(.vertical, 4)
    }
    
    private func displayZoneName(for parkingSpot: ParkingSpot) -> String {
        if let zoneName = parkingSpot.zoneName,
           !zoneName.isEmpty,
           zoneName != "nil",
           zoneName != "null" {
            return zoneName
        }
        
        switch parkingSpot.id {
        case "ps1": return "TP Avenue Parking"
        case "ps2": return "Medical College"
        default:
            if parkingSpot.id.hasPrefix("ps") {
                let number = parkingSpot.id.replacingOccurrences(of: "ps", with: "")
                return "Parking Zone \(number.uppercased())"
            }
            return "Parking Location \(parkingSpot.id.uppercased())"
        }
    }

    private func setupInitialData() {
        userId = authViewModel.getCurrentUserId() ?? ""

        if let parking = selectedParking {
            spotId = parking.id
            lotId = parking.lotId ?? "default-lot"
        }
    }

    private func isFormValid() -> Bool {
        let hasUserId = !userId.isEmpty
        let hasVehicle = !selectedVehicle.isEmpty
        let hasSpotId = !spotId.isEmpty
        let hasLotId = !lotId.isEmpty
        let hasParking = selectedParking != nil
        
        let calendar = Calendar.current
        let checkInHour = calendar.component(.hour, from: checkInTime)
        let checkInMinute = calendar.component(.minute, from: checkInTime)
        let checkOutHour = calendar.component(.hour, from: checkOutTime)
        let checkOutMinute = calendar.component(.minute, from: checkOutTime)
        
        // ✅ Allow 0 and 30 minute intervals
        let validTimeFormat = (checkInMinute == 0 || checkInMinute == 30) &&
                              (checkOutMinute == 0 || checkOutMinute == 30)
        let validTimeRange = (checkInHour >= 8 && checkInHour < 20) &&
                             (checkOutHour > 8 && checkOutHour <= 20)
        // In isFormValid() function
        let validDuration = checkOutTime >= checkInTime.addingTimeInterval(3600)  // ✅ Changed > to >=

        let notInPast = checkInTime >= Date().addingTimeInterval(-300)
        let validDate = isDateOnBookingDay(checkInTime) && isDateOnBookingDay(checkOutTime)

        return hasUserId && hasVehicle && hasSpotId && hasLotId && hasParking &&
               validTimeFormat && validTimeRange && validDuration && validDate && notInPast
    }

    private func isDateOnBookingDay(_ date: Date) -> Bool {
        let calendar = Calendar.current
        let today = calendar.startOfDay(for: Date())
        let tomorrow = calendar.date(byAdding: .day, value: 1, to: today)!
        let dateDay = calendar.startOfDay(for: date)
        
        return dateDay == today || dateDay == tomorrow
    }
    
    private func getCurrentUserId() -> String? {
        if let userId = UserDefaults.standard.string(forKey: "currentUserId") {
            return userId
        }
        
        if let userData = UserDefaults.standard.data(forKey: "userData"),
           let user = try? JSONDecoder().decode(Users.self, from: userData) {
            return user.id
        }
        
        return nil
    }

    private func formatDateOnly(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .none
        return formatter.string(from: date)
    }
    
    private func formatDateTime(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateStyle = .short
        formatter.timeStyle = .short
        return formatter.string(from: date)
    }

    private func createBooking() {
        guard isFormValid() else {
            showError("Please ensure all fields are filled correctly")
            return
        }
        
        guard let currentUserId = authViewModel.getCurrentUserId(),
              userId == currentUserId else {
            showError("Invalid user session. Please log in again.")
            return
        }
        
        guard selectedParking != nil else {
            showError("Please select a parking spot first")
            return
        }
        
        isLoading = true
        
        let finalSpotId = selectedParking?.id ?? spotId
        let finalLotId = lotId
        
        let isoFormatter = ISO8601DateFormatter()
        isoFormatter.formatOptions = [.withInternetDateTime]
        isoFormatter.timeZone = TimeZone(identifier: "UTC")
        
        let checkInString = isoFormatter.string(from: checkInTime)
        let checkOutString = isoFormatter.string(from: checkOutTime)
        
        print("📋 Creating booking:")
        print("   User: \(currentUserId)")
        print("   Vehicle: \(selectedVehicle)")
        print("   Booking Date: \(formatDateOnly(bookingDate))")
        print("   Check-in: \(checkInString)")
        print("   Check-out: \(checkOutString)")
        
        APIService.shared.createBooking(
            spotId: finalSpotId,
            userId: currentUserId,
            lotId: finalLotId,
            vehicleNumber: selectedVehicle,
            checkInTime: checkInString,
            checkOutTime: checkOutString
        ) { result in
            DispatchQueue.main.async {
                self.isLoading = false
                
                switch result {
                case .success(let booking):
                    print("✅ Booking created successfully")
                    self.bookingDetails = booking
                    self.showingBookingSummary = true
                    self.homeViewModel.fetchAllData()
                    
                case .failure(let error):
                    print("❌ Booking failed: \(error)")
                    self.showError("Failed to create booking: \(error.localizedDescription)")
                }
            }
        }
    }

    private func showError(_ message: String) {
        errorMessage = message
        showingError = true
    }

    private func calculateTotal() -> Double {
        let hours = checkOutTime.timeIntervalSince(checkInTime) / 3600.0
        return hours * homeViewModel.parkingConfig.hourlyRate
    }

    @ViewBuilder
    private func bookingSummaryView() -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("Duration:")
                Spacer()
                Text(formatDuration(checkOutTime.timeIntervalSince(checkInTime) / 3600.0))
                    .fontWeight(.medium)
            }
            
            HStack {
                Text("Rate:")
                Spacer()
                Text("₹\(String(format: "%.2f", homeViewModel.parkingConfig.hourlyRate))/hr")
                    .fontWeight(.medium)
            }
            
            Divider()
            
            HStack {
                Text("Estimated Total:")
                    .fontWeight(.semibold)
                Spacer()
                Text("₹\(String(format: "%.2f", calculateTotal()))")
                    .fontWeight(.bold)
                    .foregroundColor(.green)
            }
        }
    }

    private func formatDuration(_ hours: Double) -> String {
        let totalMinutes = Int(hours * 60)
        let hrs = totalMinutes / 60
        let mins = totalMinutes % 60
        
        if hrs > 0 && mins > 0 {
            return "\(hrs)h \(mins)m"
        } else if hrs > 0 {
            return "\(hrs)h"
        } else {
            return "\(mins)m"
        }
    }
}

struct AddVehicleSheet: View {
    @Binding var newVehicleNumber: String
    let onAdd: (String) -> Void
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationView {
            Form {
                Section("Add New Vehicle") {
                    TextField("Vehicle Number (e.g., KA01AB1234)", text: $newVehicleNumber)
                        .textFieldStyle(RoundedBorderTextFieldStyle())
                        .autocapitalization(.allCharacters)
                        .autocorrectionDisabled()
                }
                
                Section {
                    Text("Enter your vehicle registration number. This will be used for parking bookings.")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            .navigationTitle("Add Vehicle")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Add") {
                        let trimmedVehicle = newVehicleNumber.trimmingCharacters(in: .whitespacesAndNewlines)
                        onAdd(trimmedVehicle)
                        dismiss()
                    }
                    .disabled(newVehicleNumber.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
        }
    }
}
