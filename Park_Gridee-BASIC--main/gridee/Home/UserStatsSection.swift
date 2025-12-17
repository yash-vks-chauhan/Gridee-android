//import SwiftUI
//
//struct UserStatsSection: View {
//    let activeBookings: Int
//    let walletBalance: String
//    let onWalletTap: () -> Void
//    
//    @State private var userId: String = ""
//    
//    var body: some View {
//        VStack(spacing: 12) {
//            // User ID Section
////            HStack {
////                Text("User ID:")
////                    .font(.caption)
////                    .foregroundColor(.gray)
////                Spacer()
////                Text(userId.isEmpty ? "Not Found" : userId)
////                    .font(.caption)
////                    .foregroundColor(.black)
////            }
////            .padding(.horizontal, 20)
//            
//            // Existing Stats Section
//            HStack(spacing: 16) {
//                VStack(spacing: 4) {
//                    Text("\(activeBookings)")
//                        .font(.title2)
//                        .fontWeight(.bold)
////                        .foregroundColor(.black)
//                        .foregroundColor(AppColors.primaryText)
//                    Text("Active Bookings")
//                        .font(.caption)
////                        .foregroundColor(.gray)
//                        .foregroundColor(AppColors.secondaryText)
//                }
//                .frame(maxWidth: .infinity)
//                .padding()
////                .background(Color.white)
//                .background(AppColors.cardBackground)
//                .cornerRadius(12)
//                
//                Button(action: onWalletTap) {
//                    VStack(spacing: 4) {
//                        Text(walletBalance)
//                            .font(.title2)
//                            .fontWeight(.bold)
//                            .foregroundColor(.green)
//                        Text("Wallet Balance")
//                            .font(.caption)
////                            .foregroundColor(.gray)
//                            .foregroundColor(AppColors.secondaryText)
//                    }
//                    .frame(maxWidth: .infinity)
//                    .padding()
////                    .background(Color.white)
//                    .background(AppColors.cardBackground)
//                    .cornerRadius(12)
//                }
//                .buttonStyle(PlainButtonStyle())
//            }
//            .padding(.horizontal, 20)
//        }
//        .padding(.top, 16)
//        .onAppear {
//            if let id = getCurrentUserId() {
//                userId = id
//            }
//        }
//    }
//    
//    // Fetch User ID from UserDefaults
//    private func getCurrentUserId() -> String? {
//        return UserDefaults.standard.string(forKey: "currentUserId")
//    }
//}


import SwiftUI

struct UserStatsSection: View {
    let activeBookings: Int
    let pendingBookings: Int
    let onActiveTap: () -> Void      // ✅ ADD: Active booking tap
    let onPendingTap: () -> Void
    
    @State private var userId: String = ""
    
    var body: some View {
        VStack(spacing: 12) {
            HStack(spacing: 16) {
                // ✅ UPDATED: Active Bookings now tappable
                Button(action: onActiveTap) {
                    VStack(spacing: 4) {
                        Text("\(activeBookings)")
                            .font(.title2)
                            .fontWeight(.bold)
                            .foregroundColor(AppColors.primaryText)
                        Text("Active Bookings")
                            .font(.caption)
                            .foregroundColor(AppColors.secondaryText)
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(AppColors.cardBackground)
                    .cornerRadius(12)
                }
                .buttonStyle(PlainButtonStyle())
                
                // ✅ Pending Bookings (already tappable)
                Button(action: onPendingTap) {
                    VStack(spacing: 4) {
                        Text("\(pendingBookings)")
                            .font(.title2)
                            .fontWeight(.bold)
                            .foregroundColor(.orange)
                        Text("Pending Bookings")
                            .font(.caption)
                            .foregroundColor(AppColors.secondaryText)
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(AppColors.cardBackground)
                    .cornerRadius(12)
                }
                .buttonStyle(PlainButtonStyle())
            }
            .padding(.horizontal, 20)
        }
        .padding(.top, 16)
        .onAppear {
            if let id = getCurrentUserId() {
                userId = id
            }
        }
    }
    
    private func getCurrentUserId() -> String? {
        return UserDefaults.standard.string(forKey: "currentUserId")
    }
}
