 
import SwiftUI

struct SheetContainerView: View {
    @Binding var activeContent: SheetContent?
    let initialContent: SheetContent
    

   var body: some View {
        NavigationView {
            switch initialContent {
            case .profile:
                ProfilePageContent(activeContent: $activeContent)
                Text("Profile View")
                    .navigationTitle("Profile")
                
            case .wallet:
                WalletPage()
                Text("Wallet View")
                    .navigationTitle("Wallet")
                
            case .booking:
                       // FIXED: Remove ParkingSelectionView() and go directly to CreateBookingView
                BookingPageContent(activeContent: $activeContent)
                    .navigationTitle("Bookings")
            
            case .home:
                Text("Home View")
                    .navigationTitle("Home")
            }
        }
    }
}
// MARK: - Booking Page Content
struct bb: View {
    @Binding var activeContent: SheetContent?
    @EnvironmentObject var authViewModel: AuthViewModel
    
    
    var body: some View {
        VStack(spacing: 20) {
            Text("My Bookings")
                .font(.largeTitle)
                .fontWeight(.bold)
                .padding(.top, 20)
            
            // Active Booking Card
            VStack(spacing: 16) {
                Text("Active Booking")
                    .font(.headline)
                    .foregroundColor(.green)
                
                VStack(spacing: 12) {
                    HStack {
                        Text("Location:")
                        Spacer()
                        Text("TP Avenue Parking")
                            .fontWeight(.semibold)
                    }
                    
                    HStack {
                        Text("Spot:")
                        Spacer()
                        Text("A-23")
                            .fontWeight(.semibold)
                    }
                    
                    HStack {
                        Text("Duration:")
                        Spacer()
                        Text("2 hours")
                            .fontWeight(.semibold)
                    }
                    
                    HStack {
                        Text("Amount:")
                        Spacer()
                        Text("₹50.00")
                            .fontWeight(.semibold)
                            .foregroundColor(.green)
                    }
                }
                
                Button("End Booking") {
                    print("End Booking tapped")
                }
                .padding()
                .frame(maxWidth: .infinity)
                .background(Color.red)
                .foregroundColor(.white)
                .cornerRadius(12)
            }
            .padding()
            .background(Color.white)
            .cornerRadius(16)
            .shadow(color: .black.opacity(0.1), radius: 4, x: 0, y: 2)
            
            // Recent Bookings
            VStack(alignment: .leading, spacing: 12) {
                Text("Recent Bookings")
                    .font(.headline)
                    .padding(.horizontal)
                
                VStack(spacing: 8) {
                    BookingRow(location: "Mall Parking", date: "Sept 10", amount: "₹25.00", status: "Completed")
                    BookingRow(location: "Office Complex", date: "Sept 09", amount: "₹45.00", status: "Completed")
                    BookingRow(location: "Airport Parking", date: "Sept 08", amount: "₹120.00", status: "Completed")
                }
                .padding()
                .background(Color.white)
                .cornerRadius(12)
                .shadow(color: .black.opacity(0.1), radius: 2, x: 0, y: 1)
            }
            
            Spacer()
            
            // Bottom Tab Bar
            BookingBottomTabBar(activeContent: $activeContent)
        }
        .padding()
        .background(Color(UIColor.systemGroupedBackground))
        .navigationTitle("Bookings")
        .navigationBarTitleDisplayMode(.inline)
    }
}

// MARK: - Booking Row
struct BookingRow: View {
    let location: String
    let date: String
    let amount: String
    let status: String
    
    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(location)
                    .font(.body)
                    .fontWeight(.medium)
                Text(date)
                    .font(.caption)
                    .foregroundColor(.gray)
            }
            
            Spacer()
            
            VStack(alignment: .trailing, spacing: 2) {
                Text(amount)
                    .font(.body)
                    .fontWeight(.semibold)
                Text(status)
                    .font(.caption)
                    .foregroundColor(.green)
            }
        }
    }
}

// MARK: - Wallet Bottom Tab Bar
struct WalletBottomTabBar: View {
    @Binding var activeContent: SheetContent?
    
