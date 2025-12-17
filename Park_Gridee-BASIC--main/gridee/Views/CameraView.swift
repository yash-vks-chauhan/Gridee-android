import SwiftUI
import AVFoundation
import Vision

struct CameraView: View {
    @StateObject private var cameraModel = CameraViewModel()
    @State private var operationMode: OperationMode = .checkIn
    
    enum OperationMode: String {
        case checkIn = "Check-In"
        case checkOut = "Check-Out"
    }
    
    var body: some View {
        ZStack {
            // Camera preview
            CameraPreviewView(cameraModel: cameraModel)
                .ignoresSafeArea()
            
            // Overlay UI
            VStack(spacing: 0) {
                // Top status bar
                topStatusBar
                
                Spacer()
                
                // Detection frame guide
                detectionGuide
                
                Spacer()
                
                // Detection result
                if let plate = cameraModel.detectedPlate {
                    detectionResultCard
                }
                
                // Mode selector buttons at bottom
                modeSelector
            }
        }
        .onAppear {
            cameraModel.startSession()
        }
        .onDisappear {
            cameraModel.stopSession()
        }
        .onChange(of: operationMode) { newMode in
            cameraModel.operationMode = newMode
            print("🔄 Operation mode changed to: \(newMode.rawValue)")
        }
    }
    
    // MARK: - UI Components
    
    var topStatusBar: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text("License Plate Scanner")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(.white)
                
                HStack(spacing: 6) {
                    Circle()
                        .fill(cameraModel.canScan ? Color.green : Color.orange)
                        .frame(width: 8, height: 8)
                    Text(cameraModel.canScan ? "Ready to scan" : "Cooldown...")
                        .font(.system(size: 12))
                        .foregroundColor(.white)
                    
                    // Show current mode
                    Text("• \(operationMode.rawValue)")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundColor(operationMode == .checkIn ? .green : .blue)
                }
            }
            
            Spacer()
        }
        .padding()
        .background(Color.black.opacity(0.7))
    }
    
    var detectionGuide: some View {
        RoundedRectangle(cornerRadius: 12)
            .stroke(cameraModel.detectedPlate != nil ? Color.green : Color.white, lineWidth: 3)
            .frame(width: 300, height: 100)
            .overlay(
                VStack(spacing: 4) {
                    Text("Align license plate here")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(.white)
                    
                    Text(operationMode.rawValue)
                        .font(.system(size: 12))
                        .foregroundColor(operationMode == .checkIn ? .green : .blue)
                }
                .padding(8)
                .background(Color.black.opacity(0.6))
                .cornerRadius(6)
                .offset(y: -70)
            )
    }
    
    var detectionResultCard: some View {
        VStack(spacing: 12) {
            HStack(spacing: 10) {
                Image(systemName: cameraModel.operationSuccess ? "checkmark.circle.fill" : "car.fill")
                    .font(.system(size: 24))
                    .foregroundColor(cameraModel.operationSuccess ? .green : (operationMode == .checkIn ? .green : .blue))
                
                Text(cameraModel.detectedPlate ?? "")
                    .font(.system(size: 24, weight: .bold))
                    .foregroundColor(.white)
            }
            
            if cameraModel.isProcessing {
                HStack(spacing: 8) {
                    ProgressView()
                        .tint(.white)
                    Text("Processing \(operationMode.rawValue.lowercased())...")
                        .font(.system(size: 14))
                        .foregroundColor(.white)
                }
            }
            
            if let message = cameraModel.statusMessage {
                Text(message)
                    .font(.system(size: 14))
                    .foregroundColor(cameraModel.operationSuccess ? .green : .red)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal)
            }
        }
        .padding()
        .background(Color.black.opacity(0.7))
        .cornerRadius(12)
        .padding(.horizontal)
        .padding(.bottom, 20)
    }
    
    var modeSelector: some View {
        HStack(spacing: 16) {
            // Check-In Button
            Button(action: {
                operationMode = .checkIn
            }) {
                HStack(spacing: 8) {
                    Image(systemName: "arrow.down.circle.fill")
                        .font(.system(size: 20))
                    
                    Text("Check-In")
                        .font(.system(size: 16, weight: .semibold))
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 16)
                .background(operationMode == .checkIn ? Color.green : Color(UIColor(red: 0.2, green: 0.2, blue: 0.2, alpha: 1)))
                .foregroundColor(.white)
                .cornerRadius(12)
            }
            
            // Check-Out Button
            Button(action: {
                operationMode = .checkOut
            }) {
                HStack(spacing: 8) {
                    Image(systemName: "arrow.up.circle.fill")
                        .font(.system(size: 20))
                    
                    Text("Check-Out")
                        .font(.system(size: 16, weight: .semibold))
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 16)
                .background(operationMode == .checkOut ? Color.blue : Color(UIColor(red: 0.2, green: 0.2, blue: 0.2, alpha: 1)))
                .foregroundColor(.white)
                .cornerRadius(12)
            }
        }
        .padding(.horizontal, 16)
        .padding(.bottom, 40)
        .background(Color.black.opacity(0.5))
    }
}

