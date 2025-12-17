//
//
//
//import Foundation
//import SwiftUI
//
//class BookingViewModel: ObservableObject {
//    @Published var currentBooking: Bookings? = nil
//    @Published var timerString: String = ""
//    @Published var activeBookings: [Bookings] = []
//    @Published var completedBookings: [Bookings] = []
//    @Published var pendingBookings: [Bookings] = []
//    @Published var allBookings: [Bookings] = []
//    @Published var isLoading: Bool = false
//    @Published var errorMessage: String = ""
//    @Published var showingCreateBooking: Bool = false
//    
//    private var timer: Timer?
//    private var refreshTimer: Timer?
//    
//    init() {
//        startTimer()
//        updateTimerString()
//        loadBookings()
//        startAutoRefresh()
//    }
//    
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
//    func endParking() {
//        currentBooking = nil
//        print("Parking ended")
//    }
//    
//    func extendTime() {
//        print("Extend time requested")
//    }
//    
//    // ✅ UPDATED: Load bookings for the current user
//    func loadBookings() {
//        guard let currentUserId = getCurrentUserId() else {
//            print("❌ No user ID found, cannot load bookings")
//            errorMessage = "User not logged in"
//            return
//        }
//        
//        print("🔄 Loading bookings for user: \(currentUserId)")
//        isLoading = true
//        
//        APIService.shared.fetchUserBookings(userId: currentUserId) { [weak self] result in
//            DispatchQueue.main.async {
//                self?.isLoading = false
//                switch result {
//                case .success(let bookings):
//                    print("✅ Loaded \(bookings.count) bookings for user")
//                    self?.allBookings = bookings
//                    self?.categorizeBookings(bookings)
//                case .failure(let error):
//                    print("❌ Failed to load bookings: \(error.localizedDescription)")
//                    self?.errorMessage = error.localizedDescription
//                }
//            }
//        }
//    }
//    
//    private func categorizeBookings(_ bookings: [Bookings]) {
//        activeBookings = bookings.filter { $0.status.uppercased() == "ACTIVE" }
//        pendingBookings = bookings.filter {
//            $0.status.uppercased() == "PENDING" || $0.status.uppercased() == "CONFIRMED"
//        }
//        completedBookings = bookings.filter {
//            $0.status.uppercased() == "COMPLETED" || $0.status.uppercased() == "FINISHED"
//        }
//    }
//    
//    // ✅ UPDATED: Create booking with proper user ID validation
////    func createBooking(spotId: String, userId: String, lotId: String, vehicleNumber: String?, checkInTime: String, checkOutTime: String) {
////        // ✅ VALIDATE USER ID MATCHES CURRENT USER
////        guard let currentUserId = getCurrentUserId() else {
////            print("❌ No current user ID found")
////            errorMessage = "User not logged in"
////            return
////        }
////        
////        guard userId == currentUserId else {
////            print("❌ User ID mismatch: provided=\(userId), current=\(currentUserId)")
////            errorMessage = "Invalid user session"
////            return
////        }
////        
////        print("🎯 Creating booking for user: \(currentUserId)")
////        isLoading = true
////        
////        APIService.shared.createBooking(
////            spotId: spotId,
////            userId: currentUserId, // ✅ Use validated current user ID
////            lotId: lotId,
////            vehicleNumber: vehicleNumber,
////            checkInTime: checkInTime,
////            checkOutTime: checkOutTime
////        ) { [weak self] result in
////            DispatchQueue.main.async {
////                self?.isLoading = false
////                switch result {
////                case .success(let booking):
////                    print("✅ Booking created successfully: \(booking.id)")
////                    self?.allBookings.append(booking)
////                    if booking.isActive {
////                        self?.activeBookings.append(booking)
////                    }
////                    self?.showingCreateBooking = false
////                    self?.loadBookings() // Refresh all bookings
////                case .failure(let error):
////                    print("❌ Failed to create booking: \(error.localizedDescription)")
////                    self?.errorMessage = error.localizedDescription
////                }
////            }
////        }
////    }
//    func createBooking(spotId: String, userId: String, lotId: String, vehicleNumber: String?, checkInTime: String, checkOutTime: String) {
//        guard let currentUserId = getCurrentUserId() else {
//            print("❌ No current user ID found")
//            errorMessage = "User not logged in"
//            return
//        }
//        
//        guard userId == currentUserId else {
//            print("❌ User ID mismatch: provided=\(userId), current=\(currentUserId)")
//            errorMessage = "Invalid user session"
//            return
//        }
//        
//        print("🎯 Creating booking for user: \(currentUserId)")
//        isLoading = true
//        
//        APIService.shared.createBooking(
//            spotId: spotId,
//            userId: currentUserId,
//            lotId: lotId,
//            vehicleNumber: vehicleNumber,
//            checkInTime: checkInTime,
//            checkOutTime: checkOutTime
//        ) { [weak self] result in
//            DispatchQueue.main.async {
//                self?.isLoading = false
//                switch result {
//                case .success(let booking):
//                    print("✅ Booking created successfully: \(booking.id)")
//                    print("📌 Booking status: \(booking.status)")
//                    
//                    // ✅ FIXED: Add to allBookings first
//                    self?.allBookings.append(booking)
//                    
//                    // ✅ FIXED: Categorize based on status
//                    let status = booking.status.uppercased()
//                    if status == "PENDING" || status == "CONFIRMED" {
//                        self?.pendingBookings.append(booking)
//                        print("✅ Added to pending bookings")
//                    } else if status == "ACTIVE" {
//                        self?.activeBookings.append(booking)
//                        print("✅ Added to active bookings")
//                    }
//                    
//                    self?.showingCreateBooking = false
//                    
//                    // ✅ Refresh all bookings after a short delay
//                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
//                        self?.loadBookings()
//                    }
//                    
//                case .failure(let error):
//                    print("❌ Failed to create booking: \(error.localizedDescription)")
//                    self?.errorMessage = error.localizedDescription
//                }
//            }
//        }
//    }
//
//    // ✅ NEW: Helper method to get current user ID
//    private func getCurrentUserId() -> String? {
//        // Try multiple sources for user ID
//        if let userId = UserDefaults.standard.string(forKey: "currentUserId") {
//            return userId
//        }
//        
//        // Fallback to stored user data
//        if let userData = UserDefaults.standard.data(forKey: "userData"),
//           let user = try? JSONDecoder().decode(Users.self, from: userData) {
//            return user.id
//        }
//        
//        return nil
//    }
//    
//    func refreshBookings() {
//        loadBookings()
//    }
//    
//    private func startAutoRefresh() {
//        refreshTimer = Timer.scheduledTimer(withTimeInterval: 30.0, repeats: true) { [weak self] _ in
//            self?.loadBookings()
//        }
//    }
//    
//    private func stopAutoRefresh() {
//        refreshTimer?.invalidate()
//        refreshTimer = nil
//    }
//    
//    deinit {
//        timer?.invalidate()
//        stopAutoRefresh()
//    }
//}





