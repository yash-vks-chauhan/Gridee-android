//
//  HelpSupportView.swift
//  gridee
//
//  Created by Rishabh on 08/11/25.
//
import SwiftUI

struct HelpSupportView: View {
    @State private var selectedFAQCategory: FAQCategory = .bookings
    @State private var expandedFAQIndex: Int? = nil
    @State private var showSupportForm = false
    @State private var supportMessage = ""
    @State private var supportEmail = ""
    @State private var selectedIssueType: IssueType = .booking
    @State private var isSubmittingTicket = false
    @State private var showSuccessMessage = false
    
    enum FAQCategory: String, CaseIterable {
        case bookings = "Bookings"
        case payments = "Payments"
        case vehicles = "Vehicles"
        case account = "Account"
        case general = "General"
    }
    
    enum IssueType: String, CaseIterable {
        case booking = "Booking Issue"
        case payment = "Payment Issue"
        case technical = "Technical Issue"
        case other = "Other"
    }
    
    var body: some View {
        ZStack {
            Color(UIColor { $0.userInterfaceStyle == .dark ? UIColor(red: 0.1, green: 0.1, blue: 0.1, alpha: 1) : UIColor.white })
                .ignoresSafeArea()
            
            VStack(spacing: 0) {
                // Header
                HStack {
                    Text("Help & Support")
                        .font(.system(size: 28, weight: .bold))
                        .foregroundColor(.white)
                    Spacer()
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 16)
                
                ScrollView {
                    VStack(spacing: 20) {
                        // Quick Contact Section
                        quickContactSection
                        
                        // FAQs Section
                        faqSection
                        
                        // Support Form Button
                        supportFormButton
                        
                        // Additional Resources
                        additionalResourcesSection
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 20)
                }
            }
            
            // Success Message Overlay
            if showSuccessMessage {
                VStack {
                    Spacer()
                    HStack {
                        Image(systemName: "checkmark.circle.fill")
                            .foregroundColor(.green)
                            .font(.system(size: 20))
                        
                        Text("Support ticket submitted successfully!")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundColor(.white)
                        
                        Spacer()
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                    .background(Color(UIColor(red: 0.2, green: 0.2, blue: 0.2, alpha: 1)))
                    .cornerRadius(8)
                    .padding(.horizontal, 16)
                    .padding(.bottom, 20)
                }
                .transition(.move(edge: .bottom))
            }
        }
        .sheet(isPresented: $showSupportForm) {
            supportFormSheet
        }
    }
    
    // MARK: - Quick Contact Section
    var quickContactSection: some View {
        VStack(spacing: 12) {
            Text("Get in Touch")
                .font(.system(size: 18, weight: .semibold))
                .foregroundColor(.white)
                .frame(maxWidth: .infinity, alignment: .leading)
            
            HStack(spacing: 12) {
                contactCard(
                    icon: "envelope.fill",
                    title: "Email",
                    subtitle: "support@gridee.app",
                    action: {
                        openEmail()
                    }
                )
                
                contactCard(
                    icon: "phone.fill",
                    title: "Call",
                    subtitle: "+91 9876543210",
                    action: {
                        openPhone()
                    }
                )
            }
            
            HStack(spacing: 12) {
                contactCard(
                    icon: "clock.fill",
                    title: "Support Hours",
                    subtitle: "Mon-Fri, 9AM-6PM",
                    action: {}
                )
                
                contactCard(
                    icon: "message.fill",
                    title: "Chat",
                    subtitle: "Available 24/7",
                    action: {}
                )
            }
        }
        .padding(16)
        .background(Color(UIColor(red: 0.15, green: 0.15, blue: 0.15, alpha: 1)))
        .cornerRadius(12)
    }
    
    func contactCard(icon: String, title: String, subtitle: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(spacing: 8) {
                Image(systemName: icon)
                    .font(.system(size: 20))
                    .foregroundColor(.blue)
                
                Text(title)
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(.white)
                
                Text(subtitle)
                    .font(.system(size: 11))
                    .foregroundColor(.gray)
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity)
            .padding(12)
            .background(Color(UIColor(red: 0.12, green: 0.12, blue: 0.12, alpha: 1)))
            .cornerRadius(10)
        }
    }
    
