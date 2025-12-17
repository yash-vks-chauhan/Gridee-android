//import Foundation
//import Combine
//import SwiftUI
//
//// ✅ USER-SPECIFIC VEHICLE MANAGER
//class SharedVehicleManager: ObservableObject {
//    static let shared = SharedVehicleManager()
//    
//    @Published var vehicles: [VehicleData] = []
//    @Published var isLoading: Bool = false
//    
//    private var currentUserId: String? {
//        return UserDefaults.standard.string(forKey: "currentUserId")
//    }
//    
//    private init() {
//        print("🚗 SharedVehicleManager: Initialized")
//    }
//    
//    // ✅ Fetch vehicles from backend on login
////    func fetchVehiclesFromBackend(userId: String, completion: @escaping (Bool) -> Void) {
////        print("📥 Fetching vehicles from backend for user: \(userId)")
////        isLoading = true
////        
////        APIService.shared.fetchUserVehicles(userId: userId) { [weak self] result in
////            DispatchQueue.main.async {
////                self?.isLoading = false
////                
////                switch result {
////                case .success(let vehicleNumbers):
////                    print("✅ Fetched \(vehicleNumbers.count) vehicles from backend")
////                    
////                    // Convert to VehicleData and save locally
////                    self?.vehicles = vehicleNumbers.map { vehicleNum in
////                        VehicleData(
////                            id: UUID(),
////                            registrationNumber: vehicleNum,
////                            vehicleName: "Vehicle",
////                            colorHex: "#007AFF"
////                        )
////                    }
////                    
////                    self?.saveToUserDefaults()
////                    completion(true)
////                    
////                case .failure(let error):
////                    print("❌ Failed to fetch vehicles: \(error)")
////                    self?.vehicles = []
////                    completion(false)
////                }
////            }
////        }
////    }
//    
//    // ✅ Add vehicle to backend
//    func addVehicleToBackend(_ vehicle: String, completion: @escaping (Bool, String?) -> Void) {
//        guard let userId = UserDefaults.standard.string(forKey: "currentUserId") else {
//            print("❌ No user ID found")
//            completion(false, "User not found")
//            return
//        }
//        
//        print("🚗 Adding vehicle to backend: \(vehicle)")
//        
//        // ✅ Get all vehicles including the new one
//        var allVehicles = getVehicleNumbers()
//        if !allVehicles.contains(vehicle) {
//            allVehicles.append(vehicle)
//        }
//        
//        // ✅ Call new APIService method
//        APIService.shared.addVehiclesToBackend(userId: userId, vehicleNumbers: allVehicles) { result in
//            DispatchQueue.main.async {
//                switch result {
//                case .success(let updatedVehicles):
//                    print("✅ Backend updated - Vehicles: \(updatedVehicles)")
//                    
//                    // ✅ Save locally
//                    self.vehicleNumber = updatedVehicles
//                    UserDefaults.standard.set(updatedVehicles, forKey: "userVehicles")
//                    UserDefaults.standard.set(vehicle, forKey: "primaryVehicle")
//                    
//                    completion(true, "Vehicle added successfully")
//                    
//                case .failure(let error):
//                    print("❌ Backend error: \(error)")
//                    completion(false, error.localizedDescription)
//                }
//            }
//        }
//    }
//
//    // ✅ Keep existing fetchVehiclesFromBackend
//    func fetchVehiclesFromBackend(userId: String, completion: @escaping (Bool) -> Void) {
//        APIService.shared.fetchUserVehicles(userId: userId) { result in
//            DispatchQueue.main.async {
//                switch result {
//                case .success(let vehicles):
//                    print("✅ Fetched vehicles: \(vehicles)")
//                    self.vehicleNumbers = vehicles
//                    UserDefaults.standard.set(vehicles, forKey: "userVehicles")
//                    completion(true)
//                    
//                case .failure(let error):
//                    print("❌ Fetch error: \(error)")
//                    completion(false)
//                }
//            }
//        }
//    }
//
//    
//    // ✅ Load vehicles from local storage
//    func loadVehicles() {
//        guard let userId = currentUserId else {
//            print("⚠️ No user ID - cannot load vehicles")
//            vehicles = []
//            return
//        }
//        
//        print("🔄 Loading vehicles for user: \(userId)")
//        if let saved = loadFromUserDefaults(userId: userId) {
//            vehicles = saved
//            print("✅ Loaded \(saved.count) vehicles for user \(userId)")
//        } else {
//            vehicles = []
//            print("📭 No vehicles found locally for user \(userId)")
//        }
//    }
//    
//    // ✅ Clear vehicles on logout
//    func clearVehicles() {
//        print("🗑️ Clearing all vehicles")
//        vehicles = []
//    }
//    
//    // ✅ Remove vehicle (local only - implement backend delete if needed)
//    func removeVehicle(at index: Int) {
//        guard index < vehicles.count else { return }
//        let removed = vehicles.remove(at: index)
//        saveToUserDefaults()
//        print("✅ Removed vehicle: \(removed.registrationNumber)")
//    }
//    
//    // ✅ Get vehicle numbers as array
//    func getVehicleNumbers() -> [String] {
//        return vehicles.map { $0.registrationNumber }
//    }
//    
//    // ✅ Get primary vehicle
//    func getPrimaryVehicle() -> String? {
//        return vehicles.first?.registrationNumber
//    }
//    
//    // MARK: - Private Storage Methods
//    
//    private func saveToUserDefaults() {
//        guard let userId = currentUserId else {
//            print("❌ Cannot save vehicles - no user ID")
//            return
//        }
//        
//        let key = "vehicles_\(userId)"
//        
//        if let encoded = try? JSONEncoder().encode(vehicles) {
//            UserDefaults.standard.set(encoded, forKey: key)
//            print("💾 Saved \(vehicles.count) vehicles for user \(userId)")
//        }
//    }
//    
//    private func loadFromUserDefaults(userId: String) -> [VehicleData]? {
//        let key = "vehicles_\(userId)"
//        
//        guard let data = UserDefaults.standard.data(forKey: key),
//              let decoded = try? JSONDecoder().decode([VehicleData].self, from: data) else {
//            return nil
//        }
//        return decoded
//    }
//}
//
//// ✅ VEHICLE MODEL
//struct VehicleData: Identifiable, Equatable, Codable {
//    let id: UUID
//    var registrationNumber: String
//    var vehicleName: String
//    var colorHex: String
//    
//    var swiftUIColor: Color {
//        Color(hex: colorHex) ?? .blue
//    }
//    
//    init(id: UUID, registrationNumber: String, vehicleName: String = "Vehicle", colorHex: String = "#007AFF") {
//        self.id = id
//        self.registrationNumber = registrationNumber
//        self.vehicleName = vehicleName
//        self.colorHex = colorHex
//    }
//    
//    static func == (lhs: VehicleData, rhs: VehicleData) -> Bool {
//        return lhs.id == rhs.id && lhs.registrationNumber == rhs.registrationNumber
//    }
//}
//
//// ✅ COLOR EXTENSION
//extension Color {
//    init?(hex: String) {
//        var hexSanitized = hex.trimmingCharacters(in: .whitespacesAndNewlines)
//        hexSanitized = hexSanitized.replacingOccurrences(of: "#", with: "")
//        
//        var rgb: UInt64 = 0
//        guard Scanner(string: hexSanitized).scanHexInt64(&rgb) else { return nil }
//        
//        let r = Double((rgb & 0xFF0000) >> 16) / 255.0
//        let g = Double((rgb & 0x00FF00) >> 8) / 255.0
//        let b = Double(rgb & 0x0000FF) / 255.0
//        
//        self.init(red: r, green: g, blue: b)
//    }
//    
//    func toHex() -> String {
//        let components = UIColor(self).cgColor.components ?? [0, 0, 0, 1]
//        let r = Int(components[0] * 255.0)
//        let g = Int(components[1] * 255.0)
//        let b = Int(components[2] * 255.0)
//        return String(format: "#%02X%02X%02X", r, g, b)
//    }
//}


