//import SwiftUI
//
//struct SplashView: View {
//    @State private var logoScale: CGFloat = 0.7
//    @State private var logoOpacity: Double = 0.0
//    @State private var textOffset: CGFloat = 30
//    @State private var textOpacity: Double = 0.0
//    @State private var backgroundOpacity: Double = 1.0
//    @State private var pulseScale: CGFloat = 1.0
//    
//    var body: some View {
//        ZStack {
//            // ✅ BLACK BACKGROUND
//            Color.black
//                .ignoresSafeArea()
//                .opacity(backgroundOpacity)
//            
//            // Main Content
//            VStack(spacing: 0) {
//                Spacer()
//                
//                // Logo Section
//                VStack(spacing: 24) {
//                    // Logo with Pulse Animation
//                    ZStack {
//                        // Outer Pulse Ring
//                        Circle()
//                            .stroke(Color.white.opacity(0.3), lineWidth: 2)
//                            .frame(width: 120, height: 120)
//                            .scaleEffect(pulseScale)
//                            .opacity(logoOpacity * 0.7)
//                        
//                        // Inner Circle
//                        Circle()
//                            .fill(Color.white.opacity(0.1))
//                            .frame(width: 100, height: 100)
//                            .scaleEffect(logoScale)
//                            .opacity(logoOpacity)
//                        
//                        // Main Logo Icon - WHITE
//                        Image(systemName: "car.circle.fill")
//                            .font(.system(size: 40, weight: .medium))
//                            .foregroundColor(.white)
//                            .scaleEffect(logoScale)
//                            .opacity(logoOpacity)
//                    }
//                    
//                    // App Name - WHITE
//                    VStack(spacing: 8) {
//                        Text("gridee")
//                            .font(.system(size: 48, weight: .bold, design: .rounded))
//                            .foregroundColor(.white)
//                            .opacity(textOpacity)
//                            .offset(y: textOffset)
//                        
//                        // Tagline
//                        Text("Smart Parking Solutions")
//                            .font(.title3)
//                            .fontWeight(.light)
//                            .foregroundColor(.white.opacity(0.8))
//                            .opacity(textOpacity)
//                            .offset(y: textOffset)
//                    }
//                }
//                
//                Spacer()
//                
//                // Loading Section - WHITE
//                VStack(spacing: 16) {
//                    // Elegant Loading Dots
//                    HStack(spacing: 6) {
//                        ForEach(0..<3) { index in
//                            Circle()
//                                .fill(Color.white)
//                                .frame(width: 6, height: 6)
//                                .scaleEffect(logoScale)
//                                .opacity(textOpacity)
//                                .animation(
//                                    .easeInOut(duration: 0.8)
//                                    .repeatForever(autoreverses: true)
//                                    .delay(Double(index) * 0.3),
//                                    value: logoScale
//                                )
//                        }
//                    }
//                    
//                    Text("Loading...")
//                        .font(.caption)
//                        .fontWeight(.light)
//                        .foregroundColor(.white.opacity(0.7))
//                        .opacity(textOpacity)
//                        .tracking(2.0) // Letter spacing for elegance
//                }
//                .padding(.bottom, 80)
//            }
//        }
//        .onAppear {
//            startSmoothAnimations()
//        }
//    }
//    
//    // ✅ ULTRA-SMOOTH ANIMATION SEQUENCE
//    private func startSmoothAnimations() {
//        // Phase 1: Logo appears with bounce
//        withAnimation(.spring(response: 1.0, dampingFraction: 0.6, blendDuration: 0)) {
//            logoScale = 1.0
//            logoOpacity = 1.0
//        }
//        
//        // Phase 2: Text slides up smoothly (delayed)
//        withAnimation(.easeOut(duration: 0.8).delay(0.3)) {
//            textOffset = 0
//            textOpacity = 1.0
//        }
//        
//        // Phase 3: Pulse animation (continuous)
//        withAnimation(.easeInOut(duration: 2.0).repeatForever(autoreverses: true).delay(0.5)) {
//            pulseScale = 1.2
//        }
//    }
//}
//
//#Preview {
//    SplashView()
//}

//import SwiftUI
//
//struct SplashView: View {
//    @State private var opacity: Double = 0
//    @State private var scale: CGFloat = 0.8
//    
//    var body: some View {
//        ZStack {
//            // Pure black background
//            Color.black
//                .ignoresSafeArea()
//            
//            // "Gridee" text with Uber-style animation
//            Text("Gridee")
//                .font(.system(size: 72, weight: .bold, design: .rounded))
//                .foregroundColor(.white)
//                .opacity(opacity)
//                .scaleEffect(scale)
//        }
//        .onAppear {
//            // Smooth fade-in and scale animation like Uber
//            withAnimation(.easeInOut(duration: 1.2)) {
//                opacity = 1.0
//                scale = 1.0
//            }
//        }
//    }
//}
//
//struct SplashView_Previews: PreviewProvider {
//    static var previews: some View {
//        SplashView()
//    }
//}


