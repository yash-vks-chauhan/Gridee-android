
import SwiftUI

struct NoSpotsAvailableCard: View {
    let onRefresh: () -> Void
    
    var body: some View {
        VStack(spacing: 16) {
            HStack(spacing: 16) {
                ZStack {
                    Circle()
                        .fill(Color.red.opacity(0.1))
                        .frame(width: 48, height: 48)
                    Image(systemName: "car.fill")
                        .font(.system(size: 20, weight: .medium))
                        .foregroundColor(.red)
                }
                
                VStack(alignment: .leading, spacing: 4) {
                    Text("No Spots Available")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundColor(.black)
                    
                    Text("0 Available")
                        .font(.system(size: 14, weight: .medium))
                        .foregroundColor(.red)
                    
                    Text("All parking spots are currently occupied")
                        .font(.system(size: 12))
                        .foregroundColor(.gray)
                }
                
                Spacer()
                
                Button(action: onRefresh) {
                    Text("Refresh")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(.white)
                        .frame(width: 64, height: 36)
                        .background(Color.blue)
                        .cornerRadius(8)
                }
            }
        }
        .padding(20)
        .background(Color.white)
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.08), radius: 4, x: 0, y: 2)
        .padding(.horizontal, 20)
        .padding(.top, 20)
    }
}