import Foundation
import Combine
import SwiftUI

// ✅ USER-SPECIFIC VEHICLE MANAGER
class SharedVehicleManager: ObservableObject {
    static let shared = SharedVehicleManager()
    
    @Published var vehicles: [VehicleData] = []
    @Published var isLoading: Bool = false
    
    private var currentUserId: String? {
        return UserDefaults.standard.string(forKey: "currentUserId")
    }
    
    private init() {
        print("🚗 SharedVehicleManager: Initialized")
    }
    
    // ✅ Fetch vehicles from backend on login
    func fetchVehiclesFromBackend(userId: String, completion: @escaping (Bool) -> Void) {
        print("📥 Fetching vehicles from backend for user: \(userId)")
        isLoading = true
        
        APIService.shared.fetchUserVehicles(userId: userId) { [weak self] result in
            DispatchQueue.main.async {
                self?.isLoading = false
                
                switch result {
                case .success(let vehicleNumbers):
                    print("✅ Fetched \(vehicleNumbers.count) vehicles from backend")
                    
                    // ✅ Convert to VehicleData and save locally
                    self?.vehicles = vehicleNumbers.map { vehicleNum in
                        VehicleData(
                            id: UUID(),
                            registrationNumber: vehicleNum,
                            vehicleName: "Vehicle",
                            colorHex: "#007AFF"
                        )
                    }
                    
                    self?.saveToUserDefaults()
                    completion(true)
                    
                case .failure(let error):
                    print("❌ Failed to fetch vehicles: \(error)")
                    self?.vehicles = []
                    completion(false)
                }
            }
        }
    }
    
