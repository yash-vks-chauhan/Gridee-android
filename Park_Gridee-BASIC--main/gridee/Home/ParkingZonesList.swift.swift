////
////  ParkingZonesList.swift.swift
////  gridee
////
////  Created by admin85 on 20/09/25.
////
//import SwiftUI
//
//struct ParkingZonesList: View {
//    @EnvironmentObject var homeViewModel: HomeViewModel
//    @State private var selectedSpotId: String?
//    @State private var isBooking: Bool = false
//    
//    var body: some View {
//        VStack(alignment: .leading, spacing: 16) {
//            // Header
//            HStack {
//                Text("Available Parking Zones")
//                    .font(.title2)
//                    .fontWeight(.bold)
//                
//                Spacer()
//                
//                Button("Refresh") {
//                    homeViewModel.fetchAllData()
//                }
//                .font(.caption)
//                .foregroundColor(.blue)
//            }
//            .padding(.horizontal, 20)
//            
//            // Total Stats
//            HStack {
//                VStack(alignment: .leading) {
//                    Text("Total Available")
//                        .font(.caption)
//                        .foregroundColor(.gray)
//                    Text("\(homeViewModel.totalAvailableSpots)")
//                        .font(.title3)
//                        .fontWeight(.bold)
//                        .foregroundColor(.green)
//                }
//                
//                Spacer()
//                
//                VStack(alignment: .trailing) {
//                    Text("Active Zones")
//                        .font(.caption)
//                        .foregroundColor(.gray)
//                    Text("\(homeViewModel.parkingSpots.filter { ($0.available ?? 0) > 0 }.count)")
//                        .font(.title3)
//                        .fontWeight(.bold)
//                        .foregroundColor(.blue)
//                }
//            }
//            .padding(.horizontal, 20)
//            .padding(.vertical, 12)
//            .background(Color(.systemGray6))
//            .cornerRadius(12)
//            .padding(.horizontal, 20)
//            
//            // Parking Cards List
//            ScrollView {
//                LazyVStack(spacing: 0) {
//                    ForEach(homeViewModel.parkingSpots) { spot in
//                        RealTimeParkingCard(
//                            parkingSpot: spot,
//                            totalAvailable: homeViewModel.totalAvailableSpots,
//                            isBooking: isBooking && selectedSpotId == spot.id
//                        ) {
//                            handleBookTap(for: spot)
//                        }
//                    }
//                }
//            }
//            .refreshable {
//                homeViewModel.fetchAllData()
//            }
//        }
//    }
//    
//    private func handleBookTap(for spot: ParkingSpot) {
//        guard (spot.available ?? 0) > 0 else { return }
//        
//        selectedSpotId = spot.id
//        isBooking = true
//        
//        print("🎯 Booking spot: \(spot.displayName) (ID: \(spot.id))")
//        
//        // Simulate booking process
//        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
//            homeViewModel.createQuickBooking(for: spot.id)
//            
//            // Reset booking state after a delay
//            DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
//                isBooking = false
//                selectedSpotId = nil
//            }
//        }
//    }
//}
//
