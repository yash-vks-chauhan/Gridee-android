



import SwiftUI

struct BookingPageContent: View {
    @EnvironmentObject var homeViewModel: HomeViewModel
    @Binding var activeContent: SheetContent?

    @State private var selectedBooking: Bookings? = nil
    @State private var showingBookingDetail = false
    @State private var selectedTab = 0
    @State private var isLoadingBookings = false
    let initialSelectedTab: Int
    init(activeContent: Binding<SheetContent?>, initialSelectedTab: Int = 0) {
           self._activeContent = activeContent
           self.initialSelectedTab = initialSelectedTab
       }

    private var activeBookings: [Bookings] {
        homeViewModel.userBookings
            .filter { $0.status.lowercased() == "active" }
            .sorted { booking1, booking2 in
                if let date1 = parseBookingDate(booking1.checkInTime),
                   let date2 = parseBookingDate(booking2.checkInTime) {
                    return date1 > date2
                }
                return booking1.id > booking2.id
            }
    }
    
    private var pendingBookings: [Bookings] {
        homeViewModel.userBookings
            .filter { $0.status.lowercased() == "pending" }
            .sorted { booking1, booking2 in
                if let date1 = parseBookingDate(booking1.checkInTime),
                   let date2 = parseBookingDate(booking2.checkInTime) {
                    return date1 > date2
                }
                return booking1.id > booking2.id
            }
    }
    
    private var completedBookings: [Bookings] {
        homeViewModel.userBookings
            .filter { $0.status.lowercased() == "completed" }
            .sorted { booking1, booking2 in
                if let date1 = parseBookingDate(booking1.checkOutTime),
                   let date2 = parseBookingDate(booking2.checkOutTime) {
                    return date1 > date2
                }
                if let date1 = parseBookingDate(booking1.checkInTime),
                   let date2 = parseBookingDate(booking2.checkInTime) {
                    return date1 > date2
                }
                return booking1.id > booking2.id
            }
    }

    var currentTabBookings: [Bookings] {
        switch selectedTab {
        case 0: return activeBookings
        case 1: return pendingBookings
        case 2: return completedBookings
        default: return []
        }
    }

    var body: some View {
        VStack(spacing: 0) {
            headerSection
            segmentedPicker
            bookingsList
            Spacer(minLength: 120)
        }
        .background(Color(.systemGroupedBackground))
        .sheet(isPresented: $showingBookingDetail) {
            if let booking = selectedBooking {
                BookingDetailView(booking: booking)
            }
        }
        .onAppear {
            homeViewModel.fetchAllData()
        }
        .refreshable {
            homeViewModel.fetchAllData()
        }
    }
    
    private var headerSection: some View {
        HStack {
            Text("Bookings")
                .font(.largeTitle)
                .fontWeight(.bold)
                .foregroundColor(.primary)
            
            Spacer()
        }
        .padding(.horizontal, 16)
        .padding(.top, 8)
        .padding(.bottom, 16)
    }
    
    private var segmentedPicker: some View {
        Picker("Booking Type", selection: $selectedTab) {
            Text("Active (\(activeBookings.count))").tag(0)
            Text("Pending (\(pendingBookings.count))").tag(1)
            Text("Completed (\(completedBookings.count))").tag(2)
        }
        .pickerStyle(SegmentedPickerStyle())
        .padding(.horizontal, 16)
        .padding(.bottom, 16)
    }
    
