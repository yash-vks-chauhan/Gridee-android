# Dummy Data Summary

## Overview
Successfully added dummy data to the `parkingdb` MongoDB database for testing the booking system.

## Data Added

### 1. Parking Lots (3 locations)

| ID | Name | Location | Total Spots | Available |
|----|------|----------|-------------|-----------|
| 68fb2b892e1e37136f71b153 | TP Avenue Parking | T.P. Scheme, Bhopal | 100 | 85 |
| 68fb2b892e1e37136f71b154 | DB City Mall Parking | DB City Mall, Bhopal | 200 | 150 |
| 68fb2b892e1e37136f71b155 | New Market Parking | New Market, Bhopal | 50 | 30 |

### 2. Parking Spots (5 zones)

| Zone Name | Parking Lot | Capacity | Available | Rate/Hour |
|-----------|-------------|----------|-----------|-----------|
| TP Avenue - Zone A | TP Avenue Parking | 50 | 45 | ₹20 |
| TP Avenue - Zone B | TP Avenue Parking | 50 | 40 | ₹15 |
| DB City Mall - Basement 1 | DB City Mall | 100 | 80 | ₹25 |
| DB City Mall - Basement 2 | DB City Mall | 100 | 70 | ₹20 |
| New Market - Main Area | New Market | 50 | 30 | ₹30 |

### 3. Bookings (8 total - 2 each status)

#### Pending Bookings (2)
- **Booking 1**: MP09AB1234 | TP Avenue - Zone A | ₹60 | Tomorrow
- **Booking 2**: MP09XY5678 | DB City Mall - Basement 1 | ₹75 | Day after tomorrow

#### Completed Bookings (2)
- **Booking 1**: MP09CD9012 | TP Avenue - Zone B | ₹45 | 2 days ago
- **Booking 2**: MP09EF3456 | New Market - Main Area | ₹90 | 5 days ago

#### Cancelled Bookings (2)
- **Booking 1**: MP09GH7890 | DB City Mall - Basement 2 | ₹50 | Yesterday
- **Booking 2**: MP09IJ2345 | TP Avenue - Zone A | ₹40 | 3 days ago

#### Active Bookings (2)
- **Booking 1**: MP09KL6789 | DB City Mall - Basement 1 | ₹100 | Currently in use
- **Booking 2**: MP09MN0123 | New Market - Main Area | ₹60 | Currently in use

## Booking Status Distribution

```
Total Bookings: 8
├── Pending: 2 (25%)
├── Active: 2 (25%)
├── Completed: 2 (25%)
└── Cancelled: 2 (25%)
```

## How to View Data

### View all bookings by status:
```bash
# Pending
mongosh parkingdb --eval "db.bookings.find({status: 'pending'}).pretty()"

# Active
mongosh parkingdb --eval "db.bookings.find({status: 'active'}).pretty()"

# Completed
mongosh parkingdb --eval "db.bookings.find({status: 'completed'}).pretty()"

# Cancelled
mongosh parkingdb --eval "db.bookings.find({status: 'cancelled'}).pretty()"
```

### View all parking locations:
```bash
mongosh parkingdb --eval "db.parking_lots.find().pretty()"
```

### View all parking spots:
```bash
mongosh parkingdb --eval "db.parking_spots.find().pretty()"
```

### Count bookings by status:
```bash
mongosh parkingdb --eval "db.bookings.aggregate([{$group: {_id: '$status', count: {$sum: 1}}}])"
```

## Testing in Android App

The Android app should now be able to:
1. **Fetch parking locations** - Shows 3 locations (TP Avenue, DB City Mall, New Market)
2. **View available spots** - Shows 5 different zones across the locations
3. **View bookings by status**:
   - Pending tab: 2 bookings
   - Active tab: 2 bookings
   - Completed tab: 2 bookings
   - Cancelled tab: 2 bookings

## API Endpoints to Test

```bash
# Get all parking lots
GET http://localhost:8080/api/parking-lots

# Get parking spots for a specific lot
GET http://localhost:8080/api/parking-spots?lotId=68fb2b892e1e37136f71b153

# Get user bookings by status
GET http://localhost:8080/api/bookings?status=pending
GET http://localhost:8080/api/bookings?status=active
GET http://localhost:8080/api/bookings?status=completed
GET http://localhost:8080/api/bookings?status=cancelled
```

## Clean Up Data

If you want to remove all dummy data:
```bash
mongosh parkingdb --eval "db.parking_lots.deleteMany({})"
mongosh parkingdb --eval "db.parking_spots.deleteMany({})"
mongosh parkingdb --eval "db.bookings.deleteMany({})"
```

Or run the script again with the delete statements uncommented at the top of `add_dummy_data.js`.
