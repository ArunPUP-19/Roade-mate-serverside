package com.routemate.backend.trip;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for trip lifecycle edge cases, validation errors,
 * invalid state transitions, and authorization enforcement.
 *
 * This complements MultiAccountTripFlowTest by focusing on negative
 * and boundary scenarios rather than the happy path.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TripLifecycleEdgeCaseTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String tokenDriver;     // arun@routemate.com — creates trips
    private String tokenPassenger;  // priya@routemate.com — joins trips
    private String tokenPassenger2; // suresh@routemate.com — another passenger

    // Trip IDs created during tests
    private String cancelTestTripId;
    private String rejectTestTripId;
    private String leaveTestTripId;
    private String fullTripId;
    private String lockPendingTripId;

    private String loginAndGetToken(String email) throws Exception {
        String body = String.format("{\"email\":\"%s\",\"password\":\"password123\"}", email);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private String createTrip(String token, int seats) throws Exception {
        String body = String.format("""
            {
                "startingLocation": "Test City %d",
                "destination": "Dest City %d",
                "date": "2026-10-01",
                "time": "09:00",
                "seatsAvailable": %d,
                "vehicleDetails": "Test Vehicle"
            }
            """, seats, seats, seats);
        MvcResult result = mockMvc.perform(post("/api/trips")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("trip").get("id").asText();
    }

    private String joinTrip(String tripId, String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/trips/" + tripId + "/join")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        // Find the new PENDING PASSENGER participant public ID
        JsonNode participants = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("trip").get("participants");
        for (JsonNode p : participants) {
            if ("PASSENGER".equals(p.get("role").asText()) && "PENDING".equals(p.get("confirmationStatus").asText())) {
                return p.get("id").asText();
            }
        }
        return null;
    }

    @BeforeAll
    void setup() throws Exception {
        tokenDriver = loginAndGetToken("arun@routemate.com");
        tokenPassenger = loginAndGetToken("priya@routemate.com");
        tokenPassenger2 = loginAndGetToken("suresh@routemate.com");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  VALIDATION TESTS (DV-01 through DV-09)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(1)
    @DisplayName("DV-01: Create trip with blank startingLocation — 400")
    void createTrip_blankStartingLocation() throws Exception {
        String body = """
            {"startingLocation":"","destination":"Chennai","date":"2026-10-01","time":"09:00","seatsAvailable":2}
            """;
        mockMvc.perform(post("/api/trips")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + tokenDriver)
                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    @Order(2)
    @DisplayName("DV-02: Create trip with blank destination — 400")
    void createTrip_blankDestination() throws Exception {
        String body = """
            {"startingLocation":"Coimbatore","destination":"","date":"2026-10-01","time":"09:00","seatsAvailable":2}
            """;
        mockMvc.perform(post("/api/trips")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + tokenDriver)
                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    @Order(3)
    @DisplayName("DV-03: Create trip with blank date — 400")
    void createTrip_blankDate() throws Exception {
        String body = """
            {"startingLocation":"Coimbatore","destination":"Chennai","date":"","time":"09:00","seatsAvailable":2}
            """;
        mockMvc.perform(post("/api/trips")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + tokenDriver)
                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    @Order(4)
    @DisplayName("DV-04: Create trip with blank time — 400")
    void createTrip_blankTime() throws Exception {
        String body = """
            {"startingLocation":"Coimbatore","destination":"Chennai","date":"2026-10-01","time":"","seatsAvailable":2}
            """;
        mockMvc.perform(post("/api/trips")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + tokenDriver)
                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    @Order(5)
    @DisplayName("DV-05: Create trip with seatsAvailable=0 — 400")
    void createTrip_zeroSeats() throws Exception {
        String body = """
            {"startingLocation":"Coimbatore","destination":"Chennai","date":"2026-10-01","time":"09:00","seatsAvailable":0}
            """;
        mockMvc.perform(post("/api/trips")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + tokenDriver)
                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    @Order(6)
    @DisplayName("DV-06: Create trip with negative seatsAvailable — 400")
    void createTrip_negativeSeats() throws Exception {
        String body = """
            {"startingLocation":"Coimbatore","destination":"Chennai","date":"2026-10-01","time":"09:00","seatsAvailable":-1}
            """;
        mockMvc.perform(post("/api/trips")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + tokenDriver)
                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    @Order(7)
    @DisplayName("DV-07: Register with duplicate email — 409")
    void register_duplicateEmail() throws Exception {
        String body = """
            {"name":"Duplicate","email":"arun@routemate.com","password":"password123"}
            """;
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.message").value("Email is already in use"));
    }

    @Test
    @Order(8)
    @DisplayName("DV-08: Login with wrong password — 401")
    void login_wrongPassword() throws Exception {
        String body = """
            {"email":"arun@routemate.com","password":"wrongpassword"}
            """;
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(9)
    @DisplayName("DV-09: Register with invalid email format — 400")
    void register_invalidEmail() throws Exception {
        String body = """
            {"name":"BadEmail","email":"not-an-email","password":"password123"}
            """;
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(10)
    @DisplayName("EC-20: Access non-existent trip — 404")
    void accessNonExistentTrip() throws Exception {
        mockMvc.perform(get("/api/trips/99999"))
                .andExpect(status().isNotFound());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TRIP CANCELLATION FLOW
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(20)
    @DisplayName("CANCEL-01: Create trip, have passenger join, then cancel — trip CANCELLED")
    void cancelTrip_withParticipant() throws Exception {
        cancelTestTripId = createTrip(tokenDriver, 3);

        // Passenger joins
        joinTrip(cancelTestTripId, tokenPassenger);

        // Cancel
        mockMvc.perform(put("/api/trips/" + cancelTestTripId + "/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + tokenDriver))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trip.status").value("CANCELLED"));
    }

    @Test
    @Order(21)
    @DisplayName("EC-05: Join a CANCELLED trip — rejected")
    void joinCancelledTrip_rejected() throws Exception {
        mockMvc.perform(post("/api/trips/" + cancelTestTripId + "/join")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + tokenPassenger2))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.message", containsString("not accepting new participants")));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  REJECT FLOW
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(30)
    @DisplayName("REJECT-01: Reject a pending passenger — REJECTED, seats restored")
    void rejectPassenger_seatsRestored() throws Exception {
        rejectTestTripId = createTrip(tokenDriver, 2);

        // Passenger joins (seats: 2 → 1)
        String pId = joinTrip(rejectTestTripId, tokenPassenger);

        // Reject
        MvcResult result = mockMvc.perform(put("/api/trips/" + rejectTestTripId + "/participants/" + pId + "/reject")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + tokenDriver))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Participant rejected"))
                .andReturn();

        // Seats should be restored to 2
        JsonNode trip = objectMapper.readTree(result.getResponse().getContentAsString()).get("trip");
        Assertions.assertEquals(2, trip.get("seatsAvailable").asInt());
    }

    @Test
    @Order(31)
    @DisplayName("EC-10: Reject an already-rejected participant — rejected")
    void rejectAlreadyRejected() throws Exception {
        // Create fresh scenario
        String tripId = createTrip(tokenDriver, 2);
        String pId = joinTrip(tripId, tokenPassenger);

        // First reject succeeds
        mockMvc.perform(put("/api/trips/" + tripId + "/participants/" + pId + "/reject")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + tokenDriver))
                .andExpect(status().isOk());

        // Second reject fails
        mockMvc.perform(put("/api/trips/" + tripId + "/participants/" + pId + "/reject")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + tokenDriver))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.message", containsString("not in PENDING status")));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  LEAVE TRIP FLOW
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(40)
    @DisplayName("LEAVE-01: Passenger leaves trip — seats restored, participant CANCELLED")
    void passengerLeaves_seatsRestored() throws Exception {
        leaveTestTripId = createTrip(tokenDriver, 3);
        joinTrip(leaveTestTripId, tokenPassenger); // seats: 3→2

        MvcResult result = mockMvc.perform(delete("/api/trips/" + leaveTestTripId + "/leave")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + tokenPassenger))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("You have left the trip"))
                .andReturn();

        // Seats should be restored to 3
        JsonNode trip = objectMapper.readTree(result.getResponse().getContentAsString()).get("trip");
        Assertions.assertEquals(3, trip.get("seatsAvailable").asInt());
    }

    @Test
    @Order(41)
    @DisplayName("EC-11: Driver tries to leave own trip — rejected")
    void driverCannotLeave() throws Exception {
        String tripId = createTrip(tokenDriver, 2);

        mockMvc.perform(delete("/api/trips/" + tripId + "/leave")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + tokenDriver))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.message", containsString("Cancel the trip instead")));
    }

    @Test
    @Order(42)
    @DisplayName("LEAVE-02: Non-participant tries to leave — rejected")
    void nonParticipantCannotLeave() throws Exception {
        String tripId = createTrip(tokenDriver, 2);

        mockMvc.perform(delete("/api/trips/" + tripId + "/leave")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + tokenPassenger))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.message", containsString("not a participant")));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  INVALID STATE TRANSITIONS
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(50)
    @DisplayName("EC-16: Start an ACTIVE trip — rejected (must be LOCKED/CONFIRMED)")
    void startActiveTrip_rejected() throws Exception {
        String tripId = createTrip(tokenDriver, 3);

        mockMvc.perform(put("/api/trips/" + tripId + "/start")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + tokenDriver))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.message", containsString("LOCKED or CONFIRMED to start")));
    }

    @Test
    @Order(51)
    @DisplayName("EC-17: Complete a non-LIVE trip — rejected")
    void completeNonLiveTrip_rejected() throws Exception {
        String tripId = createTrip(tokenDriver, 3);

        mockMvc.perform(put("/api/trips/" + tripId + "/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + tokenDriver))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.message", containsString("Only LIVE trips can be completed")));
    }

    @Test
    @Order(52)
    @DisplayName("EC-18: Lock an ACTIVE trip (no pending participants) — rejected")
    void lockActiveTrip_rejected() throws Exception {
        String tripId = createTrip(tokenDriver, 3);

        mockMvc.perform(put("/api/trips/" + tripId + "/lock")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + tokenDriver))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.message", containsString("CONFIRMED or PENDING_CONFIRMATION to lock")));
    }

    @Test
    @Order(53)
    @DisplayName("EC-08: Non-organizer tries to lock trip — rejected")
    void nonOrganizerCannotLock() throws Exception {
        String tripId = createTrip(tokenDriver, 2);
        joinTrip(tripId, tokenPassenger);

        mockMvc.perform(put("/api/trips/" + tripId + "/lock")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + tokenPassenger))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.message", containsString("Only the trip organizer")));
    }

    @Test
    @Order(54)
    @DisplayName("EC-08b: Non-organizer tries to start trip — rejected")
    void nonOrganizerCannotStart() throws Exception {
        // Create trip, join, confirm, to reach CONFIRMED state
        String tripId = createTrip(tokenDriver, 2);
        String pId = joinTrip(tripId, tokenPassenger);
        mockMvc.perform(put("/api/trips/" + tripId + "/participants/" + pId + "/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + tokenDriver))
                .andExpect(status().isOk());

        // Non-organizer tries to start
        mockMvc.perform(put("/api/trips/" + tripId + "/start")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + tokenPassenger))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.message", containsString("Only the trip organizer")));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  LOCK WITH PENDING PARTICIPANTS (auto-reject)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(60)
    @DisplayName("EC-19: Lock trip with pending participants — auto-rejects them, seats restored")
    void lockWithPending_autoRejects() throws Exception {
        lockPendingTripId = createTrip(tokenDriver, 3);
        joinTrip(lockPendingTripId, tokenPassenger);   // seats: 3→2
        joinTrip(lockPendingTripId, tokenPassenger2);   // seats: 2→1

        // Lock — should auto-reject both pending participants
        MvcResult result = mockMvc.perform(put("/api/trips/" + lockPendingTripId + "/lock")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + tokenDriver))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trip.status").value("LOCKED"))
                .andReturn();

        // Verify seats restored
        JsonNode trip = objectMapper.readTree(result.getResponse().getContentAsString()).get("trip");
        Assertions.assertEquals(3, trip.get("seatsAvailable").asInt());

        // Verify both passengers are REJECTED
        JsonNode participants = trip.get("participants");
        int rejectedCount = 0;
        for (JsonNode p : participants) {
            if ("PASSENGER".equals(p.get("role").asText()) && "REJECTED".equals(p.get("confirmationStatus").asText())) {
                rejectedCount++;
            }
        }
        Assertions.assertEquals(2, rejectedCount, "Both pending passengers should be auto-rejected");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FULL TRIP (seats exhausted)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(70)
    @DisplayName("EC-06: Join when 0 seats available — trip becomes FULL, next join rejected")
    void fullTrip_noMoreJoins() throws Exception {
        fullTripId = createTrip(tokenDriver, 1); // Only 1 seat!

        // First join takes the last seat
        joinTrip(fullTripId, tokenPassenger); // seats: 1→0

        // Verify trip is FULL
        mockMvc.perform(get("/api/trips/" + fullTripId))
                .andExpect(jsonPath("$.status").value("FULL"));

        // Second join should be rejected
        mockMvc.perform(post("/api/trips/" + fullTripId + "/join")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + tokenPassenger2))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.message", containsString("not accepting new participants")));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  RIDE REQUEST TESTS
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(80)
    @DisplayName("HP-08: Create ride request — 201, visible in GET /api/requests")
    void createRideRequest() throws Exception {
        String body = """
            {"startingLocation":"Erode, Tamil Nadu","destination":"Salem, Tamil Nadu","date":"2026-10-05","preferredTime":"10:00"}
            """;
        mockMvc.perform(post("/api/requests")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + tokenPassenger)
                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Ride request published successfully!"))
                .andExpect(jsonPath("$.request.startingLocation").value("Erode, Tamil Nadu"));

        // Verify it's visible in the list
        mockMvc.perform(get("/api/requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requests", hasSize(greaterThanOrEqualTo(2)))); // seed + new
    }

    @Test
    @Order(81)
    @DisplayName("RIDE-REQ: Unauthenticated POST to /api/requests — 401")
    void createRideRequest_unauthenticated() throws Exception {
        String body = """
            {"startingLocation":"A","destination":"B","date":"2026-10-05"}
            """;
        mockMvc.perform(post("/api/requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isUnauthorized());
    }
}
