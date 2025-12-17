import AVFoundation
import SwiftUI

class QRScannerViewModel: NSObject, ObservableObject {
    @Published var scannedCode: String?
    @Published var isScanning = false
    @Published var permissionGranted = false
    @Published var scanAction: ScanAction = .checkIn
    @Published var scanMessage: String = ""
    @Published var isProcessing = false
    
    // ✅ ADD: Track what action will be performed
    enum ScanAction {
        case checkIn
        case checkOut
        case error
    }
    
    private var captureSession: AVCaptureSession?
    private let sessionQueue = DispatchQueue(label: "qr.scanner.session")
    
    override init() {
        super.init()
        checkCameraPermission()
    }
    
    func checkCameraPermission() {
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            permissionGranted = true
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { [weak self] granted in
                DispatchQueue.main.async {
                    self?.permissionGranted = granted
                    if granted {
                        self?.setupCaptureSession()
                    }
                }
            }
        case .denied, .restricted:
            permissionGranted = false
        @unknown default:
            permissionGranted = false
        }
        
        if permissionGranted {
            setupCaptureSession()
        }
    }
    
    private func setupCaptureSession() {
        sessionQueue.async { [weak self] in
            guard let self = self else { return }
            
            let session = AVCaptureSession()
            
            guard let videoCaptureDevice = AVCaptureDevice.default(for: .video) else {
                print("❌ No video capture device available")
                return
            }
            
            do {
                let videoInput = try AVCaptureDeviceInput(device: videoCaptureDevice)
                
                if session.canAddInput(videoInput) {
                    session.addInput(videoInput)
                }
                
                let metadataOutput = AVCaptureMetadataOutput()
                
                if session.canAddOutput(metadataOutput) {
                    session.addOutput(metadataOutput)
                    metadataOutput.setMetadataObjectsDelegate(self, queue: DispatchQueue.main)
                    metadataOutput.metadataObjectTypes = [.qr]
                }
                
                self.captureSession = session
            } catch {
                print("❌ Error setting up capture session: \(error)")
            }
        }
    }
    
    func startScanning() {
        guard permissionGranted else {
            print("⚠️ Camera permission not granted")
            return
        }
        
        sessionQueue.async { [weak self] in
            self?.captureSession?.startRunning()
            DispatchQueue.main.async {
                self?.isScanning = true
                print("📷 Scanner started")
            }
        }
    }
    
    func stopScanning() {
        sessionQueue.async { [weak self] in
            self?.captureSession?.stopRunning()
            DispatchQueue.main.async {
                self?.isScanning = false
                print("📷 Scanner stopped")
            }
        }
    }
    
    func resetScanning() {
        scannedCode = nil
        scanMessage = ""
        isProcessing = false
        startScanning()
    }
    
    func getCaptureSession() -> AVCaptureSession? {
        return captureSession
    }
    
    // ✅ NEW: Determine action based on booking status
    func determineActionForBooking(bookingId: String, completion: @escaping (ScanAction, Bookings?) -> Void) {
        print("🔍 Fetching booking status for ID: \(bookingId)")
        isProcessing = true
        
        guard let url = URL(string: "\(APIService.backendBaseURL)/bookings/\(bookingId)") else {
            print("❌ Invalid booking URL")
            completion(.error, nil)
            isProcessing = false
            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        
        // Add authentication
        let loginString = "rajeev:parking"
        if let loginData = loginString.data(using: .utf8) {
            let base64LoginString = loginData.base64EncodedString()
            request.setValue("Basic \(base64LoginString)", forHTTPHeaderField: "Authorization")
        }
        
        APIService.shared.session.dataTask(with: request) { [weak self] data, response, error in
            DispatchQueue.main.async {
                self?.isProcessing = false
                
                if let error = error {
                    print("❌ Network error fetching booking: \(error.localizedDescription)")
                    self?.scanMessage = "Network error"
                    completion(.error, nil)
                    return
                }
                
                guard let httpResp = response as? HTTPURLResponse,
                      (200...299).contains(httpResp.statusCode) else {
                    print("❌ Failed to fetch booking - invalid response")
                    self?.scanMessage = "Booking not found"
                    completion(.error, nil)
                    return
                }
                
                guard let data = data else {
                    print("❌ No booking data received")
                    self?.scanMessage = "No data received"
                    completion(.error, nil)
                    return
                }
                
                do {
                    let booking = try JSONDecoder().decode(Bookings.self, from: data)
                    print("✅ Booking fetched successfully")
                    print("   Booking ID: \(booking.id)")
                    print("   Status: \(booking.status)")
                    print("   QR Scanned: \(booking.qrCodeScanned ?? false)")
                    
                    // ✅ CRITICAL: Determine action based on qrCodeScanned flag
                    if booking.qrCodeScanned == true {
                        // Already checked in - perform checkout
                        self?.scanAction = .checkOut
                        self?.scanMessage = "Ready for CHECK-OUT"
                        print("🚗 Action: CHECK-OUT")
                        completion(.checkOut, booking)
                    } else {
                        // Not checked in yet - perform check-in
                        self?.scanAction = .checkIn
                        self?.scanMessage = "Ready for CHECK-IN"
                        print("🅿️ Action: CHECK-IN")
                        completion(.checkIn, booking)
                    }
                    
                } catch {
                    print("❌ Failed to decode booking: \(error)")
                    self?.scanMessage = "Invalid booking data"
                    completion(.error, nil)
                }
            }
        }.resume()
    }
    
    // ✅ NEW: Perform check-in
    func performCheckIn(bookingId: String, completion: @escaping (Bool, String?) -> Void) {
        print("🅿️ Performing CHECK-IN for booking: \(bookingId)")
        
        guard let url = URL(string: "\(APIService.backendBaseURL)/bookings/\(bookingId)/checkin") else {
            completion(false, "Invalid URL")
            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        
        let loginString = "rajeev:parking"
        if let loginData = loginString.data(using: .utf8) {
            let base64LoginString = loginData.base64EncodedString()
            request.setValue("Basic \(base64LoginString)", forHTTPHeaderField: "Authorization")
        }
        
        APIService.shared.session.dataTask(with: request) { data, response, error in
            DispatchQueue.main.async {
                if let error = error {
                    print("❌ Check-in error: \(error.localizedDescription)")
                    completion(false, error.localizedDescription)
                    return
                }
                
                guard let httpResp = response as? HTTPURLResponse,
                      (200...299).contains(httpResp.statusCode) else {
                    completion(false, "Check-in failed")
                    return
                }
                
                print("✅ CHECK-IN successful")
                completion(true, "Vehicle checked in successfully")
            }
        }.resume()
    }
    
    // ✅ NEW: Perform check-out
    func performCheckOut(bookingId: String, completion: @escaping (Bool, String?) -> Void) {
        print("🚗 Performing CHECK-OUT for booking: \(bookingId)")
        
        guard let url = URL(string: "\(APIService.backendBaseURL)/bookings/\(bookingId)/checkout") else {
            completion(false, "Invalid URL")
            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        
        let loginString = "rajeev:parking"
        if let loginData = loginString.data(using: .utf8) {
            let base64LoginString = loginData.base64EncodedString()
            request.setValue("Basic \(base64LoginString)", forHTTPHeaderField: "Authorization")
        }
        
        APIService.shared.session.dataTask(with: request) { data, response, error in
            DispatchQueue.main.async {
                if let error = error {
                    print("❌ Check-out error: \(error.localizedDescription)")
                    completion(false, error.localizedDescription)
                    return
                }
                
                guard let httpResp = response as? HTTPURLResponse,
                      (200...299).contains(httpResp.statusCode) else {
                    completion(false, "Check-out failed")
                    return
                }
                
                print("✅ CHECK-OUT successful")
                completion(true, "Vehicle checked out successfully")
            }
        }.resume()
    }
}

extension QRScannerViewModel: AVCaptureMetadataOutputObjectsDelegate {
    func metadataOutput(_ output: AVCaptureMetadataOutput, didOutput metadataObjects: [AVMetadataObject], from connection: AVCaptureConnection) {
        
        if let metadataObject = metadataObjects.first {
            guard let readableObject = metadataObject as? AVMetadataMachineReadableCodeObject else { return }
            guard let stringValue = readableObject.stringValue else { return }
            
            // Only process if not already scanned and not currently processing
            if scannedCode == nil && !isProcessing {
                AudioServicesPlaySystemSound(SystemSoundID(kSystemSoundID_Vibrate))
                scannedCode = stringValue
                print("✅ QR Code detected: \(stringValue)")
                
                // ✅ Automatically determine action
                determineActionForBooking(bookingId: stringValue) { action, booking in
                    // Action determined - UI can now show appropriate buttons
                    print("📊 Action determined: \(action)")
                }
            }
        }
    }
}
