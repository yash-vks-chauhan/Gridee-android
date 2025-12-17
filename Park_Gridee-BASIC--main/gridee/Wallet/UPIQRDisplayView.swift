//import SwiftUI
//
//struct UPIPaymentView: View {
//    let amount: Double
//    let orderId: String
//    let onSuccess: (String) -> Void
//    let onFailure: () -> Void
//    
//    @Environment(\.dismiss) var dismiss
//    @State private var selectedApp: UPIApp?
//    
//    enum UPIApp: String, CaseIterable {
//        case phonePe = "PhonePe"
//        case googlePay = "Google Pay"
//        case paytm = "Paytm"
//        
//        var scheme: String {
//            switch self {
//            case .phonePe: return "phonepe://"
//            case .googlePay: return "gpay://"
//            case .paytm: return "paytmmp://"
//            }
//        }
//        
//        var icon: String {
//            switch self {
//            case .phonePe: return "p.circle.fill"
//            case .googlePay: return "g.circle.fill"
//            case .paytm: return "indianrupeesign.circle.fill"
//            }
//        }
//    }
//    
//    var body: some View {
//        NavigationView {
//            VStack(spacing: 24) {
//                VStack(spacing: 8) {
//                    Text("Pay Amount")
//                        .font(.subheadline)
//                        .foregroundColor(.secondary)
//                    Text("₹\(String(format: "%.2f", amount))")
//                        .font(.system(size: 36, weight: .bold))
//                }
//                .padding(.top, 32)
//                
//                Divider()
//                
//                VStack(alignment: .leading, spacing: 16) {
//                    Text("Select Payment App")
//                        .font(.headline)
//                        .padding(.horizontal)
//                    
//                    ForEach(UPIApp.allCases, id: \.self) { app in
//                        Button(action: {
//                            selectedApp = app
//                            openUPIApp(app)
//                        }) {
//                            HStack {
//                                Image(systemName: app.icon)
//                                    .font(.title2)
//                                    .foregroundColor(.blue)
//                                
//                                Text(app.rawValue)
//                                    .font(.headline)
//                                
//                                Spacer()
//                                
//                                Image(systemName: "chevron.right")
//                                    .foregroundColor(.secondary)
//                            }
//                            .padding()
//                            .background(Color(.systemGray6))
//                            .cornerRadius(12)
//                        }
//                        .buttonStyle(PlainButtonStyle())
//                    }
//                }
//                .padding(.horizontal)
//                
//                Spacer()
//                
//                VStack(spacing: 12) {
//                    Text("After completing payment in the app:")
//                        .font(.subheadline)
//                        .foregroundColor(.secondary)
//                        .multilineTextAlignment(.center)
//                    
//                    HStack(spacing: 12) {
//                        Button(action: {
//                            onFailure()
//                            dismiss()
//                        }) {
//                            Text("Payment Failed")
//                                .font(.headline)
//                                .foregroundColor(.white)
//                                .frame(maxWidth: .infinity)
//                                .padding()
//                                .background(Color.red)
//                                .cornerRadius(12)
//                        }
//                        
//                        Button(action: {
//                            let mockPaymentId = "PAY_\(UUID().uuidString.prefix(12))"
//                            onSuccess(mockPaymentId)
//                            dismiss()
//                        }) {
//                            Text("Payment Success")
//                                .font(.headline)
//                                .foregroundColor(.white)
//                                .frame(maxWidth: .infinity)
//                                .padding()
//                                .background(Color.green)
//                                .cornerRadius(12)
//                        }
//                    }
//                }
//                .padding()
//            }
//            .navigationTitle("UPI Payment")
//            .navigationBarTitleDisplayMode(.inline)
//            .toolbar {
//                ToolbarItem(placement: .navigationBarLeading) {
//                    Button("Cancel") {
//                        onFailure()
//                        dismiss()
//                    }
//                }
//            }
//        }
//    }
//    
//    private func openUPIApp(_ app: UPIApp) {
//        if let url = URL(string: app.scheme), UIApplication.shared.canOpenURL(url) {
//            UIApplication.shared.open(url)
//        }
//    }
//}
