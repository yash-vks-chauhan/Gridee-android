


import Foundation
import Combine

class HomeViewModel: ObservableObject {
    @Published var notificationCount: Int = 0
    @Published var lastUpdated: Date = Date()
    @Published var searchText: String = ""
    @Published var isLoading: Bool = false
    @Published var parkingSpots: [ParkingSpot] = []
    @Published var userBookings: [Bookings] = []
    @Published var walletBalance: Double = 0.0
    @Published var errorMessage: String = ""

    @Published var currentBooking: Bookings? = nil
    @Published var timerString: String = ""
    @Published var showingCreateBooking: Bool = false

    // ✅ UPDATED: Synced with SharedVehicleManager
    @Published var userVehicles: [String] = []
    @Published var selectedVehicle: String = ""
    @Published var isLoadingVehicles: Bool = false
    @Published var individualBookingStates: [String: Bool] = [:]

    // ✅ ADDED: Search functionality
    @Published var isSearching: Bool = false
    @Published var searchResults: [ParkingSpot] = []

    // === Time Slot Modal State ===
    @Published var showingTimeSlotModal: Bool = false
    @Published var selectedTimeSlot: Date = Calendar.current.date(
        bySettingHour: 8, minute: 0, second: 0, of: Date())!
    @Published var selectedStartTime: Date = Calendar.current.date(bySettingHour: 8, minute: 0, second: 0, of: Date())!
    @Published var selectedEndTime: Date = Calendar.current.date(bySettingHour: 9, minute: 0, second: 0, of: Date())!
    @Published var parkingConfig: ParkingConfig = .default



    // ✅ ADD REFERENCE TO AuthViewModel
    private var authViewModel: AuthViewModel?
    var pendingBookingsCount: Int {
        userBookings.filter { $0.status.lowercased() == "pending" }.count
    }

    
    var availableParkingSpot: ParkingSpot? {
        parkingSpots.first { ($0.available ?? 0) > 0 }
    }

    var totalAvailableSpots: Int {
        parkingSpots.reduce(0) { $0 + ($1.available ?? 0) }
    }

    var activeBookingsCount: Int {
        userBookings.filter { $0.isActive }.count
    }

    var formattedWalletBalance: String {
        String(format: "₹%.2f", walletBalance)
    }

    // ✅ ADDED: Computed property for filtered parking spots based on search
    var filteredParkingSpots: [ParkingSpot] {
        if searchText.isEmpty {
            return parkingSpots
        }
        
        return parkingSpots.filter { spot in
            let locationName = getLocationName(for: spot.id).lowercased()
            let searchLower = searchText.lowercased()
            
            return locationName.contains(searchLower) ||
                   spot.id.lowercased().contains(searchLower) ||
                   (spot.available != nil && String(spot.available!).contains(searchText))
        }
    }

    private var cancellables = Set<AnyCancellable>()
    private var timer: Timer?
    private var refreshTimer: Timer?

    init() {
        print("🏠 HomeViewModel initializing...")
        startTimer()
        updateTimerString()
        fetchAllData()
        startAutoRefresh()
        setupSearchObserver()
        print("🏠 HomeViewModel initialization complete")
    }
        
    // ✅ ADDED: Search functionality
    private func setupSearchObserver() {
        $searchText
            .debounce(for: .milliseconds(300), scheduler: RunLoop.main)
            .sink { [weak self] searchText in
                self?.performSearch(searchText)
            }
            .store(in: &cancellables)
    }
    
    // ✅ ADDED: Search method
    private func performSearch(_ query: String) {
        isSearching = !query.isEmpty
        searchResults = filteredParkingSpots
        
        if !query.isEmpty {
            print("🔍 Searching for: \(query)")
            print("🔍 Found \(searchResults.count) results")
        }
    }
    
    // ✅ ADDED: Helper function for location names
    private func getLocationName(for spotId: String) -> String {
        switch spotId {
        case "ps1":
            return "TP Avenue Parking"
        case "ps2":
            return "Medical College"
        case "ps3":
            return "City Center Parking"
        default:
            if spotId.hasPrefix("ps") {
                let number = spotId.replacingOccurrences(of: "ps", with: "")
                return "Parking Zone \(number.uppercased())"
            }
            return "Parking Location \(spotId.uppercased())"
        }
    }
    
