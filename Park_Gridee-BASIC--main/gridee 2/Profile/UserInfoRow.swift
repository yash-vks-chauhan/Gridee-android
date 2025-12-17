
import SwiftUI

struct UserInfoRow: View {
    @ObservedObject var authViewModel: AuthViewModel
    
    var body: some View {
        HStack(spacing: 16) {
            ProfileAvatarView(user: authViewModel.currentUser)
            UserDetailsView(authViewModel: authViewModel)
            Spacer()
            EditButton(authViewModel: authViewModel)
        }
        .padding(.vertical, 8)
    }
}