//import Foundation
//import SwiftUI
//
//class BookingViewModel: ObservableObject {
//    @Published var currentBooking: Bookings? = nil
//    @Published var timerString: String = ""
//    @Published var activeBookings: [Bookings] = []
//    @Published var completedBookings: [Bookings] = []
//    @Published var pendingBookings: [Bookings] = []
//    @Published var allBookings: [Bookings] = []
//    @Published var isLoading: Bool = false
//    @Published var errorMessage: String = ""
//    @Published var showingCreateBooking: Bool = false
//    
//    private var timer: Timer?
//    private var refreshTimer: Timer?
//    
//    init() {
//        startTimer()
//        updateTimerString()
//        loadBookings()
//        startAutoRefresh()
//    }
//    
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
//    func endParking() {
//        currentBooking = nil
//        print("Parking ended")
//    }
//    
//    func extendTime() {
//        print("Extend time requested")
//    }
//    
//    // ✅ UPDATED: Load bookings for the current user
//    func loadBookings() {
//        guard let currentUserId = getCurrentUserId() else {
//            print("❌ No user ID found, cannot load bookings")
//            errorMessage = "User not logged in"
//            return
//        }
//        
//        print("🔄 Loading bookings for user: \(currentUserId)")
//        isLoading = true
//        
//        APIService.shared.fetchUserBookings(userId: currentUserId) { [weak self] result in
//            DispatchQueue.main.async {
//                self?.isLoading = false
//                switch result {
//                case .success(let bookings):
//                    print("✅ Loaded \(bookings.count) bookings for user")
//                    
//                    // ✅ ADD THIS DEBUG LOGGING
//                    for booking in bookings {
//                        print("📋 Booking ID: \(booking.id)")
//                            print("   Status: \(booking.status)")
//                            print("   Check-in: \(booking.checkInTime ?? "nil")")
//                            print("   Check-out: \(booking.checkOutTime ?? "nil")")
//                            print("   Total Hours: \(booking.totalHours)")
//                            print("   Amount: \(booking.amount ?? 0)")
//                            print("   Total Amount: \(booking.totalAmount)")
//                            print("---")
//                    }
//                    
//                    self?.allBookings = bookings
//                    self?.categorizeBookings(bookings)
//                    
//                    // ✅ ADD THIS DEBUG LOGGING
//                    print("📊 After categorization:")
//                    print("   Active: \(self?.activeBookings.count ?? 0)")
//                    print("   Pending: \(self?.pendingBookings.count ?? 0)")
//                    print("   Completed: \(self?.completedBookings.count ?? 0)")
//                    
//                case .failure(let error):
//                    print("❌ Failed to load bookings: \(error.localizedDescription)")
//                    self?.errorMessage = error.localizedDescription
//                }
//            }
//        }
//    }
//
//    
//    private func categorizeBookings(_ bookings: [Bookings]) {
//        activeBookings = bookings.filter { $0.status.uppercased() == "ACTIVE" }
//        pendingBookings = bookings.filter {
//            $0.status.uppercased() == "PENDING" || $0.status.uppercased() == "CONFIRMED"
//        }
//        completedBookings = bookings.filter {
//            $0.status.uppercased() == "COMPLETED" || $0.status.uppercased() == "FINISHED"
//        }
//    }
//    
//    // ✅ UPDATED: Create booking with proper user ID validation
////    func createBooking(spotId: String, userId: String, lotId: String, vehicleNumber: String?, checkInTime: String, checkOutTime: String) {
////        // ✅ VALIDATE USER ID MATCHES CURRENT USER
////        guard let currentUserId = getCurrentUserId() else {
////            print("❌ No current user ID found")
////            errorMessage = "User not logged in"
////            return
////        }
////
////        guard userId == currentUserId else {
////            print("❌ User ID mismatch: provided=\(userId), current=\(currentUserId)")
////            errorMessage = "Invalid user session"
////            return
////        }
////
////        print("🎯 Creating booking for user: \(currentUserId)")
////        isLoading = true
////
////        APIService.shared.createBooking(
////            spotId: spotId,
////            userId: currentUserId, // ✅ Use validated current user ID
////            lotId: lotId,
////            vehicleNumber: vehicleNumber,
////            checkInTime: checkInTime,
////            checkOutTime: checkOutTime
////        ) { [weak self] result in
////            DispatchQueue.main.async {
////                self?.isLoading = false
////                switch result {
////                case .success(let booking):
////                    print("✅ Booking created successfully: \(booking.id)")
////                    self?.allBookings.append(booking)
////                    if booking.isActive {
////                        self?.activeBookings.append(booking)
////                    }
////                    self?.showingCreateBooking = false
////                    self?.loadBookings() // Refresh all bookings
////                case .failure(let error):
////                    print("❌ Failed to create booking: \(error.localizedDescription)")
////                    self?.errorMessage = error.localizedDescription
////                }
////            }
////        }
////    }
//    // ✅ MARK: - Booking Management (UPDATED)
//
//    // ✅ Create booking with proper validation
//    func createBooking(spotId: String, userId: String, lotId: String,
//                       vehicleNumber: String?, checkInTime: String, checkOutTime: String) {
//        // ✅ Validate current user
//        guard let currentUserId = getCurrentUserId() else {
//            print("❌ No current user ID found")
//            errorMessage = "User not logged in"
//            return
//        }
//        
//        guard userId == currentUserId else {
//            print("❌ User ID mismatch: provided=\(userId), current=\(currentUserId)")
//            errorMessage = "Invalid user session"
//            return
//        }
//        
//        // ✅ Validate vehicle number
//        guard let vehicle = vehicleNumber, !vehicle.isEmpty else {
//            print("❌ No vehicle selected")
//            errorMessage = "Please select a vehicle"
//            return
//        }
//        
//        print("🎯 Creating booking for user: \(currentUserId)")
//        print("   Spot: \(spotId)")
//        print("   Vehicle: \(vehicle)")
//        print("   Check-in: \(checkInTime)")
//        print("   Check-out: \(checkOutTime)")
//        
//        isLoading = true
//        individualBookingStates[spotId] = true
//        
//        // ✅ Call APIService with validated data
//        APIService.shared.createBooking(
//            spotId: spotId,
//            userId: currentUserId,
//            lotId: lotId,
//            vehicleNumber: vehicle,
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
//                    print("📌 Booking status: \(booking.status)")
//                    
//                    // ✅ Add to bookings list
//                    self?.userBookings.append(booking)
//                    
//                    // ✅ Categorize based on status
//                    let status = booking.status.lowercased()
//                    if status.contains("pending") || status.contains("confirmed") {
//                        print("✅ Added to pending bookings")
//                    } else if status.contains("active") {
//                        self?.currentBooking = booking
//                        print("✅ Set as current active booking")
//                    }
//                    
//                    // ✅ Clear selection and show success
//                    self?.errorMessage = ""
//                    self?.showingCreateBooking = false
//                    
//                    // ✅ Refresh all data after booking creation
//                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
//                        self?.fetchAllData()
//                    }
//                    
//                case .failure(let error):
//                    print("❌ Failed to create booking: \(error.localizedDescription)")
//                    self?.errorMessage = "Failed to create booking: \(error.localizedDescription)"
//                }
//            }
//        }
//    }
//
//    // ✅ Load all bookings from backend
//   
//
//    // ✅ Cancel booking
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
//    // ✅ Extend booking
//    func extendBooking(_ booking: Bookings, additionalMinutes: Int,
//                       completion: @escaping (Bool, String?) -> Void) {
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
//    // ✅ Helper: Parse ISO8601 dates
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
//
//    // ✅ NEW: Helper method to get current user ID
//    private func getCurrentUserId() -> String? {
//        // Try multiple sources for user ID
//        if let userId = UserDefaults.standard.string(forKey: "currentUserId") {
//            return userId
//        }
//        
//        // Fallback to stored user data
//        if let userData = UserDefaults.standard.data(forKey: "userData"),
//           let user = try? JSONDecoder().decode(Users.self, from: userData) {
//            return user.id
//        }
//        
//        return nil
//    }
//    
//    func refreshBookings() {
//        loadBookings()
//    }
//    
//    private func startAutoRefresh() {
//        refreshTimer = Timer.scheduledTimer(withTimeInterval: 30.0, repeats: true) { [weak self] _ in
//            self?.loadBookings()
//        }
//    }
//    
//    private func stopAutoRefresh() {
//        refreshTimer?.invalidate()
//        refreshTimer = nil
//    }
//    
//    deinit {
//        timer?.invalidate()
//        stopAutoRefresh()
//    }
//}
//