    // ✅ ADDED: Clear search method
    func clearSearch() {
        searchText = ""
        isSearching = false
        searchResults = []
    }
    func fetchParkingConfig() {
        APIService.shared.fetchParkingConfig { [weak self] result in
            DispatchQueue.main.async {
                switch result {
                case .success(let config):
                    self?.parkingConfig = config
                    print("✅ Parking config loaded: ₹\(config.hourlyRate)/hr")
                case .failure(let error):
                    print("⚠️ Failed to load config, using defaults: \(error)")
                    self?.parkingConfig = .default
                }
            }
        }
    }
    
    // ✅ CRITICAL: Set AuthViewModel reference
    func setAuthViewModel(_ authViewModel: AuthViewModel) {
        print("🔗 Setting AuthViewModel reference in HomeViewModel")
        self.authViewModel = authViewModel
        
        // ✅ IMMEDIATE: Fetch vehicles when AuthViewModel is connected
        DispatchQueue.main.async {
            self.fetchUserVehicles()
        }
        
        print("✅ AuthViewModel reference set successfully")
    }
    
    // ✅ NEW: Check if specific parking spot is being booked
    func isBooking(spotId: String) -> Bool {
        return individualBookingStates[spotId] ?? false
    }
    func fetchAvailableSpots(lotId: String, startTime: Date, endTime: Date) {
        print("🔄 Fetching available spots with time range")
        
        isLoading = true
        errorMessage = ""
        
        APIService.shared.fetchAvailableSpots(
            lotId: lotId,
            startTime: startTime,
            endTime: endTime
        ) { [weak self] result in
            DispatchQueue.main.async {
                self?.isLoading = false
                
                switch result {
                case .success(let spots):
                    print("✅ Fetched \(spots.count) available spots")
                    self?.parkingSpots = spots
                    
                    if self?.isSearching == true {
                        self?.searchResults = self?.filteredParkingSpots ?? []
                    }
                    
                case .failure(let error):
                    print("❌ Error: \(error)")
                    // ✅ FALLBACK: If API fails, fetch all spots instead
                    self?.errorMessage = "Could not filter by time. Showing all spots."
                    self?.fetchAllData()
                }
            }
        }
    }

    // ✅ UPDATED: Get user ID with multiple fallback methods
    func getCurrentUserId() -> String? {
        // Method 1: Try to get from AuthViewModel
        if let authViewModel = authViewModel {
            if let userId = authViewModel.getCurrentUserId() {
                print("✅ Got user ID from AuthViewModel: \(userId)")
                return userId
            } else {
                print("⚠️ AuthViewModel has no user ID")
            }
        } else {
            print("⚠️ No AuthViewModel reference in HomeViewModel")
        }
        
        // Method 2: Fallback to UserDefaults
        if let userId = UserDefaults.standard.string(forKey: "currentUserId") {
            print("✅ Got user ID from UserDefaults: \(userId)")
            return userId
        }
        
        // Method 3: Last fallback to stored user data
        if let userData = UserDefaults.standard.data(forKey: "userData"),
           let user = try? JSONDecoder().decode(Users.self, from: userData) {
            print("✅ Got user ID from stored userData: \(user.id)")
            return user.id
        }
        
        print("❌ No user ID found in HomeViewModel - all methods failed")
        return nil
    }

    // ✅ UPDATED: Get current user vehicle from SharedVehicleManager
    func getCurrentUserVehicle() -> String? {
        // First try SharedVehicleManager
        if let vehicle = SharedVehicleManager.shared.getPrimaryVehicle() {
            print("✅ Got current vehicle from SharedVehicleManager: \(vehicle)")
            return vehicle
        }
        
        // Then try AuthViewModel
        if let authViewModel = authViewModel,
           let vehicle = authViewModel.getCurrentUserVehicle() {
            print("✅ Got current vehicle from AuthViewModel: \(vehicle)")
            return vehicle
        }
        
        // Fallback to selected vehicle
        if !selectedVehicle.isEmpty {
            print("✅ Using selected vehicle: \(selectedVehicle)")
            return selectedVehicle
        }
        
        print("⚠️ No current vehicle found")
        return nil
    }

    // ✅ UPDATED: Fetch User Vehicles from SharedVehicleManager
    func fetchUserVehicles() {
        print("🚗 Fetching user vehicles from SharedVehicleManager...")
        
        isLoadingVehicles = true
        
        // ✅ LOAD FROM SHARED VEHICLE MANAGER
        SharedVehicleManager.shared.loadVehicles()
        
        // ✅ SYNC WITH LOCAL PROPERTIES
        userVehicles = SharedVehicleManager.shared.getVehicleNumbers()
        selectedVehicle = SharedVehicleManager.shared.getPrimaryVehicle() ?? ""
        
        isLoadingVehicles = false
        
        print("✅ HomeViewModel: Synced \(userVehicles.count) vehicles from SharedVehicleManager")
        print("   Vehicles: \(userVehicles)")
        print("   Selected: '\(selectedVehicle)'")
    }

