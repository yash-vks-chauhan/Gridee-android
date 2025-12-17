
import SwiftUI

struct ProfileBottomTabBar: View {
    @Binding var activeContent: SheetContent?
    
    var body: some View {
        HStack(spacing: 0) {
            ProfileTabButton(icon: "house", title: "Home", isSelected: false) {
                activeContent = nil
            }
            ProfileTabButton(icon: "calendar", title: "Bookings", isSelected: false) {
                withAnimation(.easeInOut(duration: 0.5)) {
                    activeContent = .booking
                }
            }
            ProfileTabButton(icon: "wallet.pass", title: "Wallet", isSelected: false) {
                withAnimation(.easeInOut(duration: 0.5)) {
                    activeContent = .wallet
                }
            }
            ProfileTabButton(icon: "person.circle", title: "Profile", isSelected: true) {
                print("Already on Profile")
            }
        }
        .frame(height: 80)
        .padding(.horizontal, 20)
        .padding(.vertical, 12)
        .background(Color.white.shadow(color: .black.opacity(0.06), radius: 4, x: 0, y: -2))
    }
}

struct ProfileTabButton: View {
    let icon: String
    let title: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 6) {
                Image(systemName: icon)
                    .font(.system(size: 20, weight: .medium))
                    .foregroundColor(isSelected ? .black : .gray)
                Text(title)
                    .font(.system(size: 11, weight: .medium))
                    .foregroundColor(isSelected ? .black : .gray)
            }
            .frame(maxWidth: .infinity)
            .contentShape(Rectangle())
        }
        .buttonStyle(PlainButtonStyle())
    }
}