    private var bookingsList: some View {
        ScrollView {
            if currentTabBookings.isEmpty {
                VStack(spacing: 20) {
                    Image(systemName: emptyStateIcon)
                        .font(.system(size: 48))
                        .foregroundColor(.secondary)
                    
                    VStack(spacing: 8) {
                        Text("No \(tabTitle) bookings")
                            .font(.headline)
                            .fontWeight(.semibold)
                        
                        Text("Your \(tabTitle.lowercased()) bookings will appear here")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 60)
            } else {
                LazyVStack(spacing: 12) {
                    ForEach(currentTabBookings) { booking in
                        BookingCard(booking: booking)
                            .onTapGesture {
                                print("Tapped booking ID: \(booking.id)")
                                selectedBooking = booking
                                showingBookingDetail = true
                            }
                            .padding(.horizontal, 16)
                    }
                }
                .padding(.vertical, 8)
            }
        }
    }
    
    private var tabTitle: String {
        switch selectedTab {
        case 0: return "Active"
        case 1: return "Pending"
        case 2: return "Completed"
        default: return "Bookings"
        }
    }
    
    private var emptyStateIcon: String {
        switch selectedTab {
        case 0: return "play.circle"
        case 1: return "clock.circle"
        case 2: return "checkmark.circle"
        default: return "calendar"
        }
    }
    
    private func parseBookingDate(_ dateString: String?) -> Date? {
        guard let dateString = dateString else { return nil }
        
        let isoFormatter = ISO8601DateFormatter()
        isoFormatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        if let date = isoFormatter.date(from: dateString) {
            return date
        }
        
        let simpleIsoFormatter = ISO8601DateFormatter()
        if let date = simpleIsoFormatter.date(from: dateString) {
            return date
        }
        
        let customFormatter = DateFormatter()
        customFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSXXXXX"
        if let date = customFormatter.date(from: dateString) {
            return date
        }
        
        return nil
    }
}


struct BookingCard: View {
    let booking: Bookings
    @Environment(\.colorScheme) var colorScheme
    @EnvironmentObject var homeViewModel: HomeViewModel
    
    @State private var showCancelAlert = false
    @State private var isCancelling = false
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Header with zone name and status
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(getZoneNameFromSpotId(booking.spotId))
                        .font(.headline)
                        .fontWeight(.semibold)
                        .foregroundColor(.primary)
                    
                    Text(booking.status.capitalized)
                        .font(.caption)
                        .fontWeight(.semibold)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(statusColor(booking.status).opacity(0.15))
                        .foregroundColor(statusColor(booking.status))
                        .clipShape(Capsule())
                }
                
                Spacer()
                
                Image(systemName: statusIcon(booking.status))
                    .foregroundColor(statusColor(booking.status))
                    .font(.title2)
            }
            
            // Timing information
            HStack(spacing: 8) {
                Image(systemName: "clock")
                    .foregroundColor(.secondary)
                    .font(.system(size: 14))
                
                VStack(alignment: .leading, spacing: 2) {
                    if let checkInTime = booking.checkInTime {
                        Text("Check-in: \(formatDateTimeString(checkInTime))")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    
                    if let checkOutTime = booking.checkOutTime {
                        Text("Check-out: \(formatDateTimeString(checkOutTime))")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
                
                Spacer()
            }
            
            // Bottom section with vehicle and amount
            HStack {
                if let vehicle = booking.vehicleNumber, !vehicle.isEmpty {
                    HStack(spacing: 6) {
                        Image(systemName: "motorcycle.fill")
                            .foregroundColor(.primary)
                            .font(.system(size: 12))
                        Text(vehicle)
                            .font(.caption)
                            .fontWeight(.semibold)
                            .foregroundColor(.primary)
                    }
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(Color(.systemGray6))
                    .clipShape(Capsule())
                }
                
                Spacer()
                
                if booking.totalAmount > 0 {
                    Text("₹\(String(format: "%.2f", booking.totalAmount))")
                        .font(.subheadline)
                        .fontWeight(.bold)
                        .foregroundColor(.green)
                }
            }
            
//             ✅ NEW: Cancel button for PENDING bookings
            if booking.status.lowercased() == "pending" {
                Button(action: {
                    showCancelAlert = true
                }) {
                    HStack {
                        if isCancelling {
                            ProgressView()
                                .tint(.white)
                                .scaleEffect(0.8)
                        } else {
                            Image(systemName: "xmark.circle.fill")
                            Text("Cancel Booking")
                                .fontWeight(.semibold)
                        }
                    }
                    .font(.subheadline)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 10)
                    .background(Color.red)
                    .cornerRadius(10)
                }
                .disabled(isCancelling)
                .alert("Cancel Booking?", isPresented: $showCancelAlert) {
                    Button("Cancel Booking", role: .destructive) {
                        cancelBooking()
                    }
                    Button("Keep Booking", role: .cancel) {}
                } message: {
                    Text("Are you sure you want to cancel this booking? This action cannot be undone.")
                }
            }
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(Color(.systemGray5))
                .shadow(color: .clear, radius: 0)
        )
    }
    
