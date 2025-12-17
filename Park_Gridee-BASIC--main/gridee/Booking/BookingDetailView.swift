//
//
//import SwiftUI
//
//struct BookingDetailView: View {
//    let booking: Bookings
//    @Environment(\.dismiss) private var dismiss
//    @EnvironmentObject var homeViewModel: HomeViewModel
//    @State private var showCancelAlert = false
//    @State private var isCancelling = false
//
//    
//    // ✅ QR Code State
//    @State private var showingQRCode = false
//
////    var body: some View {
////        NavigationView {
////            ScrollView {
////                VStack(spacing: 0) {
////                    // ✅ Enhanced Hero Section with Vehicle & Price
////                    heroSection
////                    
////                    // ✅ QR Code Section (Only for ACTIVE bookings)
////                    if booking.status.uppercased() == "ACTIVE" {
////                        qrCodeSection
////                    }
////                    
////                    // ✅ Main Content - Cleaner Cards
////                    VStack(spacing: 20) {
////                        timingDetailsSection
////                        pricingSection
////                        bookingReferenceSection
////                    }
////                    .padding(.horizontal, 20)
////                    .padding(.top, 30)
////                    .padding(.bottom, 40)
////                }
////            }
////            .background(Color(.systemGroupedBackground))
////            .navigationTitle("Booking Details")
////            .navigationBarTitleDisplayMode(.inline)
////            .toolbar {
////                ToolbarItem(placement: .navigationBarTrailing) {
////                    Button("Done") {
////                        dismiss()
////                    }
////                    .fontWeight(.semibold)
////                }
////            }
////        }
////        .sheet(isPresented: $showingQRCode) {
////            QRCodeDisplayView(booking: booking)
////        }
////    }
//    
//    var body: some View {
//        NavigationView {
//            ScrollView {
//                VStack(spacing: 0) {
//                    // ✅ Enhanced Hero Section with Vehicle & Price
//                    heroSection
//                    
//                    // ✅ UPDATED: QR Code Section for PENDING and ACTIVE bookings
//                    if booking.status.uppercased() == "PENDING" || booking.status.uppercased() == "ACTIVE" {
//                        qrCodeSection
//                    }
//                    
//                    // ✅ Main Content - Cleaner Cards
//                    VStack(spacing: 20) {
//                        timingDetailsSection
//                        pricingSection
//                        bookingReferenceSection
//                        
//                        // ✅ NEW: Cancel button for PENDING bookings
//                        if booking.status.uppercased() == "PENDING" {
//                            cancelBookingButton
//                        }
//                    }
//                    .padding(.horizontal, 20)
//                    .padding(.top, 30)
//                    .padding(.bottom, 40)
//                }
//            }
//            .background(Color(.systemGroupedBackground))
//            .navigationTitle("Booking Details")
//            .navigationBarTitleDisplayMode(.inline)
//            .toolbar {
//                ToolbarItem(placement: .navigationBarTrailing) {
//                    Button("Done") {
//                        dismiss()
//                    }
//                    .fontWeight(.semibold)
//                }
//               
//            }
//            .scrollIndicators(.hidden)
//        
//        }
//        .sheet(isPresented: $showingQRCode) {
//            QRCodeDisplayView(booking: booking)
//                .environmentObject(homeViewModel)
//        }
//        .alert("Cancel Booking?", isPresented: $showCancelAlert) {
//            Button("Cancel Booking", role: .destructive) {
//                cancelBooking()
//            }
//            Button("Keep Booking", role: .cancel) {}
//        } message: {
//            Text("Are you sure you want to cancel this booking? This action cannot be undone and any prepaid amount will be refunded.")
//        }
//    }
//
//
////
////    // ✅ UPDATED: QR Code Section for BOTH Pending and Active bookings
//    private var qrCodeSection: some View {
//        VStack(spacing: 16) {
//            Button(action: {
//                showingQRCode = true
//            }) {
//                HStack(spacing: 16) {
//                    // QR Icon
//                    ZStack {
//                        RoundedRectangle(cornerRadius: 12)
//                            .fill(qrIconColor.opacity(0.15))
//                            .frame(width: 50, height: 50)
//                        
//                        Image(systemName: "qrcode")
//                            .font(.system(size: 24, weight: .medium))
//                            .foregroundColor(qrIconColor)
//                    }
//                    
//                    // Text Content - Changes based on status
//                    VStack(alignment: .leading, spacing: 4) {
//                        Text(qrButtonTitle)
//                            .font(.headline)
//                            .fontWeight(.semibold)
//                            .foregroundColor(.primary)
//                        
//                        Text(qrButtonSubtitle)
//                            .font(.subheadline)
//                            .foregroundColor(.secondary)
//                    }
//                    
//                    Spacer()
//                    
//                    // Chevron
//                    Image(systemName: "chevron.right")
//                        .font(.system(size: 14, weight: .medium))
//                        .foregroundColor(.secondary)
//                }
//                .padding(20)
//                .background(Color(.systemBackground))
//                .cornerRadius(16)
//                .shadow(color: .black.opacity(0.05), radius: 8, x: 0, y: 2)
//            }
//            .buttonStyle(PlainButtonStyle())
//        }
//        .padding(.horizontal, 20)
//        .padding(.top, 20)
//    }
//
//    // ✅ NEW: Computed properties for QR button text based on status
//    private var qrIconColor: Color {
//        switch booking.status.uppercased() {
//        case "PENDING":
//            return .orange
//        case "ACTIVE":
//            return .blue
//        default:
//            return .gray
//        }
//    }
//
//    private var qrButtonTitle: String {
//        switch booking.status.uppercased() {
//        case "PENDING":
//            return "Generate Check-in QR Code"
//        case "ACTIVE":
//            return "Generate Checkout QR Code"
//        default:
//            return "Generate QR Code"
//        }
//    }
//
//    private var qrButtonSubtitle: String {
//        switch booking.status.uppercased() {
//        case "PENDING":
//            return "Show QR code for check-in"
//        case "ACTIVE":
//            return "Show QR code for checkout"
//        default:
//            return "Show QR code"
//        }
//    }
//
//    // ✅ Enhanced Hero Section with Vehicle & Total
//    private var heroSection: some View {
//        VStack(spacing: 20) {
//            // Status Icon
//            ZStack {
//                Circle()
//                    .fill(statusColor(booking.status).opacity(0.15))
//                    .frame(width: 80, height: 80)
//                
//                Image(systemName: statusIcon(booking.status))
//                    .font(.system(size: 32, weight: .medium))
//                    .foregroundColor(statusColor(booking.status))
//            }
//            
//            // Zone Name
//            Text(getZoneNameFromSpotId(booking.spotId))
//                .font(.title2)
//                .fontWeight(.bold)
//                .foregroundColor(.primary)
//            
//            // Status Badge
//            Text(booking.status.capitalized)
//                .font(.subheadline)
//                .fontWeight(.semibold)
//                .padding(.horizontal, 16)
//                .padding(.vertical, 6)
//                .background(statusColor(booking.status).opacity(0.15))
//                .foregroundColor(statusColor(booking.status))
//                .clipShape(Capsule())
//            
//            // ✅ Vehicle & Total Amount Row
//            HStack(spacing: 24) {
//                // Vehicle Info
//                if let vehicle = getVehicleNumber(), !vehicle.isEmpty {
//                    VStack(spacing: 6) {
//                        HStack(spacing: 8) {
//                            Image(systemName: "car.fill")
//                                .font(.system(size: 16))
//                                .foregroundColor(.blue)
//                            
//                            Text(vehicle)
//                                .font(.headline)
//                                .fontWeight(.semibold)
//                                .foregroundColor(.primary)
//                        }
//                        
//                        Text("Vehicle")
//                            .font(.caption)
//                            .foregroundColor(.secondary)
//                            .textCase(.uppercase)
////                            .background(Color(.systemGray5))
//
//                    }
//                    .padding(.horizontal, 20)
//                    .padding(.vertical, 12)
////                    .background(Color(.systemBackground))
//                    .background(Color(.systemGray5))
//
//                    .clipShape(RoundedRectangle(cornerRadius: 12))
//                    .shadow(color: .black.opacity(0.05), radius: 4, x: 0, y: 1)
//                }
//                
//                // Total Amount
//                VStack(spacing: 6) {
//                    HStack(spacing: 8) {
//                        Image(systemName: "seal.fill")
//                            .font(.system(size: 16))
//                            .foregroundColor(.green)
//                        
//                        Text("₹\(String(format: "%.2f", booking.totalAmount))")
//                            .font(.headline)
//                            .fontWeight(.bold)
//                            .foregroundColor(.green)
//                    }
//                    
//                    Text("Total")
//                        .font(.caption)
//                        .foregroundColor(.secondary)
//                        .textCase(.uppercase)
////                        .background(Color(.systemGray5))
//
//                }
//                .padding(.horizontal, 20)
//                .padding(.vertical, 12)
//                .background(Color(.systemGray5))
//
//                .clipShape(RoundedRectangle(cornerRadius: 12))
//                .shadow(color: .black.opacity(0.05), radius: 4, x: 0, y: 1)
//            }
//        }
//        .frame(maxWidth: .infinity)
//        .padding(.vertical, 30)
//        .background(
//            LinearGradient(
//                colors: [Color(.systemBackground), Color(.systemGroupedBackground)],
//                startPoint: .top,
//                endPoint: .bottom
//            )
//        )
//    }
//    
//    // ✅ Simplified Timing Details Card
////    private var timingDetailsSection: some View {
////        DetailCard(title: "Timing Details") {
////            VStack(spacing: 16) {
////                if let checkInTime = booking.checkInTime {
////                    DetailRowModern(
////                        icon: "clock.arrow.circlepath",
////                        iconColor: .green,
////                        title: "Check-in",
////                        value: formatDateTimeString(checkInTime)
////                    )
////                }
////                
////                if let checkOutTime = booking.checkOutTime {
////                    DetailRowModern(
////                        icon: "clock.badge.checkmark",
////                        iconColor: .orange,
////                        title: "Check-out",
////                        value: formatDateTimeString(checkOutTime)
////                    )
////                }
////                
////                if let checkInTime = booking.checkInTime,
////                   let checkOutTime = booking.checkOutTime {
////                    DetailRowModern(
////                        icon: "timer",
////                        iconColor: .purple,
////                        title: "Duration",
////                        value: calculateDuration(from: checkInTime, to: checkOutTime)
////                    )
////                }
////            }
////        }
////    }
////
//    // ✅ UPDATED: Timing Details Section with better dark mode contrast
//    private var timingDetailsSection: some View {
//        DetailCard(title: "Timing Details") {
//            VStack(spacing: 16) {
//                if let checkInTime = booking.checkInTime {
//                    DetailRowModern(
//                        icon: "clock.arrow.circlepath",
//                        iconColor: .green,
//                        title: "Check-in",
//                        value: formatDateTimeString(checkInTime)
//                    )
//                }
//                
//                if let checkOutTime = booking.checkOutTime {
//                    DetailRowModern(
//                        icon: "clock.badge.checkmark",
//                        iconColor: .orange,
//                        title: "Check-out",
//                        value: formatDateTimeString(checkOutTime)
//                    )
//                }
//                
//                if let checkInTime = booking.checkInTime,
//                   let checkOutTime = booking.checkOutTime {
//                    DetailRowModern(
//                        icon: "timer",
//                        iconColor: .purple,
//                        title: "Duration",
//                        value: calculateDuration(from: checkInTime, to: checkOutTime)
//                    )
//                }
//            }
//        }
//    }
//
//    // ✅ UPDATED: DetailCard with better dark mode support
//    struct DetailCard<Content: View>: View {
//        let title: String
//        let content: Content
//        @Environment(\.colorScheme) var colorScheme
//        
//        init(title: String, @ViewBuilder content: () -> Content) {
//            self.title = title
//            self.content = content()
//        }
//        
//        var body: some View {
//            VStack(alignment: .leading, spacing: 16) {
//                Text(title)
//                    .font(.headline)
//                    .fontWeight(.semibold)
//                    .foregroundColor(.secondary)
//                    .textCase(.uppercase)
//                    .font(.caption)
//                
//                content
//            }
//            .padding(20)
//            .background(
//                RoundedRectangle(cornerRadius: 16)
//                    .fill(colorScheme == .dark ? Color(.systemGray5) : Color(.systemBackground))
//            )
//            .shadow(color: .black.opacity(colorScheme == .dark ? 0 : 0.05), radius: 8, x: 0, y: 2)
//        }
//    }
//
//    // ✅ UPDATED: DetailRowModern with better spacing
//    struct DetailRowModern: View {
//        let icon: String
//        let iconColor: Color
//        let title: String
//        let value: String
//        @Environment(\.colorScheme) var colorScheme
//        
//        var body: some View {
//            HStack(spacing: 12) {
//                ZStack {
//                    Circle()
//                        .fill(iconColor.opacity(colorScheme == .dark ? 0.25 : 0.15))
//                        .frame(width: 36, height: 36)
//                    
//                    Image(systemName: icon)
//                        .font(.system(size: 16, weight: .medium))
//                        .foregroundColor(iconColor)
//                }
//                
//                Text(title)
//                    .font(.body)
//                    .foregroundColor(.secondary)
//                
//                Spacer()
//                
//                Text(value)
//                    .font(.body)
//                    .fontWeight(.semibold)
//                    .foregroundColor(.primary)
//            }
//        }
//    }
//
//    // ✅ Simplified Pricing Card (Rate & Hours only)
//    private var pricingSection: some View {
//        DetailCard(title: "Pricing Breakdown") {
//            VStack(spacing: 16) {
//                DetailRowModern(
//                    icon: "seal.fill",
//                    iconColor: .blue,
//                    title: "Rate per Hour",
//                    value: "₹\(String(format: "%.2f", homeViewModel.parkingConfig.hourlyRate))"
//
//                )
//                
//                DetailRowModern(
//                    icon: "clock",
//                    iconColor: .orange,
//                    title: "Total Hours",
//                    value: String(format: "%.1f hrs", booking.totalHours)
//                )
//            }
//        }
//    }
//    // ✅ NEW: Cancel Booking Button
//    private var cancelBookingButton: some View {
//        Button(action: {
//            showCancelAlert = true
//        }) {
//            HStack(spacing: 12) {
//                if isCancelling {
//                    ProgressView()
//                        .tint(.white)
//                        .scaleEffect(0.9)
//                } else {
//                    Image(systemName: "xmark.circle.fill")
//                        .font(.system(size: 18, weight: .semibold))
//                    
//                    Text("Cancel Booking")
//                        .font(.headline)
//                        .fontWeight(.semibold)
//                }
//            }
//            .foregroundColor(.white)
//            .frame(maxWidth: .infinity)
//            .padding(.vertical, 16)
//            .background(
//                RoundedRectangle(cornerRadius: 14)
//                    .fill(Color.red)
//            )
//            .shadow(color: .red.opacity(0.3), radius: 8, x: 0, y: 4)
//        }
//        .disabled(isCancelling)
//        .padding(.top, 8)
//    }
//
//    // ✅ NEW: Cancel booking function
//    private func cancelBooking() {
//        guard let userId = getCurrentUserId() else {
//            print("❌ No user ID found")
//            return
//        }
//        
//        isCancelling = true
//        
//        homeViewModel.cancelBooking(booking) { success, message in
//            DispatchQueue.main.async {
//                isCancelling = false
//                
//                if success {
//                    print("✅ Booking cancelled successfully")
//                    // Close the detail view
//                    dismiss()
//                } else {
//                    print("❌ Cancel failed: \(message ?? "Unknown error")")
//                    // You can show an error alert here if needed
//                }
//            }
//        }
//    }
//
//    // ✅ NEW: Get current user ID helper
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
//    
//    // ✅ Booking Reference Card
//    private var bookingReferenceSection: some View {
//        DetailCard(title: "Booking Reference") {
//            HStack {
//                HStack(spacing: 12) {
//                    ZStack {
//                        Circle()
//                            .fill(Color.gray.opacity(0.15))
//                            .frame(width: 32, height: 32)
//                        
//                        Image(systemName: "doc.text")
//                            .font(.system(size: 14, weight: .medium))
//                            .foregroundColor(.gray)
//                    }
//                    
//                    Text("Booking ID")
//                        .font(.body)
//                        .foregroundColor(.secondary)
//                }
//                
//                Spacer()
//                
//                // Copy Button with ID
//                Button(action: {
//                    UIPasteboard.general.string = booking.id
//                }) {
//                    HStack(spacing: 6) {
//                        Text(String(booking.id.suffix(12)))
//                            .font(.body)
//                            .fontWeight(.medium)
//                            .foregroundColor(.blue)
//                        
//                        Image(systemName: "doc.on.doc")
//                            .font(.system(size: 12))
//                            .foregroundColor(.blue)
//                    }
//                    .padding(.horizontal, 12)
//                    .padding(.vertical, 6)
//                    .background(Color.blue.opacity(0.1))
//                    .clipShape(RoundedRectangle(cornerRadius: 8))
//                }
//            }
//        }
//    }
//    
//    // ✅ Fixed getVehicleNumber to return optional
//    private func getVehicleNumber() -> String? {
//        if let vehicleNumbers = booking.vehicleNumber,
//           !vehicleNumbers.isEmpty,
//           !vehicleNumbers.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
//           vehicleNumbers != "Unknown" {
//            return vehicleNumbers
//        }
//        return nil
//    }
//    
//    // All your existing helper functions remain the same...
//    private func formatDateTimeString(_ dateString: String) -> String {
//        let isoFormatter = ISO8601DateFormatter()
//        isoFormatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
//        
//        if let date = isoFormatter.date(from: dateString) {
//            let displayFormatter = DateFormatter()
//            displayFormatter.dateFormat = "dd MMM yyyy 'at' h:mm a"
//            return displayFormatter.string(from: date)
//        }
//        
//        let simpleIsoFormatter = ISO8601DateFormatter()
//        if let date = simpleIsoFormatter.date(from: dateString) {
//            let displayFormatter = DateFormatter()
//            displayFormatter.dateFormat = "dd MMM yyyy 'at' h:mm a"
//            return displayFormatter.string(from: date)
//        }
//        
//        let customFormatter = DateFormatter()
//        customFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSXXXXX"
//        if let date = customFormatter.date(from: dateString) {
//            let displayFormatter = DateFormatter()
//            displayFormatter.dateFormat = "dd MMM yyyy 'at' h:mm a"
//            return displayFormatter.string(from: date)
//        }
//        
//        return dateString
//    }
//    
//    private func getZoneNameFromSpotId(_ spotId: String) -> String {
//        switch spotId {
//        case "ps1":
//            return "TP Avenue Parking"
//        case "ps2":
//            return "Medical College"
//        case "ps3":
//            return "City Center Parking"
//        default:
//            if spotId.hasPrefix("ps") {
//                let number = spotId.replacingOccurrences(of: "ps", with: "")
//                return "Parking Zone \(number.uppercased())"
//            }
//            return "Parking Location \(spotId.uppercased())"
//        }
//    }
//    
//    private func calculateDuration(from checkInTime: String, to checkOutTime: String) -> String {
//        let formatter = ISO8601DateFormatter()
//        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
//        
//        guard let checkIn = formatter.date(from: checkInTime),
//              let checkOut = formatter.date(from: checkOutTime) else {
//            return "N/A"
//        }
//        
//        let duration = checkOut.timeIntervalSince(checkIn)
//        let hours = duration / 3600
//        
//        let totalMinutes = Int(hours * 60)
//        let hrs = totalMinutes / 60
//        let mins = totalMinutes % 60
//        
//        if hrs > 0 && mins > 0 {
//            return "\(hrs)h \(mins)m"
//        } else if hrs > 0 {
//            return "\(hrs)h"
//        } else {
//            return "\(mins)m"
//        }
//    }
//    
//    private func statusColor(_ status: String) -> Color {
//        switch status.lowercased() {
//        case "active":
//            return .green
//        case "completed":
//            return .blue
//        case "pending":
//            return .orange
//        case "cancelled":
//            return .red
//        default:
//            return .gray
//        }
//    }
//    
//    private func statusIcon(_ status: String) -> String {
//        switch status.lowercased() {
//        case "active":
//            return "play.circle.fill"
//        case "completed":
//            return "checkmark.circle.fill"
//        case "pending":
//            return "clock.circle.fill"
//        case "cancelled":
//            return "xmark.circle.fill"
//        default:
//            return "circle"
//        }
//    }
//}
//
//// ✅ Keep the same DetailCard and DetailRowModern components
//struct DetailCard<Content: View>: View {
//    let title: String
//    let content: Content
//    
//    init(title: String, @ViewBuilder content: () -> Content) {
//        self.title = title
//        self.content = content()
//    }
//    
//    var body: some View {
//        VStack(alignment: .leading, spacing: 16) {
//            Text(title)
//                .font(.headline)
//                .fontWeight(.semibold)
//                .foregroundColor(.secondary)
//                .textCase(.uppercase)
//                .font(.caption)
//            
//            content
//        }
//        .padding(20)
//        .background(Color(.systemBackground))
//        .clipShape(RoundedRectangle(cornerRadius: 16))
//        .shadow(color: .black.opacity(0.05), radius: 8, x: 0, y: 2)
//    }
//}
//
//struct DetailRowModern: View {
//    let icon: String
//    let iconColor: Color
//    let title: String
//    let value: String
//    
//    var body: some View {
//        HStack(spacing: 12) {
//            ZStack {
//                Circle()
//                    .fill(iconColor.opacity(0.15))
//                    .frame(width: 32, height: 32)
//                
//                Image(systemName: icon)
//                    .font(.system(size: 14, weight: .medium))
//                    .foregroundColor(iconColor)
//            }
//            
//            Text(title)
//                .font(.body)
//                .foregroundColor(.secondary)
//            
//            Spacer()
//            
//            Text(value)
//                .font(.body)
//                .fontWeight(.medium)
//                .foregroundColor(.primary)
//        }
//    }
//}
//



