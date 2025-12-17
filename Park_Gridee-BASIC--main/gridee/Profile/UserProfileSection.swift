//
//import SwiftUI
//
//struct UserProfileSection: View {
//    @ObservedObject var authViewModel: AuthViewModel
//    
//    var body: some View {
//        Section {
//            VStack(spacing: 0) {
//                UserInfoRow(authViewModel: authViewModel)
//                
//                Divider()
//                    .padding(.vertical, 12)
//                
//                StatsDashboard()
//            }
//        }
////        .listRowBackground(Color.white)
//        .background(Color(UIColor.secondarySystemBackground))
//    }
//}


import SwiftUI

struct UserProfileSection: View {
    @ObservedObject var authViewModel: AuthViewModel
    @Environment(\.colorScheme) var colorScheme
    
    var body: some View {
        Section {
            VStack(spacing: 0) {
                UserInfoRow(authViewModel: authViewModel)
                
                Divider()
                    .padding(.vertical, 12)
                
                StatsDashboard()
            }
            .padding(.vertical, 8)
        }
        .listRowBackground(cardBackgroundColor)
    }
    
    // ✅ Adaptive card background
    private var cardBackgroundColor: Color {
        colorScheme == .dark ? Color(red: 0.11, green: 0.11, blue: 0.12) : Color(UIColor.secondarySystemGroupedBackground)
    }
}