//import Foundation
//import SwiftUI
//
//class BookingViewModel: ObservableObject {
//    @Published var currentBooking: Bookings? = nil
//    @Published var timerString: String = ""
//    @Published var activeBookings: [Bookings] = []
//    @Published var completedBookings: [Bookings] = []
//    @Published var pendingBookings: [Bookings] = []
//    @Published var allBookings: [Bookings] = []
//    @Published var isLoading: Bool = false
//    @Published var errorMessage: String = ""
//    @Published var showingCreateBooking: Bool = false
//    
//    @Published var bookingStates: [String: Bool] = [:]
//    
//    private var timer: Timer?
//    private var refreshTimer: Timer?
//    
//    init() {
//        startTimer()
//        updateTimerString()
//        loadBookings()
//        startAutoRefresh()
//    }
//    
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
//    func endParking() {
//        currentBooking = nil
//        print("🏁 Parking ended")
//    }
//    
//    func extendTime() {
//        print("⏱️ Extend time requested")
//    }
//    
//    func loadBookings() {
//        guard let currentUserId = getCurrentUserId() else {
//            print("❌ No user ID found, cannot load bookings")
//            errorMessage = "User not logged in"
//            return
//        }
//        
//        print("🔄 Loading bookings for user: \(currentUserId)")
//        isLoading = true
//        
//        APIService.shared.fetchUserBookings(userId: currentUserId) { [weak self] result in
//            DispatchQueue.main.async {
//                self?.isLoading = false
//                switch result {
//                case .success(let bookings):
//                    print("✅ Loaded \(bookings.count) bookings for user")
//                    
//                    self?.allBookings = bookings
//                    self?.categorizeBookings(bookings)
//                    
//                case .failure(let error):
//                    print("❌ Failed to load bookings: \(error.localizedDescription)")
//                    // ✅ Don't clear allBookings on error - keep local data
//                    // This way the booking you just created stays visible
//                    self?.errorMessage = error.localizedDescription
//                }
//            }
//        }
//    }
//
//    
//    private func categorizeBookings(_ bookings: [Bookings]) {
//        activeBookings = []
//        pendingBookings = []
//        completedBookings = []
//        
//        for booking in bookings {
//            let status = booking.status.uppercased().trimmingCharacters(in: .whitespacesAndNewlines)
//            
//            print("🔍 Categorizing booking: \(booking.id) with status: '\(status)'")
//            
//            if status == "ACTIVE" {
//                activeBookings.append(booking)
//                print("   → Added to ACTIVE")
//            } else if status == "PENDING" || status == "CONFIRMED" {
//                pendingBookings.append(booking)
//                print("   → Added to PENDING")
//            } else if status == "COMPLETED" || status == "FINISHED" {
//                completedBookings.append(booking)
//                print("   → Added to COMPLETED")
//            } else {
//                print("   → Unknown status: '\(status)' - Adding to PENDING")
//                pendingBookings.append(booking)
//            }
//        }
//        
//        print("📊 Final counts: Active=\(activeBookings.count), Pending=\(pendingBookings.count), Completed=\(completedBookings.count)")
//    }
//    
//    // ✅ REPLACE THIS ENTIRE FUNCTION WITH THE CODE BELOW
//    // In BookingViewModel.swift - Replace createBooking with this DEBUG version:
//    // ✅ Create booking (UPDATED - don't refresh on error)
//    func createBooking(spotId: String, userId: String, lotId: String,
//                       vehicleNumber: String?, checkInTime: String, checkOutTime: String) {
//        
//        guard let currentUserId = getCurrentUserId() else {
//            print("❌ No current user ID found")
//            errorMessage = "User not logged in"
//            return
//        }
//        
//        guard userId == currentUserId else {
//            errorMessage = "Invalid user session"
//            return
//        }
//        
//        guard let vehicle = vehicleNumber, !vehicle.isEmpty else {
//            errorMessage = "Please select a vehicle"
//            return
//        }
//        
//        print("🎯 Creating booking...")
//        isLoading = true
//        bookingStates[spotId] = true
//        
//        APIService.shared.createBooking(
//            spotId: spotId,
//            userId: currentUserId,
//            lotId: lotId,
//            vehicleNumber: vehicle,
//            checkInTime: checkInTime,
//            checkOutTime: checkOutTime
//        ) { [weak self] result in
//            DispatchQueue.main.async {
//                self?.isLoading = false
//                self?.bookingStates[spotId] = false
//                
//                switch result {
//                case .success(let booking):
//                    print("✅ Booking created: \(booking.id) - Status: \(booking.status)")
//                    
//                    // ✅ ADD IMMEDIATELY
//                    self?.allBookings.append(booking)
//                    
//                    let status = booking.status.uppercased().trimmingCharacters(in: .whitespacesAndNewlines)
//                    
//                    if status == "PENDING" || status == "CONFIRMED" {
//                        self?.pendingBookings.append(booking)
//                        print("✅ Added to PENDING")
//                    } else if status == "ACTIVE" {
//                        self?.activeBookings.append(booking)
//                        self?.currentBooking = booking
//                        print("✅ Added to ACTIVE")
//                    } else {
//                        self?.pendingBookings.append(booking)
//                        print("⚠️ Unknown status - added to PENDING")
//                    }
//                    
//                    self?.errorMessage = ""
//                    self?.showingCreateBooking = false
//                    
//                    // ✅ Try to refresh BUT don't overwrite on error
//                    DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
//                        self?.loadBookings()
//                    }
//                    
//                case .failure(let error):
//                    print("❌ Failed to create: \(error)")
//                    self?.errorMessage = "Failed to create booking"
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
//                    self?.allBookings.removeAll { $0.id == booking.id }
//                    self?.loadBookings()
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
//    func extendBooking(_ booking: Bookings, additionalMinutes: Int,
//                       completion: @escaping (Bool, String?) -> Void) {
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
//                    if let index = self?.allBookings.firstIndex(where: { $0.id == booking.id }) {
//                        self?.allBookings[index] = updatedBooking
//                    }
//                    
//                    self?.loadBookings()
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
//    func refreshBookings() {
//        loadBookings()
//    }
//    
//    private func startAutoRefresh() {
//        refreshTimer = Timer.scheduledTimer(withTimeInterval: 30.0, repeats: true) { [weak self] _ in
//            self?.loadBookings()
//        }
//    }
//    
//    private func stopAutoRefresh() {
//        refreshTimer?.invalidate()
//        refreshTimer = nil
//    }
//    
//    deinit {
//        timer?.invalidate()
//        stopAutoRefresh()
//    }
//}
import Foundation
import SwiftUI

