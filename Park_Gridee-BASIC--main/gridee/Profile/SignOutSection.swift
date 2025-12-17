//
//import SwiftUI
//
//struct SignOutSection: View {
//    @ObservedObject var authViewModel: AuthViewModel
//    
//    var body: some View {
//        Section {
//            SignOutButton(authViewModel: authViewModel)
//        }
////        .listRowBackground(Color.white)
//        .background(Color(UIColor.secondarySystemBackground))
//    }
//}
//
//struct SignOutButton: View {
//    @ObservedObject var authViewModel: AuthViewModel
//    
//    var body: some View {
//        Button(action: {
//            authViewModel.logout()
//        }) {
//            HStack {
//                Spacer()
//                Text("Sign Out")
//                    .font(.headline)
//                    .fontWeight(.medium)
//                    .foregroundColor(.red)
////                    .background(Color(UIColor.secondarySystemBackground))
//                Spacer()
//            }
//            .padding(.vertical, 12)
//            .contentShape(Rectangle())
//        }
//        .buttonStyle(PlainButtonStyle()) // Add this line
//    }
//}


import SwiftUI

struct SignOutSection: View {
    @ObservedObject var authViewModel: AuthViewModel
    @Environment(\.colorScheme) var colorScheme
    
    var body: some View {
        Section {
            SignOutButton(authViewModel: authViewModel)
        }
        .listRowBackground(cardBackgroundColor)
    }
    
    // ✅ Adaptive card background
    private var cardBackgroundColor: Color {
        colorScheme == .dark ? Color(red: 0.11, green: 0.11, blue: 0.12) : Color(UIColor.secondarySystemGroupedBackground)
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
                Spacer()
            }
            .padding(.vertical, 12)
            .contentShape(Rectangle())
        }
        .buttonStyle(PlainButtonStyle())
    }
}
