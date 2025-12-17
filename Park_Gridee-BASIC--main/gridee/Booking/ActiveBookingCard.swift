

// Add this ActiveBookingCard component to your existing BookingPageContent.swift file
import SwiftUI
struct ActiveBookingCard: View {
    let booking: Bookings
    @State private var timeLeft: TimeInterval = 0
    @State private var timer: Timer?
    @State private var showingQRCode = false
    
    private var statusColor: Color {
        return .green
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Spot: \(booking.spotId)")
                        .font(.headline)
                        .fontWeight(.semibold)
                    
                    HStack {
                        Circle()
                            .fill(statusColor)
                            .frame(width: 8, height: 8)
                        
                        Text("Active")
                            .font(.caption)
                            .foregroundColor(statusColor)
                            .fontWeight(.medium)
                    }
                }
                
                Spacer()
                //added this now for the qr things
                Button(action: {
                    showingQRCode = true
                }) {
                    HStack(spacing: 8) {
                        Image(systemName: "qrcode")
                            .font(.system(size: 16, weight: .medium))
                        Text("Show QR Code")
                            .font(.system(size: 14, weight: .medium))
                    }
                    .foregroundColor(.white)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)
                    .background(Color.blue)
                    .cornerRadius(8)
                }
                .sheet(isPresented: $showingQRCode) {
                    QRCodeDisplayView(booking: booking)
                }
                
                VStack(alignment: .trailing, spacing: 4) {
                    Text("₹\(String(format: "%.2f", booking.totalAmount))")
                        .font(.title3)
                        .fontWeight(.bold)
                        .foregroundColor(.primary)
                    
                    // Live Timer
                    Text(formatTimeLeft(timeLeft))
                        .font(.caption)
                        .fontWeight(.semibold)
                        .foregroundColor(.orange)
                }
            }
            
            // Parking and Vehicle details
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Label("Parking Spot", systemImage: "parkingsign")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Text(booking.spotId)
                        .font(.body)
                        .fontWeight(.medium)
                }
                
                Spacer()
                
                VStack(alignment: .trailing, spacing: 4) {
                    Label("Vehicle", systemImage: "car")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Text(booking.vehicleNumber ?? "N/A")
                        .font(.body)
                        .fontWeight(.medium)
                }
            }
            
            // Timing information
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Check-in")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Text(formatDateTime(booking.checkInTime))
                        .font(.subheadline)
                        .fontWeight(.medium)
                }
                
                Spacer()
                
                VStack(alignment: .trailing, spacing: 4) {
                    Text("Check-out")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Text(formatDateTime(booking.checkOutTime))
                        .font(.subheadline)
                        .fontWeight(.medium)
                }
            }
            
            // Progress bar showing elapsed time
            VStack(alignment: .leading, spacing: 6) {
                HStack {
                    Text("Duration: \(String(format: "%.1f", booking.totalHours))h")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    Spacer()
                    
                    if timeLeft > 0 {
                        Text("Time left: \(formatTimeLeft(timeLeft))")
                            .font(.caption)
                            .foregroundColor(.orange)
                            .fontWeight(.medium)
                    } else {
                        Text("Expired")
                            .font(.caption)
                            .foregroundColor(.red)
                            .fontWeight(.medium)
                    }
                }
                
                GeometryReader { geometry in
                    ZStack(alignment: .leading) {
                        Rectangle()
                            .fill(Color(.systemGray5))
                            .frame(height: 4)
                            .cornerRadius(2)
                        
                        Rectangle()
                            .fill(statusColor)
                            .frame(width: progressWidth(totalWidth: geometry.size.width), height: 4)
                            .cornerRadius(2)
                    }
                }
                .frame(height: 4)
            }
            
            // Action Buttons for Active Bookings
            HStack(spacing: 12) {
                Button("Extend Time") {
                    // Extend time action
                }
                .font(.caption)
                .foregroundColor(.white)
                .padding(.horizontal, 16)
                .padding(.vertical, 6)
                .background(Color.blue)
                .cornerRadius(6)
                
                Button("End Parking") {
                    // End parking action
                }
                .font(.caption)
                .foregroundColor(.white)
                .padding(.horizontal, 16)
                .padding(.vertical, 6)
                .background(Color.red)
                .cornerRadius(6)
                
                Spacer()
            }
        }
        .padding()
        .background(Color.white)
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.1), radius: 2, x: 0, y: 1)
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(statusColor.opacity(0.3), lineWidth: 2)
        )
        .onAppear {
            startTimer()
        }
        .onDisappear {
            timer?.invalidate()
        }
    }
    
    private func startTimer() {
        updateTimeLeft()
        timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { _ in
            updateTimeLeft()
        }
    }
    
    private func updateTimeLeft() {
        timeLeft = booking.timeLeft
        if timeLeft <= 0 {
            timer?.invalidate()
        }
    }
    
    private func progressWidth(totalWidth: CGFloat) -> CGFloat {
        guard let checkIn = booking.checkInDate,
              let checkOut = booking.checkOutDate else { return 0 }
        
        let totalDuration = checkOut.timeIntervalSince(checkIn)
        let elapsed = Date().timeIntervalSince(checkIn)
        let progress = min(max(elapsed / totalDuration, 0), 1)
        
        return totalWidth * progress
    }
    
    private func formatDateTime(_ dateString: String?) -> String {
        guard let dateString = dateString,
              let date = ISO8601DateFormatter().date(from: dateString) else {
            return "N/A"
        }
        
        let formatter = DateFormatter()
        formatter.dateStyle = .short
        formatter.timeStyle = .short
        return formatter.string(from: date)
    }
    
    private func formatTimeLeft(_ timeLeft: TimeInterval) -> String {
        if timeLeft <= 0 {
            return "Expired"
        }
        
        let hours = Int(timeLeft) / 3600
        let minutes = (Int(timeLeft) % 3600) / 60
        let seconds = Int(timeLeft) % 60
        
        return String(format: "%02d:%02d:%02d", hours, minutes, seconds)
    }
    
    //test
}
