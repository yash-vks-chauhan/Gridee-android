


import SwiftUI

struct WalletPage: View {
    @StateObject private var walletViewModel = WalletViewModel()
    @Environment(\.dismiss) private var dismiss
    
    @State private var showingAddMoney = false
    // ✅ REMOVED: showingAllTransactions until we fix AllTransactionsView
    // @State private var showingAllTransactions = false
    @State private var addAmount = ""
    
    var body: some View {
        NavigationView {  // ✅ ADD THIS LINE
            VStack(spacing: 0) {
                navigationHeader
                
                ScrollView {
                    VStack(spacing: 24) {
                        walletCard
                        quickAddSection
                        recentActivitySection
                        helpSection
                        
                        Spacer(minLength: 100)
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 20)
                }
            }
            .background(Color(.systemBackground))
            .navigationBarHidden(true)  // ✅ ADD THIS LINE
            .sheet(isPresented: $showingAddMoney) {
                AddMoneyView()
                    .environmentObject(walletViewModel)
            }
            .refreshable {
                walletViewModel.refreshWallet()
            }
            .alert("Error", isPresented: .constant(!walletViewModel.errorMessage.isEmpty)) {
                Button("OK") {
                    walletViewModel.clearError()
                }
            } message: {
                Text(walletViewModel.errorMessage)
            }
            .onAppear {
                walletViewModel.refreshWallet()
            }
        }  // ✅ ADD THIS LINE - Closes NavigationView
        .navigationViewStyle(.stack)  // ✅ ADD THIS LINE
    }

    
    private var navigationHeader: some View {
        HStack {
            Spacer()
            
            Text("Wallet")
                .font(.headline)
                .fontWeight(.semibold)
            
            Spacer()
        }
        .padding(.vertical, 16)
        .background(Color(.systemBackground))
    }
    
    private var walletCard: some View {
        VStack(spacing: 20) {
            HStack {
                ZStack {
                    Circle()
                        .fill(Color.white.opacity(0.2))
                        .frame(width: 40, height: 40)
                    
                    Image(systemName: "wallet.pass.fill")
                        .font(.system(size: 18))
                        .foregroundColor(.white)
                }
                
                VStack(alignment: .leading, spacing: 4) {
                    Text("Available Balance")
                        .font(.subheadline)
                        .foregroundColor(.white.opacity(0.8))
                    
                    Text(walletViewModel.formattedBalance)
                        .font(.system(size: 28, weight: .bold, design: .rounded))
                        .foregroundColor(.white)
                }
                
                Spacer()
            }
            
            Button(action: {
                showingAddMoney = true
            }) {
                HStack {
                    Image(systemName: "plus")
                        .font(.system(size: 16, weight: .semibold))
                    
                    Text("ADD MONEY")
                        .font(.subheadline)
                        .fontWeight(.semibold)
                }
                .foregroundColor(.black)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(Color.white)
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            
            if walletViewModel.isLoading {
                HStack {
                    ProgressView()
                        .scaleEffect(0.8)
                        .colorInvert()
                    
                    Text("Updating...")
                        .font(.caption)
                        .foregroundColor(.white.opacity(0.8))
                }
            }
        }
        .padding(24)
        .background(
            RoundedRectangle(cornerRadius: 20)
                .fill(.black)
        )
    }
    
    private var quickAddSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                Text("Quick Add")
                    .font(.headline)
                    .fontWeight(.semibold)
                
                Spacer()
            }
            
            HStack(spacing: 12) {
                QuickAddButton(amount: 10) {
                    walletViewModel.topUpWallet(amount: 10) { success, message in
                        if success {
                            print("✅ Quick add ₹10 successful")
                        } else {
                            print("❌ Quick add failed: \(message ?? "Unknown error")")
                        }
                    }
                }
                
                QuickAddButton(amount: 20) {
                    walletViewModel.topUpWallet(amount: 20) { success, message in
                        if success {
                            print("✅ Quick add ₹20 successful")
                        } else {
                            print("❌ Quick add failed: \(message ?? "Unknown error")")
                        }
                    }
                }
                
                QuickAddButton(amount: 100) {
                    walletViewModel.topUpWallet(amount: 100) { success, message in
                        if success {
                            print("✅ Quick add ₹100 successful")
                        } else {
                            print("❌ Quick add failed: \(message ?? "Unknown error")")
                        }
                    }
                }
            }
        }
    }

    private var recentActivitySection: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                Text("Recent Activity")
                    .font(.headline)
                    .fontWeight(.semibold)
                
                Spacer()
                
                // ✅ ADD: Show All button
                if !walletViewModel.transactions.isEmpty {
                                NavigationLink(destination: AllTransactionsView()
                                    .environmentObject(walletViewModel)
                                ) {
                                    Text("Show All")
                                        .font(.subheadline)
                                        .foregroundColor(.blue)
                                        .fontWeight(.medium)
                                }
                            }
                        }
            
            if walletViewModel.transactions.isEmpty && !walletViewModel.isLoading {
                VStack(spacing: 12) {
                    Image(systemName: "creditcard")
                        .font(.system(size: 32))
                        .foregroundColor(.secondary)
                    
                    Text("No transactions yet")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 40)
                .background(
                    RoundedRectangle(cornerRadius: 16)
                        .fill(Color(.systemGray6))
                )
            } else {
                VStack(spacing: 0) {
                    ForEach(Array(walletViewModel.recentTransactions.prefix(5).enumerated()), id: \.element.id) { index, transaction in
                        TransactionRowEnhanced(transaction: transaction)
                        
                        if index < min(4, walletViewModel.recentTransactions.count - 1) {
                            Divider()
                                .padding(.leading, 56)
                        }
                    }
                }
                .background(
                    RoundedRectangle(cornerRadius: 16)
                        .fill(Color(.systemGray6))
                )
            }
        }
    }

    private var helpSection: some View {
        HStack(spacing: 12) {
            ZStack {
                Circle()
                    .fill(Color(.systemGray5))
                    .frame(width: 32, height: 32)
                
                Image(systemName: "questionmark")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(.primary)
            }
            
            VStack(alignment: .leading, spacing: 2) {
                Text("How does the wallet work?")
                    .font(.subheadline)
                    .fontWeight(.medium)
                
                Text("Add money to pay for parking seamlessly")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color(.systemGray6))
        )
    }
}

