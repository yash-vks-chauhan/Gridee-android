import SwiftUI

struct BookingSummaryView: View {
    let booking: Bookings
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject var homeViewModel: HomeViewModel
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    // Header
                    HStack {
                        Image(systemName: "checkmark.circle.fill")
                            .foregroundColor(.green)
                            .font(.title2)
                        Text("Booking Confirmed")
                            .font(.title2)
                            .fontWeight(.semibold)
                    }
                    .padding(.top)
                    
                    Divider()
                    
                    // ✅ UPDATED: Parking Details with Zone Name
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Parking Details")
                            .font(.headline)
                            .fontWeight(.semibold)
                        
                        // Show actual zone name instead of Spot ID
                        DetailRow(title: "Parking Location", value: getZoneNameFromSpotId(booking.spotId), icon: "location.circle")
                        
                        // Show vehicle info
                        if let vehicle = booking.vehicleNumber {
                            DetailRow(title: "Vehicle", value: vehicle, icon: "car")
                        }
                        
                        // Show availability info if we have the spot data
                        if let parkingSpot = getParkingSpotById(booking.spotId) {
                            DetailRow(title: "Available Spots", value: "\(parkingSpot.available ?? 0) of \(parkingSpot.capacity ?? 0)", icon: "parkingsign")
                        }
                    }
                    .padding()
                    .background(Color(.systemGray6))
                    .cornerRadius(12)
                    
                    // ✅ UPDATED: Timing Details with proper date formatting
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Timing Details")
                            .font(.headline)
                            .fontWeight(.semibold)
                        
                        if let checkInTime = booking.checkInTime {
                            DetailRow(title: "Check-in Time", value: formatDateTimeString(checkInTime), icon: "clock.arrow.circlepath")
                        }
                        if let checkOutTime = booking.checkOutTime {
                            DetailRow(title: "Check-out Time", value: formatDateTimeString(checkOutTime), icon: "clock.badge.checkmark")
                        }
                        DetailRow(title: "Total Duration", value: formatDuration(booking.totalHours), icon: "timer")
                    }
                    .padding()
                    .background(Color(.systemGray6))
                    .cornerRadius(12)
                    
                    // Pricing Summary
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Pricing Summary")
                            .font(.headline)
                            .fontWeight(.semibold)
                        
                        DetailRow(title: "Rate per Hour", value: "₹\(String(format: "%.2f", homeViewModel.parkingConfig.hourlyRate))", icon: "indianrupeesign.circle")

                        DetailRow(title: "Total Hours", value: String(format: "%.1f hrs", booking.totalHours), icon: "clock")
                        
                        Divider()
                        
                        HStack {
                            Text("Total Amount")
                                .font(.title3)
                                .fontWeight(.semibold)
                            Spacer()
                            Text(":)\(String(format: "%.2f", booking.totalAmount))")
                                .font(.title3)
                                .fontWeight(.bold)
                                .foregroundColor(.green)
                        }
                    }
                    .padding()
                    .background(Color(.systemGray6))
                    .cornerRadius(12)
                    
                    // Booking ID
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Booking Reference")
                            .font(.headline)
                            .fontWeight(.semibold)
                        
                        HStack {
                            Text("Booking ID: \(String(booking.id.suffix(12)))")
                                .font(.monospaced(.body)())
                                .foregroundColor(.secondary)
                            Spacer()
                            Button("Copy") {
                                UIPasteboard.general.string = booking.id
                            }
                            .font(.caption)
                            .foregroundColor(.blue)
                        }
                    }
                    .padding()
                    .background(Color(.systemGray6))
                    .cornerRadius(12)
                    
                    Spacer()
                }
                .padding()
            }
            .navigationTitle("Booking Summary")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") {
                        dismiss()
                    }
                    .fontWeight(.semibold)
                }
            }
        }
    }
    
    // ✅ IMPROVED: Better date/time parsing with multiple format support
    private func formatDateTimeString(_ dateString: String) -> String {
        // Try ISO8601 format first (your current format)
        let isoFormatter = ISO8601DateFormatter()
        isoFormatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        
        if let date = isoFormatter.date(from: dateString) {
            let displayFormatter = DateFormatter()
            displayFormatter.dateStyle = .medium
            displayFormatter.timeStyle = .short
            displayFormatter.locale = Locale.current
            return displayFormatter.string(from: date)
        }
        
        // Try alternative ISO format without fractional seconds
        let simpleIsoFormatter = ISO8601DateFormatter()
        if let date = simpleIsoFormatter.date(from: dateString) {
            let displayFormatter = DateFormatter()
            displayFormatter.dateFormat = "MMM dd, yyyy • h:mm a"
            return displayFormatter.string(from: date)
        }
        
        // Try manual parsing for your specific format: 2025-09-23T21:41:35.000+00:00
        let customFormatter = DateFormatter()
        customFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSXXXXX"
        if let date = customFormatter.date(from: dateString) {
            let displayFormatter = DateFormatter()
            displayFormatter.dateFormat = "MMM dd, yyyy • h:mm a"
            return displayFormatter.string(from: date)
        }
        
        // Try another common format
        let fallbackFormatter = DateFormatter()
        fallbackFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss"
        if let date = fallbackFormatter.date(from: dateString) {
            let displayFormatter = DateFormatter()
            displayFormatter.dateFormat = "MMM dd, yyyy • h:mm a"
            return displayFormatter.string(from: date)
        }
        
        // Last fallback - return original string if all parsing fails
        return dateString
    }
    
    // ✅ NEW: Get zone name from spot ID
    private func getZoneNameFromSpotId(_ spotId: String) -> String {
        // First try to get from HomeViewModel's parking spots
        if let parkingSpot = getParkingSpotById(spotId),
           let zoneName = parkingSpot.zoneName,
           !zoneName.isEmpty,
           zoneName != "nil",
           zoneName != "null" {
            return zoneName
        }
        
        // Fallback to your known mappings
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
    
    // ✅ NEW: Helper to get parking spot from HomeViewModel
    private func getParkingSpotById(_ spotId: String) -> ParkingSpot? {
        return homeViewModel.parkingSpots.first { $0.id == spotId }
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

// ✅ Keep your existing DetailRow struct - it's perfect!
struct DetailRow: View {
    let title: String
    let value: String
    let icon: String
    var valueColor: Color = .primary
    var valueWeight: Font.Weight = .medium
    
    var body: some View {
        HStack {
            Label(title, systemImage: icon)
                .font(.subheadline)
                .foregroundColor(.secondary)
            
            Spacer()
            
            Text(value)
                .font(.subheadline)
                .fontWeight(valueWeight)
                .foregroundColor(valueColor)
        }
    }
}
