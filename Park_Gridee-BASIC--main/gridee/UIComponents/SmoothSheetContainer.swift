

import SwiftUI

struct SmoothSheetContainer: View {
    @Binding var activeContent: SheetContent?
    @State private var currentContent: SheetContent
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject var homeViewModel: HomeViewModel
    @EnvironmentObject var authViewModel: AuthViewModel

    init(activeContent: Binding<SheetContent?>, initialContent: SheetContent) {
        self._activeContent = activeContent
        self._currentContent = State(initialValue: initialContent)
    }

    var body: some View {
        NavigationView {
            ZStack {
                if currentContent == .booking {
                    BookingPageContent(activeContent: $activeContent)
                        .environmentObject(homeViewModel)
                        .environmentObject(authViewModel)
                        .transition(.move(edge: .leading))
                        .zIndex(1)
                }

                if currentContent == .wallet {
                    WalletPage()
                        .transition(.move(edge: .trailing))
                        .zIndex(0)
                }
            }
            .animation(.easeInOut(duration: 0.5), value: currentContent)
            .navigationTitle(currentContent == .booking ? "Active Booking" : "Wallet")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button {
                        dismiss()
                    } label: {
                        HStack {
                            Image(systemName: "chevron.left")
                                .font(.system(size: 16, weight: .medium))
                            Text("Back")
                                .font(.system(size: 16))
                        }
                        .foregroundColor(.primary)
                    }
                }
            }
        }
        .onChange(of: activeContent) { newContent in
            if let newContent = newContent, newContent != currentContent {
                withAnimation(.easeInOut(duration: 0.5)) {
                    currentContent = newContent
                }
            }
        }
    }
}



//import SwiftUI
//
//struct SmoothSheetContainer: View {
//    @Binding var activeContent: SheetContent?
//    @State private var currentContent: SheetContent
//    @EnvironmentObject var homeViewModel: HomeViewModel
//    @Environment(\.dismiss) private var dismiss
//    
//    init(activeContent: Binding<SheetContent?>, initialContent: SheetContent) {
//        self._activeContent = activeContent
//        self._currentContent = State(initialValue: initialContent)
//    }
//    
//    var body: some View {
//        NavigationView {
//            Group {
//                switch currentContent {
//                case .booking:
//                    BookingPageContent(activeContent: $activeContent)
//                        .environmentObject(homeViewModel)
//                case .wallet:
//                    WalletPage()
//                case .profile:
//                    ProfilePageContent(activeContent: $activeContent)
//                case .home:
//                    Text("Welcome to Home")
//                }
//            }
//        }
//    }
//}