class BookingViewModel: ObservableObject {
    @Published var currentBooking: Bookings? = nil
    @Published var timerString: String = ""
    @Published var activeBookings: [Bookings] = []
    @Published var completedBookings: [Bookings] = []
    @Published var pendingBookings: [Bookings] = []
    @Published var allBookings: [Bookings] = []
    @Published var isLoading: Bool = false
    @Published var errorMessage: String = ""
    @Published var showingCreateBooking: Bool = false
    @Published var bookingStates: [String: Bool] = [:]
    
    private var timer: Timer?
    private var refreshTimer: Timer?
    
    init() {
        startTimer()
        updateTimerString()
        loadBookings()
        startAutoRefresh()
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
        print("🏁 Parking ended")
    }
    
    // ✅ MAIN: Load bookings from backend
    func loadBookings() {
        guard let currentUserId = getCurrentUserId() else {
            print("❌ No user ID found")
            errorMessage = "User not logged in"
            return
        }
        
        print("🔄 Loading bookings for user: \(currentUserId)")
        isLoading = true
        
        APIService.shared.fetchUserBookings(userId: currentUserId) { [weak self] result in
            DispatchQueue.main.async {
                self?.isLoading = false
                switch result {
                case .success(let bookings):
                    print("✅ Loaded \(bookings.count) bookings")
                    
                    self?.allBookings = bookings
                    self?.categorizeBookings(bookings)
                    
                    print("📊 Categorization:")
                    print("   Active: \(self?.activeBookings.count ?? 0)")
                    print("   Pending: \(self?.pendingBookings.count ?? 0)")
                    print("   Completed: \(self?.completedBookings.count ?? 0)")
                    
                case .failure(let error):
                    print("❌ Failed to load: \(error)")
                    self?.errorMessage = error.localizedDescription
                }
            }
        }
    }
    
