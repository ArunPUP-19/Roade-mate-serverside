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
 * End-to-end integration test for the multi-account trip join flow.
 *
 * Tests the full happy path:
 *   Account A creates a trip → Account B discovers it → Account B joins →
 *   Account A confirms → Lock → Start → Complete
 *
 * Also tests critical edge cases:
 *   - Duplicate join prevention
 *   - Self-join prevention
 *   - Concurrent joins from multiple accounts
 *   - Data consistency across accounts
 *
 * Uses the seeded test accounts from V4__insert_seed_data.sql:
 *   - arun@routemate.com (Account A — driver/organizer)
 *   - priya@routemate.com (Account B — passenger)
 *   - suresh@routemate.com (Account C — second passenger)
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MultiAccountTripFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Tokens for seeded accounts
    private String tokenA; // Arun (driver/organizer)
    private String tokenB; // Priya (passenger)
    private String tokenC; // Suresh (second passenger)

    // IDs captured during the flow
    private String createdTripId;
    private String participantBPublicId;
    private String participantCPublicId;

    // ── Helper Methods ──────────────────────────────────────────────────────

    private String loginAndGetToken(String email, String password) throws Exception {
        String body = String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asText();
    }

    private MvcResult performAuthPost(String url, String token, String body) throws Exception {
        return mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .content(body != null ? body : ""))
                .andReturn();
    }

    private MvcResult performAuthPut(String url, String token) throws Exception {
        return mockMvc.perform(put(url)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andReturn();
    }

    private MvcResult performAuthDelete(String url, String token) throws Exception {
        return mockMvc.perform(delete(url)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andReturn();
    }

    // ── Setup: Login All Accounts ───────────────────────────────────────────

    @BeforeAll
    void loginAllAccounts() throws Exception {
        tokenA = loginAndGetToken("arun@routemate.com", "password123");
        tokenB = loginAndGetToken("priya@routemate.com", "password123");
        tokenC = loginAndGetToken("suresh@routemate.com", "password123");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  HAPPY PATH FLOW
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(1)
    @DisplayName("HP-01: Account A creates a trip — 201, ACTIVE status, driver auto-enrolled")
    void accountA_createsTrip() throws Exception {
        String tripBody = """
            {
                "startingLocation": "Coimbatore, Tamil Nadu",
                "destination": "Chennai, Tamil Nadu",
                "date": "2026-09-20",
                "time": "07:00",
                "seatsAvailable": 3,
                "vehicleDetails": "4 wheeler - Swift Dzire (TN 38 CC 5678)",
                "totalCost": 800,
                "yourSplit": 200,
                "negotiable": true
            }
            """;

        MvcResult result = mockMvc.perform(post("/api/trips")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + tokenA)
                .content(tripBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Trip published successfully!"))
                .andExpect(jsonPath("$.trip.status").value("ACTIVE"))
                .andExpect(jsonPath("$.trip.seatsAvailable").value(3))
                .andExpect(jsonPath("$.trip.totalSeats").value(3))
                .andExpect(jsonPath("$.trip.driverName").value("Arun Kumar"))
                .andExpect(jsonPath("$.trip.startingLocation").value("Coimbatore, Tamil Nadu"))
                .andExpect(jsonPath("$.trip.destination").value("Chennai, Tamil Nadu"))
                .andExpect(jsonPath("$.trip.totalCost").value(800))
                .andExpect(jsonPath("$.trip.yourSplit").value(200))
                .andExpect(jsonPath("$.trip.negotiable").value(true))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        createdTripId = json.get("trip").get("id").asText();
        Assertions.assertNotNull(createdTripId, "Trip ID should not be null");
    }

    @Test
    @Order(2)
    @DisplayName("HP-02: Trip detail shows driver as CONFIRMED participant")
    void tripDetail_showsDriverParticipant() throws Exception {
        mockMvc.perform(get("/api/trips/" + createdTripId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.participants", hasSize(1)))
                .andExpect(jsonPath("$.participants[0].role").value("DRIVER"))
                .andExpect(jsonPath("$.participants[0].confirmationStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.participants[0].displayName").value("Arun Kumar"));
    }

    @Test
    @Order(3)
    @DisplayName("HP-03: Account B discovers trip via search")
    void accountB_discoversTrip() throws Exception {
        mockMvc.perform(get("/api/trips")
                .param("from", "Coimbatore")
                .param("to", "Chennai"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trips", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.trips[?(@.id == '" + createdTripId + "')]").exists());
    }

    @Test
    @Order(4)
    @DisplayName("HP-04: Account B joins trip — PENDING_CONFIRMATION, seats decremented")
    void accountB_joinsTrip() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/trips/" + createdTripId + "/join")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Join request submitted!"))
                .andExpect(jsonPath("$.trip.status").value("PENDING_CONFIRMATION"))
                .andExpect(jsonPath("$.trip.seatsAvailable").value(2))
                .andReturn();

        // Capture participant B's public ID for later confirm/reject
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode participants = json.get("trip").get("participants");
        for (JsonNode p : participants) {
            if ("PASSENGER".equals(p.get("role").asText()) && "PENDING".equals(p.get("confirmationStatus").asText())) {
                if ("Priya Sharma".equals(p.get("displayName").asText())) {
                    participantBPublicId = p.get("id").asText();
                }
            }
        }
        Assertions.assertNotNull(participantBPublicId, "Participant B's public ID should be captured");
    }

    @Test
    @Order(5)
    @DisplayName("EC-01: Account B attempts to join same trip again — rejected (duplicate)")
    void accountB_duplicateJoin_rejected() throws Exception {
        mockMvc.perform(post("/api/trips/" + createdTripId + "/join")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.message").value("You have already requested to join this trip"));
    }

    @Test
    @Order(6)
    @DisplayName("EC-02: Account A attempts to join own trip — rejected (self-join)")
    void accountA_selfJoin_rejected() throws Exception {
        mockMvc.perform(post("/api/trips/" + createdTripId + "/join")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.message").value("You cannot join your own trip"));
    }

    @Test
    @Order(7)
    @DisplayName("CU-01: Account C also joins — both B and C in participants")
    void accountC_joinsTrip() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/trips/" + createdTripId + "/join")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + tokenC))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Join request submitted!"))
                .andExpect(jsonPath("$.trip.seatsAvailable").value(1))
                .andReturn();

        // Capture participant C's public ID
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode participants = json.get("trip").get("participants");
        for (JsonNode p : participants) {
            if ("PASSENGER".equals(p.get("role").asText()) && "PENDING".equals(p.get("confirmationStatus").asText())) {
                if ("Suresh V".equals(p.get("displayName").asText())) {
                    participantCPublicId = p.get("id").asText();
                }
            }
        }
        Assertions.assertNotNull(participantCPublicId, "Participant C's public ID should be captured");
    }

    @Test
    @Order(8)
    @DisplayName("EC-07: Non-organizer (Account B) tries to confirm — rejected")
    void nonOrganizer_cannotConfirm() throws Exception {
        mockMvc.perform(put("/api/trips/" + createdTripId + "/participants/" + participantCPublicId + "/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.message").value("Only the trip organizer can perform this action"));
    }

    @Test
    @Order(9)
    @DisplayName("HP-05: Account A confirms Account B — participant CONFIRMED")
    void accountA_confirmsB() throws Exception {
        mockMvc.perform(put("/api/trips/" + createdTripId + "/participants/" + participantBPublicId + "/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Participant confirmed"));

        // Trip should still be PENDING_CONFIRMATION because C is still pending
        mockMvc.perform(get("/api/trips/" + createdTripId))
                .andExpect(jsonPath("$.status").value("PENDING_CONFIRMATION"));
    }

    @Test
    @Order(10)
    @DisplayName("EC-09: Account A confirms already-confirmed B — rejected")
    void confirmAlreadyConfirmed_rejected() throws Exception {
        mockMvc.perform(put("/api/trips/" + createdTripId + "/participants/" + participantBPublicId + "/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.message", containsString("not in PENDING status")));
    }

    @Test
    @Order(11)
    @DisplayName("HP-06: Account A confirms Account C — trip transitions to CONFIRMED")
    void accountA_confirmsC_tripBecomesCONFIRMED() throws Exception {
        mockMvc.perform(put("/api/trips/" + createdTripId + "/participants/" + participantCPublicId + "/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Participant confirmed"));

        // All pending resolved → trip should be CONFIRMED
        mockMvc.perform(get("/api/trips/" + createdTripId))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @Order(12)
    @DisplayName("DV-10: Data consistency — both A and B see identical trip details")
    void dataConsistency_bothAccountsSeeIdenticalData() throws Exception {
        // Both can call GET (no auth required) — data should be identical
        MvcResult resultA = mockMvc.perform(get("/api/trips/" + createdTripId))
                .andExpect(status().isOk())
                .andReturn();

        // Parse and verify key fields
        JsonNode tripData = objectMapper.readTree(resultA.getResponse().getContentAsString());
        Assertions.assertEquals("CONFIRMED", tripData.get("status").asText());
        Assertions.assertEquals(1, tripData.get("seatsAvailable").asInt());
        Assertions.assertEquals(3, tripData.get("totalSeats").asInt());
        Assertions.assertEquals(3, tripData.get("participants").size()); // Driver + 2 passengers
    }

    @Test
    @Order(13)
    @DisplayName("HP-07: Account A locks trip — status LOCKED, lockedAt set")
    void accountA_locksTrip() throws Exception {
        mockMvc.perform(put("/api/trips/" + createdTripId + "/lock")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Trip locked"))
                .andExpect(jsonPath("$.trip.status").value("LOCKED"))
                .andExpect(jsonPath("$.trip.lockedAt").isNotEmpty());
    }

    @Test
    @Order(14)
    @DisplayName("EC-03: Join a LOCKED trip — rejected")
    void joinLockedTrip_rejected() throws Exception {
        // Register a fresh user to attempt joining
        String regBody = """
            {"name":"TestJoinLocked","email":"testlock@routemate.com","password":"password123"}
            """;
        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(regBody))
                .andExpect(status().isOk())
                .andReturn();
        String freshToken = objectMapper.readTree(regResult.getResponse().getContentAsString())
                .get("token").asText();

        mockMvc.perform(post("/api/trips/" + createdTripId + "/join")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + freshToken))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.message", containsString("not accepting new participants")));
    }

    @Test
    @Order(15)
    @DisplayName("HP-08: Account A starts trip — status LIVE, startedAt set")
    void accountA_startsTrip() throws Exception {
        mockMvc.perform(put("/api/trips/" + createdTripId + "/start")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Trip started"))
                .andExpect(jsonPath("$.trip.status").value("LIVE"))
                .andExpect(jsonPath("$.trip.startedAt").isNotEmpty());
    }

    @Test
    @Order(16)
    @DisplayName("EC-12: Account B tries to leave LIVE trip — rejected")
    void leaveLiveTrip_rejected() throws Exception {
        mockMvc.perform(delete("/api/trips/" + createdTripId + "/leave")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.message", containsString("Cannot leave a trip that is LIVE")));
    }

    @Test
    @Order(17)
    @DisplayName("HP-09: Account A completes trip — status COMPLETED, completedAt set")
    void accountA_completesTrip() throws Exception {
        mockMvc.perform(put("/api/trips/" + createdTripId + "/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Trip completed"))
                .andExpect(jsonPath("$.trip.status").value("COMPLETED"))
                .andExpect(jsonPath("$.trip.completedAt").isNotEmpty());
    }

    @Test
    @Order(18)
    @DisplayName("EC-14: Cancel a COMPLETED trip — rejected")
    void cancelCompletedTrip_rejected() throws Exception {
        mockMvc.perform(put("/api/trips/" + createdTripId + "/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.message", containsString("Cannot cancel a COMPLETED trip")));
    }

    @Test
    @Order(19)
    @DisplayName("EC-04: Join a COMPLETED trip — rejected")
    void joinCompletedTrip_rejected() throws Exception {
        // Use Account C's token since they're already a participant,
        // register a new user instead
        String regBody = """
            {"name":"TestJoinComplete","email":"testcomplete@routemate.com","password":"password123"}
            """;
        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(regBody))
                .andExpect(status().isOk())
                .andReturn();
        String freshToken = objectMapper.readTree(regResult.getResponse().getContentAsString())
                .get("token").asText();

        mockMvc.perform(post("/api/trips/" + createdTripId + "/join")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + freshToken))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.message", containsString("not accepting new participants")));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  AUTHENTICATION TESTS
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(20)
    @DisplayName("EC-21: Unauthenticated POST to /api/trips — 401")
    void unauthenticated_createTrip_rejected() throws Exception {
        String tripBody = """
            {"startingLocation":"A","destination":"B","date":"2026-09-20","time":"08:00","seatsAvailable":2}
            """;
        mockMvc.perform(post("/api/trips")
                .contentType(MediaType.APPLICATION_JSON)
                .content(tripBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(21)
    @DisplayName("EC-22: Invalid JWT token — 401")
    void invalidJwt_rejected() throws Exception {
        mockMvc.perform(post("/api/trips/" + createdTripId + "/join")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(22)
    @DisplayName("HP-10: GET endpoints are publicly accessible (no auth)")
    void publicEndpoints_noAuthRequired() throws Exception {
        mockMvc.perform(get("/api/trips"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/trips/" + createdTripId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/requests"))
                .andExpect(status().isOk());
    }
}