//import SwiftUI
//
//struct SplashView: View {
//    @State private var revealProgress: [CGFloat] = Array(repeating: 0, count: 6)
//    
//    private let letters = Array("Gridee")
//    
//    var body: some View {
//        ZStack {
//            // Pure black background
//            Color.black
//                .ignoresSafeArea()
//            
//            // Letter-by-letter wipe reveal
//            HStack(spacing: 2) {
//                ForEach(0..<letters.count, id: \.self) { index in
//                    Text(String(letters[index]))
//                        .font(.system(size: 72, weight: .bold, design: .rounded))
//                        .foregroundColor(.white)
//                        .mask(
//                            GeometryReader { geometry in
//                                Rectangle()
//                                    .frame(width: geometry.size.width * revealProgress[index])
//                                    .frame(maxWidth: .infinity, alignment: .leading)
//                            }
//                        )
//                }
//            }
//        }
//        .onAppear {
//            animateWipeEffect()
//        }
//    }
//    
//    private func animateWipeEffect() {
//        for index in 0..<letters.count {
//            // Stagger each letter slightly
//            DispatchQueue.main.asyncAfter(deadline: .now() + Double(index) * 0.12) {
//                withAnimation(.easeOut(duration: 0.35)) {
//                    revealProgress[index] = 1.0
//                }
//            }
//        }
//    }
//}
//
//struct SplashView_Previews: PreviewProvider {
//    static var previews: some View {
//        SplashView()
//    }
//}


import SwiftUI

struct SplashView: View {
    @State private var bikeOffset: CGFloat = -200
    @State private var bikeOpacity: Double = 0
    @State private var parkingLinesOpacity: Double = 0
    @State private var logoOpacity: Double = 0
    @State private var textOpacity: Double = 0
    @State private var textScale: CGFloat = 0.9
    @State private var fadeOut: Bool = false
    
    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            
            VStack(spacing: 0) {
                Spacer()
                
                // Parking animation scene
                ZStack {
                    // Parking slot lines (appears first)
                    HStack(spacing: 0) {
                        Rectangle()
                            .fill(Color.white.opacity(0.3))
                            .frame(width: 3, height: 60)
                        
                        Spacer()
                            .frame(width: 70)
                        
                        Rectangle()
                            .fill(Color.white.opacity(0.3))
                            .frame(width: 3, height: 60)
                    }
                    .opacity(parkingLinesOpacity)
                    
                    // Bike animation (slides in and parks)
                    Image(systemName: "car")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 50, height: 50)
                        .foregroundColor(.white)
                        .offset(x: bikeOffset)
                        .opacity(bikeOpacity)
                }
                .frame(height: 80)
                .padding(.bottom, 50)
                
                // Premium logo
//                Image(systemName: "car.circle.fill")
//                    .resizable()
//                    .scaledToFit()
//                    .frame(width: 80, height: 80)
//                    .foregroundColor(.white)
//                    .opacity(logoOpacity)
//                    .padding(.bottom, 30)
                
                // Aesthetic GRIDEE text with letter spacing
                Text("G R I D E E")
                    .font(.system(size: 52, weight: .ultraLight, design: .default))
                    .tracking(8)

                   
                    .foregroundColor(.white)
                    .opacity(textOpacity)
                    .scaleEffect(textScale)
                
                Spacer()
                Spacer()
            }
            .opacity(fadeOut ? 0 : 1)
        }
        .onAppear {
            // Animation sequence
            
            // 1. Parking lines appear
            withAnimation(.easeOut(duration: 0.5).delay(0.2)) {
                parkingLinesOpacity = 1
            }
            
            // 2. Bike enters from left
            withAnimation(.easeOut(duration: 0.4).delay(0.4)) {
                bikeOpacity = 1
            }
            
            // 3. Bike slides into parking spot
            withAnimation(.easeInOut(duration: 0.8).delay(0.5)) {
                bikeOffset = 0
            }
            
            // 4. Logo fades in
            withAnimation(.easeOut(duration: 0.7).delay(1.1)) {
                logoOpacity = 1
            }
            
            // 5. GRIDEE text appears with scale
            withAnimation(.easeOut(duration: 0.9).delay(1.4)) {
                textOpacity = 1
                textScale = 1
            }
            
            // 6. Fade out everything
            DispatchQueue.main.asyncAfter(deadline: .now() + 3.0) {
                withAnimation(.easeOut(duration: 0.7)) {
                    fadeOut = true
                }
            }
        }
    }
}

struct SplashView_Previews: PreviewProvider {
    static var previews: some View {
        SplashView()
            .preferredColorScheme(.dark)
    }
}
