import Foundation

struct Users: Codable {
    let id: String
    let name: String
    let email: String
    let phone: String
    var vehicleNumbers: [String]? // ✅ MAKE OPTIONAL TO HANDLE NULL
    let firstUser: Bool?
    let walletCoins: Int?
    let createdAt: String?
    let passwordHash: String?
    
    // ✅ BACKWARD COMPATIBILITY: Handle both single and multiple vehicles
    var vehicleNumber: String? {
        if let vehicles = vehicleNumbers, !vehicles.isEmpty {
            return vehicles.first
        }
        return nil
    }
    
    // ✅ SAFE ACCESS: Get vehicles array safely
    var safeVehicleNumbers: [String] {
            // Try vehicleNumbers array first
            if let numbers = vehicleNumbers, !numbers.isEmpty {
                return numbers
            }
            
            // Fallback to single vehicleNumber
            if let number = vehicleNumber, !number.isEmpty {
                return [number]
            }
            
            return []
        }
}

//struct Bookings: Codable, Identifiable {
//    let id: String
//    let spotId: String
//    let userId: String
//    let lotId: String
//    let status: String
//    let amount: Double?
//    let checkInTime: String?
//    let checkOutTime: String?
//    let qrCode: String?
//    let vehicleNumber: String?
//    let totalHours: Double
//    let totalAmount: Double
//    
//    // ✅ CRITICAL: Add CodingKeys to handle MongoDB field mapping
//    enum CodingKeys: String, CodingKey {
//        case id = "_id"              // MongoDB uses _id
//        case spotId
//        case userId
//        case lotId
//        case status
//        case amount
//        case checkInTime
//        case checkOutTime
//        case qrCode
//        case vehicleNumber
//        case totalHours
//        case totalAmount
//    }
//    
//    // ✅ FLEXIBLE: Custom decoder to handle missing/optional fields
//    init(from decoder: Decoder) throws {
//        let container = try decoder.container(keyedBy: CodingKeys.self)
//        
//        // Required fields with fallbacks
//        self.id = (try? container.decode(String.self, forKey: .id)) ?? UUID().uuidString
//        self.spotId = try container.decode(String.self, forKey: .spotId)
//        self.userId = try container.decode(String.self, forKey: .userId)
//        self.lotId = try container.decode(String.self, forKey: .lotId)
//        self.status = (try? container.decode(String.self, forKey: .status)) ?? "ACTIVE"
//        
//        // Optional fields
//        self.amount = try? container.decode(Double.self, forKey: .amount)
//        self.checkInTime = try? container.decode(String.self, forKey: .checkInTime)
//        self.checkOutTime = try? container.decode(String.self, forKey: .checkOutTime)
//        self.qrCode = try? container.decode(String.self, forKey: .qrCode)
//        self.vehicleNumber = try? container.decode(String.self, forKey: .vehicleNumber)
//        
//        // Handle numeric fields with fallbacks
//        self.totalHours = (try? container.decode(Double.self, forKey: .totalHours)) ?? 1.0
//        self.totalAmount = (try? container.decode(Double.self, forKey: .totalAmount)) ?? 2.5
//    }
//    
//    var isActive: Bool {
//        status.uppercased() == "ACTIVE"
//    }
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
//}


struct Bookings: Codable, Identifiable {
    let id: String
    let spotId: String
    let userId: String
    let lotId: String
    let status: String
    let amount: Double?
    let checkInTime: String?
    let checkOutTime: String?
    let qrCode: String?
    let vehicleNumber: String?
    let totalHours: Double
    let totalAmount: Double
    let qrCodeScanned: Bool?  // ✅ ADD THIS
    
    enum CodingKeys: String, CodingKey {
        case id = "id"
        case spotId
        case userId
        case lotId
        case status
        case amount
        case checkInTime
        case checkOutTime
        case qrCode
        case vehicleNumber
        case totalHours
        case totalAmount
        case qrCodeScanned  // ✅ ADD THIS
    }
    
