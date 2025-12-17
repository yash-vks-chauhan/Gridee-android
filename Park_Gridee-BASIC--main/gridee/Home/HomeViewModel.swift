//import Foundation
//import Combine
//
//class HomeViewModel: ObservableObject {
//    // MARK: - Published Properties
//    @Published var notificationCount: Int = 0
//    @Published var lastUpdated: Date = Date()
//    @Published var searchText: String = ""
//    @Published var isLoading: Bool = false
//    @Published var errorMessage: String = ""
//    
//    // Parking & Bookings - ✅ REMOVED allParkingSpots (redundant)
//    @Published var parkingSpots: [ParkingSpot] = []
//    @Published var userBookings: [Bookings] = []
//    @Published var currentBooking: Bookings? = nil
//    @Published var individualBookingStates: [String: Bool] = [:]
//    
//    // Wallet & Vehicles
//    @Published var walletBalance: Double = 0.0
//    @Published var userVehicles: [String] = []
//    @Published var selectedVehicle: String = ""
//    @Published var isLoadingVehicles: Bool = false
//    
//    // Search
//    @Published var isSearching: Bool = false
//    @Published var searchResults: [ParkingSpot] = []
//    
//    // Time Slots
//    @Published var showingTimeSlotModal: Bool = false
//    @Published var showingCreateBooking: Bool = false
//    @Published var selectedStartTime: Date = Calendar.current.date(bySettingHour: 8, minute: 0, second: 0, of: Date())!
//    @Published var selectedEndTime: Date = Calendar.current.date(bySettingHour: 9, minute: 0, second: 0, of: Date())!
//    @Published var parkingConfig: ParkingConfig = .default
//    @Published var timerString: String = ""
//    
//    // MARK: - Private Properties
//    private var authViewModel: AuthViewModel?
//    private var cancellables = Set<AnyCancellable>()
//    private var timer: Timer?
//    private var refreshTimer: Timer?
//    
//    // MARK: - Computed Properties
//    var pendingBookingsCount: Int {
//        userBookings.filter { $0.status.lowercased() == "pending" }.count
//    }
//    
//    var availableParkingSpot: ParkingSpot? {
//        parkingSpots.first { ($0.available ?? 0) > 0 }
//    }
//    
//    var totalAvailableSpots: Int {
//        parkingSpots.reduce(0) { $0 + ($1.available ?? 0) }
//    }
//    
//    var activeBookingsCount: Int {
//        userBookings.filter { $0.isActive }.count
//    }
//    
//    var formattedWalletBalance: String {
//        String(format: "₹%.2f", walletBalance)
//    }
//    
//    // ✅ IMPROVED: Search now includes zoneName
//    var filteredParkingSpots: [ParkingSpot] {
//        if searchText.isEmpty {
//            return parkingSpots
//        }
//        
//        return parkingSpots.filter { spot in
//            let locationName = getLocationName(for: spot.id).lowercased()
//            let zoneName = (spot.zoneName ?? "").lowercased()
//            let searchLower = searchText.lowercased()
//            
//            return locationName.contains(searchLower) ||
//                   zoneName.contains(searchLower) ||
//                   spot.id.lowercased().contains(searchLower) ||
//                   (spot.available != nil && String(spot.available!).contains(searchText))
//        }
//    }
//    
//    // MARK: - Initialization
//    init() {
//        print("🏠 HomeViewModel initializing...")
//        startTimer()
//        updateTimerString()
//        setupSearchObserver()
//        startAutoRefresh()
//        
//        // ✅ REMOVED: NotificationCenter observer for college filtering
//        
//        print("🏠 HomeViewModel initialization complete")
//    }
//    
//    deinit {
//        print("🏠 HomeViewModel deinitializing")
//        timer?.invalidate()
//        stopAutoRefresh()
//        NotificationCenter.default.removeObserver(self)
//    }
//    
//    // MARK: - Auth Connection
//    func setAuthViewModel(_ authViewModel: AuthViewModel) {
//        print("🔗 Setting AuthViewModel reference in HomeViewModel")
//        self.authViewModel = authViewModel
//        APIService.shared.setAuthViewModel(authViewModel)
//            
//        
//        DispatchQueue.main.async {
//            self.fetchUserVehicles()
//            self.fetchAllData()
//        }
//        
//        print("✅ AuthViewModel reference set successfully")
//    }
//    
//    // MARK: - Data Fetching
//    func fetchAllData() {
//        print("🔄 Fetching all parking data...")
//        isLoading = true
//        errorMessage = ""
//        lastUpdated = Date()
//        individualBookingStates.removeAll()
//        
//        // ✅ SIMPLIFIED: Direct fetch without college filtering
//        fetchParkingSpots()
//        fetchUserBookings()
//        fetchUserVehicles()
//        fetchWalletBalance()
//    }
//    
//    // ✅ NEW: Simplified parking spots fetch
////    private func fetchParkingSpots() {
////        print("🅿️ Fetching parking spots...")
////        
////        APIService.fetchParkingSpots { [weak self] spots, error in
////            guard let self = self else { return }
////            
////            DispatchQueue.main.async {
////                self.isLoading = false
////                
////                if let error = error {
////                    print("❌ Error fetching parking spots: \(error.localizedDescription)")
////                    self.errorMessage = error.localizedDescription
////                    return
////                }
////                
////                if let spots = spots {
////                    print("✅ Successfully fetched \(spots.count) parking spots")
////                    self.parkingSpots = spots
////                    
////                    // Update search results if searching
////                    if self.isSearching {
////                        self.searchResults = self.filteredParkingSpots
////                    }
////                } else {
////                    print("⚠️ No spots received")
////                    self.parkingSpots = []
////                }
////            }
////        }
////    }
////
//    private func fetchParkingSpots() {
//        print("🅿️ Fetching parking spots...")
//
//        APIService.fetchParkingSpots { [weak self] result in
//            guard let self = self else { return }
//
//            DispatchQueue.main.async {
//                self.isLoading = false
//
//                switch result {
//                case .success(let spots):
//                    print("✅ Successfully fetched \(spots.count) parking spots")
//                    self.parkingSpots = spots
//                    self.errorMessage = ""          // clear old error
//                    if self.isSearching {
//                        self.searchResults = self.filteredParkingSpots
//                    }
//
//                case .failure(let apiError):
//                    print("❌ Error fetching parking spots: \(apiError.localizedDescription)")
//                    self.parkingSpots = []          // keep list empty
//
//                    // friendlier message for user
//                    switch apiError {
//                    case .authenticationRequired:
//                        self.errorMessage = "Session expired. Please log in again."
//                    case .serverError(let code):
//                        self.errorMessage = "Server error (\(code)). Please try again later."
//                    case .decodingError:
//                        self.errorMessage = "App could not read parking data. Please update or try again."
//                    default:
//                        self.errorMessage = "Could not load parking spots. Check your connection and retry."
//                    }
//                }
//            }
//        }
//    }
//
//    // ✅ NEW: Time-based availability fetch
//    func fetchAvailableSpots(lotId: String, startTime: Date, endTime: Date) {
//        print("🔄 Fetching available spots with time range")
//        print("   Lot ID: \(lotId)")
//        print("   Start: \(startTime)")
//        print("   End: \(endTime)")
//        
//        isLoading = true
//        errorMessage = ""
//        
//        // ✅ FIXED: Proper method call syntax
//        APIService.shared.fetchAvailableSpots(
//            lotId: lotId,
//            startTime: startTime,
//            endTime: endTime
//        ) { [weak self] result in
//            DispatchQueue.main.async {
//                self?.isLoading = false
//                
//                switch result {
//                case .success(let spots):
//                    print("✅ Fetched \(spots.count) available spots for time range")
//                    self?.parkingSpots = spots
//                    
//                    if self?.isSearching == true {
//                        self?.searchResults = self?.filteredParkingSpots ?? []
//                    }
//                    
//                case .failure(let error):
//                    print("❌ Error fetching spots by time: \(error)")
////                    self?.errorMessage = "Could not filter by time. Showing all spots."
//                    // Fallback to all spots
//                    self?.fetchParkingSpots()
//                }
//            }
//        }
//    }
//
//    private func fetchUserBookings() {
//        guard let userId = getCurrentUserId() else {
//            print("❌ Cannot fetch user bookings - no user ID")
//            return
//        }
//        
//        print("🔄 Fetching bookings for user: \(userId)")
//        APIService.shared.fetchUserBookings(userId: userId) { [weak self] result in
//            DispatchQueue.main.async {
//                switch result {
//                case .success(let bookings):
//                    print("✅ Fetched \(bookings.count) user bookings")
//                    self?.userBookings = bookings
//                    self?.currentBooking = bookings.first { $0.isActive }
//                    
//                case .failure(let error):
//                    print("❌ Failed to fetch user bookings: \(error.localizedDescription)")
//                    self?.userBookings = []
//                }
//            }
//        }
//    }
//    
//    private func fetchWalletBalance() {
//        guard let userId = getCurrentUserId() else {
//            print("❌ No user ID found for wallet")
//            return
//        }
//        
//        print("💰 Fetching wallet balance for user: \(userId)")
//        
//        APIService.shared.fetchWallet(userId: userId) { [weak self] result in
//            DispatchQueue.main.async {
//                switch result {
//                case .success(let wallet):
//                    print("✅ Wallet balance updated: ₹\(wallet.balance)")
//                    self?.walletBalance = wallet.balance
//                    
//                case .failure(let error):
//                    print("❌ Failed to fetch wallet: \(error)")
//                    self?.walletBalance = 0.0
//                }
//            }
//        }
//    }
//    
//    // MARK: - Vehicle Management
//    // ✅ Fetch vehicles from backend
//    func fetchUserVehicles() {
//        guard let userId = getCurrentUserId() else {
//            print("⚠️ No user ID - cannot fetch vehicles")
//            userVehicles = []
//            selectedVehicle = ""
//            return
//        }
//        
//        print("🚗 Fetching vehicles from backend for user: \(userId)")
//        isLoadingVehicles = true
//        
//        SharedVehicleManager.shared.fetchVehiclesFromBackend(userId: userId) { [weak self] success in
//            DispatchQueue.main.async {
//                self?.isLoadingVehicles = false
//                
//                if success {
//                    // Get vehicles from SharedVehicleManager after backend sync
//                    self?.userVehicles = SharedVehicleManager.shared.getVehicleNumbers()
//                    self?.selectedVehicle = SharedVehicleManager.shared.getPrimaryVehicle() ?? ""
//                    
//                    print("✅ HomeViewModel: Synced \(self?.userVehicles.count ?? 0) vehicles")
//                    print("   Vehicles: \(self?.userVehicles ?? [])")
//                    print("   Selected: '\(self?.selectedVehicle ?? "")'")
//                } else {
//                    print("❌ Failed to fetch vehicles from backend")
//                    self?.userVehicles = []
//                    self?.selectedVehicle = ""
//                }
//            }
//        }
//    }
//    // In HomeViewModel
//    func getNextValidTimeSlot(hour: Int, minute: Int) -> (hour: Int, minute: Int) {
//        var totalMinutes = (hour * 60) + minute + 5
//        
//        let remainder = totalMinutes % 30
//        if remainder != 0 {
//            totalMinutes += (30 - remainder)
//        }
//        
//        var nextHour = totalMinutes / 60
//        let nextMinute = totalMinutes % 60
//        
//        if nextHour >= 20 {
//            nextHour = 8
//            return (nextHour, 0)
//        }
//        
//        if nextHour < 8 {
//            nextHour = 8
//            return (nextHour, 0)
//        }
//        
//        return (nextHour, nextMinute)
//    }
//
//
//    // ✅ Add vehicle to backend
//    func addVehicle(_ vehicle: String) {
//        guard !vehicle.isEmpty else {
//            print("❌ Cannot add empty vehicle")
//            return
//        }
//        
//        print("🚗 Adding vehicle: \(vehicle)")
//        
//        // ✅ This calls SharedVehicleManager which we just fixed
//        SharedVehicleManager.shared.addVehicleToBackend(vehicle) { [weak self] success, message in
//            DispatchQueue.main.async {
//                if success {
//                    print("✅ Vehicle added successfully")
//                    self?.userVehicles = SharedVehicleManager.shared.getVehicleNumbers()
//                    self?.selectedVehicle = vehicle
//                } else {
//                    print("❌ Failed: \(message ?? "Unknown error")")
//                    self?.errorMessage = message ?? "Failed to add vehicle"
//                }
//            }
//        }
//    }
//
//
//    // MARK: - Booking Management
//    // ✅ IMPROVED: Uses selectedStartTime and selectedEndTime from time modal
//    func createBooking(for spotId: String, with vehicleNumber: String) {
//        guard let userId = getCurrentUserId() else {
//            print("❌ Cannot create booking - no user ID")
//            errorMessage = "User not logged in. Please login again."
//            return
//        }
//        
//        guard !vehicleNumber.isEmpty else {
//            errorMessage = "Please select a vehicle"
//            return
//        }
//        
//        let lotId = parkingSpots.first(where: { $0.id == spotId })?.lotId ?? "default-lot"
//        
//        // ✅ Use selected times from time modal
//        let checkInTime = ISO8601DateFormatter().string(from: selectedStartTime)
//        let checkOutTime = ISO8601DateFormatter().string(from: selectedEndTime)
//        
//        print("🎯 Creating booking:")
//        print("   User ID: \(userId)")
//        print("   Spot ID: \(spotId)")
//        print("   Vehicle: \(vehicleNumber)")
//        print("   Check-in: \(checkInTime)")
//        print("   Check-out: \(checkOutTime)")
//        
//        isLoading = true
//        individualBookingStates[spotId] = true
//        
//        // ✅ Call new method with all parameters
//        APIService.shared.createBooking(
//            spotId: spotId,
//            userId: userId,
//            lotId: lotId,
//            vehicleNumber: vehicleNumber,
//            checkInTime: checkInTime,
//            checkOutTime: checkOutTime
//        ) { [weak self] result in
//            DispatchQueue.main.async {
//                self?.isLoading = false
//                self?.individualBookingStates[spotId] = false
//                
//                switch result {
//                case .success(let booking):
//                    print("✅ Booking created successfully: \(booking.id)")
//                    self?.userBookings.append(booking)
//                    if booking.isActive {
//                        self?.currentBooking = booking
//                    }
//                    self?.errorMessage = ""
//                    self?.fetchAllData()
//                    
//                case .failure(let error):
//                    print("❌ Failed to create booking: \(error)")
//                    self?.errorMessage = "Failed to create booking: \(error.localizedDescription)"
//                }
//            }
//        }
//    }
//
//    
//    func cancelBooking(_ booking: Bookings, completion: @escaping (Bool, String?) -> Void) {
//        guard let userId = getCurrentUserId() else {
//            completion(false, "User not logged in")
//            return
//        }
//        
//        print("🗑️ Cancelling booking: \(booking.id)")
//        
//        APIService.shared.cancelBooking(userId: userId, bookingId: booking.id) { [weak self] result in
//            DispatchQueue.main.async {
//                switch result {
//                case .success:
//                    print("✅ Booking cancelled successfully")
//                    self?.userBookings.removeAll { $0.id == booking.id }
//                    self?.fetchAllData()
//                    completion(true, "Booking cancelled successfully")
//                    
//                case .failure(let error):
//                    print("❌ Failed to cancel booking: \(error)")
//                    completion(false, error.localizedDescription)
//                }
//            }
//        }
//    }
//    
//    func extendBooking(_ booking: Bookings, additionalMinutes: Int, completion: @escaping (Bool, String?) -> Void) {
//        guard let userId = getCurrentUserId() else {
//            completion(false, "User not logged in")
//            return
//        }
//        
//        guard let currentCheckOutString = booking.checkOutTime,
//              let currentCheckOut = parseISO8601Date(currentCheckOutString) else {
//            completion(false, "Invalid checkout time")
//            return
//        }
//        
//        let newCheckOut = currentCheckOut.addingTimeInterval(TimeInterval(additionalMinutes * 60))
//        
//        let formatter = ISO8601DateFormatter()
//        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
//        let newCheckOutString = formatter.string(from: newCheckOut)
//        
//        print("⏰ Extending booking by \(additionalMinutes) minutes")
//        
//        APIService.shared.extendBooking(
//            userId: userId,
//            bookingId: booking.id,
//            newCheckOutTime: newCheckOutString
//        ) { [weak self] result in
//            DispatchQueue.main.async {
//                switch result {
//                case .success(let updatedBooking):
//                    print("✅ Booking extended successfully")
//                    
//                    if let index = self?.userBookings.firstIndex(where: { $0.id == booking.id }) {
//                        self?.userBookings[index] = updatedBooking
//                    }
//                    
//                    self?.fetchAllData()
//                    completion(true, "Booking extended successfully")
//                    
//                case .failure(let error):
//                    print("❌ Failed to extend booking: \(error)")
//                    completion(false, error.localizedDescription)
//                }
//            }
//        }
//    }
//    
//    // MARK: - Search
//    private func setupSearchObserver() {
//        $searchText
//            .debounce(for: .milliseconds(300), scheduler: RunLoop.main)
//            .sink { [weak self] searchText in
//                self?.performSearch(searchText)
//            }
//            .store(in: &cancellables)
//    }
//    
//    private func performSearch(_ query: String) {
//        isSearching = !query.isEmpty
//        searchResults = filteredParkingSpots
//        
//        if !query.isEmpty {
//            print("🔍 Searching for: \(query)")
//            print("🔍 Found \(searchResults.count) results")
//        }
//    }
//    
//    func clearSearch() {
//        searchText = ""
//        isSearching = false
//        searchResults = []
//    }
//    
//    // MARK: - Timer Management
//    func startTimer() {
//        timer?.invalidate()
//        timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { [weak self] _ in
//            self?.updateTimerString()
//        }
//    }
//    
//    func updateTimerString() {
//        guard let booking = currentBooking, let _ = booking.checkInDate else {
//            timerString = ""
//            return
//        }
//        
//        let left = booking.timeLeft
//        if left <= 0 {
//            timerString = "00:00:00"
//        } else {
//            let hours = Int(left) / 3600
//            let minutes = (Int(left) % 3600) / 60
//            let seconds = Int(left) % 60
//            timerString = String(format: "%02d:%02d:%02d", hours, minutes, seconds)
//        }
//    }
//    
//    private func startAutoRefresh() {
//        refreshTimer = Timer.scheduledTimer(withTimeInterval: 30, repeats: true) { [weak self] _ in
//            print("⏰ Auto-refresh triggered")
//            self?.fetchAllData()
//        }
//    }
//    
//    private func stopAutoRefresh() {
//        refreshTimer?.invalidate()
//        refreshTimer = nil
//    }
//    
//    // MARK: - Helper Functions
//    func getCurrentUserId() -> String? {
//        if let userId = authViewModel?.getCurrentUserId() {
//            return userId
//        }
//        
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
//    func getCurrentUserVehicle() -> String? {
//        if let vehicle = SharedVehicleManager.shared.getPrimaryVehicle() {
//            return vehicle
//        }
//        
//        if let vehicle = authViewModel?.getCurrentUserVehicle() {
//            return vehicle
//        }
//        
//        if !selectedVehicle.isEmpty {
//            return selectedVehicle
//        }
//        
//        return nil
//    }
//    
//    func isBooking(spotId: String) -> Bool {
//        return individualBookingStates[spotId] ?? false
//    }
//    
//    // ✅ IMPROVED: Uses zoneName from actual spot data
//    private func getLocationName(for spotId: String) -> String {
//        // Try to find the actual spot first
//        if let spot = parkingSpots.first(where: { $0.id == spotId }),
//           let zoneName = spot.zoneName, !zoneName.isEmpty {
//            return zoneName
//        }
//        
//        // Fallback to hardcoded names for known spots
//        switch spotId {
//        case "ps1": return "TP Avenue Parking"
//        case "ps2": return "Medical College"
//        case "ps3": return "City Center Parking"
//        default:
//            if spotId.hasPrefix("ps") {
//                let number = spotId.replacingOccurrences(of: "ps", with: "")
//                return "Parking Zone \(number.uppercased())"
//            }
//            return "Parking Location \(spotId.uppercased())"
//        }
//    }
//    
//    private func parseISO8601Date(_ dateString: String) -> Date? {
//        let isoFormatter = ISO8601DateFormatter()
//        isoFormatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
//        
//        if let date = isoFormatter.date(from: dateString) {
//            return date
//        }
//        
//        let simpleFormatter = ISO8601DateFormatter()
//        return simpleFormatter.date(from: dateString)
//    }
//    
//    // MARK: - Public Actions
//    func refreshBookings() {
//        print("🔄 Manual refresh requested")
//        fetchAllData()
//    }
//    
//    func clearError() {
//        errorMessage = ""
//    }
//    
//    func startRealTimeUpdates() {
//        print("📡 Starting real-time updates")
//        fetchAllData()
//    }
//    
//    func stopRealTimeUpdates() {
//        print("📡 Stopping real-time updates")
//        stopAutoRefresh()
//        timer?.invalidate()
//    }
//}
//