//     ✅ NEW: Cancel booking function
    private func cancelBooking() {
        isCancelling = true
        
        homeViewModel.cancelBooking(booking) { success, message in
            DispatchQueue.main.async {
                isCancelling = false
                
                if success {
                    print("✅ Booking cancelled")
                } else {
                    print("❌ Cancel failed: \(message ?? "Unknown error")")
                }
            }
        }
  }
    
    private func formatDateTimeString(_ dateString: String) -> String {
        let isoFormatter = ISO8601DateFormatter()
        isoFormatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        
        if let date = isoFormatter.date(from: dateString) {
            let displayFormatter = DateFormatter()
            displayFormatter.dateFormat = "MMM dd • h:mm a"
            return displayFormatter.string(from: date)
        }
        
        let simpleIsoFormatter = ISO8601DateFormatter()
        if let date = simpleIsoFormatter.date(from: dateString) {
            let displayFormatter = DateFormatter()
            displayFormatter.dateFormat = "MMM dd • h:mm a"
            return displayFormatter.string(from: date)
        }
        
        let customFormatter = DateFormatter()
        customFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSXXXXX"
        if let date = customFormatter.date(from: dateString) {
            let displayFormatter = DateFormatter()
            displayFormatter.dateFormat = "MMM dd • h:mm a"
            return displayFormatter.string(from: date)
        }
        
        return dateString
    }
    
    private func getZoneNameFromSpotId(_ spotId: String) -> String {
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
    
    private func statusColor(_ status: String) -> Color {
        switch status.lowercased() {
        case "active":
            return .green
        case "completed":
            return .blue
        case "pending":
            return .orange
        case "cancelled":
            return .red
        default:
            return .gray
        }
    }
    
    private func statusIcon(_ status: String) -> String {
        switch status.lowercased() {
        case "active":
            return "play.circle.fill"
        case "completed":
            return "checkmark.circle.fill"
        case "pending":
            return "clock.circle.fill"
        case "cancelled":
            return "xmark.circle.fill"
        default:
            return "circle"
        }
    }
}

    private func formatDateTimeString(_ dateString: String) -> String {
        let isoFormatter = ISO8601DateFormatter()
        isoFormatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        
        if let date = isoFormatter.date(from: dateString) {
            let displayFormatter = DateFormatter()
            displayFormatter.dateFormat = "MMM dd • h:mm a"
            return displayFormatter.string(from: date)
        }
        
        let simpleIsoFormatter = ISO8601DateFormatter()
        if let date = simpleIsoFormatter.date(from: dateString) {
            let displayFormatter = DateFormatter()
            displayFormatter.dateFormat = "MMM dd • h:mm a"
            return displayFormatter.string(from: date)
        }
        
        let customFormatter = DateFormatter()
        customFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSXXXXX"
        if let date = customFormatter.date(from: dateString) {
            let displayFormatter = DateFormatter()
            displayFormatter.dateFormat = "MMM dd • h:mm a"
            return displayFormatter.string(from: date)
        }
        
        return dateString
    }
    
    private func getZoneNameFromSpotId(_ spotId: String) -> String {
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
    
    private func statusColor(_ status: String) -> Color {
        switch status.lowercased() {
        case "active":
            return .green
        case "completed":
            return .blue
        case "pending":
            return .orange
        case "cancelled":
            return .red
        default:
            return .gray
        }
    }
    
    private func statusIcon(_ status: String) -> String {
        switch status.lowercased() {
        case "active":
            return "play.circle.fill"
        case "completed":
            return "checkmark.circle.fill"
        case "pending":
            return "clock.circle.fill"
        case "cancelled":
            return "xmark.circle.fill"
        default:
            return "circle"
        }
    }

