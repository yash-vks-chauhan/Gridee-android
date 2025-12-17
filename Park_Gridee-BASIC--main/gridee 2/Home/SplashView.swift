import SwiftUI

struct SplashView: View {
    @State private var logoScale: CGFloat = 0.7
    @State private var logoOpacity: Double = 0.0
    @State private var textOffset: CGFloat = 30
    @State private var textOpacity: Double = 0.0
    @State private var backgroundOpacity: Double = 1.0
    @State private var pulseScale: CGFloat = 1.0
    
    var body: some View {
        ZStack {
            // ✅ BLACK BACKGROUND
            Color.black
                .ignoresSafeArea()
                .opacity(backgroundOpacity)
            
            // Main Content
            VStack(spacing: 0) {
                Spacer()
                
                // Logo Section
                VStack(spacing: 24) {
                    // Logo with Pulse Animation
                    ZStack {
                        // Outer Pulse Ring
                        Circle()
                            .stroke(Color.white.opacity(0.3), lineWidth: 2)
                            .frame(width: 120, height: 120)
                            .scaleEffect(pulseScale)
                            .opacity(logoOpacity * 0.7)
                        
                        // Inner Circle
                        Circle()
                            .fill(Color.white.opacity(0.1))
                            .frame(width: 100, height: 100)
                            .scaleEffect(logoScale)
                            .opacity(logoOpacity)
                        
                        // Main Logo Icon - WHITE
                        Image(systemName: "car.circle.fill")
                            .font(.system(size: 40, weight: .medium))
                            .foregroundColor(.white)
                            .scaleEffect(logoScale)
                            .opacity(logoOpacity)
                    }
                    
                    // App Name - WHITE
                    VStack(spacing: 8) {
                        Text("gridee")
                            .font(.system(size: 48, weight: .bold, design: .rounded))
                            .foregroundColor(.white)
                            .opacity(textOpacity)
                            .offset(y: textOffset)
                        
                        // Tagline
                        Text("Smart Parking Solutions")
                            .font(.title3)
                            .fontWeight(.light)
                            .foregroundColor(.white.opacity(0.8))
                            .opacity(textOpacity)
                            .offset(y: textOffset)
                    }
                }
                
                Spacer()
                
                // Loading Section - WHITE
                VStack(spacing: 16) {
                    // Elegant Loading Dots
                    HStack(spacing: 6) {
                        ForEach(0..<3) { index in
                            Circle()
                                .fill(Color.white)
                                .frame(width: 6, height: 6)
                                .scaleEffect(logoScale)
                                .opacity(textOpacity)
                                .animation(
                                    .easeInOut(duration: 0.8)
                                    .repeatForever(autoreverses: true)
                                    .delay(Double(index) * 0.3),
                                    value: logoScale
                                )
                        }
                    }
                    
                    Text("Loading...")
                        .font(.caption)
                        .fontWeight(.light)
                        .foregroundColor(.white.opacity(0.7))
                        .opacity(textOpacity)
                        .tracking(2.0) // Letter spacing for elegance
                }
                .padding(.bottom, 80)
            }
        }
        .onAppear {
            startSmoothAnimations()
        }
    }
    
    // ✅ ULTRA-SMOOTH ANIMATION SEQUENCE
    private func startSmoothAnimations() {
        // Phase 1: Logo appears with bounce
        withAnimation(.spring(response: 1.0, dampingFraction: 0.6, blendDuration: 0)) {
            logoScale = 1.0
            logoOpacity = 1.0
        }
        
        // Phase 2: Text slides up smoothly (delayed)
        withAnimation(.easeOut(duration: 0.8).delay(0.3)) {
            textOffset = 0
            textOpacity = 1.0
        }
        
        // Phase 3: Pulse animation (continuous)
        withAnimation(.easeInOut(duration: 2.0).repeatForever(autoreverses: true).delay(0.5)) {
            pulseScale = 1.2
        }
    }
}

#Preview {
    SplashView()
}