import SwiftUI

struct BookingDetailView: View {
    let booking: Bookings
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject var homeViewModel: HomeViewModel
    @State private var showCancelAlert = false
    @State private var isCancelling = false

    
    // ✅ QR Code State
    @State private var showingQRCode = false

//    var body: some View {
//        NavigationView {
//            ScrollView {
//                VStack(spacing: 0) {
//                    // ✅ Enhanced Hero Section with Vehicle & Price
//                    heroSection
//
//                    // ✅ QR Code Section (Only for ACTIVE bookings)
//                    if booking.status.uppercased() == "ACTIVE" {
//                        qrCodeSection
//                    }
//
//                    // ✅ Main Content - Cleaner Cards
//                    VStack(spacing: 20) {
//                        timingDetailsSection
//                        pricingSection
//                        bookingReferenceSection
//                    }
//                    .padding(.horizontal, 20)
//                    .padding(.top, 30)
//                    .padding(.bottom, 40)
//                }
//            }
//            .background(Color(.systemGroupedBackground))
//            .navigationTitle("Booking Details")
//            .navigationBarTitleDisplayMode(.inline)
//            .toolbar {
//                ToolbarItem(placement: .navigationBarTrailing) {
//                    Button("Done") {
//                        dismiss()
//                    }
//                    .fontWeight(.semibold)
//                }
//            }
//        }
//        .sheet(isPresented: $showingQRCode) {
//            QRCodeDisplayView(booking: booking)
//        }
//    }
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 0) {
                    // ✅ Enhanced Hero Section with Vehicle & Price
                    heroSection
                    
                    // ✅ UPDATED: QR Code Section for PENDING and ACTIVE bookings
                    if booking.status.uppercased() == "PENDING" || booking.status.uppercased() == "ACTIVE" {
                        qrCodeSection
                    }
                    
                    // ✅ Main Content - Cleaner Cards
                    VStack(spacing: 20) {
                        timingDetailsSection
                        pricingSection
                        bookingReferenceSection
                        
                        // ✅ NEW: Cancel button for PENDING bookings
                        if booking.status.uppercased() == "PENDING" {
                            cancelBookingButton
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 30)
                    .padding(.bottom, 40)
                }
            }
            .background(Color(.systemGroupedBackground))
            .navigationTitle("Booking Details")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") {
                        dismiss()
                    }
                    .fontWeight(.semibold)
                }
               
            }
            .scrollIndicators(.hidden)
        
        }
        .sheet(isPresented: $showingQRCode) {
            QRCodeDisplayView(booking: booking)
                .environmentObject(homeViewModel)
        }
        .alert("Cancel Booking?", isPresented: $showCancelAlert) {
            Button("Cancel Booking", role: .destructive) {
                cancelBooking()
            }
            Button("Keep Booking", role: .cancel) {}
        } message: {
            Text("Are you sure you want to cancel this booking? This action cannot be undone and any prepaid amount will be refunded.")
        }
    }


