

import SwiftUI

// Placeholder for ActiveBookingSummary used in CreateBookingView
struct ActiveBookingSummary: View {
    let booking: Bookings

    var body: some View {
        VStack(alignment: .leading) {
            Text("Vehicle: \(booking.vehicleNumber ?? "Unknown")")
            Text("Spot ID: \(String(describing: booking.spotId))")
            Text("Lot ID: \(String(describing: booking.lotId))")
            Text("Status: \(booking.status)")
        }
        .padding()
    }
}

// Placeholder for selectedParkingView used in CreateBookingView
struct selectedParkingView: View {
    let parkingSpot: ParkingSpot

    var body: some View {
        VStack(alignment: .leading) {
            Text("Zone Name: \(parkingSpot.zoneName ?? "N/A")")
            Text("Capacity: \(parkingSpot.capacity ?? 0)")
            Text("Available: \(parkingSpot.available ?? 0)")
        }
        .padding()
    }
}

// Placeholder for bookingSummaryView used in CreateBookingView
struct bookingSummaryView: View {
    var body: some View {
        Text("Booking summary will be shown here.")
            .padding()
    }
}
