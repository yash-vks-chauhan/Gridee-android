package com.parking.app.service;

import com.parking.app.config.JwtUtil;
import com.parking.app.constants.Role;
import com.parking.app.dto.UpdateUserRequestDto;
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
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w-\\.]+@[\\w-\\.]+\\.[a-z]{2,}$",
            Pattern.CASE_INSENSITIVE);

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
        if (userRequest == null)
            throw new IllegalArgumentException("User data is required.");

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
        if (userRequest.getPassword() == null || userRequest.getPassword().isEmpty()) {
            throw new IllegalArgumentException("Password is required.");
        }

        Optional<ParkingLot> lot = Optional.empty();
        if (StringUtils.hasText(userRequest.getParkingLotName())) {
            lot = parkingLotRepository.findByName(userRequest.getParkingLotName().trim());
            if (lot.isEmpty()) {
                throw new IllegalArgumentException("Parking lot not found: " + userRequest.getParkingLotName());
            }
        }

        if (userRepository.findByEmailAndActive(userRequest.getEmail(), true).isPresent()) {
            // TODO: check for deleted user
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
        if (lot.isPresent()) {
            user.setParkingLotId(lot.get().getId());
            user.setParkingLotName(lot.get().getName());
        } else {
            user.setParkingLotId(null);
            user.setParkingLotName(null);
        }
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

    public Users updateUser(String id, UpdateUserRequestDto userDetails) {
        Users existingUser = userRepository.findById(id).orElse(null);
        if (existingUser == null || existingUser.isActive() == false) {
            return null;
        }

        // Update only non-null/non-empty fields
        if (userDetails.getName() != null && !userDetails.getName().trim().isEmpty()) {
            existingUser.setName(userDetails.getName());
        }
        if (StringUtils.hasText(userDetails.getParkingLotName())) {
            ParkingLot lot = parkingLotRepository.findByName(userDetails.getParkingLotName()).orElse(null);
            if (lot == null) {
                throw new IllegalArgumentException(
                        "Parking lot not found with name: " + userDetails.getParkingLotName());
            }
            existingUser.setParkingLotId(lot.getId());
            existingUser.setParkingLotName(lot.getName());
        }

        if (!CollectionUtils.isEmpty(userDetails.getVehicleNumbers())) {
            List<String> sanitized = userDetails.getVehicleNumbers().stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .distinct()
                    .toList();
            existingUser.setVehicleNumbers(sanitized);
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
        if (existingUser == null)
            return null;
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

    /**
     * Create or return an existing user for social sign-in (e.g., Google).
     * Only requires a verified email; other fields are optional.
     */
    public Users upsertSocialUser(String email, String name) {
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("Email is required for social sign-in");
        }
        String normalizedEmail = email.trim().toLowerCase();

        Users existing = userRepository.findByEmailAndActive(normalizedEmail, true).orElse(null);
        if (existing != null) {
            // Normalize optional fields to avoid nulls in responses
            if (existing.getPhone() == null) {
                existing.setPhone("");
            }
            if (existing.getVehicleNumbers() == null) {
                existing.setVehicleNumbers(new java.util.ArrayList<>());
            }
            if (!StringUtils.hasText(existing.getRole())) {
                existing.setRole(Role.USER.name());
            }
            if (existing.getCheckInPin() == null) {
                existing.setCheckInPin(generateUniqueCheckInPin());
            }
            userRepository.save(existing);
            return existing;
        }

        Users user = new Users();
        user.setEmail(normalizedEmail);
        user.setName(StringUtils.hasText(name) ? name.trim() : normalizedEmail);
        user.setVehicleNumbers(new java.util.ArrayList<>());
        user.setPhone("");
        user.setWalletCoins(0);
        user.setFirstUser(true);
        user.setActive(true);
        user.setRole(Role.USER.name());
        user.setCheckInPin(generateUniqueCheckInPin());
        user.setCreatedAt(new Date());
        user.setUpdatedAt(new Date());
        return userRepository.save(user);
    }

    /**
     * Return an existing active user by email (no auto-creation).
     * Normalizes optional fields to avoid nulls in responses.
     */
    public Users getActiveUserByEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("Email is required for social sign-in");
        }
        String normalizedEmail = email.trim().toLowerCase();

        return userRepository.findByEmailAndActive(normalizedEmail, true)
                .map(user -> {
                    if (user.getPhone() == null) {
                        user.setPhone("");
                    }
                    if (user.getVehicleNumbers() == null) {
                        user.setVehicleNumbers(new java.util.ArrayList<>());
                    }
                    if (!StringUtils.hasText(user.getRole())) {
                        user.setRole(Role.USER.name());
                    }
                    if (user.getCheckInPin() == null) {
                        user.setCheckInPin(generateUniqueCheckInPin());
                    }
                    return userRepository.save(user);
                })
                .orElse(null);
    }

    public void deleteUser(String id) {
        Users existingUser = userRepository.findById(id).orElse(null);
        if (existingUser == null)
            return;
        existingUser.setActive(false);
        existingUser.setUpdatedAt(Date.from(Instant.now()));
        userRepository.save(existingUser);
    }
}
