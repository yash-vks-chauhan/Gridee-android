//import Foundation
//import SwiftUI
//
//// MARK: - Users Model
//struct Users: Codable, Identifiable {
//    let id: String
//    let name: String
//    let email: String
//    let phone: String
//    var vehicleNumbers: [String]?  // ✅ CHANGED FROM 'let' TO 'var'
//    let firstUser: Bool?
//    let walletCoins: Double?
//    let createdAt: String?
//    let updatedAt: String?
//    let role: String?
//    let parkingLotId: String?
//    let parkingLotName: String?
//    let active: Bool?
//    
//    var password: String? = nil
//    
//    enum CodingKeys: String, CodingKey {
//        case id, name, email, phone, vehicleNumbers, firstUser, walletCoins
//        case createdAt, updatedAt, role, parkingLotId, parkingLotName, active
//    }
//
//
//
//
//    
//    var safeVehicleNumbers: [String] {
//        return (vehicleNumbers?.filter { !$0.isEmpty && !$0.contains("USER_") })!
//    }
//    
//    var displayRole: String {
//        return role?.uppercased() ?? "USER"
//    }
//    
//    enum Role: String, Codable {
//        case user = "USER"
//        case admin = "ADMIN"
//        case transformer = "OPERATOR"
//    }
//}
//
//// MARK: - Bookings Model
//struct Bookings: Codable, Identifiable {
//    let id: String
//    let userId: String
//    let lotId: String
//    let spotId: String
//    let status: String
//    let amount: Double
//    let qrCode: String?
//    let checkInTime: String?
//    let checkOutTime: String?
//    let createdAt: String?
//    let vehicleNumber: String?
//    let qrCodeScanned: Bool
//    let actualCheckInTime: String?
//    let autoCompleted: Bool?
//    
//    
//    enum CodingKeys: String, CodingKey {
//        case id = "id"
//        case userId, lotId, spotId, status, amount
//        case qrCode, checkInTime, checkOutTime
//        case createdAt, vehicleNumber
//        case qrCodeScanned, actualCheckInTime, autoCompleted
//    }
//    
//    var totalHours: Double {
//        guard let checkInStr = checkInTime,
//              let checkOutStr = checkOutTime else { return 1.0 }
//        return Self.calculateHours(checkIn: checkInStr, checkOut: checkOutStr)
//    }
//    
//    var totalAmount: Double {
//        return totalHours * 5.0
//    }
//    
//    var isActive: Bool { status.uppercased() == "ACTIVE" }
//    var isPending: Bool { status.uppercased() == "PENDING" }
//    var isCompleted: Bool { status.uppercased() == "COMPLETED" }
//    var isCancelled: Bool { status.uppercased() == "CANCELLED" }
//    
//    var checkInDate: Date? {
//        guard let checkInTime = checkInTime else { return nil }
//        return ISO8601DateFormatter().date(from: checkInTime)
//    }
//    
//    var checkOutDate: Date? {
//        guard let checkOutTime = checkOutTime else { return nil }
//        return ISO8601DateFormatter().date(from: checkOutTime)
//    }
//    
//    var timeLeft: TimeInterval {
//        guard let checkOutDate = checkOutDate else { return 0 }
//        return max(0, checkOutDate.timeIntervalSince(Date()))
//    }
//    
//    private static func calculateHours(checkIn: String, checkOut: String) -> Double {
//        let formatter = ISO8601DateFormatter()
//        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
//        
//        guard let checkInDate = formatter.date(from: checkIn),
//              let checkOutDate = formatter.date(from: checkOut) else {
//            return 1.0
//        }
//        
//        let duration = checkOutDate.timeIntervalSince(checkInDate)
//        return max(duration / 3600.0, 1.0)
//    }
//}
//
//// MARK: - ParkingSpot Model
//struct ParkingSpot: Codable, Identifiable {
//    let id: String
//    let lotId: String?
//    let zoneName: String?
//    let capacity: Int?
//    let available: Int?
//    let status: String?
//    let bookingRate: Double?
//    let checkInPenaltyRate: Double?
//    let checkOutPenaltyRate: Double?
//    let description: String?
//    
//    enum CodingKeys: String, CodingKey {
//        case id = "id"
//        case lotId, zoneName, capacity, available, status
//        case bookingRate, checkInPenaltyRate, checkOutPenaltyRate
//        case description
//    }
//    
//    var displayName: String {
//        return zoneName ?? "Parking Zone"
//    }
//    
//    var isAvailable: Bool {
//        guard let available = available else { return false }
//        return available > 0 && status?.uppercased() == "AVAILABLE"
//    }
//    
//    var displayRate: Double {
//        return bookingRate ?? 5.0
//    }
//    
//    var availabilityPercentage: Double {
//        guard let capacity = capacity, capacity > 0,
//              let available = available else { return 0 }
//        return Double(available) / Double(capacity) * 100
//    }
//}
//
//// MARK: - Wallet Model
//struct Wallet: Codable, Identifiable {
//    let id: String
//    let userId: String
//    let balance: Double
//    let lastUpdated: String?
//    let createdAt: String?
//    let transactions: [TransactionRef]?
//    
//    enum CodingKeys: String, CodingKey {
//        case id = "id"
//        case userId, balance, lastUpdated, createdAt, transactions
//    }
//    
//    struct TransactionRef: Codable {
//        let referenceId: String?
//        let type: String?
//        let amount: Double?
//        let status: String?
//    }
//    
//    var displayBalance: String {
//        return String(format: "₹%.2f", balance)
//    }
//}
//
//
//
//import Foundation
//
//struct Transactions: Codable, Identifiable {
//    let id: String
//    let userId: String
//    let type: String?              // ✅ Optional - can be null
//    let amount: Double
//    let currency: String?          // ✅ Optional - can be null (THE FIX)
//    let method: String?            // ✅ Optional - can be null (THE FIX)
//    let referenceId: String?
//    let gateway: String?           // ✅ Optional - can be null (THE FIX)
//    let gatewayOrderId: String?
//    let gatewayPaymentId: String?
//    let timestamp: String
//    let status: String
//    let failureReason: String?
//    let metadata: String?
//    
//    // ✅ Helper properties for display
//    var displayType: String {
//        type ?? "Transaction"
//    }
//    
//    var displayCurrency: String {
//        currency ?? "INR"
//    }
//    
//    var displayMethod: String {
//        method ?? "Internal"
//    }
//    
//    var displayGateway: String {
//        gateway ?? "System"
//    }
//    
//    var formattedAmount: String {
//        let absAmount = abs(amount)
//        return String(format: "₹%.2f", absAmount)
//    }
//    
//    var isDebit: Bool {
//        amount < 0
//    }
//    
//    var isCredit: Bool {
//        amount >= 0
//    }
//    
//    var statusColor: String {
//        switch status.lowercased() {
//        case "success", "completed":
//            return "green"
//        case "failed":
//            return "red"
//        case "pending":
//            return "orange"
//        default:
//            return "gray"
//        }
//    }
//    
//    var formattedDate: String {
//        let isoFormatter = ISO8601DateFormatter()
//        if let date = isoFormatter.date(from: timestamp) {
//            let formatter = DateFormatter()
//            formatter.dateStyle = .medium
//            formatter.timeStyle = .short
//            return formatter.string(from: date)
//        }
//        return timestamp
//    }
//}
//
//
//
//// MARK: - JWT Login Response
//// MARK: - JWT Login Response
//// ✅ FIXED: Matches the actual backend response structure
//struct JWTLoginResponse: Codable {
//    let token: String
//    let tokenType: String?
//    let user: Users
//    let message: String?
//    
//    enum CodingKeys: String, CodingKey {
//        case token
//        case tokenType
//        case user
//        case message
//    }
//}
//
//
//
//// MARK: - QR Validation Result
//struct QrValidationResult: Codable {
//    let valid: Bool
//    let penalty: Double?
//    let message: String?
//    let bookingId: String?
//    let timestamp: String?
//}
//
//// MARK: - Parking Config
//struct ParkingConfig: Codable {
//    let hourlyRate: Double
//    let penaltyRate: Double?
//    let checkInPenaltyRate: Double?
//    let checkOutPenaltyRate: Double?
//    
//    static let `default` = ParkingConfig(
//        hourlyRate: 5.0,
//        penaltyRate: 10.0,
//        checkInPenaltyRate: 10.0,
//        checkOutPenaltyRate: 10.0
//    )
//}
//
//// MARK: - App Colors (UI Helper)
//struct AppColors {
//    static var primaryBackground: Color {
//        Color(UIColor.systemBackground)
//    }
//    
//    static var secondaryBackground: Color {
//        Color(UIColor.secondarySystemBackground)
//    }
//    
//    static var cardBackground: Color {
//        Color(UIColor.secondarySystemBackground)
//    }
//    
//    static var searchBarBackground: Color {
//        Color(UIColor.tertiarySystemBackground)
//    }
//    
//    static var primaryText: Color {
//        Color(UIColor.label)
//    }
//    
//    static var secondaryText: Color {
//        Color(UIColor.secondaryLabel)
//    }
//    
//    static let success = Color.green
//    static let warning = Color.orange
//    static let error = Color.red
//    static let info = Color.blue
//}