struct TransactionRowEnhanced: View {
    let transaction: Transactions
    
    var body: some View {
        HStack(spacing: 12) {
            ZStack {
                Circle()
                    .fill(iconBackgroundColor)
                    .frame(width: 32, height: 32)
                
                Image(systemName: iconName)
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(iconColor)
            }
            
            VStack(alignment: .leading, spacing: 2) {
                Text(transaction.description ?? "Transaction")
                    .font(.subheadline)
                    .fontWeight(.medium)
                
                if let timestamp = transaction.timestamp {
                    Text(formatDateEnhanced(timestamp))
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            
            Spacer()
            
            Text(formatAmount(transaction.amount))
                .font(.subheadline)
                .fontWeight(.semibold)
                .foregroundColor(transaction.amount >= 0 ? .green : .primary)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 12)
    }
    
    private var iconName: String {
        if let description = transaction.description?.lowercased() {
            if description.contains("top") || description.contains("add") {
                return "arrow.down"
            } else if description.contains("parking") {
                return "p.circle.fill"
            } else if description.contains("refund") {
                return "arrow.up.right"
            }
        }
        return transaction.amount >= 0 ? "arrow.down" : "arrow.up"
    }
    
    private var iconColor: Color {
        if let description = transaction.description?.lowercased() {
            if description.contains("parking") {
                return .blue
            }
        }
        return .white
    }
    
    private var iconBackgroundColor: Color {
        if let description = transaction.description?.lowercased() {
            if description.contains("parking") {
                return .blue.opacity(0.15)
            }
        }
        return .black
    }
    
    private func formatDateEnhanced(_ dateString: String) -> String {
        let formatters = [
            { () -> DateFormatter in
                let f = DateFormatter()
                f.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSXXXXX"
                return f
            }(),
            { () -> DateFormatter in
                let f = DateFormatter()
                f.dateFormat = "yyyy-MM-dd'T'HH:mm:ssXXXXX"
                return f
            }(),
            { () -> DateFormatter in
                let f = DateFormatter()
                f.dateFormat = "yyyy-MM-dd'T'HH:mm:ss"
                return f
            }(),
            { () -> DateFormatter in
                let f = DateFormatter()
                f.dateFormat = "yyyy-MM-dd HH:mm:ss"
                return f
            }()
        ]
        
        let isoFormatter = ISO8601DateFormatter()
        isoFormatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        if let date = isoFormatter.date(from: dateString) {
            return formatDisplayDate(date)
        }
        
        for formatter in formatters {
            if let date = formatter.date(from: dateString) {
                return formatDisplayDate(date)
            }
        }
        
        return dateString
    }
    
    private func formatDisplayDate(_ date: Date) -> String {
        let displayFormatter = DateFormatter()
        let calendar = Calendar.current
        
        if calendar.isDateInToday(date) {
            displayFormatter.dateFormat = "h:mm a"
            return "Today • \(displayFormatter.string(from: date))"
        } else if calendar.isDateInYesterday(date) {
            displayFormatter.dateFormat = "h:mm a"
            return "Yesterday • \(displayFormatter.string(from: date))"
        } else {
            displayFormatter.dateFormat = "dd MMM • h:mm a"
            return displayFormatter.string(from: date)
        }
    }
    
    private func formatAmount(_ amount: Double) -> String {
        let prefix = amount >= 0 ? "+" : ""
        return "\(prefix)₹\(String(format: "%.0f", abs(amount)))"
    }
}

struct QuickAddButton: View {
    let amount: Int
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            VStack(spacing: 8) {
                Text("₹\(amount)")
                    .font(.title3)
                    .fontWeight(.bold)
                
                Text("Add ₹\(amount)")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 16)
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(Color(.systemGray6))
            )
        }
        .buttonStyle(PlainButtonStyle())
    }
}
