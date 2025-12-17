


import SwiftUI

@main
struct grideeApp: App {
    @StateObject private var authViewModel = AuthViewModel()
    @StateObject private var homeViewModel = HomeViewModel()
    @State private var activeContent: SheetContent? = .home
    @State private var showSplash = true
    @StateObject private var themeManager = ThemeManager.shared
    
    var body: some Scene {
        WindowGroup {
            ZStack {
                if showSplash {
                    // ✅ SPLASH SCREEN WITH SMOOTH TRANSITION
                    SplashView()
                        .transition(.asymmetric(
                            insertion: .opacity,
                            removal: .opacity.combined(with: .scale(scale: 0.95))
                        ))
                        .zIndex(1)
                        .onAppear {
                            // ✅ SMOOTH TRANSITION TO MAIN APP
                            DispatchQueue.main.asyncAfter(deadline: .now() + 3.0) {
                                withAnimation(.easeInOut(duration: 0.8)) {
                                    showSplash = false
                                }
                            }
                        }
                } else {
                    // ✅ MAIN APP WITH ELEGANT ENTRANCE
                    ContentView(/*activeContent: $activeContent*/)
                        .environmentObject(authViewModel)
                        .environmentObject(homeViewModel)
                        .environmentObject(themeManager) //dark theme ke liye hai
                        .preferredColorScheme(themeManager.isDarkMode ? .dark : .light)//dark theme hai
                        .transition(.asymmetric(
                            insertion: .opacity.combined(with: .scale(scale: 1.05)),
                            removal: .opacity
                        ))
                        .zIndex(0)
                }
            }
            .onAppear {
                setupApp()
            }
            .onChange(of: authViewModel.isAuthenticated) { isAuthenticated in
                handleAuthenticationChange(isAuthenticated)
            }
        }
    }
    
    private func setupApp() {
        homeViewModel.setAuthViewModel(authViewModel)
        print("🔗 App.swift: Connected HomeViewModel to AuthViewModel")
        print("🎬 App.swift: Black & White splash screen displayed")
    }
    
    private func handleAuthenticationChange(_ isAuthenticated: Bool) {
        if isAuthenticated {
            homeViewModel.setAuthViewModel(authViewModel)
            print("✅ App.swift: Re-connected HomeViewModel after login")
            if let userId = authViewModel.getCurrentUserId() {
                print("✅ App.swift: User logged in with ID: \(userId)")
            }
        } else {
            print("🚪 App.swift: User logged out")
        }
    }
}
