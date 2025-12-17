

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
    @StateObject private var themeManager = ThemeManager.shared  // ✅ Create instance

    var body: some View {
        Group {
            if authViewModel.isAuthenticated {
                MainTabView()
                    .environmentObject(authViewModel)
                    .environmentObject(themeManager)
            } else {
                LoginView()
                    .environmentObject(authViewModel)
                    .environmentObject(themeManager)
            }
        }
        .preferredColorScheme(themeManager.isDarkMode ? .dark : .light)  // ✅ Apply theme
    }
}

// MARK: - Main Tab Navigation
//struct MainTabView: View {
//    @StateObject private var homeViewModel = HomeViewModel()
//    @EnvironmentObject var authViewModel: AuthViewModel
//    @EnvironmentObject var themeManager: ThemeManager  // ✅ Added
//    @State private var selectedTab: SheetContent = .home
//    
//    var body: some View {
//        TabView(selection: $selectedTab) {
//            HomeContentView()
//                .environmentObject(homeViewModel)
//                .environmentObject(authViewModel)
//                .tabItem {
//                    Label("Home", systemImage: "house")
//                }
//                .tag(SheetContent.home)
//
//            BookingPageContent(activeContent: .constant(selectedTab))
//                .environmentObject(homeViewModel)
//                .tabItem {
//                    Label("Bookings", systemImage: "calendar")
//                }
//                .tag(SheetContent.booking)
//
//            WalletPage()
//                .tabItem {
//                    Label("Wallet", systemImage: "wallet.pass")
//                }
//                .tag(SheetContent.wallet)
//
//            ProfilePageContent(activeContent: .constant(selectedTab))
//                .environmentObject(authViewModel)
//                .environmentObject(themeManager)  // ✅ Pass to Profile
//                .tabItem {
//                    Label("Profile", systemImage: "person.circle")
//                }
//                .tag(SheetContent.profile)
//        }
//        .onAppear {
//            homeViewModel.startRealTimeUpdates()
//            homeViewModel.setAuthViewModel(authViewModel)
//            
//            DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
//                homeViewModel.showingTimeSlotModal = true
//            }
//        }
//        .onDisappear {
//            homeViewModel.stopRealTimeUpdates()
//        }
//    }
//}
struct MainTabView: View {
    @StateObject private var homeViewModel = HomeViewModel()
    @EnvironmentObject var authViewModel: AuthViewModel
    @EnvironmentObject var themeManager: ThemeManager
    @State private var selectedTab: SheetContent = .home
    @State private var selectedBookingTab: Int = 0  // ✅ ADD THIS
    
    var body: some View {
        TabView(selection: $selectedTab) {
            HomeContentView(
                selectedTab: $selectedTab,
                selectedBookingTab: $selectedBookingTab  // ✅ PASS IT
            )
                .environmentObject(homeViewModel)
                .environmentObject(authViewModel)
                .tabItem {
                    Label("Home", systemImage: "house")
                }
                .tag(SheetContent.home)

            BookingPageContent(
                activeContent: .constant(selectedTab),
                initialSelectedTab: selectedBookingTab  // ✅ PASS IT
            )
                .environmentObject(homeViewModel)
                .tabItem {
                    Label("Bookings", systemImage: "calendar")
                }
                .tag(SheetContent.booking)

            WalletPage()
                .tabItem {
                    Label("Wallet", systemImage: "wallet.pass")
                }
                .tag(SheetContent.wallet)

            ProfilePageContent(activeContent: .constant(selectedTab))
                .environmentObject(authViewModel)
                .environmentObject(themeManager)
                .tabItem {
                    Label("Profile", systemImage: "person.circle")
                }
                .tag(SheetContent.profile)
        }
        .onAppear {
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
//    let ac: AppColors
    @Binding var selectedTab: SheetContent  // ✅ ADD THIS
    @Binding var selectedBookingTab: Int
    @State private var showingPendingBooking = false
    @State private var showingActiveBooking = false  // ✅ ADD THIS

//
    @State private var showingBookingSuccess = false
    @State private var selectedParkingSpot: ParkingSpot?
    @State private var showingCreateBooking = false
    @State private var selectedBooking: Bookings?
    @State private var sheetType: BookingSheetType?
    enum BookingSheetType: Identifiable {
//            case activeQR
//            case pendingDetails
        case qrCode
            
            var id: Int { hashValue }
        }// ✅ ADD THIS (if not already there)

    
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
                        }
                        .background(AppColors.primaryBackground)
                    } else {
                        NoSpotsAvailableCard(onRefresh: {
                            homeViewModel.fetchAllData()
                        })
                    }
//                    if !homeViewModel.userBookings.isEmpty || homeViewModel.walletBalance > 0 {
//                        UserStatsSection(
//                            activeBookings: homeViewModel.activeBookingsCount,
//                            pendingBookings: homeViewModel.pendingBookingsCount,
//                            onPendingTap: { }  // ❌ Empty action
//                        )
//                    }

                    
                    if !homeViewModel.userBookings.isEmpty || homeViewModel.walletBalance > 0 {
                        UserStatsSection(
                            activeBookings: homeViewModel.activeBookingsCount,
                            pendingBookings: homeViewModel.pendingBookingsCount,
                            onActiveTap: {
                                // ✅ Show QR Code for active booking
                                if let firstActive = homeViewModel.userBookings.first(where: { $0.status.lowercased() == "active" }) {
                                    selectedBooking = firstActive
                                    sheetType = .qrCode
                                }
                            },
                            onPendingTap: {
                                // ✅ Show QR Code for pending booking
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
                    selectedTime: $homeViewModel.selectedTimeSlot,
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
                .transition(.scale.combined(with: .opacity))
                .zIndex(999)
            }
        }
        .background(AppColors.primaryBackground)
        .sheet(isPresented: $showingCreateBooking) {
            if let parkingSpot = selectedParkingSpot {
                CreateBookingView(selectedParking: parkingSpot)
                    .environmentObject(homeViewModel)
            }
        }
//        .sheet(isPresented: $showingPendingBooking) {
//            if let booking = selectedBooking {
//                BookingDetailView(booking: booking)
//                    .environmentObject(homeViewModel)
//
        // ✅ UPDATED: Show QR Code for active bookings
        .sheet(isPresented: $showingActiveBooking) {
            if let booking = selectedBooking {
                QRCodeDisplayView(booking: booking)
                    .environmentObject(homeViewModel)
            }
        }

        // ✅ Show booking details for pending bookings
        
            .sheet(item: $sheetType) { type in
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
