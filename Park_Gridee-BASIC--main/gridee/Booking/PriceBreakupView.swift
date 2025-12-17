//
//  PriceBreakupView.swift
//  gridee
//
//  Created by Rishabh on 11/10/25.
//
import SwiftUI

struct PriceBreakup: Codable {
    let bookingId: String
    let status: String
    let vehicleNumber: String
    let checkInTime: String
    let checkOutTime: String
    let actualCheckOutTime: String?
    let bookedHours: Double
    let actualHours: Double?
    let hourlyRate: Double
    let baseAmount: Double
    let extraCharges: Double
    let penaltyCharges: Double
    let refundAmount: Double
    let totalAmount: Double
    let amountPaid: Double
}

struct PriceBreakupView: View {
    let booking: Bookings
    @State private var breakup: PriceBreakup?
    @State private var isLoading = true
    @State private var errorMessage = ""
    @Environment(\.dismiss) private var dismiss
    
    var body: some View {
        NavigationView {
            Group {
                if isLoading {
                    ProgressView("Loading breakup...")
                } else if let breakup = breakup {
                    breakupContent(breakup)
                } else {
                    errorView
                }
            }
            .navigationTitle("Price Breakup")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") {
                        dismiss()
                    }
                }
            }
        }
        .onAppear {
            fetchBreakup()
        }
    }
    
    @ViewBuilder
    private func breakupContent(_ breakup: PriceBreakup) -> some View {
        ScrollView {
            VStack(spacing: 20) {
                // Status Badge
                statusBadge(breakup.status)
                
                // Time Details
                timeSection(breakup)
                
                // Price Breakup
                priceSection(breakup)
                
                // Total
                totalSection(breakup)
            }
            .padding()
        }
        .scrollIndicators(.hidden)
    }
    
    @ViewBuilder
    private func statusBadge(_ status: String) -> some View {
        HStack {
            Image(systemName: statusIcon(status))
            Text(status.capitalized)
                .fontWeight(.semibold)
        }
        .font(.headline)
        .foregroundColor(.white)
        .padding(.horizontal, 20)
        .padding(.vertical, 10)
        .background(statusColor(status))
        .cornerRadius(20)
    }
    
    @ViewBuilder
    private func timeSection(_ breakup: PriceBreakup) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Booking Details")
                .font(.headline)
            
            DetailRow(
                title: "Vehicle",
                value: breakup.vehicleNumber,
                icon: "car.fill"
            )
            
            DetailRow(
                title: "Booked Hours",
                value: String(format: "%.1fh", breakup.bookedHours),
                icon: "clock.fill"
            )
            
            if let actualHours = breakup.actualHours {
                DetailRow(
                    title: "Actual Hours",
                    value: String(format: "%.1fh", actualHours),
                    icon: "clock.badge.checkmark.fill",
                    valueColor: actualHours > breakup.bookedHours ? .orange : .green
                )
            }
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(12)
    }

    
    @ViewBuilder
    private func priceSection(_ breakup: PriceBreakup) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Price Breakup")
                .font(.headline)
            
            PriceRow(
                label: "Base Amount",
                value: breakup.baseAmount,
                description: String(format: "%.1fh × ₹%.2f/hr", breakup.bookedHours, breakup.hourlyRate)
            )
            
            if breakup.extraCharges > 0 {
                PriceRow(
                    label: "Extra Charges",
                    value: breakup.extraCharges,
                    description: "Overstay charges",
                    valueColor: .orange
                )
            }
            
            if breakup.penaltyCharges > 0 {
                PriceRow(
                    label: "Penalty Charges",
                    value: breakup.penaltyCharges,
                    description: "Late cancellation",
                    valueColor: .red
                )
            }
            
            if breakup.refundAmount > 0 {
                PriceRow(
                    label: "Refund Amount",
                    value: breakup.refundAmount,
                    description: "Credited to wallet",
                    valueColor: .green
                )
            }
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(12)
    }
    
    @ViewBuilder
    private func totalSection(_ breakup: PriceBreakup) -> some View {
        VStack(spacing: 12) {
            Divider()
            
            HStack {
                Text("Amount Paid")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                Spacer()
                Text("₹\(String(format: "%.2f", breakup.amountPaid))")
                    .font(.subheadline)
            }
            
            HStack {
                Text("Total Amount")
                    .font(.title3)
                    .fontWeight(.bold)
                Spacer()
                Text("₹\(String(format: "%.2f", breakup.totalAmount))")
                    .font(.title3)
                    .fontWeight(.bold)
                    .foregroundColor(.blue)
            }
            
            if breakup.status.lowercased() == "cancelled" && breakup.refundAmount > 0 {
                Text("Refund of ₹\(String(format: "%.2f", breakup.refundAmount)) credited to wallet")
                    .font(.caption)
                    .foregroundColor(.green)
                    .padding(.top, 4)
            }
        }
        .padding()
        .background(Color.blue.opacity(0.1))
        .cornerRadius(12)
    }
    
    private var errorView: some View {
        VStack(spacing: 12) {
            Image(systemName: "exclamationmark.triangle")
                .font(.largeTitle)
                .foregroundColor(.orange)
            Text(errorMessage.isEmpty ? "Failed to load breakup" : errorMessage)
                .multilineTextAlignment(.center)
        }
        .padding()
    }
    
    private func fetchBreakup() {
        let userId = booking.userId

        
        let urlString = "\(APIService.backendBaseURL)/users/\(userId)/bookings/\(booking.id)/breakup"
        guard let url = URL(string: urlString) else { return }
        
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        
        let loginString = "rajeev:parking"
        guard let loginData = loginString.data(using: .utf8) else { return }
        let base64LoginString = loginData.base64EncodedString()
        request.setValue("Basic \(base64LoginString)", forHTTPHeaderField: "Authorization")
        
        URLSession.shared.dataTask(with: request) { data, response, error in
            DispatchQueue.main.async {
                isLoading = false
                
                if let error = error {
                    errorMessage = error.localizedDescription
                    return
                }
                
                guard let data = data else {
                    errorMessage = "No data received"
                    return
                }
                
                do {
                    breakup = try JSONDecoder().decode(PriceBreakup.self, from: data)
                } catch {
                    errorMessage = "Failed to parse response"
                }
            }
        }.resume()
    }

    
    private func statusIcon(_ status: String) -> String {
        switch status.lowercased() {
        case "completed": return "checkmark.circle.fill"
        case "cancelled": return "xmark.circle.fill"
        default: return "info.circle.fill"
        }
    }
    
    private func statusColor(_ status: String) -> Color {
        switch status.lowercased() {
        case "completed": return .green
        case "cancelled": return .red
        default: return .blue
        }
    }
}

// UNCOMMENTED DetailRow - This was your issue!
//struct DetailRow: View {
//    let label: String
//    let value: String
//    let icon: String
//    var valueColor: Color = .primary
//    
//    var body: some View {
//        HStack {
//            Image(systemName: icon)
//                .foregroundColor(.blue)
//            Text(label)
//                .foregroundColor(.secondary)
//            Spacer()
//            Text(value)
//                .fontWeight(.medium)
//                .foregroundColor(valueColor)
//        }
//    }
//}

struct PriceRow: View {
    let label: String
    let value: Double
    var description: String? = nil
    var valueColor: Color = .primary
    
    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(label)
                Spacer()
                Text("₹\(String(format: "%.2f", value))")
                    .fontWeight(.medium)
                    .foregroundColor(valueColor)
            }
            
            if let description = description {
                Text(description)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
    }
}