    // ✅ UPDATED: Add vehicle through SharedVehicleManager
    func addVehicle(_ vehicle: String) {
        guard !vehicle.isEmpty else {
            print("❌ Cannot add empty vehicle")
            return
        }
        
        // ✅ ADD TO SHARED VEHICLE MANAGER
        SharedVehicleManager.shared.addVehicleByNumber(vehicle)
        
        // ✅ SYNC WITH LOCAL PROPERTIES
        userVehicles = SharedVehicleManager.shared.getVehicleNumbers()
        
        // If no vehicle is selected, select the new one
        if selectedVehicle.isEmpty {
            selectedVehicle = vehicle
        }
        
        print("✅ HomeViewModel: Added vehicle \(vehicle)")
        print("   Total vehicles: \(userVehicles.count)")
    }

    func startTimer() {
        timer?.invalidate()
        timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { [weak self] _ in
            self?.updateTimerString()
        }
    }

    func updateTimerString() {
        guard let booking = currentBooking, let _ = booking.checkInDate else {
            timerString = ""
            return
        }
        let left = booking.timeLeft
        if left <= 0 {
            timerString = "00:00:00"
        } else {
            let hours = Int(left) / 3600
            let minutes = (Int(left) % 3600) / 60
            let seconds = Int(left) % 60
            timerString = String(format: "%02d:%02d:%02d", hours, minutes, seconds)
        }
    }

    func endParking() {
        currentBooking = nil
        print("Parking ended")
    }

    func extendTime() {
        print("Extend time requested")
    }

    func fetchAllData() {
        print("🔄 Fetching all parking data...")
        isLoading = true
        errorMessage = ""
        lastUpdated = Date()
        
        // Clear individual booking states when refreshing
        individualBookingStates.removeAll()
        
        // Call your API service
        APIService.fetchParkingSpots { [weak self] (spots, error) in
            DispatchQueue.main.async {
                self?.isLoading = false
                
                if let error = error {
                    print("❌ Error fetching parking spots: \(error.localizedDescription)")
                    self?.errorMessage = error.localizedDescription
                    return
                }
                
                if let spots = spots {
                    print("✅ Successfully fetched \(spots.count) parking spots")
                    self?.parkingSpots = spots
                    
                    // ✅ UPDATE: Refresh search results if searching
                    if self?.isSearching == true {
                        self?.searchResults = self?.filteredParkingSpots ?? []
                    }
                    
                    // ✅ FIXED: Fetch real wallet balance instead of mock
                    self?.fetchWalletBalance()
                } else {
                    print("⚠️ No spots received")
                    self?.parkingSpots = []
                }
            }
        }
        
        // Also fetch user bookings and vehicles
        fetchUserBookings()
        fetchUserVehicles()
        fetchParkingConfig()
//        fetchWalletTransactions()
    }
    
    
    // ✅ NEW: Fetch wallet balance from API
    private func fetchWalletBalance() {
        guard let userId = getCurrentUserId() else {
            print("❌ HOME: No user ID found for wallet")
            return
        }
        
        print("💰 HOME: Fetching wallet balance for user: \(userId)")
        
        APIService.shared.fetchWallet(userId: userId) { [weak self] result in
            DispatchQueue.main.async {
                switch result {
                case .success(let wallet):
                    print("✅ HOME: Wallet balance updated: ₹\(wallet.balance)")
                    self?.walletBalance = wallet.balance
                    
                case .failure(let error):
                    print("❌ HOME: Failed to fetch wallet: \(error)")
                    self?.walletBalance = 0.0
                }
            }
        }
    }
    
