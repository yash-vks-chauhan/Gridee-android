import SwiftUI

struct StatsDashboard: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @EnvironmentObject var homeViewModel: HomeViewModel // ✅ Add if you have booking data here
    
    var body: some View {
        HStack {
            Spacer()
            
            // ✅ Real Bookings Count
            ProfileStatItem(
                value: "\(getTotalBookings())",
                label: "Bookings"
            )
            
            Spacer()
            
            // ✅ Real Total Spent from wallet/bookings
//            ProfileStatItem(
//                value: "₹\(getTotalSpent())",
//                label: "Total Spent"
//            )
            
            Spacer()
            
            // ✅ Real Vehicle Count (or keep app rating)
            ProfileStatItem(
                value: "\(getVehicleCount())",
                label: "Vehicles"
            )
            
            Spacer()
        }
        .padding(.vertical, 8)
    }
    
    // MARK: - Helper Functions
    
    private func getTotalBookings() -> Int {
        // ✅ Option 1: Get from HomeViewModel if you track bookings there
        // return homeViewModel.userBookings.count
        
        // ✅ Option 2: Get from AuthViewModel wallet coins (approximate)
        if let walletCoins = authViewModel.currentUser?.walletCoins {
            return Int(max(0, walletCoins / 10)) // Estimate: 10 coins per booking
        }
        
        // ✅ Option 3: Fallback - keep original or use 0
        return 0
    }
    
    private func getTotalSpent() -> String {
        // ✅ Option 1: Calculate from wallet coins used
        if let walletCoins = authViewModel.currentUser?.walletCoins {
            let spent = max(0, 2000 - walletCoins) // Assuming started with 2000 coins
            return formatCurrency(Int(spent))
        }
        
        // ✅ Option 2: If you track spending elsewhere
        // return formatCurrency(homeViewModel.totalUserSpending)
        
        // ✅ Option 3: Fallback
        return "0"
    }
    
    private func getVehicleCount() -> Int {
        // ✅ Real vehicle count from user
        return authViewModel.getCurrentUserVehicles().count
    }
    
    private func formatCurrency(_ amount: Int) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.maximumFractionDigits = 0
        return formatter.string(from: NSNumber(value: amount)) ?? "\(amount)"
    }
}
