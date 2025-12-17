//import SwiftUI
//
//struct ProfilePageContent: View {
//    @Binding var activeContent: SheetContent?
//    @EnvironmentObject var authViewModel: AuthViewModel
//    @EnvironmentObject var homeViewModel: HomeViewModel
//    
//    var body: some View {
//        VStack(spacing: 0) {
//            List {
//                UserProfileSection(authViewModel: authViewModel)
//                
//                ProfileOptionsSection()
//                    // ✅ REMOVED: ProfileOptionsSection doesn't need homeViewModel
//                    // It's just UI elements like Settings, Privacy, etc.
//                
//                SignOutSection(authViewModel: authViewModel)
//            }
//            .listStyle(.insetGrouped)
//            .scrollContentBackground(.hidden)
//            .background(Color(UIColor.systemGroupedBackground))
//            
//            Spacer()
//            
//            AppVersionView()
//        }
//        .navigationTitle("Profile")
//        .navigationBarTitleDisplayMode(.inline)
//        .background(Color(UIColor.systemGroupedBackground))
//    }
//}

import SwiftUI

struct ProfilePageContent: View {
    @Binding var activeContent: SheetContent?
    @EnvironmentObject var authViewModel: AuthViewModel
    @EnvironmentObject var homeViewModel: HomeViewModel
    @Environment(\.colorScheme) var colorScheme
    
    var body: some View {
        VStack(spacing: 0) {
            // ✅ NEW: Profile heading matching Bookings/Wallet style
            profileHeader
            
            List {
                UserProfileSection(authViewModel: authViewModel)
                
                ProfileOptionsSection()
                
                SignOutSection(authViewModel: authViewModel)
            }
            .listStyle(.insetGrouped)
            .scrollContentBackground(.hidden)
            .background(backgroundColor)
            
            Spacer()
            
            AppVersionView()
        }
        .navigationBarHidden(true) // ✅ Hide navigation bar
        .background(backgroundColor)
    }
    
    // ✅ NEW: Profile header matching Bookings/Wallet style
    private var profileHeader: some View {
        HStack {
            Text("Profile")
                .font(.largeTitle)
                .fontWeight(.bold)
            
            Spacer()
        }
        .padding(.horizontal, 20)
        .padding(.top, 16)
        .padding(.bottom, 8)
        .background(backgroundColor)
    }
    
    // ✅ Adaptive background color
    private var backgroundColor: Color {
        colorScheme == .dark ? Color.black : Color(UIColor.systemGroupedBackground)
    }
}