import Foundation
import Combine

class HomeViewModel: ObservableObject {
    // MARK: - Published Properties
    @Published var notificationCount: Int = 0
    @Published var lastUpdated: Date = Date()
    @Published var searchText: String = ""
    @Published var isLoading: Bool = false
    @Published var errorMessage: String = ""
    
    // Parking & Bookings - ✅ REMOVED allParkingSpots (redundant)
    @Published var parkingSpots: [ParkingSpot] = []
    @Published var userBookings: [Bookings] = []
    @Published var currentBooking: Bookings? = nil
    @Published var individualBookingStates: [String: Bool] = [:]
    
    // Wallet & Vehicles
    @Published var walletBalance: Double = 0.0
    @Published var userVehicles: [String] = []
    @Published var selectedVehicle: String = ""
    @Published var isLoadingVehicles: Bool = false
    
    // Search
    @Published var isSearching: Bool = false
    @Published var searchResults: [ParkingSpot] = []
    
    // Time Slots
    @Published var showingTimeSlotModal: Bool = false
    @Published var showingCreateBooking: Bool = false
    @Published var selectedStartTime: Date = Calendar.current.date(bySettingHour: 8, minute: 0, second: 0, of: Date())!
    @Published var selectedEndTime: Date = Calendar.current.date(bySettingHour: 9, minute: 0, second: 0, of: Date())!
    @Published var parkingConfig: ParkingConfig = .default
    @Published var timerString: String = ""
    
