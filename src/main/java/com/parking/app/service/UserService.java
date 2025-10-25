package com.parking.app.service;

import com.parking.app.config.JwtUtil;
import com.parking.app.dto.UserRequestDto;
import com.parking.app.model.ParkingLot;
import com.parking.app.model.Users;
import com.parking.app.repository.ParkingLotRepository;
import com.parking.app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.regex.Pattern;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ParkingLotRepository parkingLotRepository;
    @Autowired
    private JwtUtil jwtUtil;

    private static final Random RANDOM = new Random();
    // Email Regex (simple, you may replace with a more robust pattern)
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w-\\.]+@[\\w-\\.]+\\.[a-z]{2,}$", Pattern.CASE_INSENSITIVE);

    // Phone Regex (for example, 10-15 digits)
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9]{10,15}$");

    /**
     * Generate a unique 6-digit PIN for check-in authentication
     */
    private String generateUniqueCheckInPin() {
        String pin;
        int attempts = 0;
        int maxAttempts = 100;

        do {
            // Generate 6-digit PIN (100000 to 999999)
            pin = String.format("%06d", 100000 + RANDOM.nextInt(900000));
            attempts++;

            if (attempts > maxAttempts) {
                throw new RuntimeException("Unable to generate unique PIN after " + maxAttempts + " attempts");
            }
        } while (userRepository.findByCheckInPin(pin).isPresent());

        return pin;
    }

    public Users createUser(UserRequestDto userRequest) {
        if (userRequest == null) throw new IllegalArgumentException("User data is required.");

        // Validate required fields
        if (userRequest.getName() == null || userRequest.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Name is required.");
        }
        if (userRequest.getEmail() == null || userRequest.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required.");
        }
        if (!EMAIL_PATTERN.matcher(userRequest.getEmail()).matches()) {
            throw new IllegalArgumentException("Invalid email format.");
        }
        if (userRequest.getPhone() == null || userRequest.getPhone().trim().isEmpty()) {
            throw new IllegalArgumentException("Phone is required.");
        }
        if (!PHONE_PATTERN.matcher(userRequest.getPhone()).matches()) {
            throw new IllegalArgumentException("Invalid phone format.");
        }
        if (userRequest.getParkingLotName() == null || userRequest.getParkingLotName().trim().isEmpty()) {
            throw new IllegalArgumentException("Parking lot name is required.");
        }
        if (userRequest.getPassword() == null || userRequest.getPassword().isEmpty()) {
            throw new IllegalArgumentException("Password is required.");
        }

        // Fetch parking lot by name
        ParkingLot lot = parkingLotRepository.findByName(userRequest.getParkingLotName());
        if (lot == null) {
            throw new IllegalArgumentException("Parking lot not found: " + userRequest.getParkingLotName());
        }

        if (userRepository.findByEmailAndActive(userRequest.getEmail(), true).isPresent()) {
            //TODO: check for deleted user
            throw new IllegalArgumentException("Email already registered.");
        }
        if (userRepository.findByPhoneAndActive(userRequest.getPhone(), true).isPresent()) {
            throw new IllegalArgumentException("Phone already registered.");
        }

        // Create new user entity
        Users user = new Users();
        user.setName(userRequest.getName());
        user.setEmail(userRequest.getEmail());
        user.setPhone(userRequest.getPhone());
        user.setVehicleNumbers(userRequest.getVehicleNumbers());
        user.setParkingLotId(lot.getId());
        user.setParkingLotName(lot.getName());
        user.setWalletCoins(0);
        user.setFirstUser(true);
        user.setCreatedAt(new java.util.Date());

        // Hash password
        String hashedPassword = BCrypt.hashpw(userRequest.getPassword(), BCrypt.gensalt());
        user.setPasswordHash(hashedPassword);

        // Generate unique check-in PIN
        user.setCheckInPin(generateUniqueCheckInPin());

        return userRepository.save(user);
    }


    // Authenticate user by email or phone and plain password (only active users)
    public Users authenticate(String emailOrPhone, String plainPassword) {
        if (emailOrPhone == null || emailOrPhone.trim().isEmpty() || plainPassword == null || plainPassword.isEmpty()) {
            return null;
        }
        // Try to find active user by email or phone
        Users user = userRepository.findByEmailAndActive(emailOrPhone, true).orElse(null);
        if (user == null) {
            user = userRepository.findByPhoneAndActive(emailOrPhone, true).orElse(null);
        }

        if (user != null && BCrypt.checkpw(plainPassword, user.getPasswordHash())) {
            return user;
        }
        return null;
    }

    public List<Users> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<Users> findById(String userId) {
        return userRepository.findById(userId);
    }

    public Users updateUser(String id, UserRequestDto userDetails) {
        Users existingUser = userRepository.findById(id).orElse(null);
        if (existingUser == null || existingUser.isActive() == false) {
            return null;
        }

        // Update only non-null/non-empty fields
        if (userDetails.getName() != null && !userDetails.getName().trim().isEmpty()) {
            existingUser.setName(userDetails.getName());
        }
        if(StringUtils.hasText(userDetails.getParkingLotId())){
            ParkingLot lot = parkingLotRepository.findById(userDetails.getParkingLotId()).orElse(null);
            if(lot==null){
                throw new IllegalArgumentException("Parking lot not found with id: " + userDetails.getParkingLotId());
            }
            existingUser.setParkingLotId(lot.getId());
            existingUser.setParkingLotName(lot.getName());
        }

        if(!CollectionUtils.isEmpty(userDetails.getVehicleNumbers())){
            existingUser.getVehicleNumbers();
        }

        if (userDetails.getEmail() != null && !userDetails.getEmail().trim().isEmpty()) {
            if (!EMAIL_PATTERN.matcher(userDetails.getEmail()).matches()) {
                throw new IllegalArgumentException("Invalid email format.");
            }
            // Check email uniqueness if changed (only active users)
            if (!userDetails.getEmail().equals(existingUser.getEmail()) &&
                    userRepository.findByEmailAndActive(userDetails.getEmail(), true).isPresent()) {
                throw new IllegalArgumentException("Email already registered.");
            }
            existingUser.setEmail(userDetails.getEmail());
        }

        if (userDetails.getPhone() != null && !userDetails.getPhone().trim().isEmpty()) {
            if (!PHONE_PATTERN.matcher(userDetails.getPhone()).matches()) {
                throw new IllegalArgumentException("Invalid phone format.");
            }
            // Check phone uniqueness if changed (only active users)
            if (!userDetails.getPhone().equals(existingUser.getPhone()) &&
                    userRepository.findByPhoneAndActive(userDetails.getPhone(), true).isPresent()) {
                throw new IllegalArgumentException("Phone already registered.");
            }
            existingUser.setPhone(userDetails.getPhone());
        }


        if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
            String hashedPassword = BCrypt.hashpw(userDetails.getPassword(), BCrypt.gensalt());
            existingUser.setPasswordHash(hashedPassword);
        }
        existingUser.setUpdatedAt(Date.from(Instant.now()));
        return userRepository.save(existingUser);
    }

    public Users addUserVehicles(String userId, List<String> vehicleNumbers) {
        Users existingUser = userRepository.findById(userId).orElse(null);
        if (existingUser == null) return null;
        if (vehicleNumbers == null || vehicleNumbers.isEmpty()) {
            throw new IllegalArgumentException("Vehicle numbers list cannot be empty.");
        }
        List<String> currentVehicles = existingUser.getVehicleNumbers();
        if (currentVehicles == null) {
            currentVehicles = new java.util.ArrayList<>();
        }
        for (String v : vehicleNumbers) {
            if (v != null && !v.trim().isEmpty() && !currentVehicles.contains(v.trim())) {
                currentVehicles.add(v.trim());
            }
        }
        existingUser.setVehicleNumbers(currentVehicles);
        return userRepository.save(existingUser);
    }

    public void deleteUser(String id) {
        Users existingUser = userRepository.findById(id).orElse(null);
        if (existingUser == null) return;
        existingUser.setActive(false);
        existingUser.setUpdatedAt(Date.from(Instant.now()));
        userRepository.save(existingUser);
    }
}
