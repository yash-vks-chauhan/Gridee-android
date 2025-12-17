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
        print("Parking ended")
    }
    
    func extendTime() {
        print("Extend time requested")
    }
    
    // ✅ UPDATED: Load bookings for the current user
    func loadBookings() {
        guard let currentUserId = getCurrentUserId() else {
            print("❌ No user ID found, cannot load bookings")
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
                    print("✅ Loaded \(bookings.count) bookings for user")
                    
                    // ✅ ADD THIS DEBUG LOGGING
                    for booking in bookings {
                        print("📋 Booking ID: \(booking.id)")
                            print("   Status: \(booking.status)")
                            print("   Check-in: \(booking.checkInTime ?? "nil")")
                            print("   Check-out: \(booking.checkOutTime ?? "nil")")
                            print("   Total Hours: \(booking.totalHours)")
                            print("   Amount: \(booking.amount ?? 0)")
                            print("   Total Amount: \(booking.totalAmount)")
                            print("---")
                    }
                    
                    self?.allBookings = bookings
                    self?.categorizeBookings(bookings)
                    
                    // ✅ ADD THIS DEBUG LOGGING
                    print("📊 After categorization:")
                    print("   Active: \(self?.activeBookings.count ?? 0)")
                    print("   Pending: \(self?.pendingBookings.count ?? 0)")
                    print("   Completed: \(self?.completedBookings.count ?? 0)")
                    
                case .failure(let error):
                    print("❌ Failed to load bookings: \(error.localizedDescription)")
                    self?.errorMessage = error.localizedDescription
                }
            }
        }
    }

    
    private func categorizeBookings(_ bookings: [Bookings]) {
        activeBookings = bookings.filter { $0.status.uppercased() == "ACTIVE" }
        pendingBookings = bookings.filter {
            $0.status.uppercased() == "PENDING" || $0.status.uppercased() == "CONFIRMED"
        }
        completedBookings = bookings.filter {
            $0.status.uppercased() == "COMPLETED" || $0.status.uppercased() == "FINISHED"
        }
    }
    
    // ✅ UPDATED: Create booking with proper user ID validation
//    func createBooking(spotId: String, userId: String, lotId: String, vehicleNumber: String?, checkInTime: String, checkOutTime: String) {
//        // ✅ VALIDATE USER ID MATCHES CURRENT USER
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
//            userId: currentUserId, // ✅ Use validated current user ID
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
//                    self?.allBookings.append(booking)
//                    if booking.isActive {
//                        self?.activeBookings.append(booking)
//                    }
//                    self?.showingCreateBooking = false
//                    self?.loadBookings() // Refresh all bookings
//                case .failure(let error):
//                    print("❌ Failed to create booking: \(error.localizedDescription)")
//                    self?.errorMessage = error.localizedDescription
//                }
//            }
//        }
//    }
    func createBooking(spotId: String, userId: String, lotId: String, vehicleNumber: String?, checkInTime: String, checkOutTime: String) {
        guard let currentUserId = getCurrentUserId() else {
            print("❌ No current user ID found")
            errorMessage = "User not logged in"
            return
        }
        
        guard userId == currentUserId else {
            print("❌ User ID mismatch: provided=\(userId), current=\(currentUserId)")
            errorMessage = "Invalid user session"
            return
        }
        
        print("🎯 Creating booking for user: \(currentUserId)")
        isLoading = true
        
        APIService.shared.createBooking(
            spotId: spotId,
            userId: currentUserId,
            lotId: lotId,
            vehicleNumber: vehicleNumber,
            checkInTime: checkInTime,
            checkOutTime: checkOutTime
        ) { [weak self] result in
            DispatchQueue.main.async {
                self?.isLoading = false
                switch result {
                case .success(let booking):
                    print("✅ Booking created successfully: \(booking.id)")
                    print("📌 Booking status: \(booking.status)")
                    
                    // ✅ FIXED: Add to allBookings first
                    self?.allBookings.append(booking)
                    
                    // ✅ FIXED: Categorize based on status
                    let status = booking.status.uppercased()
                    if status == "PENDING" || status == "CONFIRMED" {
                        self?.pendingBookings.append(booking)
                        print("✅ Added to pending bookings")
                    } else if status == "ACTIVE" {
                        self?.activeBookings.append(booking)
                        print("✅ Added to active bookings")
                    }
                    
                    self?.showingCreateBooking = false
                    
                    // ✅ Refresh all bookings after a short delay
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                        self?.loadBookings()
                    }
                    
                case .failure(let error):
                    print("❌ Failed to create booking: \(error.localizedDescription)")
                    self?.errorMessage = error.localizedDescription
                }
            }
        }
    }

    // ✅ NEW: Helper method to get current user ID
    private func getCurrentUserId() -> String? {
        // Try multiple sources for user ID
        if let userId = UserDefaults.standard.string(forKey: "currentUserId") {
            return userId
        }
        
        // Fallback to stored user data
        if let userData = UserDefaults.standard.data(forKey: "userData"),
           let user = try? JSONDecoder().decode(Users.self, from: userData) {
            return user.id
        }
        
        return nil
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
        refreshTimer = nil
    }
    
    deinit {
        timer?.invalidate()
        stopAutoRefresh()
    }
}

