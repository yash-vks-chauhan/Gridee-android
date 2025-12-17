import Foundation
import SwiftUI

class WalletViewModel: ObservableObject {
    @Published var wallet: Wallet?
    @Published var transactions: [Transactions] = []
    @Published var isLoading: Bool = false
    @Published var errorMessage: String = ""
    @Published var showingAddMoney: Bool = false
    
    init() {
        loadWalletData()
    }
    
    var formattedBalance: String {
        guard let wallet = wallet else { return "₹0.00" }
        return String(format: "₹%.2f", wallet.balance)
    }
    
    var recentTransactions: [Transactions] {
        return transactions
            .sorted { transaction1, transaction2 in
                guard let date1 = parseTransactionDate(transaction1.timestamp),
                      let date2 = parseTransactionDate(transaction2.timestamp) else {
                    return false
                }
                return date1 > date2
            }
    }

    private func parseTransactionDate(_ dateString: String?) -> Date? {
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
        
        let customFormatter1 = DateFormatter()
        customFormatter1.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSXXXXX"
        if let date = customFormatter1.date(from: dateString) {
            return date
        }
        
        let customFormatter2 = DateFormatter()
        customFormatter2.dateFormat = "yyyy-MM-dd'T'HH:mm:ssXXXXX"
        if let date = customFormatter2.date(from: dateString) {
            return date
        }
        
        let customFormatter3 = DateFormatter()
        customFormatter3.dateFormat = "yyyy-MM-dd'T'HH:mm:ss"
        if let date = customFormatter3.date(from: dateString) {
            return date
        }
        
        return nil
    }

    func loadWalletData() {
        guard let userId = getCurrentUserId() else {
            errorMessage = "User not logged in"
            return
        }
        
        print("🔄 Loading wallet data for user: \(userId)")
        isLoading = true
        errorMessage = ""
        
        APIService.shared.fetchWallet(userId: userId) { [weak self] (result: Result<Wallet, APIError>) in
            DispatchQueue.main.async {
                switch result {
                case .success(let wallet):
                    print("✅ Wallet loaded successfully: ₹\(wallet.balance)")
                    self?.wallet = wallet
                    self?.loadTransactions(userId: userId)
                    
                case .failure(let error):
                    print("❌ Failed to load wallet: \(error.localizedDescription)")
                    self?.isLoading = false
                    self?.errorMessage = error.localizedDescription
                }
            }
        }
    }
    
    private func loadTransactions(userId: String) {
        APIService.shared.fetchWalletTransactions(userId: userId) { [weak self] (result: Result<[Transactions], APIError>) in
            DispatchQueue.main.async {
                self?.isLoading = false
                
                switch result {
                case .success(let transactions):
                    print("✅ Loaded \(transactions.count) transactions")
                    self?.transactions = transactions
                    
                case .failure(let error):
                    print("❌ Failed to load transactions: \(error.localizedDescription)")
                    self?.errorMessage = error.localizedDescription
                }
            }
        }
    }
    
    func initiatePayment(amount: Double, completion: @escaping (Bool, String?) -> Void) {
        guard let userId = getCurrentUserId() else {
            completion(false, "User not logged in")
            return
        }
        
        guard amount > 0 else {
            completion(false, "Amount must be greater than 0")
            return
        }
        
        print("💰 Initiating Razorpay payment: ₹\(amount)")
        isLoading = true
        errorMessage = ""
        
        PaymentService.shared.initiatePayment(userId: userId, amount: amount) { [weak self] result in
            DispatchQueue.main.async {
                self?.isLoading = false
                
                switch result {
                case .success(let orderId):
                    print("✅ Order ID received: \(orderId)")
                    // ✅ Return order ID, NOT marking as complete
                    completion(true, orderId)
                    
                case .failure(let error):
                    print("❌ Payment initiation failed: \(error.localizedDescription)")
                    self?.errorMessage = error.localizedDescription
                    completion(false, error.localizedDescription)
                }
            }
        }
    }

    
    // ✅ NEW: Confirm payment after Razorpay success/failure
    func confirmPayment(orderId: String, paymentId: String, success: Bool, amount: Double, completion: @escaping (Bool, String?) -> Void) {
        guard let userId = getCurrentUserId() else {
            completion(false, "User not logged in")
            return
        }
        
        print("🔄 Confirming payment - OrderID: \(orderId), Success: \(success)")
        isLoading = true
        errorMessage = ""
        
        PaymentService.shared.confirmPayment(
            orderId: orderId,
            paymentId: paymentId,
            success: success,
            userId: userId,
            amount: amount
        ) { [weak self] result in
            DispatchQueue.main.async {
                self?.isLoading = false
                
                switch result {
                case .success(let isSuccess):
                    if isSuccess {
                        print("✅ Payment confirmed successfully")
                        self?.showingAddMoney = false
                        self?.refreshWallet()
                        completion(true, "₹\(String(format: "%.0f", amount)) added successfully!")
                    } else {
                        print("❌ Payment verification failed")
                        self?.errorMessage = "Payment verification failed"
                        completion(false, "Payment verification failed")
                    }
                    
                case .failure(let error):
                    print("❌ Failed to confirm payment: \(error.localizedDescription)")
                    self?.errorMessage = error.localizedDescription
                    completion(false, error.localizedDescription)
                }
            }
        }
    }
    
    func showAddMoney() {
        showingAddMoney = true
    }
    
    func hideAddMoney() {
        showingAddMoney = false
    }
    
    func refreshWallet() {
        loadWalletData()
    }
    
    func clearError() {
        errorMessage = ""
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
