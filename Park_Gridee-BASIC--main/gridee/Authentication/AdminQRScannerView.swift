import SwiftUI
import CodeScanner

struct AdminQRScannerView: View {
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject var homeViewModel: HomeViewModel
    @EnvironmentObject var authViewModel: AuthViewModel
    
    @State private var isProcessing = false
    @State private var lastScannedCode = ""
    @State private var lastScanTime: Date?
    @State private var showAlert = false
    @State private var alertTitle = ""
    @State private var alertMessage = ""
    @State private var scanType: ScanType = .checkIn
    @State private var shouldResetScanner = UUID()
    @State private var showLogoutConfirmation = false
    
    enum ScanType {
        case checkIn
        case checkOut
    }
    var body: some View {
        NavigationView {
            ZStack {
                CodeScannerView(
                    codeTypes: [.qr],
                    simulatedData: generateSimulatedData(),
                    completion: handleScan
                )
                .id(shouldResetScanner)
                
                VStack {
                    Spacer()
                    
                    Picker("Scan Type", selection: $scanType) {
                        Text("Check-In").tag(ScanType.checkIn)
                        Text("Check-Out").tag(ScanType.checkOut)
                    }
                    .pickerStyle(SegmentedPickerStyle())
                    .padding()
                    .background(Color.black.opacity(0.7))
                    .cornerRadius(12)
                    .padding()
                    .disabled(isProcessing)
                }
            }
            .navigationTitle("Admin QR Scanner")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") { dismiss() }
                        .foregroundColor(.white)
                }
                
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: { showLogoutConfirmation = true }) {
                        HStack(spacing: 4) {
                            Image(systemName: "rectangle.portrait.and.arrow.right")
                            Text("Logout")
                        }
                        .foregroundColor(.red)
                        .fontWeight(.semibold)
                    }
                    .disabled(isProcessing)
                }
            }
            .alert(alertTitle, isPresented: $showAlert) {
                Button("OK") { resetScanner() }
            } message: { Text(alertMessage) }
            .alert("Logout", isPresented: $showLogoutConfirmation) {
                Button("Cancel", role: .cancel) { }
                Button("Logout", role: .destructive) { performLogout() }
            } message: { Text("Are you sure you want to logout?") }
        }
    }
}

extension AdminQRScannerView {
    
    // MARK: - Scan Handler
    private func handleScan(result: Result<ScanResult, ScanError>) {
        guard !isProcessing else { return }
        
        switch result {
        case .success(let scanResult):
            let scannedString = scanResult.string
            let now = Date()
            
            // Prevent rapid duplicate scans
            if let lastScan = lastScanTime,
               lastScannedCode == scannedString,
               now.timeIntervalSince(lastScan) < 3.0 { return }
            
            lastScannedCode = scannedString
            lastScanTime = now
            
            guard let data = scannedString.data(using: .utf8),
                  let json = try? JSONSerialization.jsonObject(with: data) as? [String: String],
                  let bookingId = json["bookingId"],
                  json["type"] == "gridee_parking" else {
                showError("Invalid QR Code", message: "QR code format is incorrect.")
                return
            }
            
            if scanType == .checkIn {
                processCheckIn(bookingId: bookingId)
            } else {
                processCheckOut(bookingId: bookingId)
            }
            
        case .failure(let error):
            showError("Scan Error", message: error.localizedDescription)
        }
    }
    
    // MARK: - Process Check-In
    private func processCheckIn(bookingId: String) {
        isProcessing = true
        print("🔵 Starting check-in for \(bookingId)")
        
        fetchBookingData(bookingId: bookingId) { result in
            switch result {
            case .success(let booking):
                guard booking.status.uppercased() == "PENDING" else {
                    DispatchQueue.main.async {
                        self.isProcessing = false
                        self.showError("Invalid Status",
                                       message: "Booking is \(booking.status.uppercased()). Only PENDING bookings can check-in.")
                        self.resetScanner()
                    }
                    return
                }
                
                validateCheckIn(userId: booking.userId, bookingId: bookingId) { validationResult in
                    DispatchQueue.main.async {
                        self.isProcessing = false
                        switch validationResult {
                        case .success(let updatedBooking):
                            self.showSuccess(
                                "Check-In Successful",
                                message: "Vehicle: \(updatedBooking.vehicleNumber ?? "N/A")\nStatus: \(updatedBooking.status.uppercased())"
                            )
                            self.homeViewModel.fetchAllData()
                        case .failure(let error):
                            self.showError("Check-In Failed", message: error.localizedDescription)
                        }
                    }
                }
                
            case .failure:
                DispatchQueue.main.async {
                    self.isProcessing = false
                    self.showError("Error", message: "Failed to fetch booking details.")
                }
            }
        }
    }
    