import Foundation
import SwiftUI

// MARK: - Users Model
struct Users: Codable, Identifiable {
    let id: String
    let name: String
    let email: String
    let phone: String
    var vehicleNumbers: [String]?  // ✅ CHANGED FROM 'let' TO 'var'
    let firstUser: Bool?
    let walletCoins: Double?
    let createdAt: String?
    let updatedAt: String?
    let role: String?
    let parkingLotId: String?
    let parkingLotName: String?
    let active: Bool?
    
    var password: String? = nil
    
    enum CodingKeys: String, CodingKey {
        case id, name, email, phone, vehicleNumbers, firstUser, walletCoins
        case createdAt, updatedAt, role, parkingLotId, parkingLotName, active
    }




    
    var safeVehicleNumbers: [String] {
        return (vehicleNumbers?.filter { !$0.isEmpty && !$0.contains("USER_") })!
    }
    
    var displayRole: String {
        return role?.uppercased() ?? "USER"
    }
    
    enum Role: String, Codable {
        case user = "USER"
        case admin = "ADMIN"
        case transformer = "OPERATOR"
    }
}

// MARK: - Bookings Model
struct Bookings: Codable, Identifiable {
    let id: String
    let userId: String
    let lotId: String
    let spotId: String
    let status: String
    let amount: Double
    let qrCode: String?
    let checkInTime: String?
    let checkOutTime: String?
    let createdAt: String?
    let vehicleNumber: String?
    let qrCodeScanned: Bool
    let actualCheckInTime: String?
    let autoCompleted: Bool?
    
    
    enum CodingKeys: String, CodingKey {
        case id = "id"
        case userId, lotId, spotId, status, amount
        case qrCode, checkInTime, checkOutTime
        case createdAt, vehicleNumber
        case qrCodeScanned, actualCheckInTime, autoCompleted
    }
    