    var mongoId: String {
            return id
        }
//    init(from decoder: Decoder) throws {
//        let container = try decoder.container(keyedBy: CodingKeys.self)
//        
//        self.id = (try? container.decode(String.self, forKey: .id)) ?? UUID().uuidString
//        self.spotId = try container.decode(String.self, forKey: .spotId)
//        self.userId = try container.decode(String.self, forKey: .userId)
//        self.lotId = try container.decode(String.self, forKey: .lotId)
//        self.status = (try? container.decode(String.self, forKey: .status)) ?? "ACTIVE"
//        
//        self.amount = try? container.decode(Double.self, forKey: .amount)
//        self.checkInTime = try? container.decode(String.self, forKey: .checkInTime)
//        self.checkOutTime = try? container.decode(String.self, forKey: .checkOutTime)
//        self.qrCode = try? container.decode(String.self, forKey: .qrCode)
//        self.vehicleNumber = try? container.decode(String.self, forKey: .vehicleNumber)
//        self.qrCodeScanned = try? container.decode(Bool.self, forKey: .qrCodeScanned)  // ✅ ADD THIS
//        
//        self.totalHours = (try? container.decode(Double.self, forKey: .totalHours)) ?? 1.0
//        self.totalAmount = (try? container.decode(Double.self, forKey: .totalAmount)) ?? 2.5
//    }
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        
        // ✅ STRICT: Require _id, throw error if missing
        self.id = try container.decode(String.self, forKey: .id)
        self.spotId = try container.decode(String.self, forKey: .spotId)
        self.userId = try container.decode(String.self, forKey: .userId)
        self.lotId = try container.decode(String.self, forKey: .lotId)
        self.status = (try? container.decode(String.self, forKey: .status)) ?? "PENDING"
        
        self.amount = try? container.decode(Double.self, forKey: .amount)
        self.checkInTime = try? container.decode(String.self, forKey: .checkInTime)
        self.checkOutTime = try? container.decode(String.self, forKey: .checkOutTime)
        self.qrCode = try? container.decode(String.self, forKey: .qrCode)
        self.vehicleNumber = try? container.decode(String.self, forKey: .vehicleNumber)
        self.qrCodeScanned = try? container.decode(Bool.self, forKey: .qrCodeScanned)
        
        let decodedTotalHours = try? container.decode(Double.self, forKey: .totalHours)
        let decodedTotalAmount = try? container.decode(Double.self, forKey: .totalAmount)
        
        if let hours = decodedTotalHours, let amount = decodedTotalAmount {
            self.totalHours = hours
            self.totalAmount = amount
        } else {
            let calculatedHours = Self.calculateHours(
                checkIn: self.checkInTime,
                checkOut: self.checkOutTime
            )
            self.totalHours = calculatedHours
            self.totalAmount = calculatedHours * 5.0
        }
    }

    // ✅ ADD THIS HELPER METHOD
    private static func calculateHours(checkIn: String?, checkOut: String?) -> Double {
        guard let checkInStr = checkIn,
              let checkOutStr = checkOut else {
            return 1.0 // Default fallback
        }
        
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        
        guard let checkInDate = formatter.date(from: checkInStr),
              let checkOutDate = formatter.date(from: checkOutStr) else {
            // Try without fractional seconds
            let simpleFormatter = ISO8601DateFormatter()
            guard let checkInDate = simpleFormatter.date(from: checkInStr),
                  let checkOutDate = simpleFormatter.date(from: checkOutStr) else {
                return 1.0
            }
            
            let duration = checkOutDate.timeIntervalSince(checkInDate)
            return max(duration / 3600.0, 1.0) // At least 1 hour
        }
        
        let duration = checkOutDate.timeIntervalSince(checkInDate)
        return max(duration / 3600.0, 1.0) // At least 1 hour
    }

    
    var isActive: Bool {
        status.uppercased() == "ACTIVE"
    }
    
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
//    var mongoId: String {
//            return _id
//        }
}





