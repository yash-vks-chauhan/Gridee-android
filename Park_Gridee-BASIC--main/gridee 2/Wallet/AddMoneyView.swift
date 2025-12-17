//
//  AddMoneyView.swift
//  gridee
//
//  Created by admin85 on 01/10/25.
//
import SwiftUI

struct AddMoneyView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var selectedAmount: Double = 100
    @State private var customAmount: String = ""
    @State private var showingUPIQR = false
    @State private var isLoading = false
    @State private var errorMessage = ""
    @State private var showingError = false
    
    // Predefined amounts
    private let predefinedAmounts: [Double] = [50, 100, 200, 500, 1000, 2000]
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 24) {
                    // Header
                    VStack(spacing: 8) {
                        Image(systemName: "plus.circle.fill")
                            .font(.system(size: 50))
                            .foregroundColor(.green)
                        
                        Text("Add Money to Wallet")
                            .font(.title2)
                            .fontWeight(.bold)
                        
                        Text("Select amount or enter custom amount")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    .padding(.top)
                    
                    // Amount Selection
                    VStack(spacing: 20) {
                        // Predefined amounts
                        Text("Quick Select")
                            .font(.headline)
                            .frame(maxWidth: .infinity, alignment: .leading)
                        
                        LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 3), spacing: 12) {
                            ForEach(predefinedAmounts, id: \.self) { amount in
                                AmountButton(
                                    amount: amount,
                                    isSelected: selectedAmount == amount && customAmount.isEmpty,
                                    action: {
                                        selectedAmount = amount
                                        customAmount = ""
                                    }
                                )
                            }
                        }
                        
                        // Custom amount
                        VStack(spacing: 12) {
                            Text("Custom Amount")
                                .font(.headline)
                                .frame(maxWidth: .infinity, alignment: .leading)
                            
                            HStack {
                                Text("₹")
                                    .font(.title2)
                                    .fontWeight(.medium)
                                
                                TextField("Enter amount", text: $customAmount)
                                    .keyboardType(.numberPad)
                                    .textFieldStyle(RoundedBorderTextFieldStyle())
                                    .onChange(of: customAmount) { _, newValue in
                                        if !newValue.isEmpty {
                                            selectedAmount = Double(newValue) ?? 0
                                        }
                                    }
                            }
                        }
                    }
                    .padding()
                    .background(Color(.systemGray6))
                    .cornerRadius(12)
                    
                    // Selected amount display
                    VStack(spacing: 8) {
                        Text("Amount to Add")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                        
                        Text("₹\(String(format: "%.0f", finalAmount))")
                            .font(.system(size: 32, weight: .bold))
                            .foregroundColor(.green)
                    }
                    .padding()
                    .background(Color(.systemBackground))
                    .cornerRadius(12)
                    .shadow(radius: 2)
                    
                    // Continue button
                    Button(action: {
                        showingUPIQR = true
                    }) {
                        HStack {
                            Image(systemName: "qrcode")
                            Text("Pay with UPI")
                        }
                        .font(.headline)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.green)
                        .cornerRadius(12)
                    }
                    .disabled(finalAmount <= 0)
                    .opacity(finalAmount <= 0 ? 0.6 : 1.0)
                    
                    Spacer()
                }
                .padding(.horizontal)
            }
            .background(Color(.systemGroupedBackground))
            .navigationTitle("Add Money")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
            }
        }
        .sheet(isPresented: $showingUPIQR) {
            UPIQRDisplayView(amount: finalAmount)
        }
        .alert("Error", isPresented: $showingError) {
            Button("OK") { }
        } message: {
            Text(errorMessage)
        }
    }
    
    private var finalAmount: Double {
        if !customAmount.isEmpty {
            return Double(customAmount) ?? 0
        }
        return selectedAmount
    }
}

struct AmountButton: View {
    let amount: Double
    let isSelected: Bool
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            Text("₹\(String(format: "%.0f", amount))")
                .font(.headline)
                .foregroundColor(isSelected ? .white : .primary)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
                .background(isSelected ? Color.green : Color(.systemBackground))
                .cornerRadius(8)
                .overlay(
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(Color.green, lineWidth: isSelected ? 0 : 1)
                )
        }
        .buttonStyle(PlainButtonStyle())
    }
}

#Preview {
    AddMoneyView()
}

