//
//
//import SwiftUI
//
//struct QRCodeDisplayView: View {
//    let booking: Bookings
//    @State private var qrCodeImage: UIImage?
//    @State private var isGenerating = false
//    @State private var isProcessing = false
//    @State private var showAlert = false
//    @State private var alertTitle = ""
//    @State private var alertMessage = ""
//    @Environment(\.dismiss) private var dismiss
//    @EnvironmentObject var homeViewModel: HomeViewModel
//    
//    var body: some View {
//        NavigationView {
//            ScrollView {
//                VStack(spacing: 24) {
//                    // Header Section
//                    VStack(spacing: 8) {
//                        Image(systemName: "qrcode")
//                            .font(.system(size: 40))
//                            .foregroundColor(.blue)
//                        
//                        Text(getQRTitle())
//                            .font(.title2)
//                            .fontWeight(.bold)
//                        
//                        Text(getQRSubtitle())
//                            .font(.caption)
//                            .foregroundColor(.secondary)
//                            .multilineTextAlignment(.center)
//                    }
//                    .padding(.top)
//                    
//                    // QR Code Display Section
//                    VStack(spacing: 16) {
//                        if isGenerating {
//                            VStack(spacing: 12) {
//                                ProgressView()
//                                    .scaleEffect(1.5)
//                                Text("Generating QR Code...")
//                                    .font(.caption)
//                                    .foregroundColor(.secondary)
//                            }
//                            .frame(width: 280, height: 280)
//                            .background(Color(.systemGray6))
//                            .cornerRadius(16)
//                        } else if let qrImage = qrCodeImage {
//                            Image(uiImage: qrImage)
//                                .interpolation(.none)
//                                .resizable()
//                                .aspectRatio(contentMode: .fit)
//                                .frame(width: 280, height: 280)
//                                .background(Color.white)
//                                .cornerRadius(16)
//                                .shadow(color: .black.opacity(0.1), radius: 10, x: 0, y: 4)
//                        } else {
//                            VStack(spacing: 12) {
//                                Image(systemName: "exclamationmark.triangle")
//                                    .font(.system(size: 40))
//                                    .foregroundColor(.orange)
//                                
//                                Text("Failed to generate QR code")
//                                    .font(.headline)
//                                
//                                Button("Retry") {
//                                    generateQRCode()
//                                }
//                                .buttonStyle(.borderedProminent)
//                            }
//                            .frame(width: 280, height: 280)
//                            .background(Color(.systemGray6))
//                            .cornerRadius(16)
//                        }
//                    }
//                    
//                    // ✅ NEW: Check-in and Check-out Buttons
//                    if booking.status.uppercased() == "ACTIVE" {
//                        actionButtonsSection
//                    }
//                    
//                    // Booking Details Card
//                    bookingDetailsCard
//                    
//                    Spacer(minLength: 20)
//                }
//                .padding(.horizontal)
//            }
//            .background(Color(.systemGroupedBackground))
//            .navigationBarTitleDisplayMode(.inline)
//            .toolbar {
//                ToolbarItem(placement: .navigationBarLeading) {
//                    Button("Close") {
//                        dismiss()
//                    }
//                    .foregroundColor(.blue)
//                }
//            }
//            .alert(alertTitle, isPresented: $showAlert) {
//                Button("OK") {
//                    if alertTitle == "Success" {
//                        dismiss()
//                    }
//                }
//            } message: {
//                Text(alertMessage)
//            }
//        }
//        .onAppear {
//            generateQRCode()
//        }
//    }
//    
//    // ✅ NEW: Action Buttons Section
//    private var actionButtonsSection: some View {
//        VStack(spacing: 12) {
//            // Check-in Button
//            if booking.qrCodeScanned != true {
//                Button(action: performCheckIn) {
//                    HStack {
//                        if isProcessing {
//                            ProgressView()
//                                .progressViewStyle(CircularProgressViewStyle(tint: .white))
//                        } else {
//                            Image(systemName: "checkmark.circle.fill")
//                                .font(.system(size: 20))
//                            Text("Check In")
//                                .font(.headline)
//                        }
//                    }
//                    .frame(maxWidth: .infinity)
//                    .padding()
//                    .background(Color.green)
//                    .foregroundColor(.white)
//                    .cornerRadius(12)
//                }
//                .disabled(isProcessing)
//            }
//            
//            // Check-out Button
//            Button(action: performCheckOut) {
//                HStack {
//                    if isProcessing {
//                        ProgressView()
//                            .progressViewStyle(CircularProgressViewStyle(tint: .white))
//                    } else {
//                        Image(systemName: "arrow.right.circle.fill")
//                            .font(.system(size: 20))
//                        Text("Check Out")
//                            .font(.headline)
//                    }
//                }
//                .frame(maxWidth: .infinity)
//                .padding()
//                .background(Color.orange)
//                .foregroundColor(.white)
//                .cornerRadius(12)
//            }
//            .disabled(isProcessing)
//        }
//        .padding(.horizontal)
//    }
//    
//    private var bookingDetailsCard: some View {
//        VStack(spacing: 16) {
//            HStack {
//                Text("Booking Details")
//                    .font(.headline)
//                    .fontWeight(.semibold)
//                Spacer()
//            }
//            
//            VStack(spacing: 12) {
//                BookingDetailRow(title: "Booking ID", value: booking.id)
//                Divider()
//                BookingDetailRow(title: "Status", value: booking.status.capitalized, valueColor: getStatusColor(booking.status))
//                Divider()
//                BookingDetailRow(title: "Vehicle", value: booking.vehicleNumber ?? "No Vehicle")
//                Divider()
//                BookingDetailRow(title: "Spot ID", value: booking.spotId)
//                Divider()
//                BookingDetailRow(title: "Check-in", value: formatTime(booking.checkInTime))
//                Divider()
//                BookingDetailRow(title: "Check-out", value: formatTime(booking.checkOutTime))
//                Divider()
//                BookingDetailRow(title: "Duration", value: String(format: "%.1f hrs", booking.totalHours))
//                Divider()
//                BookingDetailRow(title: "Amount", value: formatAmount(booking.amount))
//                Divider()
//                BookingDetailRow(title: "Total Amount", value: "₹\(String(format: "%.2f", booking.totalAmount))", valueColor: .green)
//            }
//        }
//        .padding()
//        .background(Color(.systemBackground))
//        .cornerRadius(16)
//        .shadow(color: .black.opacity(0.05), radius: 5, x: 0, y: 2)
//    }
//    
//    // MARK: - API Calls
//    
//    private func performCheckIn() {
//        guard let userId = getCurrentUserId(),
//              let qrCode = booking.qrCode else {
//            showError("Missing required data")
//            return
//        }
//        
//        isProcessing = true
//        
//        APIService.shared.validateQrForCheckIn(userId: userId, bookingId: booking.id, qrCode: qrCode) { result in
//            DispatchQueue.main.async {
//                self.isProcessing = false
//                
//                switch result {
//                case .success(let validationResult):
//                    if validationResult.valid {
//                        // Perform actual check-in
//                        self.performActualCheckIn(userId: userId, qrCode: qrCode)
//                    } else {
//                        self.showError(validationResult.message ?? "QR validation failed")
//                    }
//                case .failure(let error):
//                    self.showError("Check-in failed: \(error.localizedDescription)")
//                }
//            }
//        }
//    }
//    
//    private func performActualCheckIn(userId: String, qrCode: String) {
//        APIService.shared.checkIn(userId: userId, bookingId: booking.id, qrCode: qrCode) { result in
//            DispatchQueue.main.async {
//                switch result {
//                case .success:
//                    self.showSuccess("Check-in successful!", message: "You have successfully checked in.")
//                    self.homeViewModel.fetchAllData()
//                case .failure(let error):
//                    self.showError("Check-in failed: \(error.localizedDescription)")
//                }
//            }
//        }
//    }
//    
//    private func performCheckOut() {
//        guard let userId = getCurrentUserId(),
//              let qrCode = booking.qrCode else {
//            showError("Missing required data")
//            return
//        }
//        
//        isProcessing = true
//        
//        APIService.shared.validateQrForCheckOut(userId: userId, bookingId: booking.id, qrCode: qrCode) { result in
//            DispatchQueue.main.async {
//                self.isProcessing = false
//                
//                switch result {
//                case .success(let validationResult):
//                    if validationResult.valid {
//                        // Show penalty if any
//                        if let penalty = validationResult.penalty, penalty > 0 {
//                            self.showSuccess("Check-out with Penalty", message: "Late checkout penalty: ₹\(String(format: "%.2f", penalty))")
//                        } else {
//                            self.performActualCheckOut(userId: userId, qrCode: qrCode)
//                        }
//                    } else {
//                        self.showError(validationResult.message ?? "QR validation failed")
//                    }
//                case .failure(let error):
//                    self.showError("Check-out failed: \(error.localizedDescription)")
//                }
//            }
//        }
//    }
//    
//    private func performActualCheckOut(userId: String, qrCode: String) {
//        APIService.shared.checkOut(userId: userId, bookingId: booking.id, qrCode: qrCode) { result in
//            DispatchQueue.main.async {
//                switch result {
//                case .success:
//                    self.showSuccess("Check-out successful!", message: "Thank you for using our parking service.")
//                    self.homeViewModel.fetchAllData()
//                case .failure(let error):
//                    self.showError("Check-out failed: \(error.localizedDescription)")
//                }
//            }
//        }
//    }
//    
//    // MARK: - Helper Functions
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
//    private func getQRTitle() -> String {
//        if booking.qrCodeScanned == true {
//            return "Checkout QR Code"
//        }
//        return "Check-in QR Code"
//    }
//    
//    private func getQRSubtitle() -> String {
//        if booking.qrCodeScanned == true {
//            return "Present this QR code for checkout"
//        }
//        return "Present this QR code for check-in"
//    }
//    
//    private func showError(_ message: String) {
//        alertTitle = "Error"
//        alertMessage = message
//        showAlert = true
//    }
//    
//    private func showSuccess(_ title: String, message: String) {
//        alertTitle = title
//        alertMessage = message
//        showAlert = true
//    }
//    
//    private func generateQRCode() {
//        isGenerating = true
//        qrCodeImage = nil
//        
//        DispatchQueue.global(qos: .userInitiated).async {
//            let qrData = booking.qrCode ?? booking.id
//            let qrImage = QRCodeGenerator.shared.generateQRCode(from: qrData)
//            
//            DispatchQueue.main.async {
//                self.qrCodeImage = qrImage
//                self.isGenerating = false
//            }
//        }
//    }
//    
//    private func getStatusColor(_ status: String) -> Color {
//        switch status.uppercased() {
//        case "ACTIVE": return .green
//        case "COMPLETED": return .blue
//        case "PENDING": return .orange
//        case "CANCELLED": return .red
//        default: return .gray
//        }
//    }
//    
//    private func formatTime(_ timeString: String?) -> String {
//        guard let timeString = timeString, !timeString.isEmpty else {
//            return "Not set"
//        }
//        
//        let isoFormatter = ISO8601DateFormatter()
//        isoFormatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
//        
//        if let date = isoFormatter.date(from: timeString) {
//            let displayFormatter = DateFormatter()
//            displayFormatter.dateFormat = "MMM dd, yyyy 'at' h:mm a"
//            return displayFormatter.string(from: date)
//        }
//        
//        return timeString
//    }
//    
//    private func formatAmount(_ amount: Double?) -> String {
//        guard let amount = amount else { return "₹0.00" }
//        return "₹\(String(format: "%.2f", amount))"
//    }
//}
//
//struct BookingDetailRow: View {
//    let title: String
//    let value: String
//    let valueColor: Color
//    
//    init(title: String, value: String, valueColor: Color = .primary) {
//        self.title = title
//        self.value = value
//        self.valueColor = valueColor
//    }
//    
//    var body: some View {
//        HStack {
//            Text(title)
//                .font(.body)
//                .foregroundColor(.secondary)
//            
//            Spacer()
//            
//            Text(value)
//                .font(.body)
//                .fontWeight(.medium)
//                .foregroundColor(valueColor)
//        }
//    }
//}