    // MARK: - FAQ Section
    var faqSection: some View {
        VStack(spacing: 12) {
            Text("Frequently Asked Questions")
                .font(.system(size: 18, weight: .semibold))
                .foregroundColor(.white)
                .frame(maxWidth: .infinity, alignment: .leading)
            
            // Category Tabs
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    ForEach(FAQCategory.allCases, id: \.self) { category in
                        Button(action: {
                            selectedFAQCategory = category
                            expandedFAQIndex = nil
                        }) {
                            Text(category.rawValue)
                                .font(.system(size: 13, weight: .semibold))
                                .foregroundColor(selectedFAQCategory == category ? .white : .gray)
                                .padding(.horizontal, 14)
                                .padding(.vertical, 8)
                                .background(selectedFAQCategory == category ? Color.blue : Color(UIColor(red: 0.15, green: 0.15, blue: 0.15, alpha: 1)))
                                .cornerRadius(20)
                        }
                    }
                }
                .padding(.horizontal, 0)
            }
            
            // FAQ Items
            VStack(spacing: 10) {
                ForEach(getFAQsForCategory(), id: \.id) { faq in
                    faqItemView(faq: faq)
                }
            }
        }
        .padding(16)
        .background(Color(UIColor(red: 0.15, green: 0.15, blue: 0.15, alpha: 1)))
        .cornerRadius(12)
    }
    
    func faqItemView(faq: FAQItem) -> some View {
        VStack(spacing: 0) {
            Button(action: {
                withAnimation(.easeInOut(duration: 0.2)) {
                    if expandedFAQIndex == faq.id {
                        expandedFAQIndex = nil
                    } else {
                        expandedFAQIndex = faq.id
                    }
                }
            }) {
                HStack(spacing: 12) {
                    Text(faq.question)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    
                    Image(systemName: "chevron.right")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundColor(.blue)
                        .rotationEffect(.degrees(expandedFAQIndex == faq.id ? 90 : 0))
                }
                .padding(12)
                .background(Color(UIColor(red: 0.12, green: 0.12, blue: 0.12, alpha: 1)))
                .cornerRadius(8)
            }
            
            if expandedFAQIndex == faq.id {
                Text(faq.answer)
                    .font(.system(size: 13))
                    .foregroundColor(.gray)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(12)
                    .background(Color(UIColor(red: 0.1, green: 0.1, blue: 0.1, alpha: 1)))
                    .cornerRadius(0, corners: [.bottomLeft, .bottomRight])
                    .transition(.opacity.combined(with: .move(edge: .top)))
            }
        }
    }
    
    func getFAQsForCategory() -> [FAQItem] {
        switch selectedFAQCategory {
        case .bookings:
            return [
                FAQItem(id: 1, question: "How do I book a parking spot?", answer: "Open the app, select your location, choose an available spot, and confirm your booking. Payment will be processed immediately."),
                FAQItem(id: 2, question: "Can I cancel my booking?", answer: "Yes, you can cancel up to 30 minutes before your booking time. A cancellation fee may apply depending on timing."),
                FAQItem(id: 3, question: "What if I'm late for my booking?", answer: "Contact support immediately. We may extend your booking or apply late fees based on availability.")
            ]
        case .payments:
            return [
                FAQItem(id: 4, question: "What payment methods do you accept?", answer: "We accept all major credit/debit cards, UPI, net banking, and digital wallets like Google Pay and Apple Pay."),
                FAQItem(id: 5, question: "Is my payment information secure?", answer: "Yes, all payments are encrypted using industry-standard SSL/TLS protocols and PCI DSS compliant."),
                FAQItem(id: 6, question: "Can I get a refund?", answer: "Refunds are processed within 5-7 business days. Contact support with your booking ID for assistance.")
            ]
        case .vehicles:
            return [
                FAQItem(id: 7, question: "How do I add a vehicle?", answer: "Go to Profile > My Vehicles > Add Vehicle. Enter your license plate number and vehicle details."),
                FAQItem(id: 8, question: "Can I add multiple vehicles?", answer: "Yes, you can add up to 5 vehicles to your account."),
                FAQItem(id: 9, question: "How do I remove a vehicle?", answer: "Go to My Vehicles, select the vehicle, and tap Remove. You won't be able to book with that vehicle afterward.")
            ]
        case .account:
            return [
                FAQItem(id: 10, question: "How do I verify my account?", answer: "Check your email for a verification link after registration. Click the link to verify your account."),
                FAQItem(id: 11, question: "Can I change my email?", answer: "Yes, go to Profile > Edit Profile to change your email address."),
                FAQItem(id: 12, question: "How do I reset my password?", answer: "Tap 'Forgot Password' on the login screen and follow the instructions sent to your email.")
            ]
        case .general:
            return [
                FAQItem(id: 13, question: "Is Gridee available in my city?", answer: "Gridee is currently available in major metros. Check the app to see if your city is supported."),
                FAQItem(id: 14, question: "Do you have a minimum booking duration?", answer: "Minimum booking duration is 1 hour. Maximum duration is 24 hours."),
                FAQItem(id: 15, question: "How can I report an issue?", answer: "Use the 'Submit Support Ticket' button below to report any issues. We'll respond within 24 hours.")
            ]
        }
    }
    
    // MARK: - Support Form Button
    var supportFormButton: some View {
        Button(action: { showSupportForm = true }) {
            HStack(spacing: 12) {
                Image(systemName: "exclamationmark.circle.fill")
                    .font(.system(size: 18))
                
                VStack(alignment: .leading, spacing: 4) {
                    Text("Report an Issue")
                        .font(.system(size: 14, weight: .semibold))
                    Text("Submit a support ticket")
                        .font(.system(size: 12))
                        .foregroundColor(.gray)
                }
                
                Spacer()
                
                Image(systemName: "chevron.right")
                    .font(.system(size: 12, weight: .semibold))
            }
            .foregroundColor(.white)
            .padding(14)
            .background(Color(UIColor(red: 0.15, green: 0.15, blue: 0.15, alpha: 1)))
            .cornerRadius(10)
        }
    }
    
    // MARK: - Support Form Sheet
    var supportFormSheet: some View {
        ZStack {
            Color(UIColor { $0.userInterfaceStyle == .dark ? UIColor(red: 0.1, green: 0.1, blue: 0.1, alpha: 1) : UIColor.white })
                .ignoresSafeArea()
            
            VStack(spacing: 0) {
                // Sheet Header
                HStack {
                    Text("Submit Support Ticket")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundColor(.white)
                    
                    Spacer()
                    
                    Button(action: { showSupportForm = false }) {
                        Image(systemName: "xmark.circle.fill")
                            .font(.system(size: 24))
                            .foregroundColor(.gray)
                    }
                }
                .padding(16)
                .background(Color(UIColor(red: 0.15, green: 0.15, blue: 0.15, alpha: 1)))
                
                ScrollView {
                    VStack(spacing: 16) {
                        // Issue Type Picker
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Issue Type")
                                .font(.system(size: 14, weight: .semibold))
                                .foregroundColor(.white)
                            
                            Picker("Issue Type", selection: $selectedIssueType) {
                                ForEach(IssueType.allCases, id: \.self) { type in
                                    Text(type.rawValue).tag(type)
                                }
                            }
                            .pickerStyle(.segmented)
                            .tint(.blue)
                        }
                        
                        // Email Field
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Email Address")
                                .font(.system(size: 14, weight: .semibold))
                                .foregroundColor(.white)
                            
                            TextField("your@email.com", text: $supportEmail)
                                .font(.system(size: 14))
                                .foregroundColor(.white)
                                .padding(12)
                                .background(Color(UIColor(red: 0.12, green: 0.12, blue: 0.12, alpha: 1)))
                                .cornerRadius(8)
                        }
                        
                        // Message Field
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Describe Your Issue")
                                .font(.system(size: 14, weight: .semibold))
                                .foregroundColor(.white)
                            
                            TextEditor(text: $supportMessage)
                                .font(.system(size: 14))
                                .foregroundColor(.white)
                                .padding(12)
                                .background(Color(UIColor(red: 0.12, green: 0.12, blue: 0.12, alpha: 1)))
                                .cornerRadius(8)
                                .frame(height: 150)
                        }
                        
                        // Submit Button
                        Button(action: submitSupportTicket) {
                            HStack(spacing: 8) {
                                if isSubmittingTicket {
                                    ProgressView()
                                        .tint(.white)
                                } else {
                                    Image(systemName: "paperplane.fill")
                                }
                                
                                Text(isSubmittingTicket ? "Submitting..." : "Submit Ticket")
                                    .font(.system(size: 16, weight: .semibold))
                            }
                            .frame(maxWidth: .infinity)
                            .padding(14)
                            .background(Color.blue)
                            .foregroundColor(.white)
                            .cornerRadius(10)
                        }
                        .disabled(isSubmittingTicket || supportMessage.isEmpty || supportEmail.isEmpty)
                    }
                    .padding(16)
                }
            }
        }
    }
    
    // MARK: - Additional Resources
    var additionalResourcesSection: some View {
        VStack(spacing: 12) {
            Text("Additional Resources")
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(.white)
                .frame(maxWidth: .infinity, alignment: .leading)
            
            resourceLink(icon: "doc.text.fill", title: "Terms & Conditions")
            resourceLink(icon: "lock.shield.fill", title: "Privacy Policy")
            resourceLink(icon: "info.circle.fill", title: "App Version 1.2.0")
        }
        .padding(16)
        .background(Color(UIColor(red: 0.15, green: 0.15, blue: 0.15, alpha: 1)))
        .cornerRadius(12)
    }
    
    func resourceLink(icon: String, title: String) -> some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 16))
                .foregroundColor(.blue)
            
            Text(title)
                .font(.system(size: 14, weight: .semibold))
                .foregroundColor(.white)
            
            Spacer()
            
            Image(systemName: "chevron.right")
                .font(.system(size: 12, weight: .semibold))
                .foregroundColor(.gray)
        }
        .padding(12)
        .background(Color(UIColor(red: 0.12, green: 0.12, blue: 0.12, alpha: 1)))
        .cornerRadius(8)
    }
    
    // MARK: - Support Functions
    func submitSupportTicket() {
        isSubmittingTicket = true
        
        // Simulate API call
        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
            isSubmittingTicket = false
            showSupportForm = false
            supportMessage = ""
            supportEmail = ""
            
            withAnimation {
                showSuccessMessage = true
            }
            
            DispatchQueue.main.asyncAfter(deadline: .now() + 3) {
                withAnimation {
                    showSuccessMessage = false
                }
            }
        }
    }
    
    func openEmail() {
        if let url = URL(string: "mailto:support@gridee.app") {
            UIApplication.shared.open(url)
        }
    }
    
    func openPhone() {
        if let url = URL(string: "tel:+919876543210") {
            UIApplication.shared.open(url)
        }
    }
}

// MARK: - Models
struct FAQItem: Identifiable {
    let id: Int
    let question: String
    let answer: String
}

// MARK: - Corner Radius Extension
extension View {
    func cornerRadius(_ radius: CGFloat, corners: UIRectCorner) -> some View {
        clipShape(RoundedCorner(radius: radius, corners: corners))
    }
}

struct RoundedCorner: Shape {
    var radius: CGFloat = .infinity
    var corners: UIRectCorner = .allCorners
    
    func path(in rect: CGRect) -> Path {
        let path = UIBezierPath(
            roundedRect: rect,
            byRoundingCorners: corners,
            cornerRadii: CGSize(width: radius, height: radius)
        )
        return Path(path.cgPath)
    }
}

#Preview {
    HelpSupportView()
}

