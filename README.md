# RedBus Backend

A minimal-but-production-style RedBus-like backend built with Java 21 and Spring Boot.

## Features

- **User Authentication**: Signup/login with JWT tokens
- **Trip Search**: Search trips by source city, destination city, and date
- **Multi-Segment Booking**: Book trips with multiple connected segments
- **Payment Processing**: Mock payment gateway with state machine
- **User Trip Management**: View upcoming trips for logged-in users

## Quick Start

### Option 1: Run with Docker

#### Prerequisites
- Docker 20.10+ (make sure Docker daemon is running)

#### Steps

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd redbus
   ```

2. **Build the Docker image**
   ```bash
   docker build -t redbus-backend .
   ```

3. **Run the container**
   ```bash
   docker run -p 8080:8080 redbus-backend
   ```

4. **Verify the application is running**
   ```bash
   curl http://localhost:8080/healthz
   ```

The application will be available at `http://localhost:8080`

#### Docker Compose (Alternative)

Use the included `docker-compose.yml` file:
```bash
docker compose up --build
```

### Option 2: Run Locally

#### Prerequisites
- Java 21+
- Maven 3.6+

#### Steps

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd redbus
   ```

2. **Build and run the application**
   ```bash
   mvn clean compile
   mvn spring-boot:run
   ```

The application will start on `http://localhost:8080`

## Web Interface

The application includes a complete web interface accessible through your browser:

### User Interface
- **Login Page**: `http://localhost:8080/` - User authentication
- **Signup Page**: `http://localhost:8080/signup` - User registration
- **Search Page**: `http://localhost:8080/search` - Search for bus trips
- **Booking Page**: `http://localhost:8080/book/{tripId}` - Book selected trips
- **Payment Page**: `http://localhost:8080/payment` - Process payments
- **All Bookings**: `http://localhost:8080/bookings/all-bookings` - View user bookings

### Admin Interface
- **Admin Dashboard**: `http://localhost:8080/admin` - Admin portal
- **Add Trip**: `http://localhost:8080/admin/add-trip` - Create new trips
- **View All Trips**: `http://localhost:8080/admin/trips` - Manage existing trips

**Note**: Admin interface requires no authentication for demo purposes.
**Note**: Should first create trips from admin panel, and then try user bookings

## Requirements

### Functional Requirements

1. **User Management**
   - User registration with email/password
   - User login with JWT token generation
   - User profile management

2. **Trip Management**
   - Search trips by source city, destination city, and date
   - Support multi-segment trips (connecting routes)
   - Real-time seat availability checking
   - Trip creation and management (admin)
   - Real-time seat availability tracking
   - Atomic seat reservation during booking
   - Inventory updates on booking confirmation/cancellation


3. **Booking Management**
   - Create bookings with seat reservation
   - Support partial trip bookings (source to intermediate destination)
   - Booking status tracking (RESERVED → CONFIRMED)
   - Booking expiration (5-minute TTL for reservations)

4. **Payment Processing**
   - Mock payment gateway integration
   - Payment state machine (PENDING → COMPLETED/FAILED)
   - Idempotent payment processing
   - Payment confirmation webhooks

### Non-Functional Requirements

1. **Performance**
   - Search API: 400-5000 RPS
   - Booking API: ~40 RPS
   - Payment API: ~40 RPS
   - Daily Active Users: ~10M

2. **Availability**
   - High availability for read paths (search, upcoming trips)
   - Strong consistency for inventory and payments
   - 99.9% uptime target

3. **Scalability**
   - Horizontal scaling support
   - Caching for frequently accessed data
   - Database connection pooling

4. **Security**
   - JWT-based authentication
   - Password hashing with BCrypt
   - Input validation and sanitization
   - HTTPS enforcement

## High-Level Design (HLD)

### How It Works

#### Happy Path Flow

1. **User Authentication**
   - Auth service validates credentials against User DB
   - JWT token issued with user ID and expiration (24h)
   - Token stored in browser for subsequent requests

2. **Trip Search**
   - Inventory service checks Redis cache for seat availability
   - Cache miss triggers read-through to Inventory DB
   - Results cached in Redis with TTL (5 minutes)
   - Multi-segment trip matching using graph traversal

3. **Booking Creation**
   - Order service acquires lock on trip parts
   - Atomic seat reservation in Inventory DB
   - Booking created with RESERVED status
   - Payment intent created with external gateway
   - 5-minute TTL timer starts for reservation

4. **Payment Processing**
   - Idempotent payment processing with Idempotency-Key
   - Payment state machine: PENDING → COMPLETED
   - Booking status updated: RESERVED → CONFIRMED
   - Webhook sent to confirm payment (mock implementation)

