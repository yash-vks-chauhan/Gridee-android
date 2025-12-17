import SwiftUI

struct AddMoneyView: View {
    @ObservedObject var viewModel: WalletViewModel
    @Environment(\.dismiss) var dismiss
    
    @State private var amount: String = ""
    @State private var selectedAmount: Double? = nil
    @State private var isProcessing = false
    @State private var showError = false
    @State private var errorMessage = ""
    @State private var currentOrderId: String = ""
    
    let quickAmounts: [Double] = [100, 200, 500, 1000, 2000, 5000]
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 24) {
                    VStack(spacing: 8) {
                        Text("Current Balance")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                        Text(viewModel.formattedBalance)
                            .font(.system(size: 32, weight: .bold))
                    }
                    .padding(.top, 20)
                    
                    Divider()
                    
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Enter Amount")
                            .font(.headline)
                        
                        HStack {
                            Text("₹")
                                .font(.title2)
                                .foregroundColor(.secondary)
                            TextField("0", text: $amount)
                                .keyboardType(.numberPad)
                                .font(.title)
                                .onChange(of: amount) { _ in
                                    selectedAmount = nil
                                }
                        }
                        .padding()
                        .background(Color(.systemGray6))
                        .cornerRadius(12)
                    }
                    .padding(.horizontal)
                    
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Quick Add")
                            .font(.headline)
                            .padding(.horizontal)
                        
                        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                            ForEach(quickAmounts, id: \.self) { quickAmount in
                                Button(action: {
                                    selectedAmount = quickAmount
                                    amount = String(format: "%.0f", quickAmount)
                                }) {
                                    Text("₹\(String(format: "%.0f", quickAmount))")
                                        .font(.headline)
                                        .foregroundColor(selectedAmount == quickAmount ? .white : .primary)
                                        .frame(maxWidth: .infinity)
                                        .padding()
                                        .background(selectedAmount == quickAmount ? Color.blue : Color(.systemGray6))
                                        .cornerRadius(12)
                                }
                                .scrollIndicators(.hidden)
                            }
                        }
                        .padding(.horizontal)
                    }
                    
                    Spacer()
                    
                    Button(action: {
                        initiatePayment()
                    }) {
                        if isProcessing {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                .frame(maxWidth: .infinity)
                                .padding()
                        } else {
                            Text("Add Money")
                                .font(.headline)
                                .foregroundColor(.white)
                                .frame(maxWidth: .infinity)
                                .padding()
                        }
                    }
                    .background(isValidAmount() ? Color.blue : Color.gray)
                    .cornerRadius(12)
                    .disabled(!isValidAmount() || isProcessing)
                    .padding(.horizontal)
                    .padding(.bottom, 20)
                }
            }
            .navigationTitle("Add Money")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
            }
            .alert("Error", isPresented: $showError) {
                Button("OK", role: .cancel) { }
            } message: {
                Text(errorMessage)
            }
            .onAppear {
                RazorpayManager.shared.initializeRazorpay()
            }
        }
    }
    
    private func isValidAmount() -> Bool {
        guard let amountValue = Double(amount) else { return false }
        return amountValue >= 10 && amountValue <= 100000
    }
    
    private func initiatePayment() {
        guard let amountValue = Double(amount) else { return }
        
        isProcessing = true
        
        viewModel.initiatePayment(amount: amountValue) { success, orderIdOrError in
            DispatchQueue.main.async {
                if success, let orderId = orderIdOrError {
                    currentOrderId = orderId
                    openRazorpayCheckout(orderId: orderId, amount: amountValue)
                } else {
                    isProcessing = false
                    errorMessage = orderIdOrError ?? "Failed to initiate payment"
                    showError = true
                }
            }
        }
    }
    
    private func openRazorpayCheckout(orderId: String, amount: Double) {
        guard let user = getCurrentUser() else {
            isProcessing = false
            errorMessage = "User information not found"
            showError = true
            return
        }
        
        // Dismiss sheet first
        dismiss()
        
        // Wait for dismissal, then open Razorpay
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.6) {
            RazorpayManager.shared.openCheckout(
                orderId: orderId,
                amount: amount,
                userName: user.name ?? "User",
                userEmail: user.email ?? "",
                userPhone: ""
            ) { [self] paymentId, orderId in
                confirmPaymentSuccess(orderId: orderId, paymentId: paymentId, amount: amount)
            } onFailure: { [self] in
                confirmPaymentFailure(orderId: orderId, amount: amount)
            }
        }
    }

    private func confirmPaymentSuccess(orderId: String, paymentId: String, amount: Double) {
        viewModel.confirmPayment(
            orderId: orderId,
            paymentId: paymentId,
            success: true,
            amount: amount
        ) { success, message in
            DispatchQueue.main.async {
                self.isProcessing = false
                
                if success {
                    self.viewModel.refreshWallet()
                } else {
                    print("❌ Failed to verify payment: \(message ?? "")")
                }
            }
        }
    }
    
    private func confirmPaymentFailure(orderId: String, amount: Double) {
        viewModel.confirmPayment(
            orderId: orderId,
            paymentId: "FAILED",
            success: false,
            amount: amount
        ) { _, _ in
            DispatchQueue.main.async {
                self.isProcessing = false
                print("❌ Payment cancelled or failed")
            }
        }
    }
    
    private func getCurrentUser() -> Users? {
        if let userData = UserDefaults.standard.data(forKey: "userData"),
           let user = try? JSONDecoder().decode(Users.self, from: userData) {
            return user
        }
        return nil
    }
    
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
}