    var body: some View {
        HStack(spacing: 0) {
            TabButton(icon: "house", title: "Home", isSelected: false) {
                activeContent = nil
            }
            TabButton(icon: "calendar", title: "Bookings", isSelected: false) {
                activeContent = .booking
            }
            TabButton(icon: "wallet.pass", title: "Wallet", isSelected: true) {
                print("Already on Wallet")
            }
            TabButton(icon: "person", title: "Profile", isSelected: false) {
                activeContent = .profile
            }
        }
        .frame(height: 80)
        .padding(.horizontal, 20)
        .padding(.vertical, 12)
        .background(Color.white.shadow(color: .black.opacity(0.06), radius: 4, x: 0, y: -2))
    }
}

// MARK: - Booking Bottom Tab Bar
struct BookingBottomTabBar: View {
    @Binding var activeContent: SheetContent?
    
    var body: some View {
        HStack(spacing: 0) {
            TabButton(icon: "house", title: "Home", isSelected: false) {
                activeContent = nil
            }
            TabButton(icon: "calendar", title: "Bookings", isSelected: true) {
                print("Already on Bookings")
            }
            TabButton(icon: "wallet.pass", title: "Wallet", isSelected: false) {
                activeContent = .wallet
            }
            TabButton(icon: "person", title: "Profile", isSelected: false) {
                activeContent = .profile
            }
        }
        .frame(height: 80)
        .padding(.horizontal, 20)
        .padding(.vertical, 12)
        .background(Color.white.shadow(color: .black.opacity(0.06), radius: 4, x: 0, y: -2))
    }
}

