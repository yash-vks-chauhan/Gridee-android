import SwiftUI

enum SheetContent: Identifiable {
    case booking
    case wallet
    case profile
    case home

    var id: Int { hashValue }
}

// MARK: - Root App View
struct ContentView: View {
    @StateObject private var authViewModel = AuthViewModel()
    @StateObject private var themeManager = ThemeManager.shared

    var body: some View {
        Group {
            if authViewModel.isAuthenticated {
                // ✅ CRITICAL FIX: Check operator role first
                if authViewModel.userRole == .admin {
                    CameraView()
                        .environmentObject(authViewModel)
                        .environmentObject(themeManager)
                } else {
                    MainTabView()
                        .environmentObject(authViewModel)
                        .environmentObject(themeManager)
                }
            } else {
                LoginView()
                    .environmentObject(authViewModel)
                    .environmentObject(themeManager)
            }
        }
        .preferredColorScheme(themeManager.isDarkMode ? .dark : .light)
    }
}
// MARK: - Main Tab Navigation (FOR USERS ONLY)
struct MainTabView: View {
    @StateObject private var homeViewModel = HomeViewModel()
    @EnvironmentObject var authViewModel: AuthViewModel
    @EnvironmentObject var themeManager: ThemeManager
    @State private var selectedTab: SheetContent = .home
    @State private var selectedBookingTab: Int = 0
    
    var body: some View {
        TabView(selection: $selectedTab) {
            HomeContentView(
                selectedTab: $selectedTab,
                selectedBookingTab: $selectedBookingTab
            )
                .environmentObject(homeViewModel)
                .environmentObject(authViewModel)
                .tabItem {
                    // Remove AnyView wrapper; use SwiftUI's Label directly
                    Label("Home", systemImage: "house")
                }
                .tag(SheetContent.home)

            BookingPageContent(
                activeContent: .constant(selectedTab),
                initialSelectedTab: selectedBookingTab
            )
                .environmentObject(homeViewModel)
                .tabItem {
                    Label("Bookings", systemImage: "calendar")
                }
                .tag(SheetContent.booking)

            WalletPage()
                .environmentObject(authViewModel)
                .tabItem {
                    Label("Wallet", systemImage: "wallet.pass")
                }
                .tag(SheetContent.wallet)

            ProfilePageContent(activeContent: .constant(selectedTab))
                .environmentObject(authViewModel)
                .environmentObject(homeViewModel)
                .environmentObject(themeManager)
                .tabItem {
                    Label("Profile", systemImage: "person.circle")
                }
                .tag(SheetContent.profile)
        }
        .onAppear {
            APIService.shared.setAuthViewModel(authViewModel)
            homeViewModel.startRealTimeUpdates()
            homeViewModel.setAuthViewModel(authViewModel)
            
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                homeViewModel.showingTimeSlotModal = true
            }
        }
        .onDisappear {
            homeViewModel.stopRealTimeUpdates()
        }
    }
}

// MARK: - Home Content View
struct HomeContentView: View {
    @EnvironmentObject var homeViewModel: HomeViewModel
    @EnvironmentObject var authViewModel: AuthViewModel
    @Binding var selectedTab: SheetContent
    @Binding var selectedBookingTab: Int
    @State private var showingPendingBooking = false
    @State private var showingActiveBooking = false
    @State private var showingBookingSuccess = false
    @State private var selectedParkingSpot: ParkingSpot?
    @State private var showingCreateBooking = false
    @State private var selectedBooking: Bookings?
    @State private var sheetType: BookingSheetType?
    
    enum BookingSheetType: Identifiable {
        case qrCode
        var id: Int { hashValue }
    }
    