// MARK: - Camera Preview
struct CameraPreviewView: UIViewControllerRepresentable {
    @ObservedObject var cameraModel: CameraViewModel
    
    func makeUIViewController(context: Context) -> UIViewController {
        let controller = UIViewController()
        cameraModel.setupCamera(in: controller, coordinator: context.coordinator)
        return controller
    }
    
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
    
    func makeCoordinator() -> CameraCoordinator {
        CameraCoordinator(cameraModel: cameraModel)
    }
}

// MARK: - ViewModel
class CameraViewModel: ObservableObject {
    @Published var detectedPlate: String?
    @Published var isScanning = false
    @Published var isProcessing = false
    @Published var statusMessage: String?
    @Published var operationSuccess = false
    @Published var canScan = true
    @Published var operationMode: CameraView.OperationMode = .checkIn
    
    private var captureSession: AVCaptureSession?
    private var lastAPICallTime: Date?
    private var lastDetectedPlate: String?
    
    private let apiCallCooldown: TimeInterval = 10.0
    private var samePlateCount = 0
    private let minConfidenceCount = 3
    
    func setupCamera(in controller: UIViewController, coordinator: CameraCoordinator) {
        let session = AVCaptureSession()
        session.sessionPreset = .hd1920x1080  // ✅ Higher resolution
        
        guard let captureDevice = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back),
              let input = try? AVCaptureDeviceInput(device: captureDevice) else {
            print("❌ Failed to setup camera")
            return
        }
        
        // ✅ Enhanced camera configuration
        do {
            try captureDevice.lockForConfiguration()
            if captureDevice.isFocusModeSupported(.continuousAutoFocus) {
                captureDevice.focusMode = .continuousAutoFocus
            }
            if captureDevice.isExposureModeSupported(.continuousAutoExposure) {
                captureDevice.exposureMode = .continuousAutoExposure
            }
            if captureDevice.isWhiteBalanceModeSupported(.continuousAutoWhiteBalance) {
                captureDevice.whiteBalanceMode = .continuousAutoWhiteBalance
            }
            captureDevice.unlockForConfiguration()
        } catch {
            print("❌ Failed to configure camera: \(error)")
        }
        
        session.addInput(input)
        
        let previewLayer = AVCaptureVideoPreviewLayer(session: session)
        previewLayer.frame = controller.view.bounds
        previewLayer.videoGravity = .resizeAspectFill
        controller.view.layer.addSublayer(previewLayer)
        
        let videoOutput = AVCaptureVideoDataOutput()
        videoOutput.setSampleBufferDelegate(coordinator, queue: DispatchQueue(label: "videoQueue"))
        videoOutput.alwaysDiscardsLateVideoFrames = true
        