//
//    // ✅ UPDATED: QR Code Section for BOTH Pending and Active bookings
    private var qrCodeSection: some View {
        VStack(spacing: 16) {
            Button(action: {
                showingQRCode = true
            }) {
                HStack(spacing: 16) {
                    // QR Icon
                    ZStack {
                        RoundedRectangle(cornerRadius: 12)
                            .fill(qrIconColor.opacity(0.15))
                            .frame(width: 50, height: 50)
                        
                        Image(systemName: "qrcode")
                            .font(.system(size: 24, weight: .medium))
                            .foregroundColor(qrIconColor)
                    }
                    
                    // Text Content - Changes based on status
                    VStack(alignment: .leading, spacing: 4) {
                        Text(qrButtonTitle)
                            .font(.headline)
                            .fontWeight(.semibold)
                            .foregroundColor(.primary)
                        
                        Text(qrButtonSubtitle)
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                    
                    Spacer()
                    
                    // Chevron
                    Image(systemName: "chevron.right")
                        .font(.system(size: 14, weight: .medium))
                        .foregroundColor(.secondary)
                }
                .padding(20)
                .background(Color(.systemBackground))
                .cornerRadius(16)
                .shadow(color: .black.opacity(0.05), radius: 8, x: 0, y: 2)
            }
            .buttonStyle(PlainButtonStyle())
        }
        .padding(.horizontal, 20)
        .padding(.top, 20)
    }

    // ✅ NEW: Computed properties for QR button text based on status
    private var qrIconColor: Color {
        switch booking.status.uppercased() {
        case "PENDING":
            return .orange
        case "ACTIVE":
            return .blue
        default:
            return .gray
        }
    }

    private var qrButtonTitle: String {
        switch booking.status.uppercased() {
        case "PENDING":
            return "Generate Check-in QR Code"
        case "ACTIVE":
            return "Generate Checkout QR Code"
        default:
            return "Generate QR Code"
        }
    }

    private var qrButtonSubtitle: String {
        switch booking.status.uppercased() {
        case "PENDING":
            return "Show QR code for check-in"
        case "ACTIVE":
            return "Show QR code for checkout"
        default:
            return "Show QR code"
        }
    }

    // ✅ Enhanced Hero Section with Vehicle & Total
    private var heroSection: some View {
        VStack(spacing: 20) {
            // Status Icon
            ZStack {
                Circle()
                    .fill(statusColor(booking.status).opacity(0.15))
                    .frame(width: 80, height: 80)
                
                Image(systemName: statusIcon(booking.status))
                    .font(.system(size: 32, weight: .medium))
                    .foregroundColor(statusColor(booking.status))
            }
            
            // Zone Name
            Text(getZoneNameFromSpotId(booking.spotId))
                .font(.title2)
                .fontWeight(.bold)
                .foregroundColor(.primary)
            
            // Status Badge
            Text(booking.status.capitalized)
                .font(.subheadline)
                .fontWeight(.semibold)
                .padding(.horizontal, 16)
                .padding(.vertical, 6)
                .background(statusColor(booking.status).opacity(0.15))
                .foregroundColor(statusColor(booking.status))
                .clipShape(Capsule())
            
            // ✅ Vehicle & Total Amount Row
            HStack(spacing: 24) {
                // Vehicle Info
                if let vehicle = getVehicleNumber(), !vehicle.isEmpty {
                    VStack(spacing: 6) {
                        HStack(spacing: 8) {
                            Image(systemName: "car.fill")
                                .font(.system(size: 16))
                                .foregroundColor(.blue)
                            
                            Text(vehicle)
                                .font(.headline)
                                .fontWeight(.semibold)
                                .foregroundColor(.primary)
                        }
                        
                        Text("Vehicle")
                            .font(.caption)
                            .foregroundColor(.secondary)
                            .textCase(.uppercase)
//                            .background(Color(.systemGray5))

                    }
                    .padding(.horizontal, 20)
                    .padding(.vertical, 12)
//                    .background(Color(.systemBackground))
                    .background(Color(.systemGray5))

                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .shadow(color: .black.opacity(0.05), radius: 4, x: 0, y: 1)
                }
                
                // Total Amount
                VStack(spacing: 6) {
                    HStack(spacing: 8) {
                        Image(systemName: "seal.fill")
                            .font(.system(size: 16))
                            .foregroundColor(.green)
                        
                        Text("₹\(String(format: "%.2f", booking.totalAmount))")
                            .font(.headline)
                            .fontWeight(.bold)
                            .foregroundColor(.green)
                    }
                    
                    Text("Total")
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .textCase(.uppercase)
//                        .background(Color(.systemGray5))

                }
                .padding(.horizontal, 20)
                .padding(.vertical, 12)
                .background(Color(.systemGray5))

                .clipShape(RoundedRectangle(cornerRadius: 12))
                .shadow(color: .black.opacity(0.05), radius: 4, x: 0, y: 1)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 30)
        .background(
            LinearGradient(
                colors: [Color(.systemBackground), Color(.systemGroupedBackground)],
                startPoint: .top,
                endPoint: .bottom
            )
        )
    }
    
    // ✅ Simplified Timing Details Card
//    private var timingDetailsSection: some View {
//        DetailCard(title: "Timing Details") {
//            VStack(spacing: 16) {
//                if let checkInTime = booking.checkInTime {
//                    DetailRowModern(
//                        icon: "clock.arrow.circlepath",
//                        iconColor: .green,
//                        title: "Check-in",
//                        value: formatDateTimeString(checkInTime)
//                    )
//                }
//
//                if let checkOutTime = booking.checkOutTime {
//                    DetailRowModern(
//                        icon: "clock.badge.checkmark",
//                        iconColor: .orange,
//                        title: "Check-out",
//                        value: formatDateTimeString(checkOutTime)
//                    )
//                }
//
//                if let checkInTime = booking.checkInTime,
//                   let checkOutTime = booking.checkOutTime {
//                    DetailRowModern(
//                        icon: "timer",
//                        iconColor: .purple,
//                        title: "Duration",
//                        value: calculateDuration(from: checkInTime, to: checkOutTime)
//                    )
//                }
//            }
//        }
//    }
//
    // ✅ UPDATED: Timing Details Section with better dark mode contrast
    private var timingDetailsSection: some View {
        DetailCard(title: "Timing Details") {
            VStack(spacing: 16) {
                if let checkInTime = booking.checkInTime {
                    DetailRowModern(
                        icon: "clock.arrow.circlepath",
                        iconColor: .green,
                        title: "Check-in",
                        value: formatDateTimeString(checkInTime)
                    )
                }
                
                if let checkOutTime = booking.checkOutTime {
                    DetailRowModern(
                        icon: "clock.badge.checkmark",
                        iconColor: .orange,
                        title: "Check-out",
                        value: formatDateTimeString(checkOutTime)
                    )
                }
                
                if let checkInTime = booking.checkInTime,
                   let checkOutTime = booking.checkOutTime {
                    DetailRowModern(
                        icon: "timer",
                        iconColor: .purple,
                        title: "Duration",
                        value: calculateDuration(from: checkInTime, to: checkOutTime)
                    )
                }
            }
        }
    }

    // ✅ UPDATED: DetailCard with better dark mode support
    struct DetailCard<Content: View>: View {
        let title: String
        let content: Content
        @Environment(\.colorScheme) var colorScheme
        
        init(title: String, @ViewBuilder content: () -> Content) {
            self.title = title
            self.content = content()
        }
        
        var body: some View {
            VStack(alignment: .leading, spacing: 16) {
                Text(title)
                    .font(.headline)
                    .fontWeight(.semibold)
                    .foregroundColor(.secondary)
                    .textCase(.uppercase)
                    .font(.caption)
                
                content
            }
            .padding(20)
            .background(
                RoundedRectangle(cornerRadius: 16)
                    .fill(colorScheme == .dark ? Color(.systemGray5) : Color(.systemBackground))
            )
            .shadow(color: .black.opacity(colorScheme == .dark ? 0 : 0.05), radius: 8, x: 0, y: 2)
        }
    }

    // ✅ UPDATED: DetailRowModern with better spacing
    struct DetailRowModern: View {
        let icon: String
        let iconColor: Color
        let title: String
        let value: String
        @Environment(\.colorScheme) var colorScheme
        
        var body: some View {
            HStack(spacing: 12) {
                ZStack {
                    Circle()
                        .fill(iconColor.opacity(colorScheme == .dark ? 0.25 : 0.15))
                        .frame(width: 36, height: 36)
                    
                    Image(systemName: icon)
                        .font(.system(size: 16, weight: .medium))
                        .foregroundColor(iconColor)
                }
                
                Text(title)
                    .font(.body)
                    .foregroundColor(.secondary)
                
                Spacer()
                
                Text(value)
                    .font(.body)
                    .fontWeight(.semibold)
                    .foregroundColor(.primary)
            }
        }
    }

    // ✅ Simplified Pricing Card (Rate & Hours only)
    private var pricingSection: some View {
        DetailCard(title: "Pricing Breakdown") {
            VStack(spacing: 16) {
                DetailRowModern(
                    icon: "seal.fill",
                    iconColor: .blue,
                    title: "Rate per Hour",
                    value: "₹\(String(format: "%.2f", homeViewModel.parkingConfig.hourlyRate))"

                )
                
                DetailRowModern(
                    icon: "clock",
                    iconColor: .orange,
                    title: "Total Hours",
                    value: String(format: "%.1f hrs", booking.totalHours)
                )
            }
        }
    }
    // ✅ NEW: Cancel Booking Button
    private var cancelBookingButton: some View {
        Button(action: {
            showCancelAlert = true
        }) {
            HStack(spacing: 12) {
                if isCancelling {
                    ProgressView()
                        .tint(.white)
                        .scaleEffect(0.9)
                } else {
                    Image(systemName: "xmark.circle.fill")
                        .font(.system(size: 18, weight: .semibold))
                    
                    Text("Cancel Booking")
                        .font(.headline)
                        .fontWeight(.semibold)
                }
            }
            .foregroundColor(.white)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 16)
            .background(
                RoundedRectangle(cornerRadius: 14)
                    .fill(Color.red)
            )
            .shadow(color: .red.opacity(0.3), radius: 8, x: 0, y: 4)
        }
        .disabled(isCancelling)
        .padding(.top, 8)
    }

    // ✅ NEW: Cancel booking function
    private func cancelBooking() {
        guard let userId = getCurrentUserId() else {
            print("❌ No user ID found")
            return
        }
        
        isCancelling = true
        
        homeViewModel.cancelBooking(booking) { success, message in
            DispatchQueue.main.async {
                isCancelling = false
                
                if success {
                    print("✅ Booking cancelled successfully")
                    // Close the detail view
                    dismiss()
                } else {
                    print("❌ Cancel failed: \(message ?? "Unknown error")")
                    // You can show an error alert here if needed
                }
            }
        }
    }

    // ✅ NEW: Get current user ID helper
    private func getCurrentUserId() -> String? {
        if let userId = UserDefaults.standard.string(forKey: "currentUserId") {
            return userId
        }
        
        if let userData = UserDefaults.standard.data(forKey: "userData"),
           let user = try? JSONDecoder().decode(Users.self, from: userData) {
            return user.id
        }
        
        return nil
    }

    
    // ✅ Booking Reference Card
    private var bookingReferenceSection: some View {
        DetailCard(title: "Booking Reference") {
            HStack {
                HStack(spacing: 12) {
                    ZStack {
                        Circle()
                            .fill(Color.gray.opacity(0.15))
                            .frame(width: 32, height: 32)
                        
                        Image(systemName: "doc.text")
                            .font(.system(size: 14, weight: .medium))
                            .foregroundColor(.gray)
                    }
                    
                    Text("Booking ID")
                        .font(.body)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
                
                // Copy Button with ID
                Button(action: {
                    UIPasteboard.general.string = booking.id
                }) {
                    HStack(spacing: 6) {
                        Text(String(booking.id.suffix(12)))
                            .font(.body)
                            .fontWeight(.medium)
                            .foregroundColor(.blue)
                        
                        Image(systemName: "doc.on.doc")
                            .font(.system(size: 12))
                            .foregroundColor(.blue)
                    }
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background(Color.blue.opacity(0.1))
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                }
            }
        }
    }
    
    // ✅ Fixed getVehicleNumber to return optional
    private func getVehicleNumber() -> String? {
        if let vehicleNumbers = booking.vehicleNumber,
           !vehicleNumbers.isEmpty,
           !vehicleNumbers.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
           vehicleNumbers != "Unknown" {
            return vehicleNumbers
        }
        return nil
    }
    
    // All your existing helper functions remain the same...
    private func formatDateTimeString(_ dateString: String) -> String {
        let isoFormatter = ISO8601DateFormatter()
        isoFormatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        
        if let date = isoFormatter.date(from: dateString) {
            let displayFormatter = DateFormatter()
            displayFormatter.dateFormat = "dd MMM yyyy 'at' h:mm a"
            return displayFormatter.string(from: date)
        }
        
        let simpleIsoFormatter = ISO8601DateFormatter()
        if let date = simpleIsoFormatter.date(from: dateString) {
            let displayFormatter = DateFormatter()
            displayFormatter.dateFormat = "dd MMM yyyy 'at' h:mm a"
            return displayFormatter.string(from: date)
        }
    
        let customFormatter = DateFormatter()
        customFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSXXXXX"
        if let date = customFormatter.date(from: dateString) {
            let displayFormatter = DateFormatter()
            displayFormatter.dateFormat = "dd MMM yyyy 'at' h:mm a"
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
    
    private func calculateDuration(from checkInTime: String, to checkOutTime: String) -> String {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        
        guard let checkIn = formatter.date(from: checkInTime),
              let checkOut = formatter.date(from: checkOutTime) else {
            return "N/A"
        }
        
        let duration = checkOut.timeIntervalSince(checkIn)
        let hours = duration / 3600
        
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

// ✅ Keep the same DetailCard and DetailRowModern components
struct DetailCard<Content: View>: View {
    let title: String
    let content: Content
    
    init(title: String, @ViewBuilder content: () -> Content) {
        self.title = title
        self.content = content()
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(title)
                .font(.headline)
                .fontWeight(.semibold)
                .foregroundColor(.secondary)
                .textCase(.uppercase)
                .font(.caption)
            
            content
        }
        .padding(20)
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .shadow(color: .black.opacity(0.05), radius: 8, x: 0, y: 2)
    }
}

struct DetailRowModern: View {
    let icon: String
    let iconColor: Color
    let title: String
    let value: String
    
    var body: some View {
        HStack(spacing: 12) {
            ZStack {
                Circle()
                    .fill(iconColor.opacity(0.15))
                    .frame(width: 32, height: 32)
                
                Image(systemName: icon)
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(iconColor)
            }
            
            Text(title)
                .font(.body)
                .foregroundColor(.secondary)
            
            Spacer()
            
            Text(value)
                .font(.body)
                .fontWeight(.medium)
                .foregroundColor(.primary)
        }
    }
}