    var totalHours: Double {
        guard let checkInStr = checkInTime,
              let checkOutStr = checkOutTime else { return 1.0 }
        return Self.calculateHours(checkIn: checkInStr, checkOut: checkOutStr)
    }
    
    var totalAmount: Double {
        return totalHours * 5.0
    }
    
    var isActive: Bool { status.uppercased() == "ACTIVE" }
    var isPending: Bool { status.uppercased() == "PENDING" }
    var isCompleted: Bool { status.uppercased() == "COMPLETED" }
    var isCancelled: Bool { status.uppercased() == "CANCELLED" }
    
    var checkInDate: Date? {
        guard let checkInTime = checkInTime else { return nil }
        return ISO8601DateFormatter().date(from: checkInTime)
    }
    
    var checkOutDate: Date? {
        guard let checkOutTime = checkOutTime else { return nil }
        return ISO8601DateFormatter().date(from: checkOutTime)
    }
    
    var timeLeft: TimeInterval {
        guard let checkOutDate = checkOutDate else { return 0 }
        return max(0, checkOutDate.timeIntervalSince(Date()))
    }
    
    private static func calculateHours(checkIn: String, checkOut: String) -> Double {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        
        guard let checkInDate = formatter.date(from: checkIn),
              let checkOutDate = formatter.date(from: checkOut) else {
            return 1.0
        }
        
        let duration = checkOutDate.timeIntervalSince(checkInDate)
        return max(duration / 3600.0, 1.0)
    }
}

// MARK: - ParkingSpot Model
struct ParkingSpot: Codable, Identifiable {
    let id: String
    let lotId: String?
    let zoneName: String?
    let capacity: Int?
    let available: Int?
    let status: String?
    let bookingRate: Double?
    let checkInPenaltyRate: Double?
    let checkOutPenaltyRate: Double?
    let description: String?
    
    enum CodingKeys: String, CodingKey {
        case id = "id"
        case lotId, zoneName, capacity, available, status
        case bookingRate, checkInPenaltyRate, checkOutPenaltyRate
        case description
    }
    
