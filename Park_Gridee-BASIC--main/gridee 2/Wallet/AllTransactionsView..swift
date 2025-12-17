//
//  AllTransactionsView..swift
//  gridee
//
//  Created by Rishabh on 08/10/25.
//
import SwiftUI

struct AllTransactionsView: View {
    @EnvironmentObject var walletViewModel: WalletViewModel
    @Environment(\.dismiss) private var dismiss
    
    var body: some View {
        VStack(spacing: 0) {
            // Header
            HStack {
                Button(action: { dismiss() }) {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundColor(.primary)
                }
                
                Spacer()
                
                Text("All Transactions")
                    .font(.headline)
                    .fontWeight(.semibold)
                
                Spacer()
                
                // Invisible placeholder for centering
                Image(systemName: "chevron.left")
                    .font(.system(size: 18, weight: .semibold))
                    .opacity(0)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 16)
            .background(Color(.systemBackground))
            
            Divider()
            
            // Transaction List
            if walletViewModel.transactions.isEmpty {
                VStack(spacing: 16) {
                    Spacer()
                    
                    Image(systemName: "creditcard")
                        .font(.system(size: 48))
                        .foregroundColor(.secondary)
                    
                    Text("No transactions yet")
                        .font(.headline)
                        .foregroundColor(.secondary)
                    
                    Text("Your transaction history will appear here")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                    
                    Spacer()
                }
                .padding()
            } else {
                ScrollView {
                    LazyVStack(spacing: 0) {
                        ForEach(Array(walletViewModel.recentTransactions.enumerated()), id: \.element.id) { index, transaction in
                            TransactionRowEnhanced(transaction: transaction)
                            
                            if index < walletViewModel.recentTransactions.count - 1 {
                                Divider()
                                    .padding(.leading, 56)
                            }
                        }
                    }
                    .background(Color(.systemBackground))
                    
                    Spacer(minLength: 20)
                }
            }
        }
        .background(Color(.systemGroupedBackground))
        .navigationBarHidden(true)
    }
}

