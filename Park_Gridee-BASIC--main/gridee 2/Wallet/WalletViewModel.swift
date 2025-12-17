


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
    
    // In WalletViewModel
    var recentTransactions: [Transactions] {
        return transactions
            .sorted { transaction1, transaction2 in
                // Sort by timestamp, latest first
                guard let date1 = parseTransactionDate(transaction1.timestamp),
                      let date2 = parseTransactionDate(transaction2.timestamp) else {
                    return false
                }
                return date1 > date2 // Latest first
            }
    }

    // Helper function to parse transaction dates
    // ✅ FIXED: WalletViewModel parseTransactionDate function
    private func parseTransactionDate(_ dateString: String?) -> Date? {
        guard let dateString = dateString else { return nil }
        
        // ✅ Try ISO8601DateFormatter first
        let isoFormatter = ISO8601DateFormatter()
        isoFormatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        if let date = isoFormatter.date(from: dateString) {
            return date
        }
        
        // ✅ Try simple ISO8601DateFormatter
        let simpleIsoFormatter = ISO8601DateFormatter()
        if let date = simpleIsoFormatter.date(from: dateString) {
            return date
        }
        
        // ✅ Try DateFormatter instances (properly typed)
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

    // ✅ FIXED: Load wallet data with proper type annotations
    func loadWalletData() {
        guard let userId = getCurrentUserId() else {
            errorMessage = "User not logged in"
            return
        }
        
        print("🔄 Loading wallet data for user: \(userId)")
        isLoading = true
        errorMessage = ""
        
        // ✅ FIXED: Add explicit type annotation
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
    
    // ✅ FIXED: Load transactions with proper type annotations
    private func loadTransactions(userId: String) {
        // ✅ FIXED: Add explicit type annotation
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
    
    // ✅ UPDATED: Top up wallet - called from UPI confirmation
    func topUpWallet(amount: Double, completion: @escaping (Bool, String?) -> Void) {
        guard let userId = getCurrentUserId() else {
            completion(false, "User not logged in")
            return
        }
        
        guard amount > 0 else {
            completion(false, "Amount must be greater than 0")
            return
        }
        
        print("💰 Processing UPI payment confirmation: ₹\(amount)")
        isLoading = true
        errorMessage = ""
        
        // ✅ FIXED: Add explicit type annotation
        APIService.shared.topUpWallet(userId: userId, amount: amount) { [weak self] (result: Result<Wallet, APIError>) in
            DispatchQueue.main.async {
                self?.isLoading = false
                
                switch result {
                case .success(let updatedWallet):
                    print("✅ Wallet topped up successfully: ₹\(updatedWallet.balance)")
                    self?.wallet = updatedWallet
                    self?.showingAddMoney = false
                    // Refresh transactions to show the new topup transaction
                    self?.loadTransactions(userId: userId)
                    completion(true, "₹\(String(format: "%.0f", amount)) added successfully!")
                    
                case .failure(let error):
                    print("❌ Failed to top up wallet: \(error.localizedDescription)")
                    self?.errorMessage = error.localizedDescription
                    completion(false, error.localizedDescription)
                }
            }
        }
    }
    
    // ✅ NEW: Show add money interface
    func showAddMoney() {
        showingAddMoney = true
    }
    
    // ✅ NEW: Hide add money interface
    func hideAddMoney() {
        showingAddMoney = false
    }
    
    func refreshWallet() {
        loadWalletData()
    }
    
    func clearError() {
        errorMessage = ""
    }
    
    // ✅ Helper method to get current user ID
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