    // ✅ Categorize bookings by status
    private func categorizeBookings(_ bookings: [Bookings]) {
        activeBookings = []
        pendingBookings = []
        completedBookings = []
        
        for booking in bookings {
            let status = booking.status.uppercased().trimmingCharacters(in: .whitespacesAndNewlines)
            print("🔍 Categorizing: \(booking.id) → \(status)")
            
            switch status {
            case "ACTIVE":
                activeBookings.append(booking)
                if currentBooking == nil {
                    currentBooking = booking
                }
            case "PENDING", "CONFIRMED":
                pendingBookings.append(booking)
            case "COMPLETED", "FINISHED", "CANCELLED":
                completedBookings.append(booking)
            default:
                print("⚠️ Unknown status: \(status)")
                pendingBookings.append(booking)
            }
        }
    }
    
    // ✅ Create Booking
    func createBooking(spotId: String, userId: String, lotId: String,
                       vehicleNumber: String?, checkInTime: String, checkOutTime: String) {
        
        guard let currentUserId = getCurrentUserId() else {
            errorMessage = "User not logged in"
            return
        }
        
        guard userId == currentUserId else {
            errorMessage = "Invalid user session"
            return
        }
        
        guard let vehicle = vehicleNumber, !vehicle.isEmpty else {
            errorMessage = "Please select a vehicle"
            return
        }
        
        print("🎯 Creating booking...")
        isLoading = true
        bookingStates[spotId] = true
        
        APIService.shared.createBooking(
            spotId: spotId,
            userId: currentUserId,
            lotId: lotId,
            vehicleNumber: vehicle,
            checkInTime: checkInTime,
            checkOutTime: checkOutTime
        ) { [weak self] result in
            DispatchQueue.main.async {
                self?.isLoading = false
                self?.bookingStates[spotId] = false
                
                switch result {
                case .success(let booking):
                    print("✅ Booking created: \(booking.id) - Status: \(booking.status)")
                    
                    // ✅ Add immediately to array
                    self?.allBookings.append(booking)
                    
                    let status = booking.status.uppercased()
                    if status == "PENDING" || status == "CONFIRMED" {
                        self?.pendingBookings.append(booking)
                        print("✅ Added to PENDING")
                    } else if status == "ACTIVE" {
                        self?.activeBookings.append(booking)
                        self?.currentBooking = booking
                        print("✅ Added to ACTIVE")
                    }
                    
                    self?.errorMessage = ""
                    self?.showingCreateBooking = false
                    
                case .failure(let error):
                    print("❌ Failed: \(error)")
                    self?.errorMessage = "Failed to create booking"
                }
            }
        }
    }
    