import SwiftUI
import CoreImage.CIFilterBuiltins

struct QRCodeDisplayView: View {
    let booking: Bookings
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject var homeViewModel: HomeViewModel
    
    @State private var isProcessing = false
    @State private var isCheckingOut = false
    @State private var showAlert = false
    @State private var alertTitle = ""
    @State private var alertMessage = ""
    
    private let context = CIContext()
    private let filter = CIFilter.qrCodeGenerator()
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 24) {
                    statusBadge
                    qrCodeSection
                    
                    // ✅ Check-in button for PENDING
                    if booking.status.uppercased() == "PENDING" {
                        testConfirmButton
                    }
                    
                    // ✅ Checkout button for ACTIVE
                    if booking.status.uppercased() == "ACTIVE" {
                        testCheckoutButton
                    }
                    
                    bookingInfoSection
                    instructionsSection
                    
                    Spacer(minLength: 40)
                }
                .padding()
            }
            .background(Color(.systemGroupedBackground))
            .navigationTitle("QR Code")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") {
                        dismiss()
                    }
                    .fontWeight(.semibold)
                }
            }
            .alert(alertTitle, isPresented: $showAlert) {
                Button("OK") {
                    if alertTitle.contains("Success") || alertTitle.contains("Confirmed") {
                        dismiss()
                    }
                }
            } message: {
                Text(alertMessage)
            }
        }
    }
    
    // ✅ Status Badge
    private var statusBadge: some View {
        HStack {
            Circle()
                .fill(statusColor)
                .frame(width: 12, height: 12)
            
            Text(booking.status.uppercased())
                .font(.headline)
                .fontWeight(.semibold)
                .foregroundColor(statusColor)
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 10)
        .background(statusColor.opacity(0.15))
        .clipShape(Capsule())
    }
    
    // ✅ QR Code Section
    private var qrCodeSection: some View {
        VStack(spacing: 16) {
            if let qrImage = generateQRCode(from: booking.id) {
                Image(uiImage: qrImage)
                    .interpolation(.none)
                    .resizable()
                    .scaledToFit()
                    .frame(width: 250, height: 250)
                    .padding()
                    .background(Color.white)
                    .cornerRadius(20)
                    .shadow(color: .black.opacity(0.1), radius: 10, x: 0, y: 4)
            } else {
                RoundedRectangle(cornerRadius: 20)
                    .fill(Color.gray.opacity(0.2))
                    .frame(width: 250, height: 250)
                    .overlay(
                        Text("QR Code Error")
                            .foregroundColor(.gray)
                    )
            }
            
            Text("Booking ID: \(String(booking.id.suffix(12)))")
                .font(.caption)
                .foregroundColor(.secondary)
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                .background(Color(.systemGray6))
                .cornerRadius(8)
        }
    }
    
    // ✅ Test Check-In Button (PENDING)
    private var testConfirmButton: some View {
        Button(action: testConfirmBooking) {
            HStack(spacing: 12) {
                if isProcessing {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                } else {
                    Image(systemName: "checkmark.shield.fill")
                        .font(.system(size: 20))
                    Text("Test: Confirm Check-in")
                        .font(.headline)
                }
            }
            .frame(maxWidth: .infinity)
            .padding()
            .background(
                LinearGradient(
                    colors: [Color.purple, Color.purple.opacity(0.8)],
                    startPoint: .leading,
                    endPoint: .trailing
                )
            )
            .foregroundColor(.white)
            .cornerRadius(12)
            .shadow(color: .purple.opacity(0.3), radius: 8, x: 0, y: 4)
        }
        .disabled(isProcessing || isCheckingOut)
        .padding(.horizontal)
    }
    
    // ✅ Test Checkout Button (ACTIVE)
    private var testCheckoutButton: some View {
        Button(action: testCheckout) {
            HStack(spacing: 12) {
                if isCheckingOut {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                } else {
                    Image(systemName: "arrow.right.square.fill")
                        .font(.system(size: 20))
                    Text("Test: Confirm Check-out")
                        .font(.headline)
                }
            }
            .frame(maxWidth: .infinity)
            .padding()
            .background(
                LinearGradient(
                    colors: [Color.blue, Color.blue.opacity(0.8)],
                    startPoint: .leading,
                    endPoint: .trailing
                )
            )
            .foregroundColor(.white)
            .cornerRadius(12)
            .shadow(color: .blue.opacity(0.3), radius: 8, x: 0, y: 4)
        }
        .disabled(isCheckingOut || isProcessing)
        .padding(.horizontal)
    }
    
    // ✅ Booking Info Section
    private var bookingInfoSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Booking Information")
                .font(.headline)
                .fontWeight(.semibold)
            
            VStack(spacing: 12) {
                InfoRow(icon: "mappin.circle.fill", title: "Location", value: getZoneName(booking.spotId), color: .blue)
                InfoRow(icon: "car.fill", title: "Vehicle", value: booking.vehicleNumber ?? "N/A", color: .green)
                
                if let checkIn = booking.checkInTime {
                    InfoRow(icon: "clock.arrow.circlepath", title: "Check-in", value: formatTime(checkIn), color: .orange)
                }
                
                if let checkOut = booking.checkOutTime {
                    InfoRow(icon: "clock.badge.checkmark", title: "Check-out", value: formatTime(checkOut), color: .purple)
                }
            }
            .padding()
            .background(Color(.systemBackground))
            .cornerRadius(12)
        }
    }
    
    // ✅ Instructions Section
    private var instructionsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(instructionsTitle)
                .font(.headline)
                .fontWeight(.semibold)
            
            Text(instructionsText)
                .font(.subheadline)
                .foregroundColor(.secondary)
                .lineSpacing(4)
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
    }
    
    // ✅ Test Check-In Function
    private func testConfirmBooking() {
        guard let userId = getCurrentUserId() else {
            showError("User not logged in")
            return
        }
        
        isProcessing = true
        
        let bookingId = booking.mongoId
        let qrCodeValue = bookingId
        
        print("🧪 TEST: Validating QR check-in")
        print("   User ID: \(userId)")
        print("   MongoDB Booking ID: \(bookingId)")
        print("   QR Code: \(qrCodeValue)")
        
        APIService.shared.validateQRCheckIn(userId: userId, bookingId: bookingId, qrCode: qrCodeValue) { result in
            DispatchQueue.main.async {
                self.isProcessing = false
                
                switch result {
                case .success(let checkedInBooking):
                    print("✅ Check-in successful!")
                    print("📋 Booking Details:")
                    print("   ID: \(checkedInBooking.id)")
                    print("   Status: \(checkedInBooking.status)")
                    print("   Check-in Time: \(checkedInBooking.checkInTime ?? "nil")")
                    print("   Check-out Time: \(checkedInBooking.checkOutTime ?? "nil")")
                    print("   Total Hours: \(checkedInBooking.totalHours)")
                    print("   Total Amount: ₹\(checkedInBooking.totalAmount)")
                    
                    self.showSuccess("Check-in Confirmed", message: "Booking is now \(checkedInBooking.status.uppercased())")
                    
                    DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
                        self.homeViewModel.fetchAllData()
                        self.dismiss()
                    }
                    
                case .failure(let error):
                    print("❌ Check-in failed: \(error)")
                    self.showError("Check-in failed: \(error.localizedDescription)")
                }
            }
        }
    }
    
    // ✅ Test Checkout Function
    private func testCheckout() {
        guard let userId = getCurrentUserId() else {
            showError("User not logged in")
            return
        }
        
        isCheckingOut = true
        
        let bookingId = booking.mongoId
        let qrCodeValue = bookingId
        
        print("🧪 TEST: Validating QR checkout")
        print("   User ID: \(userId)")
        print("   MongoDB Booking ID: \(bookingId)")
        print("   QR Code: \(qrCodeValue)")
        
        APIService.shared.validateQRCheckOut(userId: userId, bookingId: bookingId, qrCode: qrCodeValue) { result in
            DispatchQueue.main.async {
                self.isCheckingOut = false
                
                switch result {
                case .success(let checkedOutBooking):
                    print("✅ Checkout successful!")
                    print("📋 Booking Details:")
                    print("   ID: \(checkedOutBooking.id)")
                    print("   Status: \(checkedOutBooking.status)")
                    print("   Check-out Time: \(checkedOutBooking.checkOutTime ?? "nil")")
                    print("   Total Hours: \(checkedOutBooking.totalHours)")
                    print("   Total Amount: ₹\(checkedOutBooking.totalAmount)")
                    
                    self.showSuccess("Checkout Confirmed", message: "Booking is now \(checkedOutBooking.status.uppercased()). Total: ₹\(String(format: "%.2f", checkedOutBooking.totalAmount))")
                    
                    DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
                        self.homeViewModel.fetchAllData()
                        self.dismiss()
                    }
                    
                case .failure(let error):
                    print("❌ Checkout failed: \(error)")
                    self.showError("Checkout failed: \(error.localizedDescription)")
                }
            }
        }
    }
    
    // ✅ Helper Functions
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
    
    private func showSuccess(_ title: String, message: String) {
        alertTitle = title
        alertMessage = message
        showAlert = true
    }
    
    private func showError(_ message: String) {
        alertTitle = "Error"
        alertMessage = message
        showAlert = true
    }
    
    private func generateQRCode(from string: String) -> UIImage? {
        filter.message = Data(string.utf8)
        
        if let outputImage = filter.outputImage {
            if let cgImage = context.createCGImage(outputImage, from: outputImage.extent) {
                return UIImage(cgImage: cgImage)
            }
        }
        
        return nil
    }
    
    private func getZoneName(_ spotId: String) -> String {
        switch spotId {
        case "ps1": return "TP Avenue Parking"
        case "ps2": return "Medical College"
        case "ps3": return "City Center Parking"
        default: return "Parking Zone \(spotId.uppercased())"
        }
    }
    
    private func formatTime(_ dateString: String) -> String {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        
        if let date = formatter.date(from: dateString) {
            let displayFormatter = DateFormatter()
            displayFormatter.dateFormat = "MMM dd, h:mm a"
            return displayFormatter.string(from: date)
        }
        
        return dateString
    }
    
    private var statusColor: Color {
        switch booking.status.uppercased() {
        case "ACTIVE": return .green
        case "PENDING": return .orange
        case "COMPLETED": return .blue
        default: return .gray
        }
    }
    
    private var instructionsTitle: String {
        switch booking.status.uppercased() {
        case "PENDING":
            return "Check-in Instructions"
        case "ACTIVE":
            return "Check-out Instructions"
        default:
            return "Instructions"
        }
    }
    
    private var instructionsText: String {
        switch booking.status.uppercased() {
        case "PENDING":
            return "Show this QR code at the parking entrance. The staff will scan it to confirm your check-in and activate your booking."
        case "ACTIVE":
            return "Show this QR code when leaving the parking. The staff will scan it to process your checkout and calculate the final amount."
        default:
            return "This booking is no longer active."
        }
    }
}

// ✅ Info Row Component
struct InfoRow: View {
    let icon: String
    let title: String
    let value: String
    let color: Color
    
    var body: some View {
        HStack(spacing: 12) {
            ZStack {
                Circle()
                    .fill(color.opacity(0.15))
                    .frame(width: 36, height: 36)
                
                Image(systemName: icon)
                    .font(.system(size: 16))
                    .foregroundColor(color)
            }
            
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.caption)
                    .foregroundColor(.secondary)
                
                Text(value)
                    .font(.body)
                    .fontWeight(.medium)
            }
            
            Spacer()
        }
    }
}
