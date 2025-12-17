

//import SwiftUI
//import Firebase
//
//@main
//struct GrideeApp: App {
//    // Connect AppDelegate for Firebase and notifications
//    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
//    
//    // View models
//    @StateObject private var authViewModel = AuthViewModel()
//    
//    // UI state
//    @State private var showSplash = true
//    @State private var selectedTab = 0
//    
//    var body: some Scene {
//        WindowGroup {
//            ZStack {
//                // Main content
//                if showSplash {
//                    SplashView()
//                        .onAppear {
//                            DispatchQueue.main.asyncAfter(deadline: .now() + 3) {
//                                withAnimation(.easeOut(duration: 0.5)) {
//                                    showSplash = false
//                                }
//                            }
//                        }
//                } else if authViewModel.isAuthenticated {
//                    if authViewModel.userRole == .transformer{
//                        AdminQRScannerView()
//                            .environmentObject(authViewModel)
//                    } else {
//                        ContentView()
//                            .environmentObject(authViewModel)
//                    }
//                } else {
//                    LoginView()
//                        .environmentObject(authViewModel)
//                }
//            }
//            .onReceive(NotificationCenter.default.publisher(for: NSNotification.Name("OpenBooking"))) { notification in
//                // Handle notification tap to open booking details
//                if let bookingId = notification.userInfo?["bookingId"] as? String {
//                    print("📬 Opening booking: \(bookingId)")
//                    // Navigate to booking details
//                    // You can update selectedTab or use NavigationLink
//                }
//            }
//        }
//    }
//}



import SwiftUI
import Firebase

@main
struct GrideeApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    @StateObject private var authViewModel = AuthViewModel()
    @StateObject private var themeManager = ThemeManager.shared // ✅ Added
    @State private var showSplash = true
    @State private var selectedTab = 0
    
    var body: some Scene {
        WindowGroup {
            ZStack {
                if showSplash {
                    SplashView()
                        .onAppear {
                            DispatchQueue.main.asyncAfter(deadline: .now() + 3) {
                                withAnimation(.easeOut(duration: 0.5)) {
                                    showSplash = false
                                }
                            }
                        }
                } else if authViewModel.isAuthenticated {
                    if authViewModel.userRole == .transformer {
                        AdminQRScannerView()
                            .environmentObject(authViewModel)
                            .environmentObject(themeManager)
                    } else {
                        ContentView()
                            .environmentObject(authViewModel)
                            .environmentObject(themeManager)
                    }
                } else {
                    LoginView()
                        .environmentObject(authViewModel)
                        .environmentObject(themeManager)
                }
            }
            .preferredColorScheme(themeManager.isDarkMode ? .dark : .light) // ✅ THIS IS CRITICAL
            .onReceive(NotificationCenter.default.publisher(for: NSNotification.Name("OpenBooking"))) { notification in
                if let bookingId = notification.userInfo?["bookingId"] as? String {
                    print("📬 Opening booking: \(bookingId)")
                }
            }
        }
    }
}
