import Foundation

class PaymentService: NSObject, URLSessionDelegate {
    static let shared = PaymentService()
    
    private static let backendBaseURL = APIService.backendBaseURL
    
    // ✅ Add reference to AuthViewModel for JWT token
    weak var authViewModel: AuthViewModel?
    
    private lazy var session: URLSession = {
        let config = URLSessionConfiguration.default
        return URLSession(configuration: config, delegate: self, delegateQueue: nil)
    }()
    
    private override init() {}
    
    // ✅ Connect to AuthViewModel to get JWT token
    func setAuthViewModel(_ viewModel: AuthViewModel) {
        self.authViewModel = viewModel
        print("🔗 PaymentService connected to AuthViewModel")
    }
    
    // Accept self-signed certificates
    func urlSession(_ session: URLSession, didReceive challenge: URLAuthenticationChallenge, completionHandler: @escaping (URLSession.AuthChallengeDisposition, URLCredential?) -> Void) {
        if challenge.protectionSpace.authenticationMethod == NSURLAuthenticationMethodServerTrust {
            if let serverTrust = challenge.protectionSpace.serverTrust {
                completionHandler(.useCredential, URLCredential(trust: serverTrust))
                return
            }
        }
        completionHandler(.performDefaultHandling, nil)
    }
    
    // ✅ Add JWT auth header instead of Basic auth
    private func addAuthHeader(to request: inout URLRequest) {
        print("🔍 PaymentService: Adding auth header")
        
        guard let token = authViewModel?.jwtToken else {
            print("⚠️ WARNING: No JWT token in PaymentService!")
            print("   Payment requests may fail with 401/403")
            return
        }
        
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        print("✅ Added JWT token to payment request")
    }
    
    func initiatePayment(userId: String, amount: Double, completion: @escaping (Result<String, APIError>) -> Void) {
        guard let url = URL(string: Self.backendBaseURL + "/payments/initiate") else {
            print("❌ Invalid payment initiate URL")
            completion(.failure(.invalidURL))
            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        
        // ✅ Use JWT authentication
        addAuthHeader(to: &request)
        
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        
        let body: [String: Any] = [
            "userId": userId,
            "amount": amount
        ]
        
        do {
            request.httpBody = try JSONSerialization.data(withJSONObject: body)
            
            if let bodyString = String(data: request.httpBody!, encoding: .utf8) {
                print("📤 Payment initiate request body: \(bodyString)")
            }
        } catch {
            print("❌ Failed to serialize request body")
            completion(.failure(.unknown(error)))
            return
        }
        
        print("🚀 Initiating payment to: \(url.absoluteString)")
        print("   User ID: \(userId)")
        print("   Amount: ₹\(amount)")
        
        session.dataTask(with: request) { data, response, error in
            DispatchQueue.main.async {
                if let error = error {
                    print("❌ Network error: \(error.localizedDescription)")
                    completion(.failure(.networkError(error)))
                    return
                }
                
                guard let httpResponse = response as? HTTPURLResponse else {
                    print("❌ Invalid HTTP response")
                    completion(.failure(.badResponse))
                    return
                }
                
                print("📡 Payment initiate response status: \(httpResponse.statusCode)")
                
                if httpResponse.statusCode == 401 || httpResponse.statusCode == 403 {
                    print("❌ Authentication failed for payment")
                    completion(.failure(.authenticationRequired))
                    return
                }
                
                guard (200...299).contains(httpResponse.statusCode) else {
                    print("❌ Server error: \(httpResponse.statusCode)")
                    completion(.failure(.serverError(httpResponse.statusCode)))
                    return
                }
                
                guard let data = data else {
                    print("❌ No data received")
                    completion(.failure(.noData))
                    return
                }
                
                if let jsonString = String(data: data, encoding: .utf8) {
                    print("📋 Payment initiate response: \(jsonString)")
                }
                
                do {
                    if let json = try JSONSerialization.jsonObject(with: data) as? [String: Any],
                       let orderId = json["orderId"] as? String {
                        print("✅ Order ID created: \(orderId)")
                        completion(.success(orderId))
                    } else {
                        print("❌ Invalid response format - missing orderId")
                        completion(.failure(.decodingError(NSError(domain: "PaymentService", code: -1, userInfo: [NSLocalizedDescriptionKey: "Missing orderId"]))))
                    }
                } catch {
                    print("❌ JSON parsing error: \(error)")
                    completion(.failure(.decodingError(error)))
                }
            }
        }.resume()
    }
    
    func confirmPayment(orderId: String, paymentId: String, success: Bool, userId: String, amount: Double, completion: @escaping (Result<Bool, APIError>) -> Void) {
        guard let url = URL(string: Self.backendBaseURL + "/payments/callback") else {
            print("❌ Invalid payment callback URL")
            completion(.failure(.invalidURL))
            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        
        // ✅ Use JWT authentication
        addAuthHeader(to: &request)
        
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        
        let body: [String: Any] = [
            "orderId": orderId,
            "paymentId": paymentId,
            "success": success,
            "userId": userId,
            "amount": amount
        ]
        
        do {
            request.httpBody = try JSONSerialization.data(withJSONObject: body)
            
            if let bodyString = String(data: request.httpBody!, encoding: .utf8) {
                print("📤 Payment callback request body: \(bodyString)")
            }
        } catch {
            print("❌ Failed to serialize callback request")
            completion(.failure(.unknown(error)))
            return
        }
        
        print("🚀 Confirming payment to: \(url.absoluteString)")
        print("   Order ID: \(orderId)")
        print("   Payment ID: \(paymentId)")
        print("   Success: \(success)")
        
        session.dataTask(with: request) { data, response, error in
            DispatchQueue.main.async {
                if let error = error {
                    print("❌ Network error: \(error.localizedDescription)")
                    completion(.failure(.networkError(error)))
                    return
                }
                
                guard let httpResponse = response as? HTTPURLResponse else {
                    print("❌ Invalid HTTP response")
                    completion(.failure(.badResponse))
                    return
                }
                
                print("📡 Payment callback response status: \(httpResponse.statusCode)")
                
                if httpResponse.statusCode == 401 || httpResponse.statusCode == 403 {
                    print("❌ Authentication failed for payment callback")
                    completion(.failure(.authenticationRequired))
                    return
                }
                
                guard (200...299).contains(httpResponse.statusCode) else {
                    print("❌ Server error: \(httpResponse.statusCode)")
                    completion(.failure(.serverError(httpResponse.statusCode)))
                    return
                }
                
                guard let data = data else {
                    print("❌ No data received")
                    completion(.failure(.noData))
                    return
                }
                
                if let jsonString = String(data: data, encoding: .utf8) {
                    print("📋 Payment callback response: \(jsonString)")
                }
                
                do {
                    if let json = try JSONSerialization.jsonObject(with: data) as? [String: Any],
                       let status = json["status"] as? String {
                        let isSuccess = status == "success"
                        print("✅ Payment confirmation result: \(isSuccess)")
                        completion(.success(isSuccess))
                    } else {
                        print("❌ Invalid response format - missing status")
                        completion(.failure(.decodingError(NSError(domain: "PaymentService", code: -1, userInfo: [NSLocalizedDescriptionKey: "Missing status"]))))
                    }
                } catch {
                    print("❌ JSON parsing error: \(error)")
                    completion(.failure(.decodingError(error)))
                }
            }
        }.resume()
    }
}

