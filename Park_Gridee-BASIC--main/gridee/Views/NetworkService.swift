//import Foundation
//
//// MARK: - CheckInRequestDto for encoding
//struct CheckInRequestDto: Codable {
//    let mode: String           // Should be "VEHICLE_NUMBER" for your use case
//    let qrCode: String?
//    let vehicleNumber: String?
//    let pin: String?
//
//    enum CodingKeys: String, CodingKey {
//        case mode, qrCode, vehicleNumber, pin
//    }
//
//    init(mode: String, qrCode: String? = nil, vehicleNumber: String? = nil, pin: String? = nil) {
//        self.mode = mode
//        self.qrCode = qrCode
//        self.vehicleNumber = vehicleNumber
//        self.pin = pin
//    }
//}
//
//// MARK: - NetworkError Enum
//enum NetworkError: Error {
//    case invalidURL
//    case requestFailed
//    case responseUnsuccessful
//    case decodingFailed
//    case encodingFailed
//}
//
//// MARK: - NetworkService (using your real Bookings model)
//class NetworkService {
//    static let shared = NetworkService()
//
//    private init() {}
//
//    // MARK: - CheckIn for Operator
//    func checkIn(vehicleNumber: String, completion: @escaping (Result<Bookings, NetworkError>) -> Void) {
//        makeRequest(endpoint: "checkin", vehicleNumber: vehicleNumber, completion: completion)
//    }
//
//    // MARK: - CheckOut for Operator
//    func checkOut(vehicleNumber: String, completion: @escaping (Result<Bookings, NetworkError>) -> Void) {
//        makeRequest(endpoint: "checkout", vehicleNumber: vehicleNumber, completion: completion)
//    }
//
//    // MARK: - Helper to Handle CheckIn/CheckOut Requests
//    private func makeRequest(
//        endpoint: String,
//        vehicleNumber: String,
//        completion: @escaping (Result<Bookings, NetworkError>) -> Void
//    ) {
//        guard let url = URL(string: "https://your-api-endpoint.com/\(endpoint)") else {
//            return completion(.failure(.invalidURL))
//        }
//
//        var request = URLRequest(url: url)
//        request.httpMethod = "POST"
//        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
//        
//        // Optional: Add authorization header if needed
//        // request.setValue("Bearer \(AuthViewModel.shared.token)", forHTTPHeaderField: "Authorization")
//
//        let dto = CheckInRequestDto(mode: "VEHICLE_NUMBER", qrCode: nil, vehicleNumber: vehicleNumber, pin: nil)
//        
//        guard let jsonData = try? JSONEncoder().encode(dto) else {
//            return completion(.failure(.encodingFailed))
//        }
//        
//        request.httpBody = jsonData
//
//        URLSession.shared.dataTask(with: request) { data, response, error in
//            if let error = error {
//                return completion(.failure(.requestFailed))
//            }
//            
//            guard let httpResponse = response as? HTTPURLResponse,
//                  (200...299).contains(httpResponse.statusCode) else {
//                return completion(.failure(.responseUnsuccessful))
//            }
//            
//            guard let data = data else {
//                return completion(.failure(.responseUnsuccessful))
//            }
//            
//            do {
//                let booking = try JSONDecoder().decode(Bookings.self, from: data)
//                completion(.success(booking))
//            } catch {
//                completion(.failure(.decodingFailed))
//            }
//        }.resume()
//    }
//}