    var body: some View {
        ZStack {
            VStack(spacing: 0) {
                RealTimeHeaderView(
                    notificationCount: homeViewModel.notificationCount,
                    userInitials: currentUserInitials,
                    lastUpdated: homeViewModel.lastUpdated
                )
                
                ParkingSearchBar(
                    searchText: $homeViewModel.searchText,
                    isSearching: $homeViewModel.isSearching
                )
                .padding(.bottom, 8)

                if homeViewModel.isSearching {
                    ScrollView {
                        ParkingSearchResults(
                            searchText: homeViewModel.searchText,
                            parkingSpots: homeViewModel.parkingSpots,
                            onBookTap: { spot in
                                selectedParkingSpot = spot
                                showingCreateBooking = true
                            }
                        )
                        .padding(.top, 8)
                        
                        Spacer(minLength: 120)
                    }
                    .background(AppColors.primaryBackground)
                } else {
                    if homeViewModel.isLoading && homeViewModel.parkingSpots.isEmpty {
                        LoadingParkingCard()
                    } else if !homeViewModel.parkingSpots.isEmpty {
                        ScrollView {
                            LazyVStack(spacing: 0) {
                                ForEach(homeViewModel.parkingSpots, id: \.id) { spot in
                                    RealTimeParkingCard(
                                        parkingSpot: spot,
                                        totalAvailable: homeViewModel.totalAvailableSpots,
                                        isBooking: homeViewModel.isBooking(spotId: spot.id),
                                        onBookTap: {
                                            selectedParkingSpot = spot
                                            showingCreateBooking = true
                                        }
                                    )
                                }
                                
                                Spacer(minLength: 120)
                            }
                            .scrollIndicators(.hidden)
                        }
                        .background(AppColors.primaryBackground)
                    } else {
                        NoSpotsAvailableCard(onRefresh: {
                            homeViewModel.fetchAllData()
                        })
                    }
                    
                    if !homeViewModel.userBookings.isEmpty || homeViewModel.walletBalance > 0 {
                        UserStatsSection(
                            activeBookings: homeViewModel.activeBookingsCount,
                            pendingBookings: homeViewModel.pendingBookingsCount,
                            onActiveTap: {
                                if let firstActive = homeViewModel.userBookings.first(where: { $0.status.lowercased() == "active" }) {
                                    selectedBooking = firstActive
                                    sheetType = .qrCode
                                }
                            },
                            onPendingTap: {
                                if let firstPending = homeViewModel.userBookings.first(where: { $0.status.lowercased() == "pending" }) {
                                    selectedBooking = firstPending
                                    sheetType = .qrCode
                                }
                            }
                        )
                    }
                }

                if !homeViewModel.errorMessage.isEmpty {
                    ErrorBanner(
                        message: homeViewModel.errorMessage,
                        onDismiss: { homeViewModel.clearError() }
                    )
                }
                
                Spacer()
            }
            .blur(radius: homeViewModel.showingTimeSlotModal ? 6 : 0)
            .disabled(homeViewModel.showingTimeSlotModal)
            
            if homeViewModel.showingTimeSlotModal {
                Color.black.opacity(0.3)
                    .ignoresSafeArea()
                
                TimeSlotModalView(
                    selectedStartTime: $homeViewModel.selectedStartTime,
                    selectedEndTime: $homeViewModel.selectedEndTime,
                    isPresented: $homeViewModel.showingTimeSlotModal,
                    onConfirm: { startTime, endTime in
                        homeViewModel.selectedStartTime = startTime
                        homeViewModel.selectedEndTime = endTime
                        
                        homeViewModel.fetchAvailableSpots(
                            lotId: "default-lot",
                            startTime: startTime,
                            endTime: endTime
                        )
                    }
                )
                .transition(.scale.combined(with: AnyTransition.opacity))
                .zIndex(999)
            }
        }
        .background(AppColors.primaryBackground)
        .fullScreenCover(isPresented: $showingCreateBooking) {
            if let parkingSpot = selectedParkingSpot {
                CreateBookingView(selectedParking: parkingSpot)
                    .environmentObject(homeViewModel)
            }
        }
        .sheet(isPresented: $showingActiveBooking) {
            if let booking = selectedBooking {
                QRCodeDisplayView(booking: booking)
                    .environmentObject(homeViewModel)
            }
        }
        .sheet(item: $sheetType) { _ in
            if let booking = selectedBooking {
                QRCodeDisplayView(booking: booking)
                    .environmentObject(homeViewModel)
            }
        }
        .refreshable {
            homeViewModel.fetchAllData()
        }
        .alert("Booking Successful! 🎉", isPresented: $showingBookingSuccess) {
            Button("OK") { }
        } message: {
            Text("Your parking spot has been reserved. Check the Bookings tab for details.")
        }
    }
    
    private var currentUserInitials: String {
        guard let user = authViewModel.currentUser else { return "RS" }
        let components = user.name.split(separator: " ")
        let firstInitial = components.first?.first ?? "R"
        let lastInitial = components.count > 1 ? String(components.last?.first ?? "S") : "S"
        return String(firstInitial) + String(lastInitial)
    }
}
