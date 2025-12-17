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
    @State private var bookingDate = Date()
    
    @State private var checkInHourSelection: Int = 8
    @State private var checkInMinuteSelection: Int = 0
    @State private var checkOutHourSelection: Int = 9
    @State private var checkOutMinuteSelection: Int = 0
    
    @State private var showingBookingSummary = false
    @State private var bookingDetails: Bookings?

    init(selectedParking: ParkingSpot? = nil) {
        self.selectedParking = selectedParking
    }

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
//            AddVehicleSheet(
//                newVehicleNumber: $newVehicleNumber,
//                onAdd: { vehicle in
//                    // Reload vehicles from backend after adding
//                    if let userId = getCurrentUserId() {
//                        loadUserVehicles(userId: userId)
//                    }
//                }
//            )
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
//            let calendar = Calendar.current
//            let now = Date()
//            let currentHour = calendar.component(.hour, from: now)
//            let currentMinute = calendar.component(.minute, from: now)
//            
//            if currentHour >= 20 {
//                bookingDate = calendar.date(byAdding: .day, value: 1, to: now) ?? now
//            } else {
//                bookingDate = now
//            }
//            
//            setupInitialData()
//            
//            let nextSlot = getNextValidTimeSlot(hour: currentHour, minute: currentMinute)
//            
//            checkInHourSelection = max(nextSlot.hour, 8)
//            checkInMinuteSelection = nextSlot.minute
//            
//            let checkOutSlot = addHoursToSlot(hour: checkInHourSelection, minute: checkInMinuteSelection, hours: 1)
//            checkOutHourSelection = checkOutSlot.hour
//            checkOutMinuteSelection = checkOutSlot.minute
//            
//            updateCheckInTime()
//            updateCheckOutTime()
//            
//            if let currentUserId = getCurrentUserId() {
//                loadUserVehicles(userId: currentUserId)
//            }
//        }
//    }
    
    var body: some View {
        NavigationView {
            formContent
                .navigationTitle("Create Booking")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    toolbarContent
                }
        }
        .presentationBackground(.black.opacity(0.98))  // Add solid background
        .sheet(isPresented: $showingAddVehicle) {
            AddVehicleSheet(
                newVehicleNumber: $newVehicleNumber,
                onAdd: { vehicle in
                    if let userId = getCurrentUserId() {
                        loadUserVehicles(userId: userId)
                    }
                }
            )
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
            let calendar = Calendar.current
            let now = Date()
            let currentHour = calendar.component(.hour, from: now)
            
            // ✅ If it's evening (after 8 PM or current hour > 8), book for tomorrow
            if currentHour >= 20 {
                bookingDate = calendar.date(byAdding: .day, value: 1, to: now) ?? now
                print("⏰ Evening time - Booking for TOMORROW")
            } else {
                bookingDate = now
                print("⏰ Morning time - Booking for TODAY")
            }
            
            // Set morning times
            checkInHourSelection = 8    // 8 AM
            checkInMinuteSelection = 0
            checkOutHourSelection = 9   // 9 AM
            checkOutMinuteSelection = 30
            
            setupInitialData()
            updateCheckInTime()
            updateCheckOutTime()
            
            if let currentUserId = getCurrentUserId() {
                loadUserVehicles(userId: currentUserId)
            }
        }



    }

    private func getNextValidTimeSlot(hour: Int, minute: Int) -> (hour: Int, minute: Int) {
        // Add 5-minute buffer
        var totalMinutes = (hour * 60) + minute + 5
        
        // Round up to next 30-minute slot
        let remainder = totalMinutes % 30
        if remainder != 0 {
            totalMinutes += (30 - remainder)
        }
        
        var nextHour = totalMinutes / 60
        let nextMinute = totalMinutes % 60
        
        // Ensure within operating hours (8 AM - 8 PM)
        if nextHour >= 20 {
            // If past 8 PM, set to tomorrow 8 AM
            nextHour = 8
            return (nextHour, 0)
        }
        
        if nextHour < 8 {
            // If before 8 AM, set to 8 AM
            nextHour = 8
            return (nextHour, 0)
        }
        
        print("⏰ Time calculation:")
        print("   Current: \(hour):\(String(format: "%02d", minute))")
        print("   With buffer: \(hour):\(String(format: "%02d", minute + 5))")
        print("   Next slot: \(nextHour):\(String(format: "%02d", nextMinute))")
        
        return (nextHour, nextMinute)
    }


    private func addHoursToSlot(hour: Int, minute: Int, hours: Int) -> (hour: Int, minute: Int) {
        let newHour = (hour + hours) % 24
        
        if newHour > 20 || (newHour == 20 && minute > 0) {
            return (20, 0)
        }
        
        return (newHour, minute)
    }

    // ✅ SIMPLIFIED: Only fetch from backend
    private func loadUserVehicles(userId: String) {
        print("🚗 Fetching vehicles from backend for user: \(userId)")
        isLoadingVehicles = true
        
        APIService.shared.fetchUserVehicles(userId: userId) { result in
            DispatchQueue.main.async {
                self.isLoadingVehicles = false
                
                switch result {
                case .success(let vehicles):
                    print("✅ Loaded \(vehicles.count) vehicles from backend: \(vehicles)")
                    self.vehicleOptions = vehicles
                    self.selectedVehicle = vehicles.first ?? ""
                    
                    // Sync to local manager
                    self.vehicleManager.vehicles = vehicles.map { VehicleData(id: UUID(), registrationNumber: $0) }
                    
                case .failure(let error):
                    print("❌ Failed to load vehicles: \(error)")
                    self.vehicleOptions = []
                    self.selectedVehicle = ""
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
                    checkOutMinute: $checkOutMinuteSelection,
                    bookingDate: bookingDate
                )
                .onChange(of: checkInHourSelection) { _, _ in updateCheckInTime() }
                .onChange(of: checkInMinuteSelection) { _, _ in updateCheckInTime() }
                .onChange(of: checkOutHourSelection) { _, _ in updateCheckOutTime() }
                .onChange(of: checkOutMinuteSelection) { _, _ in updateCheckOutTime() }
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

    private func updateCheckInTime() {
        let calendar = Calendar.current
        checkInTime = calendar.date(bySettingHour: checkInHourSelection, minute: checkInMinuteSelection, second: 0, of: bookingDate) ?? checkInTime
        homeViewModel.selectedStartTime = checkInTime
    }

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
        if let zoneName = parkingSpot.zoneName, !zoneName.isEmpty {
            return zoneName
        }
        return "Parking Zone"
    }

    private func setupInitialData() {
        userId = authViewModel.getCurrentUserId() ?? ""

        if let parking = selectedParking {
            spotId = parking.id
            lotId = parking.lotId ?? "default-lot"
        }
    }

    private func isFormValid() -> Bool {
        return !userId.isEmpty && !selectedVehicle.isEmpty && !spotId.isEmpty && selectedParking != nil
    }

    private func getCurrentUserId() -> String? {
        return authViewModel.getCurrentUserId()
    }

//    private func createBooking() {
//        guard isFormValid() else {
//            showError("Please ensure all fields are filled correctly")
//            return
//        }
//        
//        isLoading = true
//        
//        let dateFormatter = DateFormatter()
//        dateFormatter.dateFormat = "dd-MM-yyyy HH:mm"
//        dateFormatter.timeZone = TimeZone.current
//        
//        // ✅ CRITICAL FIX: Use current date/time for the base
//        let now = Date()
//        let calendar = Calendar.current
//        let currentHour = calendar.component(.hour, from: now)
//        
//        var checkInDate: Date
//        var checkOutDate: Date
//        
//        if currentHour >= 20 {
//            // After 8 PM - book for tomorrow
//            if let tomorrow = calendar.date(byAdding: .day, value: 1, to: now) {
//                checkInDate = calendar.date(bySettingHour: checkInHourSelection, minute: checkInMinuteSelection, second: 0, of: tomorrow) ?? checkInTime
//                checkOutDate = calendar.date(bySettingHour: checkOutHourSelection, minute: checkOutMinuteSelection, second: 0, of: tomorrow) ?? checkOutTime
//            } else {
//                checkInDate = checkInTime
//                checkOutDate = checkOutTime
//            }
//        } else {
//            // Before 8 PM - book for today
//            checkInDate = calendar.date(bySettingHour: checkInHourSelection, minute: checkInMinuteSelection, second: 0, of: now) ?? checkInTime
//            checkOutDate = calendar.date(bySettingHour: checkOutHourSelection, minute: checkOutMinuteSelection, second: 0, of: now) ?? checkOutTime
//        }
//        
//        let checkInString = dateFormatter.string(from: checkInDate)
//        let checkOutString = dateFormatter.string(from: checkOutDate)
//        
//        print("📋 Creating booking for vehicle: \(selectedVehicle)")
//        print("📅 Check-in: \(checkInString)")
//        print("📅 Check-out: \(checkOutString)")
//        
//        APIService.shared.createBooking(
//            spotId: spotId,
//            userId: userId,
//            lotId: lotId,
//            vehicleNumber: selectedVehicle,
//            checkInTime: checkInString,
//            checkOutTime: checkOutString
//        ) { result in
//            DispatchQueue.main.async {
//                self.isLoading = false
//                
//                switch result {
//                case .success(let booking):
//                    print("✅ Booking created successfully: \(booking.id)")
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
    private func createBooking() {
        guard isFormValid() else {
            showError("Please ensure all fields are filled correctly")
            return
        }
        
        isLoading = true
        
        // ✅ Backend expects format "dd-MM-yyyy HH:mm" but in UTC
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "dd-MM-yyyy HH:mm"
        dateFormatter.timeZone = TimeZone(identifier: "UTC") // ✅ Backend timezone
        
        let calendar = Calendar(identifier: .gregorian)
        var istCalendar = Calendar(identifier: .gregorian)
        istCalendar.timeZone = TimeZone(identifier: "Asia/Kolkata")! // IST for user display
        
        let now = Date()
        let currentHour = istCalendar.component(.hour, from: now)
        
        // Determine correct booking date in IST
        var bookingBaseDate: Date
        if currentHour >= 20 {
            bookingBaseDate = istCalendar.date(byAdding: .day, value: 1, to: istCalendar.startOfDay(for: now)) ?? now
            print("⏰ After 8 AM IST - Booking for TOMORROW")
        } else {
            bookingBaseDate = istCalendar.startOfDay(for: now)
            print("⏰ Before 8 AM IST - Booking for TODAY")
        }
        
        // Create booking times in IST first
        var checkInComponents = istCalendar.dateComponents([.year, .month, .day], from: bookingBaseDate)
        checkInComponents.hour = checkInHourSelection
        checkInComponents.minute = checkInMinuteSelection
        checkInComponents.second = 0
        checkInComponents.timeZone = TimeZone(identifier: "Asia/Kolkata")
        
        var checkOutComponents = istCalendar.dateComponents([.year, .month, .day], from: bookingBaseDate)
        checkOutComponents.hour = checkOutHourSelection
        checkOutComponents.minute = checkOutMinuteSelection
        checkOutComponents.second = 0
        checkOutComponents.timeZone = TimeZone(identifier: "Asia/Kolkata")
        
        guard let checkInDate = istCalendar.date(from: checkInComponents),
              let checkOutDate = istCalendar.date(from: checkOutComponents) else {
            showError("Invalid date configuration")
            isLoading = false
            return
        }
        
        // Validate times
        if checkInDate < now {
            showError("Check-in time must be in the future")
            isLoading = false
            return
        }
        
        if checkOutDate <= checkInDate {
            showError("Check-out must be after check-in")
            isLoading = false
            return
        }
        
        // ✅ Convert to UTC and format for backend
        let checkInString = dateFormatter.string(from: checkInDate)
        let checkOutString = dateFormatter.string(from: checkOutDate)
        
        print("📋 Creating booking for vehicle: \(selectedVehicle)")
        print("📅 Check-in IST: \(checkInHourSelection):00 on \(bookingBaseDate)")
        print("📅 Check-in UTC (Backend): \(checkInString)")
        print("📅 Check-out UTC (Backend): \(checkOutString)")
        
        APIService.shared.createBooking(
            spotId: spotId,
            userId: userId,
            lotId: lotId,
            vehicleNumber: selectedVehicle,
            checkInTime: checkInString,
            checkOutTime: checkOutString
        ) { result in
            DispatchQueue.main.async {
                self.isLoading = false
                switch result {
                case .success(let booking):
                    print("✅ ✅ ✅ BOOKING CREATED SUCCESSFULLY!")
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

// MARK: - Add Vehicle Sheet
struct AddVehicleSheet: View {
    @Binding var newVehicleNumber: String
    let onAdd: (String) -> Void
    @Environment(\.dismiss) private var dismiss
    
    @State private var isSaving = false

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
                    .disabled(isSaving)
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    if isSaving {
                        ProgressView()
                            .scaleEffect(0.8)
                    } else {
                        Button("Add") {
                            addVehicle()
                        }
                        .disabled(newVehicleNumber.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    }
                }
            }
        }
    }
    
    // ✅ SIMPLIFIED: Just call backend
    private func addVehicle() {
        let trimmed = newVehicleNumber.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        
        isSaving = true
        
        SharedVehicleManager.shared.addVehicleToBackend(trimmed) { success, message in
            DispatchQueue.main.async {
                self.isSaving = false
                
                if success {
                    print("✅ Vehicle added: \(trimmed)")
                    onAdd(trimmed)
                    dismiss()
                } else {
                    print("❌ Failed to add vehicle: \(message ?? "Unknown error")")
                    // Show error or just dismiss
                    dismiss()
                }
            }
        }
    }
}



//just for testing

//import SwiftUI
//
//struct CreateBookingView: View {
//
//    let selectedParking: ParkingSpot?
//
//    @Environment(\.dismiss) private var dismiss
//    @EnvironmentObject var homeViewModel: HomeViewModel
//    @EnvironmentObject var authViewModel: AuthViewModel
//
//    @ObservedObject private var vehicleManager = SharedVehicleManager.shared
//
//    @State private var userId = ""
//    @State private var spotId = ""
//    @State private var lotId = ""
//
//    @State private var selectedVehicle = ""
//    @State private var vehicleOptions: [String] = []
//
//    @State private var showingAddVehicle = false
//    @State private var newVehicleNumber = ""
//
//    @State private var bookingDate = Date()
//
//    @State private var checkInHourSelection = 8
//    @State private var checkInMinuteSelection = 0
//    @State private var checkOutHourSelection = 9
//    @State private var checkOutMinuteSelection = 0
//
//    @State private var checkInTime = Date()
//    @State private var checkOutTime = Date().addingTimeInterval(3600)
//
//    @State private var isLoading = false
//    @State private var isLoadingVehicles = false
//
//    @State private var errorMessage = ""
//    @State private var showingError = false
//
//    @State private var showingBookingSummary = false
//    @State private var bookingDetails: Bookings?
//
//    init(selectedParking: ParkingSpot? = nil) {
//        self.selectedParking = selectedParking
//    }
//
//    // MARK: - BODY
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
//            AddVehicleSheet(
//                newVehicleNumber: $newVehicleNumber,
//                onAdd: { _ in
//                    if let id = getCurrentUserId() {
//                        loadUserVehicles(userId: id)
//                    }
//                }
//            )
//        }
//        .sheet(isPresented: $showingBookingSummary) {
//            if let bookingDetails {
//                BookingSummaryView(booking: bookingDetails)
//                    .environmentObject(homeViewModel)
//            }
//        }
//        .alert("Error", isPresented: $showingError) {
//            Button("OK") { showingError = false }
//        } message: {
//            Text(errorMessage)
//        }
//        .onAppear(perform: setup)
//    }
//
//    // MARK: - SETUP
//    private func setup() {
//        userId = authViewModel.getCurrentUserId() ?? ""
//
//        guard let parking = selectedParking else {
//            showError("No parking selected")
//            return
//        }
//
//        spotId = parking.id
//
//        // ✅ FIX: Do NOT send fake lotId to backend
//        guard let validLotId = parking.lotId, !validLotId.isEmpty else {
//            showError("Invalid parking configuration")
//            return
//        }
//        lotId = validLotId
//
//        let calendar = Calendar.current
//        let hour = calendar.component(.hour, from: Date())
//
//        bookingDate = hour >= 20
//            ? calendar.date(byAdding: .day, value: 1, to: Date()) ?? Date()
//            : Date()
//
//        updateCheckInTime()
//        updateCheckOutTime()
//
//        if let id = getCurrentUserId() {
//            loadUserVehicles(userId: id)
//        }
//    }
//
//    // MARK: - UI
//    private var formContent: some View {
//        Form {
//            Section("Selected Parking") {
//                Text(selectedParking?.zoneName ?? "Parking Zone")
//            }
//
//            vehicleSelectionSection
//            timingSection
//        }
//    }
//
//    private var vehicleSelectionSection: some View {
//        Section("Vehicle") {
//            if isLoadingVehicles {
//                ProgressView()
//            } else {
//                Picker("Select Vehicle", selection: $selectedVehicle) {
//                    ForEach(vehicleOptions, id: \.self) {
//                        Text($0).tag($0)
//                    }
//                }
//            }
//
//            Button("Add New Vehicle") {
//                showingAddVehicle = true
//            }
//        }
//    }
//
//    private var timingSection: some View {
//        Section("Timing") {
//            HourWheelPicker(
//                checkInHour: $checkInHourSelection,
//                checkInMinute: $checkInMinuteSelection,
//                checkOutHour: $checkOutHourSelection,
//                checkOutMinute: $checkOutMinuteSelection,
//                bookingDate: bookingDate
//            )
//            .onChange(of: checkInHourSelection) { _, _ in updateCheckInTime() }
//            .onChange(of: checkInMinuteSelection) { _, _ in updateCheckInTime() }
//            .onChange(of: checkOutHourSelection) { _, _ in updateCheckOutTime() }
//            .onChange(of: checkOutMinuteSelection) { _, _ in updateCheckOutTime() }
//        }
//    }
//
//    // MARK: - TOOLBAR
//    @ToolbarContentBuilder
//    private var toolbarContent: some ToolbarContent {
//        ToolbarItem(placement: .navigationBarLeading) {
//            Button("Cancel") { dismiss() }
//                .foregroundColor(.red)
//        }
//
//        ToolbarItem(placement: .navigationBarTrailing) {
//            if isLoading {
//                ProgressView()
//            } else {
//                Button("Create") {
//                    createBooking()
//                }
//            }
//        }
//    }
//
//    // MARK: - LOGIC
//    private func updateCheckInTime() {
//        let calendar = Calendar.current
//        checkInTime = calendar.date(
//            bySettingHour: checkInHourSelection,
//            minute: checkInMinuteSelection,
//            second: 0,
//            of: bookingDate
//        ) ?? checkInTime
//    }
//
//    private func updateCheckOutTime() {
//        let calendar = Calendar.current
//        checkOutTime = calendar.date(
//            bySettingHour: checkOutHourSelection,
//            minute: checkOutMinuteSelection,
//            second: 0,
//            of: bookingDate
//        ) ?? checkOutTime
//    }
//
//    private func getCurrentUserId() -> String? {
//        authViewModel.getCurrentUserId()
//    }
//
//    private func loadUserVehicles(userId: String) {
//        isLoadingVehicles = true
//        APIService.shared.fetchUserVehicles(userId: userId) { result in
//            DispatchQueue.main.async {
//                self.isLoadingVehicles = false
//                if case let .success(vehicles) = result {
//                    self.vehicleOptions = vehicles
//                    self.selectedVehicle = vehicles.first ?? ""
//                }
//            }
//        }
//    }
//
//    // MARK: - CREATE BOOKING
//    private func createBooking() {
//        guard !spotId.isEmpty, !lotId.isEmpty else {
//            showError("Parking information missing")
//            return
//        }
//
//        isLoading = true
//
//        let formatter = DateFormatter()
//        formatter.dateFormat = "dd-MM-yyyy HH:mm"
//        formatter.timeZone = TimeZone(identifier: "UTC")
//
//        let calendar = Calendar(identifier: .gregorian)
//        let baseDate = calendar.startOfDay(for: bookingDate)
//
//        var inComp = calendar.dateComponents([.year, .month, .day], from: baseDate)
//        inComp.hour = checkInHourSelection
//        inComp.minute = checkInMinuteSelection
//
//        var outComp = calendar.dateComponents([.year, .month, .day], from: baseDate)
//        outComp.hour = checkOutHourSelection
//        outComp.minute = checkOutMinuteSelection
//
//        guard
//            let inDate = calendar.date(from: inComp),
//            let outDate = calendar.date(from: outComp),
//            outDate > inDate
//        else {
//            showError("Invalid time selection")
//            isLoading = false
//            return
//        }
//
//        APIService.shared.createBooking(
//            spotId: spotId,
//            userId: userId,
//            lotId: lotId,
//            vehicleNumber: selectedVehicle,
//            checkInTime: formatter.string(from: inDate),
//            checkOutTime: formatter.string(from: outDate)
//        ) { result in
//            DispatchQueue.main.async {
//                self.isLoading = false
//                switch result {
//                case .success(let booking):
//                    self.bookingDetails = booking
//                    self.showingBookingSummary = true
//                    self.homeViewModel.fetchAllData()
//                case .failure(let error):
//                    self.showError(error.localizedDescription)
//                }
//            }
//        }
//    }
//
//    private func showError(_ message: String) {
//        errorMessage = message
//        showingError = true
//    }
//}
//
//// MARK: - AddVehicleSheet
//struct AddVehicleSheet: View {
//
//    @Binding var newVehicleNumber: String
//    let onAdd: (String) -> Void
//
//    @Environment(\.dismiss) private var dismiss
//
//    var body: some View {
//        NavigationView {
//            Form {
//                TextField("Vehicle Number", text: $newVehicleNumber)
//                    .autocapitalization(.allCharacters)
//                    .autocorrectionDisabled()
//            }
//            .navigationTitle("Add Vehicle")
//            .toolbar {
//                ToolbarItem(placement: .navigationBarLeading) {
//                    Button("Cancel") { dismiss() }
//                }
//                ToolbarItem(placement: .navigationBarTrailing) {
//                    Button("Add") {
//                        onAdd(newVehicleNumber)
//                        dismiss()
//                    }
//                }
//            }
//        }
//    }
//}
