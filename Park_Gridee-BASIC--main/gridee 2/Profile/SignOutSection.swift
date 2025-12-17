
import SwiftUI

struct SignOutSection: View {
    @ObservedObject var authViewModel: AuthViewModel
    
    var body: some View {
        Section {
            SignOutButton(authViewModel: authViewModel)
        }
//        .listRowBackground(Color.white)
        .background(Color(UIColor.secondarySystemBackground))
    }
}

struct SignOutButton: View {
    @ObservedObject var authViewModel: AuthViewModel
    
    var body: some View {
        Button(action: {
            authViewModel.logout()
        }) {
            HStack {
                Spacer()
                Text("Sign Out")
                    .font(.headline)
                    .fontWeight(.medium)
                    .foregroundColor(.red)
//                    .background(Color(UIColor.secondarySystemBackground))
                Spacer()
            }
            .padding(.vertical, 12)
            .contentShape(Rectangle())
        }
        .buttonStyle(PlainButtonStyle()) // Add this line
    }
}