        if session.canAddOutput(videoOutput) {
            session.addOutput(videoOutput)
        }
        
        self.captureSession = session
    }
    
    func startSession() {
        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
            self?.captureSession?.startRunning()
            DispatchQueue.main.async {
                self?.isScanning = true
                self?.canScan = true
            }
        }
    }
    
    func stopSession() {
        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
            self?.captureSession?.stopRunning()
            DispatchQueue.main.async {
                self?.isScanning = false
            }
        }
    }
    
    func handleDetectedPlate(_ plateNumber: String) {
        if let lastTime = lastAPICallTime {
            let timeSinceLastCall = Date().timeIntervalSince(lastTime)
            if timeSinceLastCall < apiCallCooldown {
                let remainingTime = Int(apiCallCooldown - timeSinceLastCall)
                print("⏳ Cooldown active. Wait \(remainingTime) more seconds")
                canScan = false
                return
            }
        }
        
        if lastDetectedPlate == plateNumber {
            samePlateCount += 1
            print("🔍 Same plate detected \(samePlateCount)/\(minConfidenceCount) times: \(plateNumber)")
            
            if samePlateCount < minConfidenceCount {
                return
            }
        } else {
            lastDetectedPlate = plateNumber
            samePlateCount = 1
            print("🆕 New plate detected: \(plateNumber)")
            return
        }
        
        lastAPICallTime = Date()
        canScan = false
        detectedPlate = plateNumber
        isProcessing = true
        statusMessage = nil
        samePlateCount = 0
        
        print("📡 Calling \(operationMode.rawValue) API for plate: \(plateNumber)")
        
        // Call appropriate API based on mode
        if operationMode == .checkIn {
            callCheckInAPI(plateNumber: plateNumber)
        } else {
            callCheckOutAPI(plateNumber: plateNumber)
        }
        
        DispatchQueue.main.asyncAfter(deadline: .now() + apiCallCooldown) { [weak self] in
            self?.canScan = true
            self?.lastDetectedPlate = nil
        }
    }
    
    private func callCheckInAPI(plateNumber: String) {
        APIService.shared.checkInOperator(vehicleNumber: plateNumber) { [weak self] result in
            DispatchQueue.main.async {
                self?.isProcessing = false
                
                switch result {
                case .success(let booking):
                    print("✅ Check-in successful: \(booking)")
                    self?.statusMessage = "✅ Check-in successful!"
                    self?.operationSuccess = true
                    
                    DispatchQueue.main.asyncAfter(deadline: .now() + 5) {
                        self?.resetState()
                    }
                    
                case .failure(let error):
                    print("❌ Check-in failed: \(error.localizedDescription)")
                    self?.statusMessage = "❌ Check-in failed: \(error.localizedDescription)"
                    self?.operationSuccess = false
                    
                    DispatchQueue.main.asyncAfter(deadline: .now() + 5) {
                        self?.resetState()
                    }
                }
            }
        }
    }
    
    private func callCheckOutAPI(plateNumber: String) {
        APIService.shared.checkOutOperator(vehicleNumber: plateNumber) { [weak self] result in
            DispatchQueue.main.async {
                self?.isProcessing = false
                
                switch result {
                case .success(let booking):
                    print("✅ Check-out successful: \(booking)")
                    let amount = booking.totalAmount ?? 0.0
                    self?.statusMessage = "✅ Check-out successful! Total: ₹\(amount)"
                    self?.operationSuccess = true
                    
                    DispatchQueue.main.asyncAfter(deadline: .now() + 5) {
                        self?.resetState()
                    }
                    
                case .failure(let error):
                    print("❌ Check-out failed: \(error.localizedDescription)")
                    self?.statusMessage = "❌ Check-out failed: \(error.localizedDescription)"
                    self?.operationSuccess = false
                    
                    DispatchQueue.main.asyncAfter(deadline: .now() + 5) {
                        self?.resetState()
                    }
                }
            }
        }
    }
    
    private func resetState() {
        detectedPlate = nil
        statusMessage = nil
        operationSuccess = false
    }
}