    // ✅ UPDATED: Fetch user-specific bookings with better error handling
    private func fetchUserBookings() {
        guard let userId = getCurrentUserId() else {
            print("❌ Cannot fetch user bookings - no user ID")
            return
        }
        
        print("🔄 Fetching bookings for user: \(userId)")
        APIService.shared.fetchUserBookings(userId: userId) { [weak self] result in
            DispatchQueue.main.async {
                switch result {
                case .success(let bookings):
                    print("✅ Fetched \(bookings.count) user bookings")
                    self?.userBookings = bookings
                    
                    // Set current booking if there's an active one
                    self?.currentBooking = bookings.first { $0.isActive }
                    
                    if let activeBooking = self?.currentBooking {
                        print("✅ Found active booking: \(activeBooking.id)")
                    }
                    
                case .failure(let error):
                    print("❌ Failed to fetch user bookings: \(error.localizedDescription)")
                    self?.userBookings = []
                }
            }
        }
    }

    // ✅ UPDATED: Create booking with comprehensive validation
    func createBooking(for spotId: String, with vehicleNumber: String) {
        guard let userId = getCurrentUserId() else {
            print("❌ Cannot create booking - no user ID")
            errorMessage = "User not logged in. Please login again."
            return
        }
        
        // ✅ VALIDATE: Check if vehicle exists in SharedVehicleManager
        let availableVehicles = SharedVehicleManager.shared.getVehicleNumbers()
        if !availableVehicles.isEmpty && !availableVehicles.contains(vehicleNumber) {
            print("❌ Vehicle \(vehicleNumber) not found in available vehicles: \(availableVehicles)")
            errorMessage = "Selected vehicle not found. Please refresh and try again."
            return
        }
        
        // ✅ VALIDATE: Ensure vehicle is not empty
        guard !vehicleNumber.isEmpty else {
            print("❌ Cannot create booking with empty vehicle number")
            errorMessage = "Please select a vehicle"
            return
        }
        
        let lotId = parkingSpots.first(where: { $0.id == spotId })?.lotId ?? "default-lot"
        let checkInTime = ISO8601DateFormatter().string(from: Date())
        let checkOutTime = ISO8601DateFormatter().string(from: Date().addingTimeInterval(3600))

        print("🎯 Creating booking:")
        print("   User ID: \(userId)")
        print("   Spot ID: \(spotId)")
        print("   Vehicle: \(vehicleNumber)")
        print("   Lot ID: \(lotId)")
        
        isLoading = true
        
        APIService.shared.createBooking(
            spotId: spotId,
            userId: userId,
            lotId: lotId,
            vehicleNumber: vehicleNumber,
            checkInTime: checkInTime,
            checkOutTime: checkOutTime
        ) { [weak self] result in
            DispatchQueue.main.async {
                self?.isLoading = false
                switch result {
                case .success(let booking):
                    print("✅ Booking created successfully:")
                    print("   Booking ID: \(booking.id)")
                    print("   Vehicle: \(booking.vehicleNumber ?? "unknown")")
                    
                    self?.userBookings.append(booking)
                    if booking.isActive {
                        self?.currentBooking = booking
                    }
                    
                    self?.errorMessage = ""
                    self?.fetchAllData()
                    
                case .failure(let error):
                    print("❌ Failed to create booking: \(error.localizedDescription)")
                    self?.errorMessage = "Failed to create booking: \(error.localizedDescription)"
                }
            }
        }
    }

    // ✅ UPDATED: Create quick booking with SharedVehicleManager
    func createQuickBooking(for spotId: String) {
        print("🚀 Creating quick booking for spot \(spotId)")
        
        // ✅ USE SHARED VEHICLE MANAGER
        let vehicleToUse: String
        
        if !selectedVehicle.isEmpty {
            vehicleToUse = selectedVehicle
        } else if let primaryVehicle = SharedVehicleManager.shared.getPrimaryVehicle() {
            vehicleToUse = primaryVehicle
            selectedVehicle = vehicleToUse
        } else {
            // Force refresh vehicles
            fetchUserVehicles()
            vehicleToUse = SharedVehicleManager.shared.getPrimaryVehicle() ?? "QUICK_DEFAULT"
        }
        
        print("🎯 Quick booking using vehicle: \(vehicleToUse)")
        createBooking(for: spotId, with: vehicleToUse)
    }

    func startRealTimeUpdates() {
        print("📡 Starting real-time updates")
    }

    func stopRealTimeUpdates() {
        print("📡 Stopping real-time updates")
        stopAutoRefresh()
        timer?.invalidate()
    }

    func clearError() {
        errorMessage = ""
        print("🧹 Error message cleared")
    }

    func refreshBookings() {
        print("🔄 Manual refresh requested")
        fetchAllData()
    }

    private func startAutoRefresh() {
        refreshTimer = Timer.scheduledTimer(withTimeInterval: 30, repeats: true) { [weak self] _ in
            print("⏰ Auto-refresh triggered")
            self?.fetchAllData()
        }
    }

