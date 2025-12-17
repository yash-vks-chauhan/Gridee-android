import SwiftUI

struct RealTimeParkingCard: View {
    @Environment(\.colorScheme) var colorScheme
    @EnvironmentObject var homeViewModel: HomeViewModel  // ✅ ADD THIS
    
    let parkingSpot: ParkingSpot
    let totalAvailable: Int
    let isBooking: Bool
    let onBookTap: () -> Void
    
    // ✅ ADAPTIVE: Colors change based on dark/light mode
    private var cardBackground: Color {
        colorScheme == .dark ? Color(UIColor.secondarySystemBackground) : .white
    }
    
    private var primaryTextColor: Color {
        colorScheme == .dark ? .white : .black
    }
    
    private var secondaryTextColor: Color {
        Color(UIColor.secondaryLabel)
    }
    
    private var circleBackground: Color {
        colorScheme == .dark ? Color(UIColor.tertiarySystemBackground) : .black
    }
    
    private var circleTextColor: Color {
        colorScheme == .dark ? .white : .white
    }
    
    private var buttonBackground: Color {
        if isBooking {
            return .gray
        }
        return colorScheme == .dark ? .white : .black
    }
    
    private var buttonTextColor: Color {
        colorScheme == .dark ? .black : .white
    }
    
    // ✅ AVAILABILITY COLOR (remains same)
    private var availabilityColor: Color {
        let available = parkingSpot.available ?? 0
        if available >= 150 { return .green }
        else if available >= 50 { return .orange }
        else if available > 0 { return .red }
        else { return .gray }
    }
    
    // ✅ ZONE NAME LOGIC (unchanged)
    private var displayZoneName: String {
        switch parkingSpot.id {
        case "ps1":
            return "TP Avenue Parking"
        case "ps2":
            return "Medical College"
        case "ps3":
            return "City Center Parking"
        default:
            if let zoneName = parkingSpot.zoneName, !zoneName.isEmpty {
                return zoneName
            }
            
            if !parkingSpot.id.isEmpty {
                let shortId = String(parkingSpot.id.prefix(8))
                return "Zone \(shortId)"
            }
            
            let capacity = parkingSpot.capacity ?? 0
            
            if capacity > 0 {
                if capacity >= 500 {
                    return "Large Parking Area"
                } else if capacity >= 200 {
                    return "Medium Parking Area"
                } else {
                    return "Small Parking Area"
                }
            }
            
            return "Parking Zone"
        }
    }
    
    var body: some View {
        Button(action: isBooking ? {} : onBookTap) {
            VStack(spacing: 16) {
                HStack(spacing: 16) {
                    // ✅ ADAPTIVE: Circle changes with theme
                    ZStack {
                        Circle()
                            .fill(circleBackground)
                            .frame(width: 50, height: 50)
                        
                        Text("P")
                            .font(.system(size: 24, weight: .bold))
                            .foregroundColor(circleTextColor)
                    }
                    
                    // ✅ ADAPTIVE: Text colors change
                    VStack(alignment: .leading, spacing: 6) {
                        Text(displayZoneName)
                            .font(.system(size: 20, weight: .bold))
                            .foregroundColor(primaryTextColor)
                        
                        Text("\(parkingSpot.available ?? 0) Available")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(availabilityColor)
                        
                        // ✅ UPDATED: Dynamic rate from backend
                        //                        Text("Capacity: \(parkingSpot.capacity ?? 0) • ₹\(String(format: "%.2f", homeViewModel.parkingConfig.hourlyRate))/hour")
                        //                            .font(.system(size: 13))
                        //                            .foregroundColor(secondaryTextColor)
                        //
                    }
                    
                    Spacer()
                    
                    // ✅ ADAPTIVE: Button inverts in dark mode
                    Button(action: isBooking ? {} : onBookTap) {
                        HStack {
                            if isBooking {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle(tint: buttonTextColor))
                                    .frame(width: 16, height: 16)
                            } else {
                                Text("Park")
                                    .font(.system(size: 16, weight: .semibold))
                            }
                        }
                        .foregroundColor(buttonTextColor)
                        .frame(width: 72, height: 40)
                        .background(buttonBackground)
                        .clipShape(RoundedRectangle(cornerRadius: 20))
                    }
                    .disabled(isBooking || (parkingSpot.available ?? 0) == 0)
                }
            }
            .padding(20)
            .background(
                RoundedRectangle(cornerRadius: 16)
                    .fill(cardBackground)
                    .shadow(
                        color: colorScheme == .dark ? .clear : .black.opacity(0.06),
                        radius: 8,
                        x: 0,
                        y: 2
                    )
            )
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 8)
        .buttonStyle(PlainButtonStyle())
        .disabled(isBooking || (parkingSpot.available ?? 0) == 0)
        .onAppear {
            print("🔍 ParkingSpot Debug:")
            print("   ID: '\(parkingSpot.id)'")
            print("   zoneName: '\(parkingSpot.zoneName ?? "nil")'")
            print("   Available: \(parkingSpot.available ?? 0)")
            print("   Capacity: \(parkingSpot.capacity ?? 0)")
            print("   Hourly Rate: ₹\(homeViewModel.parkingConfig.hourlyRate)")  // ✅ ADD THIS
            print("   Color Scheme: \(colorScheme)")
            print("   Display Name: '\(displayZoneName)'")
            print("   " + String(repeating: "-", count: 40))
        }
    }
}