    // ✅ Add vehicle to backend
    func addVehicleToBackend(_ vehicle: String, completion: @escaping (Bool, String?) -> Void) {
        guard let userId = UserDefaults.standard.string(forKey: "currentUserId") else {
            print("❌ No user ID found")
            completion(false, "User not found")
            return
        }
        
        print("🚗 Adding vehicle to backend: \(vehicle)")
        
        // ✅ Get all vehicle numbers (from VehicleData array)
        var allVehicles = getVehicleNumbers()
        if !allVehicles.contains(vehicle) {
            allVehicles.append(vehicle)
        }
        
        print("📤 Sending vehicles to backend: \(allVehicles)")
        
        // ✅ Call APIService method
        APIService.shared.addVehiclesToBackend(userId: userId, vehicleNumbers: allVehicles) { [weak self] result in
            DispatchQueue.main.async {
                switch result {
                case .success(let updatedVehicles):
                    print("✅ Backend updated - Vehicles: \(updatedVehicles)")
                    
                    // ✅ Convert to VehicleData
                    self?.vehicles = updatedVehicles.map { vehicleNum in
                        VehicleData(
                            id: UUID(),
                            registrationNumber: vehicleNum,
                            vehicleName: "Vehicle",
                            colorHex: "#007AFF"
                        )
                    }
                    
                    // ✅ Save locally
                    self?.saveToUserDefaults()
                    UserDefaults.standard.set(vehicle, forKey: "primaryVehicle")
                    
                    completion(true, "Vehicle added successfully")
                    
                case .failure(let error):
                    print("❌ Backend error: \(error)")
                    completion(false, error.localizedDescription)
                }
            }
        }
    }
    
    // ✅ Load vehicles from local storage
    func loadVehicles() {
        guard let userId = currentUserId else {
            print("⚠️ No user ID - cannot load vehicles")
            vehicles = []
            return
        }
        
        print("🔄 Loading vehicles for user: \(userId)")
        if let saved = loadFromUserDefaults(userId: userId) {
            vehicles = saved
            print("✅ Loaded \(saved.count) vehicles for user \(userId)")
        } else {
            vehicles = []
            print("📭 No vehicles found locally for user \(userId)")
        }
    }
    
    // ✅ Clear vehicles on logout
    func clearVehicles() {
        print("🗑️ Clearing all vehicles")
        vehicles = []
    }
    
    // ✅ Remove vehicle (local only)
    func removeVehicle(at index: Int) {
        guard index < vehicles.count else { return }
        let removed = vehicles.remove(at: index)
        saveToUserDefaults()
        print("✅ Removed vehicle: \(removed.registrationNumber)")
    }
    
    // ✅ Get vehicle numbers as array of Strings
    func getVehicleNumbers() -> [String] {
        return vehicles.map { $0.registrationNumber }
    }
    
    // ✅ Get primary vehicle
    func getPrimaryVehicle() -> String? {
        return vehicles.first?.registrationNumber ??
               UserDefaults.standard.string(forKey: "primaryVehicle")
    }
    
    // MARK: - Private Storage Methods
    
    private func saveToUserDefaults() {
        guard let userId = currentUserId else {
            print("❌ Cannot save vehicles - no user ID")
            return
        }
        
        let key = "vehicles_\(userId)"
        
        if let encoded = try? JSONEncoder().encode(vehicles) {
            UserDefaults.standard.set(encoded, forKey: key)
            print("💾 Saved \(vehicles.count) vehicles for user \(userId)")
        }
    }
    
    private func loadFromUserDefaults(userId: String) -> [VehicleData]? {
        let key = "vehicles_\(userId)"
        
        guard let data = UserDefaults.standard.data(forKey: key),
              let decoded = try? JSONDecoder().decode([VehicleData].self, from: data) else {
            return nil
        }
        return decoded
    }
}

// ✅ VEHICLE MODEL
struct VehicleData: Identifiable, Equatable, Codable {
    let id: UUID
    var registrationNumber: String
    var vehicleName: String
    var colorHex: String
    
    var swiftUIColor: Color {
        Color(hex: colorHex) ?? .blue
    }
    
    init(id: UUID = UUID(), registrationNumber: String, vehicleName: String = "Vehicle", colorHex: String = "#007AFF") {
        self.id = id
        self.registrationNumber = registrationNumber
        self.vehicleName = vehicleName
        self.colorHex = colorHex
    }
    
    static func == (lhs: VehicleData, rhs: VehicleData) -> Bool {
        return lhs.id == rhs.id && lhs.registrationNumber == rhs.registrationNumber
    }
}

// ✅ COLOR EXTENSION
extension Color {
    init?(hex: String) {
        var hexSanitized = hex.trimmingCharacters(in: .whitespacesAndNewlines)
        hexSanitized = hexSanitized.replacingOccurrences(of: "#", with: "")
        
        var rgb: UInt64 = 0
        guard Scanner(string: hexSanitized).scanHexInt64(&rgb) else { return nil }
        
        let r = Double((rgb & 0xFF0000) >> 16) / 255.0
        let g = Double((rgb & 0x00FF00) >> 8) / 255.0
        let b = Double(rgb & 0x0000FF) / 255.0
        
        self.init(red: r, green: g, blue: b)
    }
    
    func toHex() -> String {
        let components = UIColor(self).cgColor.components ?? [0, 0, 0, 1]
        let r = Int(components[0] * 255.0)
        let g = Int(components[1] * 255.0)
        let b = Int(components[2] * 255.0)
        return String(format: "#%02X%02X%02X", r, g, b)
    }
}