// MARK: - Reusable Tab Button
struct TabButton: View {
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

// MARK: - Preview
struct SheetContainerView_Previews: PreviewProvider {
    static var previews: some View {
        SheetContainerView(activeContent: .constant(.wallet), initialContent: .wallet)
            .environmentObject(AuthViewModel())
    }
}









//import SwiftUI
//
//struct SheetContainerView: View {
//   @Binding var activeContent: SheetContent?
//   let initialContent: SheetContent
//   
//
//  var body: some View {
//       NavigationView {
//           switch initialContent {
//           case .profile:
//               ProfilePageContent(activeContent: $activeContent)
//               Text("Profile View")
//                   .navigationTitle("Profile")
//               
//           case .wallet:
//               WalletPage()
//               Text("Wallet View")
//                   .navigationTitle("Wallet")
//               
//           case .booking:
//                      // FIXED: Remove ParkingSelectionView() and go directly to CreateBookingView
//               BookingPageContent(activeContent: $activeContent)
//                   .navigationTitle("Bookings")
//           case .home:
//               Text("Welcome to Home")
//           }
//       }
//   }
//}
//// MARK: - Booking Page Content
//struct bb: View {
//   @Binding var activeContent: SheetContent?
//   @EnvironmentObject var authViewModel: AuthViewModel
//   
//   
//   var body: some View {
//       VStack(spacing: 20) {
//           Text("My Bookings")
//               .font(.largeTitle)
//               .fontWeight(.bold)
//               .padding(.top, 20)
//           
//           // Active Booking Card
//           VStack(spacing: 16) {
//               Text("Active Booking")
//                   .font(.headline)
//                   .foregroundColor(.green)
//               
//               VStack(spacing: 12) {
//                   HStack {
//                       Text("Location:")
//                       Spacer()
//                       Text("TP Avenue Parking")
//                           .fontWeight(.semibold)
//                   }
//                   
//                   HStack {
//                       Text("Spot:")
//                       Spacer()
//                       Text("A-23")
//                           .fontWeight(.semibold)
//                   }
//                   
//                   HStack {
//                       Text("Duration:")
//                       Spacer()
//                       Text("2 hours")
//                           .fontWeight(.semibold)
//                   }
//                   
//                   HStack {
//                       Text("Amount:")
//                       Spacer()
//                       Text("₹50.00")
//                           .fontWeight(.semibold)
//                           .foregroundColor(.green)
//                   }
//               }
//               
//               Button("End Booking") {
//                   print("End Booking tapped")
//               }
//               .padding()
//               .frame(maxWidth: .infinity)
//               .background(Color.red)
//               .foregroundColor(.white)
//               .cornerRadius(12)
//           }
//           .padding()
//           .background(Color.white)
//           .cornerRadius(16)
//           .shadow(color: .black.opacity(0.1), radius: 4, x: 0, y: 2)
//           
//           // Recent Bookings
//           VStack(alignment: .leading, spacing: 12) {
//               Text("Recent Bookings")
//                   .font(.headline)
//                   .padding(.horizontal)
//               
//               VStack(spacing: 8) {
//                   BookingRow(location: "Mall Parking", date: "Sept 10", amount: "₹25.00", status: "Completed")
//                   BookingRow(location: "Office Complex", date: "Sept 09", amount: "₹45.00", status: "Completed")
//                   BookingRow(location: "Airport Parking", date: "Sept 08", amount: "₹120.00", status: "Completed")
//               }
//               .padding()
//               .background(Color.white)
//               .cornerRadius(12)
//               .shadow(color: .black.opacity(0.1), radius: 2, x: 0, y: 1)
//           }
//           
//           Spacer()
//           
//           // Bottom Tab Bar
//           BookingBottomTabBar(activeContent: $activeContent)
//       }
//       .padding()
//       .background(Color(UIColor.systemGroupedBackground))
//       .navigationTitle("Bookings")
//       .navigationBarTitleDisplayMode(.inline)
//   }
//}
//
//// MARK: - Booking Row
//struct BookingRow: View {
//   let location: String
//   let date: String
//   let amount: String
//   let status: String
//   
//   var body: some View {
//       HStack {
//           VStack(alignment: .leading, spacing: 2) {
//               Text(location)
//                   .font(.body)
//                   .fontWeight(.medium)
//               Text(date)
//                   .font(.caption)
//                   .foregroundColor(.gray)
//           }
//           
//           Spacer()
//           
//           VStack(alignment: .trailing, spacing: 2) {
//               Text(amount)
//                   .font(.body)
//                   .fontWeight(.semibold)
//               Text(status)
//                   .font(.caption)
//                   .foregroundColor(.green)
//           }
//       }
//   }
//}
//
//// MARK: - Wallet Bottom Tab Bar
//struct WalletBottomTabBar: View {
//   @Binding var activeContent: SheetContent?
//   
//   var body: some View {
//       HStack(spacing: 0) {
//           TabButton(icon: "house", title: "Home", isSelected: false) {
//               activeContent = nil
//           }
//           TabButton(icon: "calendar", title: "Bookings", isSelected: false) {
//               activeContent = .booking
//           }
//           TabButton(icon: "wallet.pass", title: "Wallet", isSelected: true) {
//               print("Already on Wallet")
//           }
//           TabButton(icon: "person", title: "Profile", isSelected: false) {
//               activeContent = .profile
//           }
//       }
//       .frame(height: 80)
//       .padding(.horizontal, 20)
//       .padding(.vertical, 12)
//       .background(Color.white.shadow(color: .black.opacity(0.06), radius: 4, x: 0, y: -2))
//   }
//}
//
//// MARK: - Booking Bottom Tab Bar
//struct BookingBottomTabBar: View {
//   @Binding var activeContent: SheetContent?
//   
//   var body: some View {
//       HStack(spacing: 0) {
//           TabButton(icon: "house", title: "Home", isSelected: false) {
//               activeContent = nil
//           }
//           TabButton(icon: "calendar", title: "Bookings", isSelected: true) {
//               print("Already on Bookings")
//           }
//           TabButton(icon: "wallet.pass", title: "Wallet", isSelected: false) {
//               activeContent = .wallet
//           }
//           TabButton(icon: "person", title: "Profile", isSelected: false) {
//               activeContent = .profile
//           }
//       }
//       .frame(height: 80)
//       .padding(.horizontal, 20)
//       .padding(.vertical, 12)
//       .background(Color.white.shadow(color: .black.opacity(0.06), radius: 4, x: 0, y: -2))
//   }
//}
//
//// MARK: - Reusable Tab Button
//struct TabButton: View {
//   let icon: String
//   let title: String
//   let isSelected: Bool
//   let action: () -> Void
//   
//   var body: some View {
//       Button(action: action) {
//           VStack(spacing: 6) {
//               Image(systemName: icon)
//                   .font(.system(size: 20, weight: .medium))
//                   .foregroundColor(isSelected ? .black : .gray)
//               
//               Text(title)
//                   .font(.system(size: 11, weight: .medium))
//                   .foregroundColor(isSelected ? .black : .gray)
//           }
//           .frame(maxWidth: .infinity)
//           .contentShape(Rectangle())
//       }
//       .buttonStyle(PlainButtonStyle())
//   }
//}
//
//// MARK: - Preview
//struct SheetContainerView_Previews: PreviewProvider {
//   static var previews: some View {
//       SheetContainerView(activeContent: .constant(.wallet), initialContent: .wallet)
//           .environmentObject(AuthViewModel())
//   }
//}
//
//
//
//
//
