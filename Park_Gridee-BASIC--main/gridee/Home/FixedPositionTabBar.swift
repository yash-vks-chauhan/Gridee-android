
import SwiftUI

struct FixedPositionTabBar: View {
    @Binding var selectedTab: Int
    @Binding var activeSheet: SheetContent?
    
    private let tabHeight: CGFloat = 80
    private let tabs = [
        ("house", "Home"),
        ("calendar", "Bookings"),
        ("wallet.pass", "Wallet"),
        ("person", "Profile")
    ]
    
    var body: some View {
        HStack(spacing: 0) {
            ForEach(0..<tabs.count, id: \.self) { index in
                Button(action: {
                    handleTabSelection(index)
                }) {
                    VStack(spacing: 6) {
                        Image(systemName: tabs[index].0)
                            .font(.system(size: 20, weight: .medium))
                            .foregroundColor(selectedTab == index ? .black : .gray)
                        
                        Text(tabs[index].1)
                            .font(.system(size: 11, weight: .medium))
                            .foregroundColor(selectedTab == index ? .black : .gray)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .contentShape(Rectangle())
                }
                .buttonStyle(PlainButtonStyle())
            }
        }
        .frame(height: tabHeight)
        .padding(.horizontal, 20)
        .padding(.vertical, 12)
        .background(
            Color.white
                .shadow(color: .black.opacity(0.06), radius: 4, x: 0, y: -2)
        )
    }
    
    private func handleTabSelection(_ index: Int) {
        withAnimation(.easeInOut(duration: 0.3)) {
            selectedTab = index
            
            switch index {
            case 0:
                activeSheet = nil
            case 1:
                activeSheet = .booking
            case 2:
                activeSheet = .wallet
            case 3:
                activeSheet = .profile
            default:
                activeSheet = nil
            }
        }
    }
}