## Implementation Details

### Service Responsibilities & Boundaries

#### Authentication Service
- **Responsibilities**: User registration, login, JWT token management
- **Boundaries**: User CRUD, password hashing, token validation
- **Dependencies**: User DB, BCryptPasswordEncoder

#### Inventory Service  
- **Responsibilities**: Trip management, seat availability, multi-segment routing
- **Boundaries**: Trip/TripPart CRUD, availability calculations, search algorithms
- **Dependencies**: Inventory DB, Redis cache, City/Bus repositories

#### Order Service
- **Responsibilities**: Booking lifecycle, payment coordination, state management
- **Boundaries**: Booking CRUD, payment integration, idempotency handling
- **Dependencies**: Order DB, Payment gateway, Inventory service


#### Saga Pattern Implementation
1. **Booking Saga**
   ```
   START TRANSACTION
   ├── Reserve seats in TripPart (with lock)
   ├── Create booking (RESERVED status)
   ├── Create payment intent
   └── COMMIT
   
   On Failure:
   ├── Release reserved seats
   ├── Mark booking as CANCELLED
   └── ROLLBACK
   ```

2. **Payment Saga**
   ```
   START TRANSACTION
   ├── Update payment status (PENDING → COMPLETED)
   ├── Update booking status (RESERVED → CONFIRMED)
   └── COMMIT
   
   On Failure:
   ├── Update payment status (PENDING → FAILED)
   ├── Schedule seat release (TTL-based)
   └── ROLLBACK
   ```

#### Consistency Levels
- **Strong Consistency**: Seat reservations, payment states
- **Eventual Consistency**: Search results, user session data
- **Read-Your-Writes**: Booking status after creation

### Security Specifications

#### JWT Token Structure
```json
{
  "sub": "user_id",
  "iat": 1640995200,
  "exp": 1641081600,
  "roles": ["USER"]
}
```


## Key Implementation Details

### Trip Parts Seat Management
- **Per-Part Availability**: Available seats are tracked per `TripParts` (tripPartId) rather than per entire trip
- **Partial Trip Bookings**: Users can book specific segments of a trip (e.g., from city A to city B on a trip going A→B→C)
- **Atomic Operations**: Seat reservation is atomic across all selected trip parts
- **Accurate Pricing**: Pricing calculations based on actual trip part occupancy

### Enhanced Search with Trip Segments
- **Segment-Based Search**: Search finds trips that have segments from source to destination, not just direct trips
- **Multi-Segment Support**: Can find partial routes (e.g., search "BLR→MUM" finds trips like "BLR→MUM→DEL")
- **Connected Route Search**: Finds trips where users can travel from source to destination by combining multiple segments of the same trip
- **Filtered Results**: Search results only show trip parts that match the requested route
- **Smart Indexing**: On-demand index population for search performance with segment matching
- **City ID Based**: Search uses city IDs (e.g., "BLR", "MUM") for efficient lookup instead of city names

### Idempotency
- All booking and payment operations require an `Idempotency-Key` header
- Keys are scoped to user + operation + payload hash
- Results are cached for 24 hours to handle duplicate requests

### Concurrency
- Uses `ReentrantLock` for trip-level locking during booking
- 5-minute TTL for reserved bookings with automatic cleanup
- Thread-safe in-memory storage using `ConcurrentHashMap`

### Pricing Strategies
- **Slab Pricing**: ₹50 for first 10km, then ₹25 per 10km
- **Flat Per KM**: ₹5 per kilometer
- **Surge Pricing**: Based on occupancy ratio (50% base + occupancy multiplier)

## Data Model

### Cities
- Bangalore (BLR), Mumbai (MUM), Delhi (DEL), Chennai (CHN), Kolkata (KOL)
- Pune (PUN), Hyderabad (HYD), Ahmedabad (AMD), Jaipur (JPR), Lucknow (LKO)
- Kanpur (KNP), Nagpur (NGP), Indore (IDR), Bhopal (BPL), Cochin (COK)

### Sample Trip Structure
```
Trip: BLR → MUM → DEL
├── TripParts[0]: BLR → MUM (sequence: 1, 08:00-14:00)
└── TripParts[1]: MUM → DEL (sequence: 2, 18:00-23:00)
```

## Architecture

- **Framework**: Spring Boot 3.2.0 with Java 21
- **Security**: JWT authentication with Spring Security
- **Storage**: In-memory with `ConcurrentHashMap` for thread safety
- **Build**: Maven with Lombok for clean code
- **Validation**: Bean Validation for request validation


## Development Notes

- No tests included (as requested)
- All data is seeded on startup
- Mock payment gateway for demonstration
- Production-ready with proper error handling and logging