    var displayName: String {
        return zoneName ?? "Parking Zone"
    }
    
    var isAvailable: Bool {
        guard let available = available else { return false }
        return available > 0 && status?.uppercased() == "AVAILABLE"
    }
    
    var displayRate: Double {
        return bookingRate ?? 5.0
    }
    
    var availabilityPercentage: Double {
        guard let capacity = capacity, capacity > 0,
              let available = available else { return 0 }
        return Double(available) / Double(capacity) * 100
    }
}

// MARK: - Wallet Model
struct Wallet: Codable, Identifiable {
    let id: String
    let userId: String
    let balance: Double
    let lastUpdated: String?
    let createdAt: String?
    let transactions: [TransactionRef]?
    
    enum CodingKeys: String, CodingKey {
        case id = "id"
        case userId, balance, lastUpdated, createdAt, transactions
    }
    
    struct TransactionRef: Codable {
        let referenceId: String?
        let type: String?
        let amount: Double?
        let status: String?
    }
    
    var displayBalance: String {
        return String(format: "₹%.2f", balance)
    }
}



import Foundation

struct Transactions: Codable, Identifiable {
    let id: String
    let userId: String
    let type: String?              // ✅ Optional - can be null
    let amount: Double
    let currency: String?          // ✅ Optional - can be null (THE FIX)
    let method: String?            // ✅ Optional - can be null (THE FIX)
    let referenceId: String?
    let gateway: String?           // ✅ Optional - can be null (THE FIX)
    let gatewayOrderId: String?
    let gatewayPaymentId: String?
    let timestamp: String
    let status: String
    let failureReason: String?
    let metadata: String?
    
    // ✅ Helper properties for display
    var displayType: String {
        type ?? "Transaction"
    }
    
    var displayCurrency: String {
        currency ?? "INR"
    }
    
    var displayMethod: String {
        method ?? "Internal"
    }
    
    var displayGateway: String {
        gateway ?? "System"
    }
    
    var formattedAmount: String {
        let absAmount = abs(amount)
        return String(format: "₹%.2f", absAmount)
    }
    
    var isDebit: Bool {
        amount < 0
    }
    
    var isCredit: Bool {
        amount >= 0
    }
    
    var statusColor: String {
        switch status.lowercased() {
        case "success", "completed":
            return "green"
        case "failed":
            return "red"
        case "pending":
            return "orange"
        default:
            return "gray"
        }
    }
    
    var formattedDate: String {
        let isoFormatter = ISO8601DateFormatter()
        if let date = isoFormatter.date(from: timestamp) {
            let formatter = DateFormatter()
            formatter.dateStyle = .medium
            formatter.timeStyle = .short
            return formatter.string(from: date)
        }
        return timestamp
    }
}



// MARK: - JWT Login Response
// MARK: - JWT Login Response
// ✅ FIXED: Matches the actual backend response structure
struct JWTLoginResponse: Codable {
    let token: String
    let tokenType: String?
    let user: Users
    let message: String?
    
    enum CodingKeys: String, CodingKey {
        case token
        case tokenType
        case user
        case message
    }
}



// MARK: - QR Validation Result
struct QrValidationResult: Codable {
    let valid: Bool
    let penalty: Double?
    let message: String?
    let bookingId: String?
    let timestamp: String?
}

// MARK: - Parking Config
struct ParkingConfig: Codable {
    let hourlyRate: Double
    let penaltyRate: Double?
    let checkInPenaltyRate: Double?
    let checkOutPenaltyRate: Double?
    
    static let `default` = ParkingConfig(
        hourlyRate: 5.0,
        penaltyRate: 10.0,
        checkInPenaltyRate: 10.0,
        checkOutPenaltyRate: 10.0
    )
}

// MARK: - App Colors (UI Helper)
struct AppColors {
    static var primaryBackground: Color {
        Color(UIColor.systemBackground)
    }
    
    static var secondaryBackground: Color {
        Color(UIColor.secondarySystemBackground)
    }
    
    static var cardBackground: Color {
        Color(UIColor.secondarySystemBackground)
    }
    
    static var searchBarBackground: Color {
        Color(UIColor.tertiarySystemBackground)
    }
    
    static var primaryText: Color {
        Color(UIColor.label)
    }
    
    static var secondaryText: Color {
        Color(UIColor.secondaryLabel)
    }
    
    static let success = Color.green
    static let warning = Color.orange
    static let error = Color.red
    static let info = Color.blue
}