//struct Bookings: Codable {
//    let id: String
//    let spotId: String?
//    let userId: String?
//    let lotId: String?
//    let vehicleNumber: String?
//    let checkInTime: String?
//    let checkOutTime: String?
//    let status: String?
//    let amount: Double?
//    let createdAt: String?
//    let updatedAt: String?
//    let qrCode: String?
////    let totalHours: Double
////    let totalAmount: Double
//
//    // ✅ COMPUTED PROPERTIES FOR BookingSummaryView
//    var vehicleNumbers: String? {
//        return vehicleNumber
//    }
//
//    var totalAmount: Double {
//        return amount ?? 0.0
//    }
//
//    var totalHours: Double {
//        guard let checkInTime = checkInTime,
//              let checkOutTime = checkOutTime,
//              let checkInDate = parseDate(from: checkInTime),
//              let checkOutDate = parseDate(from: checkOutTime) else {
//            return 1.0 // Default 1 hour
//        }
//
//        let duration = checkOutDate.timeIntervalSince(checkInDate)
//        return max(duration / 3600.0, 0.1) // Minimum 0.1 hours
//    }
//
//    // ✅ COMPUTED PROPERTIES FOR ActiveBookingCard
//    var checkInDate: Date? {
//        guard let checkInTime = checkInTime else { return nil }
//        return parseDate(from: checkInTime)
//    }
//
//    var checkOutDate: Date? {
//        guard let checkOutTime = checkOutTime else { return nil }
//        return parseDate(from: checkOutTime)
//    }
//
//    var isActive: Bool {
//        return status?.lowercased() == "active" ||
//               status?.lowercased() == "confirmed" ||
//               status?.lowercased() == "ongoing"
//    }
//
//    var timeLeft: TimeInterval {
//        guard let checkOutDate = checkOutDate else { return 0 }
//        return checkOutDate.timeIntervalSinceNow
//    }
//
//    // ✅ HELPER: Parse date from string with multiple formats
//    private func parseDate(from dateString: String) -> Date? {
//        let formatters = [
//            ISO8601DateFormatter(),
//            DateFormatter.customBackendFormat,
//            DateFormatter.simpleBackendFormat
//        ]
//
//        for formatter in formatters {
//            if let formatter = formatter as? DateFormatter {
//                if let date = formatter.date(from: dateString) {
//                    return date
//                }
//            } else if let isoFormatter = formatter as? ISO8601DateFormatter {
//                if let date = isoFormatter.date(from: dateString) {
//                    return date
//                }
//            }
//        }
//
//        return nil
//    }
//}
//
//// ✅ REQUIRED: DateFormatter extensions
//extension DateFormatter {
////    static let timeOnly: DateFormatter = {
////        let formatter = DateFormatter()
////        formatter.dateFormat = "HH:mm"
////        return formatter
////    }()
//
//    static let customBackendFormat: DateFormatter = {
//        let formatter = DateFormatter()
//        formatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
//        formatter.timeZone = TimeZone(abbreviation: "UTC")
//        return formatter
//    }()
//
//    static let simpleBackendFormat: DateFormatter = {
//        let formatter = DateFormatter()
//        formatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss"
//        return formatter
//    }()
//}

// SheetContent.swift (new file or add to Models.swift)

struct Wallet: Codable, Identifiable {
    let id: String
    let userId: String
    let balance: Double
    let lastUpdated: String?
}

struct Transactions: Codable, Identifiable {
    let id: String
    let userId: String
    let amount: Double
    let transactionType: String?  // Make it optional
    let timestamp: String?        // Make it optional
    let status: String?
    let description: String?      // Add description field
    let createdAt: String?        // Add createdAt field
}

struct ParkingSpot: Codable, Identifiable {
    let id: String
    let lotId: String?
    let zoneName: String?
    let capacity: Int?
    let available: Int?
    let status: String?
    
    // Constant price per hour - no need to store in model
    static let pricePerHour: Double = 5.0
    
    // Add CodingKeys if your backend uses different property names
    enum CodingKeys: String, CodingKey {
        case id
        case lotId = "lot_id"
        case zoneName = "zoneName"
        case capacity
        case available
        case status
    }
}


import SwiftUI
struct AppColors {
    // MARK: - Backgrounds
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
    
    // MARK: - Text Colors
    static var primaryText: Color {
        Color(UIColor.label)
    }
    
    static var secondaryText: Color {
        Color(UIColor.secondaryLabel)
    }
    
    // MARK: - Accent Colors
    static let success = Color.green
    static let warning = Color.orange
    static let error = Color.red
}
//import Foundation

struct QrValidationResult: Codable {
    let valid: Bool
    let penalty: Double?  // ✅ CHANGED: Added penalty field
    let message: String?
    let bookingId: String?
    let timestamp: String?
    
    enum CodingKeys: String, CodingKey {
        case valid
        case penalty  // ✅ ADD THIS
        case message
        case bookingId
        case timestamp
    }
}

//added to fetch amount from backend
struct ParkingConfig: Codable {
    let hourlyRate: Double
    let penaltyRate: Double?
    
    static let `default` = ParkingConfig(hourlyRate: 5.0, penaltyRate: 10.0)
}

