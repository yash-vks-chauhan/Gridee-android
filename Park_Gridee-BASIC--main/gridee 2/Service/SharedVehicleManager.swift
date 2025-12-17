//
//  SharedVehicleManager.swift
//  gridee
//
//  Created by admin85 on 02/10/25.
//


import Foundation
import Combine
import SwiftUI

// ✅ SHARED VEHICLE MANAGER - Singleton
class SharedVehicleManager: ObservableObject {
    static let shared = SharedVehicleManager()
    
    @Published var vehicles: [VehicleData] = []
    @Published var isLoading: Bool = false
    
    private init() {
        print("🚗 SharedVehicleManager: Initialized")
        loadVehicles()
    }
    
    func loadVehicles() {
        print("🔄 Loading vehicles...")
        if let saved = loadFromUserDefaults() {
            vehicles = saved
            print("✅ Loaded \(saved.count) vehicles")
        }
    }
    
    func addVehicle(_ vehicle: VehicleData) {
        vehicles.append(vehicle)
        saveToUserDefaults()
        print("✅ Added vehicle: \(vehicle.registrationNumber)")
    }
    
    func addVehicleByNumber(_ registrationNumber: String) {
        let trimmed = registrationNumber.trimmingCharacters(in: .whitespacesAndNewlines)
        
        if vehicles.contains(where: { $0.registrationNumber.lowercased() == trimmed.lowercased() }) {
            print("⚠️ Vehicle already exists")
            return
        }
        
        let newVehicle = VehicleData(
            id: UUID(),
            registrationNumber: trimmed,
            vehicleName: "Vehicle",
            colorHex: "#007AFF"
        )
        addVehicle(newVehicle)
    }
    
    func updateVehicle(at index: Int, with vehicle: VehicleData) {
        guard index < vehicles.count else { return }
        vehicles[index] = vehicle
        saveToUserDefaults()
        print("✅ Updated vehicle")
    }
    
    func removeVehicle(at index: Int) {
        guard index < vehicles.count else { return }
        vehicles.remove(at: index)
        saveToUserDefaults()
        print("✅ Removed vehicle")
    }
    
    func getVehicleNumbers() -> [String] {
        return vehicles.map { $0.registrationNumber }
    }
    
    func getPrimaryVehicle() -> String? {
        return vehicles.first?.registrationNumber
    }
    
    // MARK: - Storage
    
    private func saveToUserDefaults() {
        if let encoded = try? JSONEncoder().encode(vehicles) {
            UserDefaults.standard.set(encoded, forKey: "sharedVehicles")
            print("💾 Saved \(vehicles.count) vehicles")
        }
    }
    
    private func loadFromUserDefaults() -> [VehicleData]? {
        guard let data = UserDefaults.standard.data(forKey: "sharedVehicles"),
              let decoded = try? JSONDecoder().decode([VehicleData].self, from: data) else {
            return nil
        }
        return decoded
    }
}

// ✅ SIMPLE VEHICLE MODEL - No Color conflicts
struct VehicleData: Identifiable, Equatable, Codable {
    let id: UUID
    var registrationNumber: String
    var vehicleName: String
    var colorHex: String // Store as hex string instead of Color
    
    var swiftUIColor: Color {
        Color(hex: colorHex) ?? .blue
    }
    
    init(id: UUID, registrationNumber: String, vehicleName: String, colorHex: String) {
        self.id = id
        self.registrationNumber = registrationNumber
        self.vehicleName = vehicleName
        self.colorHex = colorHex
    }
    
    static func == (lhs: VehicleData, rhs: VehicleData) -> Bool {
        return lhs.id == rhs.id &&
               lhs.registrationNumber == rhs.registrationNumber
    }
}

// ✅ HELPER EXTENSION
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
