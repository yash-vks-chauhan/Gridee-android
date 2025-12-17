////
////  ParkingCardsList.swift
////  gridee
////
////  Created by admin85 on 20/09/25.
////
//
//
//import SwiftUI
//
//struct ParkingCardsList: View {
//    let parkingSpots: [ParkingSpot]  // Use your existing array
//    let totalAvailable: Int
//    @State private var bookingStates: [String: Bool] = [:]
//    
//    var body: some View {
//        ScrollView {
//            LazyVStack(spacing: 0) {
//                ForEach(parkingSpots, id: \.id) { spot in  // Use existing id property
//                    RealTimeParkingCard(
//                        parkingSpot: spot,
//                        totalAvailable: totalAvailable,
//                        isBooking: bookingStates[spot.id] ?? false
//                    ) {
//                        handleBookTap(for: spot)
//                    }
//                }
//                .scrollIndicators(.hidden)
//            }
//        }
//        .background(Color(UIColor.systemGroupedBackground))
//    }
//    
//    private func handleBookTap(for spot: ParkingSpot) {
//        bookingStates[spot.id] = true
//        
//        // Add your existing booking logic here
//        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
//            bookingStates[spot.id] = false
//            print("Booked spot: \(spot.id)")
//        }
//    }
//}
//