    // MARK: - Private Properties
    private var authViewModel: AuthViewModel?
    private var cancellables = Set<AnyCancellable>()
    private var timer: Timer?
    private var refreshTimer: Timer?
    
    // MARK: - Computed Properties
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
    
    // ✅ IMPROVED: Search now includes zoneName
    var filteredParkingSpots: [ParkingSpot] {
        if searchText.isEmpty {
            return parkingSpots
        }
        
        return parkingSpots.filter { spot in
            let locationName = getLocationName(for: spot.id).lowercased()
            let zoneName = (spot.zoneName ?? "").lowercased()
            let searchLower = searchText.lowercased()
            
            return locationName.contains(searchLower) ||
                   zoneName.contains(searchLower) ||
                   spot.id.lowercased().contains(searchLower) ||
                   (spot.available != nil && String(spot.available!).contains(searchText))
        }
    }
    
    // MARK: - Initialization
    init() {
        print("🏠 HomeViewModel initializing...")
        startTimer()
        updateTimerString()
        setupSearchObserver()
        startAutoRefresh()
        
        // ✅ REMOVED: NotificationCenter observer for college filtering
        
        print("🏠 HomeViewModel initialization complete")
    }
    
    deinit {
        print("🏠 HomeViewModel deinitializing")
        timer?.invalidate()
        stopAutoRefresh()
        NotificationCenter.default.removeObserver(self)
    }
    