    // ✅ Cancel Booking
    // ✅ FIXED: Cancel Booking - Rename to avoid ambiguity
    // ✅ UPDATED - Use explicit APIService.shared
    func cancelBooking(_ booking: Bookings, completion: @escaping (Bool, String?) -> Void) {
        guard let userId = getCurrentUserId() else {
            completion(false, "Not logged in")
            return
        }
        
        print("🗑️ Cancelling booking: \(booking.id)")
        
        // ✅ Explicitly call APIService.shared.cancelBooking with userId and bookingId
        let apiService = APIService.shared
        apiService.cancelBooking(userId: userId, bookingId: booking.id) { [weak self] result in
            DispatchQueue.main.async {
                switch result {
                case .success:
                    print("✅ Booking cancelled successfully")
                    self?.activeBookings.removeAll { $0.id == booking.id }
                    self?.pendingBookings.removeAll { $0.id == booking.id }
                    self?.allBookings.removeAll { $0.id == booking.id }
                    
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                        self?.loadBookings()
                    }
                    
                    completion(true, "Booking cancelled successfully")
                    
                case .failure(let error):
                    print("❌ Failed to cancel: \(error)")
                    completion(false, error.localizedDescription)
                }
            }
        }
    }


    // ✅ Extend Booking
    func extendBooking(_ booking: Bookings, additionalMinutes: Int,
                       completion: @escaping (Bool, String?) -> Void) {
        guard let userId = getCurrentUserId() else {
            completion(false, "Not logged in")
            return
        }
        
        guard let checkOutString = booking.checkOutTime,
              let checkOut = parseISO8601Date(checkOutString) else {
            completion(false, "Invalid checkout time")
            return
        }
        
        let newCheckOut = checkOut.addingTimeInterval(TimeInterval(additionalMinutes * 60))
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        let newCheckOutString = formatter.string(from: newCheckOut)
        
        print("⏰ Extending booking by \(additionalMinutes) minutes")
        
        APIService.shared.extendBooking(userId: userId, bookingId: booking.id,
                                       newCheckOutTime: newCheckOutString) { [weak self] result in
            DispatchQueue.main.async {
                switch result {
                case .success(let updated):
                    print("✅ Booking extended")
                    if let index = self?.allBookings.firstIndex(where: { $0.id == booking.id }) {
                        self?.allBookings[index] = updated
                    }
                    self?.loadBookings()
                    completion(true, "Booking extended")
                    
                case .failure(let error):
                    print("❌ Failed: \(error)")
                    completion(false, error.localizedDescription)
                }
            }
        }
    }
    
    private func parseISO8601Date(_ dateString: String) -> Date? {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return formatter.date(from: dateString)
    }
    
    private func getCurrentUserId() -> String? {
        return UserDefaults.standard.string(forKey: "currentUserId")
    }
    
    func refreshBookings() {
        loadBookings()
    }
    
    private func startAutoRefresh() {
        refreshTimer = Timer.scheduledTimer(withTimeInterval: 30.0, repeats: true) { [weak self] _ in
            self?.loadBookings()
        }
    }
    
    private func stopAutoRefresh() {
        refreshTimer?.invalidate()
    }
    
    deinit {
        timer?.invalidate()
        stopAutoRefresh()
    }
}
