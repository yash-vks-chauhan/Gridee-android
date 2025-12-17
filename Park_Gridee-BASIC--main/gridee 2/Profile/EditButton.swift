

import SwiftUI

struct EditButton: View {
    @ObservedObject var authViewModel: AuthViewModel
    @State private var showingEditProfile = false
    
    var body: some View {
        Button(action: {
            showingEditProfile = true
        }) {
            Text("Edit")
                .font(.subheadline)
                .fontWeight(.medium)
                .foregroundColor(.blue)
        }
        .sheet(isPresented: $showingEditProfile) {
            EditProfileView()
                .environmentObject(authViewModel)
        }
    }
}
