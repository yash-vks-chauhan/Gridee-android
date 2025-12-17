//
//import SwiftUI
//
//struct RealTimeHeaderView: View {
//    let notificationCount: Int
//    let userInitials: String
//    let lastUpdated: Date
//    
//    private var lastUpdatedText: String {
//        let formatter = DateFormatter()
//        formatter.timeStyle = .short
//        return "Updated \(formatter.string(from: lastUpdated))"
//    }
//    
//    var body: some View {
//        VStack(spacing: 8) {
//            HStack(spacing: 24) {
//                HStack(spacing: 8) {
//                    Image(systemName: "bell.badge")
//                        .font(.system(size: 16, weight: .medium))
//                        .foregroundColor(.white)
//                    Text("\(notificationCount)")
//                        .font(.system(size: 16, weight: .bold))
//                        .foregroundColor(.white)
//                }
//                .padding(.horizontal, 16)
//                .padding(.vertical, 8)
//                .background(notificationCount > 0 ? Color.black : Color.gray.opacity(0.6))
//                .clipShape(Capsule())
//                
//                Spacer()
//                
//                HStack(spacing: 6) {
//                    Circle()
//                        .fill(Color.green)
//                        .frame(width: 8, height: 8)
//                    Text("LIVE")
//                        .font(.caption2)
//                        .fontWeight(.semibold)
//                        .foregroundColor(.green)
//                }
//                
//                Text(userInitials)
//                    .font(.system(size: 16, weight: .bold))
//                    .foregroundColor(.white)
//                    .frame(width: 40, height: 40)
//                    .background(Circle().fill(Color.black))
//            }
//            
//            HStack {
//                Text(lastUpdatedText)
//                    .font(.caption)
//                    .foregroundColor(.gray)
//                Spacer()
//            }
//        }
//        .padding(.horizontal, 20)
//        .padding(.vertical, 12)
//        .background(Color.white)
//    }
//}
//


import SwiftUI

struct RealTimeHeaderView: View {
    @Environment(\.colorScheme) var colorScheme
    let notificationCount: Int
    let userInitials: String
    let lastUpdated: Date
    
    private var headerBackground: Color {
        colorScheme == .dark ? Color(UIColor.systemBackground) : .white
    }
    
    private var textColor: Color {
        Color(UIColor.label)
    }
    
    var body: some View {
        HStack {
            // Notification Button
            Button(action: {}) {
                HStack(spacing: 4) {
                    Image(systemName: "bell.fill")
                        .font(.system(size: 14))
                    Text("\(notificationCount)")
                        .font(.system(size: 14, weight: .semibold))
                }
                .foregroundColor(Color(UIColor.secondaryLabel))
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(Color(UIColor.secondarySystemBackground))
                .cornerRadius(20)
            }
            
            Spacer()
            
            // Live Indicator
            HStack(spacing: 6) {
                Circle()
                    .fill(Color.green)
                    .frame(width: 8, height: 8)
                Text("LIVE")
                    .font(.system(size: 12, weight: .bold))
                    .foregroundColor(.green)
            }
            
            Spacer()
            
            // User Profile Button
            Button(action: {}) {
                Text(userInitials)
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(.white)
                    .frame(width: 40, height: 40)
                    .background(Color.black)
                    .clipShape(Circle())
            }
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 12)
        .background(headerBackground)
        
        // Updated timestamp
        Text("Updated \(formatTime(lastUpdated))")
            .font(.caption)
            .foregroundColor(Color(UIColor.secondaryLabel))
            .padding(.horizontal, 20)
            .padding(.bottom, 8)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(headerBackground)
    }
    
    private func formatTime(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "h:mm a"
        return formatter.string(from: date)
    }
}