    // MARK: - Process Check-Out
    private func processCheckOut(bookingId: String) {
        isProcessing = true
        print("🟢 Starting check-out for \(bookingId)")
        
        fetchBookingData(bookingId: bookingId) { result in
            switch result {
            case .success(let booking):
                guard booking.status.uppercased() == "ACTIVE" else {
                    DispatchQueue.main.async {
                        self.isProcessing = false
                        self.showError("Invalid Status",
                                       message: "Booking is \(booking.status.uppercased()). Only ACTIVE bookings can check-out.")
                        self.resetScanner()
                    }
                    return
                }
                
                validateCheckOut(userId: booking.userId, bookingId: bookingId) { validationResult in
                    DispatchQueue.main.async {
                        self.isProcessing = false
                        switch validationResult {
                        case .success(let updatedBooking):
                            let amount = String(format: "%.2f", updatedBooking.totalAmount)
                            let duration = String(format: "%.1f", updatedBooking.totalHours)
                            self.showSuccess(
                                "Check-Out Successful",
                                message: "Vehicle: \(updatedBooking.vehicleNumber ?? "N/A")\nDuration: \(duration)h\nAmount: ₹\(amount)"
                            )
                            self.homeViewModel.fetchAllData()
                        case .failure(let error):
                            self.showError("Check-Out Failed", message: error.localizedDescription)
                        }
                    }
                }
                
            case .failure:
                DispatchQueue.main.async {
                    self.isProcessing = false
                    self.showError("Error", message: "Failed to fetch booking details.")
                }
            }
        }
    }
    
    // MARK: - Fetch Booking Details
    private func fetchBookingData(bookingId: String, completion: @escaping (Result<Bookings, Error>) -> Void) {
        let endpoint = "\(APIService.backendBaseURL)/bookings/\(bookingId)"
        guard let url = URL(string: endpoint) else {
            completion(.failure(APIError.invalidURL))
            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        APIService.shared.addAuthHeader(to: &request)
        
        APIService.shared.session.dataTask(with: request) { data, response, error in
            if let error = error {
                completion(.failure(error))
                return
            }
            guard let data = data,
                  let httpResponse = response as? HTTPURLResponse,
                  (200...299).contains(httpResponse.statusCode) else {
                completion(.failure(APIError.badResponse))
                return
            }
            do {
                let booking = try JSONDecoder().decode(Bookings.self, from: data)
                completion(.success(booking))
            } catch {
                completion(.failure(error))
            }
        }.resume()
    }
    
    // MARK: - Validate Check-In API
    private func validateCheckIn(userId: String, bookingId: String, completion: @escaping (Result<Bookings, Error>) -> Void) {
        let endpoint = "\(APIService.backendBaseURL)/users/\(userId)/bookings/\(bookingId)/validate-checkin"
        callValidationAPI(endpoint: endpoint, completion: completion)
    }
    
    // MARK: - Validate Check-Out API
    private func validateCheckOut(userId: String, bookingId: String, completion: @escaping (Result<Bookings, Error>) -> Void) {
        let endpoint = "\(APIService.backendBaseURL)/users/\(userId)/bookings/\(bookingId)/validate-checkout"
        callValidationAPI(endpoint: endpoint, completion: completion)
    }
    
    // MARK: - Common Validation Handler
    private func callValidationAPI(endpoint: String, completion: @escaping (Result<Bookings, Error>) -> Void) {
        guard let url = URL(string: endpoint) else {
            completion(.failure(APIError.invalidURL))
            return
        }
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        APIService.shared.addAuthHeader(to: &request)
        
        print("🔗 Calling: \(endpoint)")
        
        APIService.shared.session.dataTask(with: request) { data, response, error in
            if let error = error {
                completion(.failure(error))
                return
            }
            guard let data = data,
                  let httpResponse = response as? HTTPURLResponse,
                  (200...299).contains(httpResponse.statusCode) else {
                completion(.failure(APIError.badResponse))
                return
            }
            do {
                let updatedBooking = try JSONDecoder().decode(Bookings.self, from: data)
                completion(.success(updatedBooking))
            } catch {
                completion(.failure(error))
            }
        }.resume()
    }
    
    // MARK: - Logout & Helpers
    private func performLogout() {
        UserDefaults.standard.removeObject(forKey: "currentUserId")
        UserDefaults.standard.removeObject(forKey: "userData")
        UserDefaults.standard.removeObject(forKey: "jwtToken")
        UserDefaults.standard.removeObject(forKey: "userRole")
        authViewModel.logout()
        dismiss()
    }
    
    private func resetScanner() {
        lastScannedCode = ""
        lastScanTime = nil
        isProcessing = false
        shouldResetScanner = UUID()
    }
    
    private func showSuccess(_ title: String, message: String) {
        alertTitle = title
        alertMessage = message
        showAlert = true
    }
    
    private func showError(_ title: String, message: String) {
        alertTitle = title
        alertMessage = message
        showAlert = true
    }
    
    private func generateSimulatedData() -> String {
        let mockData: [String: String] = [
            "bookingId": "68ef5eeeb5ab5170bb003835",
            "type": "gridee_parking",
            "version": "1.0"
        ]
        if let jsonData = try? JSONSerialization.data(withJSONObject: mockData),
           let jsonString = String(data: jsonData, encoding: .utf8) {
            return jsonString
        }
        return "{}"
    }
}
