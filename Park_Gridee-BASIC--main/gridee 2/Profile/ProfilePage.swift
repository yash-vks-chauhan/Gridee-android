import SwiftUI

struct ProfilePageContent: View {
    @Binding var activeContent: SheetContent?
    @EnvironmentObject var authViewModel: AuthViewModel
    @EnvironmentObject var homeViewModel: HomeViewModel // ✅ Add this line
    
    var body: some View {
        VStack(spacing: 0) {
            List {
                UserProfileSection(authViewModel: authViewModel)
                ProfileOptionsSection()
                    .environmentObject(homeViewModel) // ✅ Add this line
                SignOutSection(authViewModel: authViewModel)
            }
            .listStyle(.insetGrouped)
            .scrollContentBackground(.hidden)
            .background(Color(UIColor.systemGroupedBackground))
            
            Spacer()
            
            AppVersionView()
//
//            ProfileBottomTabBar(activeContent: $activeContent)
        }
        .navigationTitle("Profile")
        .navigationBarTitleDisplayMode(.inline)
        .background(Color(UIColor.systemGroupedBackground))
//        .background(Color(UIColor.systemBackground))

    }
}
