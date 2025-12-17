
import SwiftUI

struct UserDetailsView: View {
    @ObservedObject var authViewModel: AuthViewModel
    
    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(authViewModel.currentUser?.name ?? "User Name")
                .font(.title3)
                .fontWeight(.semibold)
                .foregroundColor(.primary)
             
            
            Text(authViewModel.currentUser?.phone ?? "Phone Number")
                .font(.subheadline)
                .foregroundColor(.gray)
             
            if let email = authViewModel.currentUser?.email {
                Text(email)
                    .font(.caption)
                    .foregroundColor(.gray)
            }
            
            VerificationBadge()
        }
    }
}