// MARK: - Coordinator with Enhanced OCR
// MARK: - Coordinator with Enhanced OCR
class CameraCoordinator: NSObject, AVCaptureVideoDataOutputSampleBufferDelegate {
    var cameraModel: CameraViewModel
    private var frameCounter = 0
    
    init(cameraModel: CameraViewModel) {
        self.cameraModel = cameraModel
    }
    
    func captureOutput(_ output: AVCaptureOutput, didOutput sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection) {
        frameCounter += 1
        guard frameCounter % 30 == 0 else { return }
        
        guard cameraModel.canScan && !cameraModel.isProcessing else { return }
        
        guard let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else { return }
        
        // ✅ ENHANCED: Image preprocessing
        let ciImage = CIImage(cvPixelBuffer: pixelBuffer)
        let enhancedImage = preprocessImage(ciImage)
        
        let request = VNRecognizeTextRequest { [weak self] request, error in
            guard let self = self else { return }
            
            if let error = error {
                print("❌ Vision error: \(error.localizedDescription)")
                return
            }
            
            guard let observations = request.results as? [VNRecognizedTextObservation] else {
                return
            }
            
            // ✅ ENHANCED: Collect ALL text candidates from the image
            var allTextCandidates: [String] = []
            
            for observation in observations {
                let topCandidates = observation.topCandidates(3)
                
                for candidate in topCandidates {
                    guard candidate.confidence > 0.3 else { continue }
                    allTextCandidates.append(candidate.string)
                }
            }
            
            print("📝 All detected text: \(allTextCandidates)")
            
            // ✅ NEW: Try to combine multi-line plates
            let combinedPlates = self.combineMultiLineTextIntoPossiblePlates(allTextCandidates)
            
            // ✅ ENHANCED: Process each candidate
            var plateCandidates: [(plate: String, confidence: Float)] = []
            
            for text in combinedPlates {
                let cleaned = self.cleanPlateString(text)
                
                if self.isValidLicensePlate(cleaned) {
                    // Give higher confidence to better matches
                    let confidence = self.calculatePlateConfidence(cleaned)
                    plateCandidates.append((cleaned, confidence))
                    print("🔍 Found candidate: \(cleaned) (confidence: \(confidence))")
                }
            }
            
            if let bestCandidate = plateCandidates.sorted(by: { $0.confidence > $1.confidence }).first {
                print("🏆 Best: \(bestCandidate.plate) (confidence: \(bestCandidate.confidence))")
                
                DispatchQueue.main.async {
                    self.cameraModel.handleDetectedPlate(bestCandidate.plate)
                }
            }
        }
        
        // ✅ ENHANCED: Vision settings
        request.recognitionLevel = .accurate
        request.usesLanguageCorrection = false
        request.minimumTextHeight = 0.025  // Even smaller for two-line plates
        request.revision = VNRecognizeTextRequestRevision3
        request.customWords = generateIndianLicensePlatePatterns()
        request.recognitionLanguages = ["en-US"]
        
        do {
            let requestHandler = VNImageRequestHandler(ciImage: enhancedImage, orientation: .up, options: [:])
            try requestHandler.perform([request])
        } catch {
            print("❌ Failed to perform request: \(error)")
        }
    }
    
    // MARK: - ✅ NEW: Combine Multi-Line Text
    private func combineMultiLineTextIntoPossiblePlates(_ texts: [String]) -> [String] {
        var result = texts  // Include original texts
        
        // Try combining consecutive texts (for two-line plates like TN19A / U9169)
        for i in 0..<texts.count {
            for j in (i+1)..<min(i+3, texts.count) {  // Look ahead up to 2 items
                let combined = texts[i] + texts[j]
                result.append(combined)
                print("🔗 Combined: '\(texts[i])' + '\(texts[j])' = '\(combined)'")
            }
        }
        
        return result
    }
    