    // MARK: - Auth Connection
    func setAuthViewModel(_ authViewModel: AuthViewModel) {
        print("🔗 Setting AuthViewModel reference in HomeViewModel")
        self.authViewModel = authViewModel
        APIService.shared.setAuthViewModel(authViewModel)
            
        
        DispatchQueue.main.async {
            self.fetchUserVehicles()
            self.fetchAllData()
        }
        
        print("✅ AuthViewModel reference set successfully")
    }
    
    // MARK: - Data Fetching
    func fetchAllData() {
        print("🔄 Fetching all parking data...")
        isLoading = true
        errorMessage = ""
        lastUpdated = Date()
        individualBookingStates.removeAll()
        
        // ✅ SIMPLIFIED: Direct fetch without college filtering
        fetchParkingSpots()
        fetchUserBookings()
        fetchUserVehicles()
        fetchWalletBalance()
    }
    
    // ✅ NEW: Simplified parking spots fetch
//    private func fetchParkingSpots() {
//        print("🅿️ Fetching parking spots...")
//
//        APIService.fetchParkingSpots { [weak self] spots, error in
//            guard let self = self else { return }
//
//            DispatchQueue.main.async {
//                self.isLoading = false
//
//                if let error = error {
//                    print("❌ Error fetching parking spots: \(error.localizedDescription)")
//                    self.errorMessage = error.localizedDescription
//                    return
//                }
//
//                if let spots = spots {
//                    print("✅ Successfully fetched \(spots.count) parking spots")
//                    self.parkingSpots = spots
//
//                    // Update search results if searching
//                    if self.isSearching {
//                        self.searchResults = self.filteredParkingSpots
//                    }
//                } else {
//                    print("⚠️ No spots received")
//                    self.parkingSpots = []
//                }
//            }
//        }
//    }
//
    private func fetchParkingSpots() {
        print("🅿️ Fetching parking spots...")

        APIService.fetchParkingSpots { [weak self] result in
            guard let self = self else { return }

            DispatchQueue.main.async {
                self.isLoading = false

                switch result {
                case .success(let spots):
                    print("✅ Successfully fetched \(spots.count) parking spots")
                    self.parkingSpots = spots
                    self.errorMessage = ""          // clear old error
                    if self.isSearching {
                        self.searchResults = self.filteredParkingSpots
                    }

                case .failure(let apiError):
                    print("❌ Error fetching parking spots: \(apiError.localizedDescription)")
                    self.parkingSpots = []          // keep list empty

                    // friendlier message for user
                    switch apiError {
                    case .authenticationRequired:
                        self.errorMessage = "Session expired. Please log in again."
                    case .serverError(let code):
                        self.errorMessage = "Server error (\(code)). Please try again later."
                    case .decodingError:
                        self.errorMessage = "App could not read parking data. Please update or try again."
                    default:
                        self.errorMessage = "Could not load parking spots. Check your connection and retry."
                    }
                }
            }
        }
    }

