import SwiftUI

struct UPIQRDisplayView: View {
    let amount: Double
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject var walletViewModel: WalletViewModel
    @State private var isConfirming = false
    @State private var showingSuccess = false
    @State private var errorMessage = ""
    @State private var showingError = false
    @State private var successMessage = ""
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 24) {
                    // Header
                    VStack(spacing: 8) {
                        Image(systemName: "qrcode")
                            .font(.system(size: 40))
                            .foregroundColor(.blue)
                        
                        Text("Scan to Pay")
                            .font(.title2)
                            .fontWeight(.bold)
                        
                        Text("Use any UPI app to scan and pay")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    .padding(.top)
                    
                    // Amount Display
                    VStack(spacing: 8) {
                        Text("Amount")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                        
                        Text("₹\(String(format: "%.0f", amount))")
                            .font(.system(size: 28, weight: .bold))
                            .foregroundColor(.green)
                    }
                    .padding()
                    .background(Color(.systemGray6))
                    .cornerRadius(12)
                    
                    // Placeholder QR Code
                    VStack(spacing: 16) {
                        // Generate placeholder QR with amount
                        if let qrImage = generatePlaceholderQR() {
                            Image(uiImage: qrImage)
                                .interpolation(.none)
                                .resizable()
                                .aspectRatio(contentMode: .fit)
                                .frame(width: 250, height: 250)
                                .background(Color.white)
                                .cornerRadius(12)
                                .shadow(radius: 5)
                        } else {
                            // Fallback placeholder
                            ZStack {
                                RoundedRectangle(cornerRadius: 12)
                                    .fill(Color(.systemGray6))
                                    .frame(width: 250, height: 250)
                                
                                VStack {
                                    Image(systemName: "qrcode")
                                        .font(.system(size: 60))
                                        .foregroundColor(.gray)
                                    
                                    Text("UPI QR Code")
                                        .font(.headline)
                                        .foregroundColor(.gray)
                                    
                                    Text("₹\(String(format: "%.0f", amount))")
                                        .font(.title2)
                                        .fontWeight(.bold)
                                        .foregroundColor(.green)
                                }
                            }
                        }
                        
                        Text("⚠️ This is a placeholder QR code for demo")
                            .font(.caption)
                            .foregroundColor(.orange)
                            .padding(.horizontal)
                            .multilineTextAlignment(.center)
                    }
                    
                    // Payment Instructions
                    VStack(spacing: 16) {
                        Text("How to Pay")
                            .font(.headline)
                        
                        VStack(alignment: .leading, spacing: 12) {
                            PaymentStep(number: 1, text: "Open any UPI app (GPay, PhonePe, Paytm, etc.)")
                            PaymentStep(number: 2, text: "Scan the QR code above")
                            PaymentStep(number: 3, text: "Enter the amount ₹\(String(format: "%.0f", amount))")
                            PaymentStep(number: 4, text: "Complete the payment")
                            PaymentStep(number: 5, text: "Click 'I have paid' button below")
                        }
                    }
                    .padding()
                    .background(Color(.systemBackground))
                    .cornerRadius(12)
                    
                    // Confirmation Button
                    Button(action: {
                        confirmPayment()
                    }) {
                        HStack {
                            if isConfirming {
                                ProgressView()
                                    .scaleEffect(0.8)
                                    .foregroundColor(.white)
                            } else {
                                Image(systemName: "checkmark.circle")
                            }
                            Text(isConfirming ? "Processing..." : "I have paid ₹\(String(format: "%.0f", amount))")
                        }
                        .font(.headline)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.green)
                        .cornerRadius(12)
                    }
                    .disabled(isConfirming)
                    
                    Spacer()
                }
                .padding(.horizontal)
            }
            .background(Color(.systemGroupedBackground))
            .navigationTitle("UPI Payment")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
            }
        }
        .alert("Success", isPresented: $showingSuccess) {
            Button("OK") {
                dismiss()
            }
        } message: {
            Text(successMessage)
        }
        .alert("Error", isPresented: $showingError) {
            Button("OK") { }
        } message: {
            Text(errorMessage)
        }
    }
    
    private func generatePlaceholderQR() -> UIImage? {
        // Generate QR code with placeholder UPI URL
        let upiString = "upi://pay?pa=gridee@paytm&pn=Gridee%20Parking&am=\(amount)&cu=INR&tn=Wallet%20Topup"
        
        let data = Data(upiString.utf8)
        
        guard let filter = CIFilter(name: "CIQRCodeGenerator") else { return nil }
        filter.setValue(data, forKey: "inputMessage")
        filter.setValue("M", forKey: "inputCorrectionLevel")
        
        guard let outputImage = filter.outputImage else { return nil }
        
        let scaleX: CGFloat = 10.0
        let scaleY: CGFloat = 10.0
        let scaledImage = outputImage.transformed(by: CGAffineTransform(scaleX: scaleX, y: scaleY))
        
        let context = CIContext()
        guard let cgImage = context.createCGImage(scaledImage, from: scaledImage.extent) else { return nil }
        
        return UIImage(cgImage: cgImage)
    }
    
    // ✅ FIXED: Remove [weak self] from struct - structs don't need weak references
    private func confirmPayment() {
        isConfirming = true
        
        // ✅ Use WalletViewModel for payment processing - no weak self needed in struct
        walletViewModel.topUpWallet(amount: amount) { success, message in
            DispatchQueue.main.async {
                // ✅ Direct access to self properties - no weak reference needed
                isConfirming = false
                
                if success {
                    successMessage = message ?? "Payment successful!"
                    showingSuccess = true
                } else {
                    errorMessage = message ?? "Payment failed"
                    showingError = true
                }
            }
        }
    }
}

struct PaymentStep: View {
    let number: Int
    let text: String
    
    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            ZStack {
                Circle()
                    .fill(Color.blue)
                    .frame(width: 24, height: 24)
                
                Text("\(number)")
                    .font(.caption)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
            }
            
            Text(text)
                .font(.body)
                .foregroundColor(.primary)
            
            Spacer()
        }
    }
}

#Preview {
    UPIQRDisplayView(amount: 500)
        .environmentObject(WalletViewModel())
}
