
import SwiftUI

struct UserProfileSection: View {
    @ObservedObject var authViewModel: AuthViewModel
    
    var body: some View {
        Section {
            VStack(spacing: 0) {
                UserInfoRow(authViewModel: authViewModel)
                
                Divider()
                    .padding(.vertical, 12)
                
                StatsDashboard()
            }
        }
//        .listRowBackground(Color.white)
        .background(Color(UIColor.secondarySystemBackground))
    }
}