    // ✅ NEW: Time-based availability fetch
    func fetchAvailableSpots(lotId: String, startTime: Date, endTime: Date) {
        print("🔄 Fetching available spots with time range")
        print("   Lot ID: \(lotId)")

        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withTimeZone]

        let start = formatter.string(from: startTime)
        let end = formatter.string(from: endTime)

        print("   Start: \(start)")
        print("   End: \(end)")

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
                    print("❌ Error fetching spots by time: \(error)")
                    // graceful fallback
                    self?.fetchParkingSpots()
                }
            }
        }
    }


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
                    self?.currentBooking = bookings.first { $0.isActive }
                    
                case .failure(let error):
                    print("❌ Failed to fetch user bookings: \(error.localizedDescription)")
                    self?.userBookings = []
                }
            }
        }
    }
    
    private func fetchWalletBalance() {
        guard let userId = getCurrentUserId() else {
            print("❌ No user ID found for wallet")
            return
        }
        
        print("💰 Fetching wallet balance for user: \(userId)")
        
        APIService.shared.fetchWallet(userId: userId) { [weak self] result in
            DispatchQueue.main.async {
                switch result {
                case .success(let wallet):
                    print("✅ Wallet balance updated: ₹\(wallet.balance)")
                    self?.walletBalance = wallet.balance
                    
                case .failure(let error):
                    print("❌ Failed to fetch wallet: \(error)")
                    self?.walletBalance = 0.0
                }
            }
        }
    }
    
    // MARK: - Vehicle Management
    // ✅ Fetch vehicles from backend
    func fetchUserVehicles() {
        guard let userId = getCurrentUserId() else {
            print("⚠️ No user ID - cannot fetch vehicles")
            userVehicles = []
            selectedVehicle = ""
            return
        }
        
        print("🚗 Fetching vehicles from backend for user: \(userId)")
        isLoadingVehicles = true
        
        SharedVehicleManager.shared.fetchVehiclesFromBackend(userId: userId) { [weak self] success in
            DispatchQueue.main.async {
                self?.isLoadingVehicles = false
                
                if success {
                    // Get vehicles from SharedVehicleManager after backend sync
                    self?.userVehicles = SharedVehicleManager.shared.getVehicleNumbers()
                    self?.selectedVehicle = SharedVehicleManager.shared.getPrimaryVehicle() ?? ""
                    
                    print("✅ HomeViewModel: Synced \(self?.userVehicles.count ?? 0) vehicles")
                    print("   Vehicles: \(self?.userVehicles ?? [])")
                    print("   Selected: '\(self?.selectedVehicle ?? "")'")
                } else {
                    print("❌ Failed to fetch vehicles from backend")
                    self?.userVehicles = []
                    self?.selectedVehicle = ""
                }
            }
        }
    }
    // In HomeViewModel
    func getNextValidTimeSlot(hour: Int, minute: Int) -> (hour: Int, minute: Int) {
        var totalMinutes = (hour * 60) + minute + 5
        
        let remainder = totalMinutes % 30
        if remainder != 0 {
            totalMinutes += (30 - remainder)
        }
        
        var nextHour = totalMinutes / 60
        let nextMinute = totalMinutes % 60
        
        if nextHour >= 20 {
            nextHour = 8
            return (nextHour, 0)
        }
        
        if nextHour < 8 {
            nextHour = 8
            return (nextHour, 0)
        }
        
        return (nextHour, nextMinute)
    }


    // ✅ Add vehicle to backend
    func addVehicle(_ vehicle: String) {
        guard !vehicle.isEmpty else {
            print("❌ Cannot add empty vehicle")
            return
        }
        
        print("🚗 Adding vehicle: \(vehicle)")
        
        // ✅ This calls SharedVehicleManager which we just fixed
        SharedVehicleManager.shared.addVehicleToBackend(vehicle) { [weak self] success, message in
            DispatchQueue.main.async {
                if success {
                    print("✅ Vehicle added successfully")
                    self?.userVehicles = SharedVehicleManager.shared.getVehicleNumbers()
                    self?.selectedVehicle = vehicle
                } else {
                    print("❌ Failed: \(message ?? "Unknown error")")
                    self?.errorMessage = message ?? "Failed to add vehicle"
                }
            }
        }
    }


    // MARK: - Booking Management
    // ✅ IMPROVED: Uses selectedStartTime and selectedEndTime from time modal
    func createBooking(for spotId: String, with vehicleNumber: String) {
        guard let userId = getCurrentUserId() else {
            print("❌ Cannot create booking - no user ID")
            errorMessage = "User not logged in. Please login again."
            return
        }

        guard !vehicleNumber.isEmpty else {
            errorMessage = "Please select a vehicle"
            return
        }

        guard let lotId = parkingSpots.first(where: { $0.id == spotId })?.lotId else {
            print("❌ Parking spot has no lotId")
            errorMessage = "Parking lot information missing. Please refresh."
            return
        }

        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withTimeZone]

        let checkInTime = formatter.string(from: selectedStartTime)
        let checkOutTime = formatter.string(from: selectedEndTime)

        print("🎯 Creating booking:")
        print("   User ID: \(userId)")
        print("   Spot ID: \(spotId)")
        print("   Lot ID: \(lotId)")
        print("   Vehicle: \(vehicleNumber)")
        print("   Check-in: \(checkInTime)")
        print("   Check-out: \(checkOutTime)")

        isLoading = true
        individualBookingStates[spotId] = true

        
        // ✅ Call new method with all parameters
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
                self?.individualBookingStates[spotId] = false
                
                switch result {
                case .success(let booking):
                    print("✅ Booking created successfully: \(booking.id)")
                    self?.userBookings.append(booking)
                    if booking.isActive {
                        self?.currentBooking = booking
                    }
                    self?.errorMessage = ""
                    self?.fetchAllData()
                    
                case .failure(let error):
                    print("❌ Failed to create booking: \(error)")
                    self?.errorMessage = "Failed to create booking: \(error.localizedDescription)"
                }
            }
        }
    }

    
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
                    self?.userBookings.removeAll { $0.id == booking.id }
                    self?.fetchAllData()
                    completion(true, "Booking cancelled successfully")
                    
                case .failure(let error):
                    print("❌ Failed to cancel booking: \(error)")
                    completion(false, error.localizedDescription)
                }
            }
        }
    }
    
    func extendBooking(_ booking: Bookings, additionalMinutes: Int, completion: @escaping (Bool, String?) -> Void) {
        guard let userId = getCurrentUserId() else {
            completion(false, "User not logged in")
            return
        }
        
        guard let currentCheckOutString = booking.checkOutTime,
              let currentCheckOut = parseISO8601Date(currentCheckOutString) else {
            completion(false, "Invalid checkout time")
            return
        }
        
        let newCheckOut = currentCheckOut.addingTimeInterval(TimeInterval(additionalMinutes * 60))
        
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        let newCheckOutString = formatter.string(from: newCheckOut)
        
        print("⏰ Extending booking by \(additionalMinutes) minutes")
        
        APIService.shared.extendBooking(
            userId: userId,
            bookingId: booking.id,
            newCheckOutTime: newCheckOutString
        ) { [weak self] result in
            DispatchQueue.main.async {
                switch result {
                case .success(let updatedBooking):
                    print("✅ Booking extended successfully")
                    
                    if let index = self?.userBookings.firstIndex(where: { $0.id == booking.id }) {
                        self?.userBookings[index] = updatedBooking
                    }
                    
                    self?.fetchAllData()
                    completion(true, "Booking extended successfully")
                    
                case .failure(let error):
                    print("❌ Failed to extend booking: \(error)")
                    completion(false, error.localizedDescription)
                }
            }
        }
    }
    
    // MARK: - Search
    private func setupSearchObserver() {
        $searchText
            .debounce(for: .milliseconds(300), scheduler: RunLoop.main)
            .sink { [weak self] searchText in
                self?.performSearch(searchText)
            }
            .store(in: &cancellables)
    }
    
    private func performSearch(_ query: String) {
        isSearching = !query.isEmpty
        searchResults = filteredParkingSpots
        
        if !query.isEmpty {
            print("🔍 Searching for: \(query)")
            print("🔍 Found \(searchResults.count) results")
        }
    }
    
    func clearSearch() {
        searchText = ""
        isSearching = false
        searchResults = []
    }
    
    // MARK: - Timer Management
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
    
    // MARK: - Helper Functions
    func getCurrentUserId() -> String? {
        if let userId = authViewModel?.getCurrentUserId() {
            return userId
        }
        
        if let userId = UserDefaults.standard.string(forKey: "currentUserId") {
            return userId
        }
        
        if let userData = UserDefaults.standard.data(forKey: "userData"),
           let user = try? JSONDecoder().decode(Users.self, from: userData) {
            return user.id
        }
        
        return nil
    }
    
    func getCurrentUserVehicle() -> String? {
        if let vehicle = SharedVehicleManager.shared.getPrimaryVehicle() {
            return vehicle
        }
        
        if let vehicle = authViewModel?.getCurrentUserVehicle() {
            return vehicle
        }
        
        if !selectedVehicle.isEmpty {
            return selectedVehicle
        }
        
        return nil
    }
    
    func isBooking(spotId: String) -> Bool {
        return individualBookingStates[spotId] ?? false
    }
    
    // ✅ IMPROVED: Uses zoneName from actual spot data
    private func getLocationName(for spotId: String) -> String {
        // Try to find the actual spot first
        if let spot = parkingSpots.first(where: { $0.id == spotId }),
           let zoneName = spot.zoneName, !zoneName.isEmpty {
            return zoneName
        }
        
        // Fallback to hardcoded names for known spots
        switch spotId {
        case "ps1": return "TP Avenue Parking"
        case "ps2": return "Medical College"
        case "ps3": return "City Center Parking"
        default:
            if spotId.hasPrefix("ps") {
                let number = spotId.replacingOccurrences(of: "ps", with: "")
                return "Parking Zone \(number.uppercased())"
            }
            return "Parking Location \(spotId.uppercased())"
        }
    }
    
    private func parseISO8601Date(_ dateString: String) -> Date? {
        let isoFormatter = ISO8601DateFormatter()
        isoFormatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        
        if let date = isoFormatter.date(from: dateString) {
            return date
        }
        
        let simpleFormatter = ISO8601DateFormatter()
        return simpleFormatter.date(from: dateString)
    }
    
    // MARK: - Public Actions
    func refreshBookings() {
        print("🔄 Manual refresh requested")
        fetchAllData()
    }
    
    func clearError() {
        errorMessage = ""
    }
    
    func startRealTimeUpdates() {
        print("📡 Starting real-time updates")
        fetchAllData()
    }
    
    func stopRealTimeUpdates() {
        print("📡 Stopping real-time updates")
        stopAutoRefresh()
        timer?.invalidate()
    }
}