    // MARK: - ✅ Image Preprocessing
    private func preprocessImage(_ image: CIImage) -> CIImage {
        var outputImage = image
        
        // 1. Increase contrast MORE aggressively for dirty plates
        if let contrastFilter = CIFilter(name: "CIColorControls") {
            contrastFilter.setValue(outputImage, forKey: kCIInputImageKey)
            contrastFilter.setValue(1.5, forKey: kCIInputContrastKey)  // Increased from 1.3
            contrastFilter.setValue(0.15, forKey: kCIInputBrightnessKey)  // Increased from 0.1
            if let result = contrastFilter.outputImage {
                outputImage = result
            }
        }
        
        // 2. Sharpen MORE
        if let sharpenFilter = CIFilter(name: "CISharpenLuminance") {
            sharpenFilter.setValue(outputImage, forKey: kCIInputImageKey)
            sharpenFilter.setValue(0.9, forKey: kCIInputSharpnessKey)  // Increased from 0.7
            if let result = sharpenFilter.outputImage {
                outputImage = result
            }
        }
        
        // 3. Noise reduction
        if let medianFilter = CIFilter(name: "CIMedianFilter") {
            medianFilter.setValue(outputImage, forKey: kCIInputImageKey)
            if let result = medianFilter.outputImage {
                outputImage = result
            }
        }
        
        return outputImage
    }
    
    // MARK: - ✅ ENHANCED Cleaning for IND/Special Characters
    private func cleanPlateString(_ input: String) -> String {
        var cleaned = input.uppercased()
        
        // ✅ NEW: Remove "IND" prefix/suffix (common on Indian plates)
        cleaned = cleaned.replacingOccurrences(of: "IND", with: "")
        cleaned = cleaned.replacingOccurrences(of: "INDIA", with: "")
        
        // Remove common OCR errors and noise
        cleaned = cleaned.replacingOccurrences(of: " ", with: "")
        cleaned = cleaned.replacingOccurrences(of: "-", with: "")
        cleaned = cleaned.replacingOccurrences(of: ".", with: "")
        cleaned = cleaned.replacingOccurrences(of: ",", with: "")
        cleaned = cleaned.replacingOccurrences(of: ":", with: "")
        cleaned = cleaned.replacingOccurrences(of: ";", with: "")
        cleaned = cleaned.replacingOccurrences(of: "_", with: "")
        cleaned = cleaned.replacingOccurrences(of: "|", with: "")
        cleaned = cleaned.replacingOccurrences(of: "/", with: "")
        cleaned = cleaned.replacingOccurrences(of: "\\", with: "")
        cleaned = cleaned.replacingOccurrences(of: "(", with: "")
        cleaned = cleaned.replacingOccurrences(of: ")", with: "")
        cleaned = cleaned.replacingOccurrences(of: "[", with: "")
        cleaned = cleaned.replacingOccurrences(of: "]", with: "")
        cleaned = cleaned.replacingOccurrences(of: "{", with: "")
        cleaned = cleaned.replacingOccurrences(of: "}", with: "")
        cleaned = cleaned.replacingOccurrences(of: "•", with: "")
        cleaned = cleaned.replacingOccurrences(of: "●", with: "")
        cleaned = cleaned.replacingOccurrences(of: "◆", with: "")
        cleaned = cleaned.replacingOccurrences(of: "★", with: "")
        
        // ✅ NEW: Fix common OCR misreads (more aggressive)
        cleaned = cleaned.replacingOccurrences(of: "O", with: "0")
        cleaned = cleaned.replacingOccurrences(of: "Q", with: "0")
        cleaned = cleaned.replacingOccurrences(of: "I", with: "1")
        cleaned = cleaned.replacingOccurrences(of: "L", with: "1")
        cleaned = cleaned.replacingOccurrences(of: "Z", with: "2")
        cleaned = cleaned.replacingOccurrences(of: "S", with: "5")
        cleaned = cleaned.replacingOccurrences(of: "G", with: "6")
        cleaned = cleaned.replacingOccurrences(of: "B", with: "8")
        
        return cleaned
    }
    
