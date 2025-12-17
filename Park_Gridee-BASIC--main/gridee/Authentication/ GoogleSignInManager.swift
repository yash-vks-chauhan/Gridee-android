//
//   GoogleSignInManager.swift
//  gridee
//
//  Created by Rishabh on 12/10/25.
//

import Foundation
import GoogleSignIn
import SwiftUI

class GoogleSignInManager: ObservableObject {
    static let shared = GoogleSignInManager()
    
    @Published var isSignedIn = false
    @Published var errorMessage = ""
    
    private init() {}
    
    func signIn(presentingViewController: UIViewController, completion: @escaping (String?, String?, Error?) -> Void) {
        guard let clientID = getGoogleClientID() else {
            completion(nil, nil, NSError(domain: "GoogleSignIn", code: -1, userInfo: [NSLocalizedDescriptionKey: "Google Client ID not found"]))
            return
        }
        
        let config = GIDConfiguration(clientID: clientID)
        GIDSignIn.sharedInstance.configuration = config
        
        GIDSignIn.sharedInstance.signIn(withPresenting: presentingViewController) { result, error in
            if let error = error {
                print("❌ Google Sign-In error: \(error.localizedDescription)")
                completion(nil, nil, error)
                return
            }
            
            guard let user = result?.user,
                  let idToken = user.idToken?.tokenString else {
                completion(nil, nil, NSError(domain: "GoogleSignIn", code: -2, userInfo: [NSLocalizedDescriptionKey: "Failed to get user token"]))
                return
            }
            
            let accessToken = user.accessToken.tokenString
            print("✅ Google Sign-In successful")
            print("   ID Token: \(idToken.prefix(20))...")
            print("   Access Token: \(accessToken.prefix(20))...")
            
            completion(idToken, accessToken, nil)
        }
    }
    
    func signOut() {
        GIDSignIn.sharedInstance.signOut()
        isSignedIn = false
        print("🚪 Google Sign-Out successful")
    }
    
    private func getGoogleClientID() -> String? {
        // Get from your Google Cloud Console
        // Format: "YOUR_CLIENT_ID.apps.googleusercontent.com"
        return Bundle.main.object(forInfoDictionaryKey: "GIDClientID") as? String
    }
}
