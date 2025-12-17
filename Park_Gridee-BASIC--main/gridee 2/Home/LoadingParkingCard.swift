//
//import SwiftUI
//
//struct LoadingParkingCard: View {
//    var body: some View {
//        HStack(spacing: 16) {
//            ZStack {
//                Circle()
//                    .fill(Color.gray.opacity(0.3))
//                    .frame(width: 48, height: 48)
//                ProgressView()
//                    .progressViewStyle(CircularProgressViewStyle(tint: .gray))
//                    .frame(width: 24, height: 24)
//            }
//            
//            VStack(alignment: .leading, spacing: 4) {
//                Text("Loading parking data...")
//                    .font(.system(size: 18, weight: .semibold))
//                    .foregroundColor(.gray)
//                
//                Text("Getting real-time availability")
//                    .font(.system(size: 14, weight: .medium))
//                    .foregroundColor(.gray)
//            }
//            
//            Spacer()
//        }
//        .padding(20)
//        .background(Color.white)
//        .cornerRadius(16)
//        .shadow(color: .black.opacity(0.08), radius: 4, x: 0, y: 2)
//        .padding(.horizontal, 20)
//        .padding(.top, 20)
//    }
//}
import SwiftUI

struct LoadingParkingCard: View {
    var body: some View {
        HStack(spacing: 16) {
            ZStack {
                Circle()
                    .fill(Color.gray.opacity(0.3))
                    .frame(width: 48, height: 48)
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle(tint: .gray))
                    .frame(width: 24, height: 24)
            }
            
            VStack(alignment: .leading, spacing: 4) {
                Text("Loading parking data...")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(.gray)
                
                Text("Getting real-time availability")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(.gray)
            }
            
            Spacer()
        }
        .padding(20)
        .background(Color.white)
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.08), radius: 4, x: 0, y: 2)
        .padding(.horizontal, 20)
        .padding(.top, 20)
    }
}