    // MARK: - ✅ Calculate Confidence Score
    private func calculatePlateConfidence(_ plate: String) -> Float {
        var confidence: Float = 0.5
        
        // Higher confidence if it matches strict Indian pattern
        let strictPattern = "^[A-Z]{2}[0-9]{2}[A-Z]{1,2}[0-9]{4}$"
        let regex = try? NSRegularExpression(pattern: strictPattern)
        let range = NSRange(location: 0, length: plate.utf16.count)
        
        if regex?.firstMatch(in: plate, range: range) != nil {
            confidence = 0.9
        }
        
        // Boost if it has known state codes
        let stateCodes = ["TN", "KA", "DL", "MH", "AP", "TS", "UP", "HR", "RJ", "GJ", "WB", "MP", "OR", "KL"]
        let prefix = String(plate.prefix(2))
        if stateCodes.contains(prefix) {
            confidence += 0.1
        }
        
        return min(confidence, 1.0)
    }
    
    // MARK: - ✅ Enhanced Validation
    private func isValidLicensePlate(_ plate: String) -> Bool {
        let length = plate.count
        
        // Indian plates: typically 9-10 characters, but allow 6-10 for flexibility
        guard length >= 6 && length <= 10 else {
            print("❌ Invalid length: \(length) for '\(plate)'")
            return false
        }
        
        // Must be alphanumeric only
        let isAlphanumeric = plate.rangeOfCharacter(from: CharacterSet.alphanumerics.inverted) == nil
        guard isAlphanumeric else {
            print("❌ Not alphanumeric: '\(plate)'")
            return false
        }
        
        // Must have at least one number
        let hasNumbers = plate.rangeOfCharacter(from: CharacterSet.decimalDigits) != nil
        guard hasNumbers else {
            print("❌ No numbers: '\(plate)'")
            return false
        }
        
        // Must have at least one letter
        let hasLetters = plate.rangeOfCharacter(from: CharacterSet.letters) != nil
        guard hasLetters else {
            print("❌ No letters: '\(plate)'")
            return false
        }
        
        // ✅ Indian plate pattern (flexible for variations)
        // Examples: TN22AB1234, TS07GH8647, TN19AU9169
        let flexiblePattern = "^[A-Z]{2}[0-9]{2}[A-Z]{0,3}[0-9]{3,4}$"
        let regex = try? NSRegularExpression(pattern: flexiblePattern)
        let range = NSRange(location: 0, length: plate.utf16.count)
        
        if regex?.firstMatch(in: plate, range: range) != nil {
            print("✅ Valid Indian plate: \(plate)")
            return true
        }
        
        print("⚠️ Flexible match: \(plate)")
        return true  // Still allow it
    }
    
    // MARK: - ✅ Indian Plate Patterns
    private func generateIndianLicensePlatePatterns() -> [String] {
        let stateCodes = [
            "TN", "KA", "DL", "MH", "AP", "TS", "UP", "HR", "RJ", "GJ",
            "WB", "MP", "OR", "KL", "PB", "CH", "JH", "UK", "HP", "JK",
            "GA", "AS", "BR", "CT", "MN", "ML", "MZ", "NL", "SK", "TR", "AR"
        ]
        
        var patterns: [String] = []
        for state in stateCodes {
            patterns.append(state)
            patterns.append("\(state)01")
            patterns.append("\(state)07")
            patterns.append("\(state)11")
            patterns.append("\(state)19")
            patterns.append("\(state)22")
            patterns.append("\(state)AB")
            patterns.append("\(state)GH")
            patterns.append("\(state)MH")
            patterns.append("\(state)U")
        }
        
        return patterns
    }
}
#Preview {
    CameraView()
}