    private func stopAutoRefresh() {
        refreshTimer?.invalidate()
        refreshTimer = nil
    }
    
    // ✅ DEBUGGING: Print current state
    func debugPrintState() {
        print("🐛 HomeViewModel Debug State:")
        print("   AuthViewModel: \(authViewModel != nil ? "✅ Connected" : "❌ Missing")")
        print("   User ID: \(getCurrentUserId() ?? "❌ None")")
        print("   Vehicles (Local): \(userVehicles)")
        print("   Vehicles (Shared): \(SharedVehicleManager.shared.getVehicleNumbers())")
        print("   Selected Vehicle: \(selectedVehicle)")
        print("   Bookings: \(userBookings.count)")
        print("   Current Booking: \(currentBooking?.id ?? "None")")
        print("   Wallet Balance: ₹\(walletBalance)")
        print("   Is Searching: \(isSearching)")
        print("   Search Results: \(searchResults.count)")
    }
    
    deinit {
        print("🏠 HomeViewModel deinitializing")
        timer?.invalidate()
        stopAutoRefresh()
    }
    // ✅ ADD: Cancel booking function
    func cancelBooking(_ booking: Bookings, completion: @escaping (Bool, String?) -> Void) {
        guard let userId = getCurrentUserId() else {
            completion(false, "User not logged in")
            return
        }
        
        print("🗑️ Cancelling booking: \(booking.id)")
        
        
        APIService.shared.cancelBooking(userId: userId, bookingId: booking.id) { [weak self] result in
            DispatchQueue.main.async {
                switch result {
                case .success:
                    print("✅ Booking cancelled successfully")
                    
                    // Remove from userBookings array
                    self?.userBookings.removeAll { $0.id == booking.id }
                    
                    // Refresh data
                    self?.fetchAllData()
                    
                    completion(true, "Booking cancelled successfully")
                    
                case .failure(let error):
                    print("❌ Failed to cancel booking: \(error)")
                    completion(false, error.localizedDescription)
                }
            }
        }
    }
    // ✅ NEW: Extend booking time
    // ✅ NEW: Extend booking time
    // ✅ NEW: Extend booking time
    func extendBooking(_ booking: Bookings, additionalMinutes: Int, completion: @escaping (Bool, String?) -> Void) {
        guard let userId = getCurrentUserId() else {
            completion(false, "User not logged in")
            return
        }
        
        // ✅ Parse current checkout time and add minutes
        guard let currentCheckOutString = booking.checkOutTime,
              let currentCheckOut = parseISO8601Date(currentCheckOutString) else {
            completion(false, "Invalid checkout time")
            return
        }
        
        // ✅ Calculate new checkout time
        let newCheckOut = currentCheckOut.addingTimeInterval(TimeInterval(additionalMinutes * 60))
        
        // ✅ Format to ISO8601 string
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        let newCheckOutString = formatter.string(from: newCheckOut)
        
        print("⏰ Extending booking by \(additionalMinutes) minutes")
        print("   Current checkout: \(currentCheckOutString)")
        print("   New checkout: \(newCheckOutString)")
        
        APIService.shared.extendBooking(
            userId: userId,
            bookingId: booking.id,
            newCheckOutTime: newCheckOutString  // ✅ FIX: Pass String, not Int
        ) { [weak self] result in
            DispatchQueue.main.async {
                switch result {
                case .success(let updatedBooking):
                    print("✅ Booking extended successfully")
                    
                    // Update booking in array
                    if let index = self?.userBookings.firstIndex(where: { $0.id == booking.id }) {
                        self?.userBookings[index] = updatedBooking
                    }
                    
                    // Refresh wallet and data
                    self?.fetchAllData()
                    
                    completion(true, "Booking extended successfully")
                    
                case .failure(let error):
                    print("❌ Failed to extend booking: \(error)")
                    completion(false, error.localizedDescription)
                }
            }
        }
    }

    // ✅ ADD: Helper function to parse ISO8601 dates
    private func parseISO8601Date(_ dateString: String) -> Date? {
        let isoFormatter = ISO8601DateFormatter()
        isoFormatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        
        if let date = isoFormatter.date(from: dateString) {
            return date
        }
        
        // Try without fractional seconds
        let simpleFormatter = ISO8601DateFormatter()
        return simpleFormatter.date(from: dateString)
    }


}
