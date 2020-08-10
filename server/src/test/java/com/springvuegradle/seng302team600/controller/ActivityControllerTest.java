package com.springvuegradle.seng302team600.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.springvuegradle.seng302team600.model.Activity;
import com.springvuegradle.seng302team600.model.Email;
import com.springvuegradle.seng302team600.model.User;
import com.springvuegradle.seng302team600.model.UserRole;
import com.springvuegradle.seng302team600.payload.UserRegisterRequest;
import com.springvuegradle.seng302team600.repository.ActivityParticipantRepository;
import com.springvuegradle.seng302team600.repository.ActivityRepository;
import com.springvuegradle.seng302team600.repository.EmailRepository;
import com.springvuegradle.seng302team600.repository.UserRepository;
import com.springvuegradle.seng302team600.service.ActivityTypeService;
import com.springvuegradle.seng302team600.service.UserAuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ActivityController.class)
class ActivityControllerTest {

    @MockBean
    private UserRepository userRepository;
    @MockBean
    private EmailRepository emailRepository;
    @MockBean
    private ActivityRepository activityRepository;
    @MockBean
    private UserAuthenticationService userAuthenticationService;
    @MockBean
    private ActivityTypeService activityTypeService;
    @MockBean
    private ActivityParticipantRepository activityParticipantRepository;
    @Autowired
    private MockMvc mvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    private static final Long DEFAULT_USER_ID = 1L;
    private static final Long DEFAULT_EMAIL_ID = 1L;
    private static final Long DEFAULT_ACTIVITY_ID = 1L;
    private static Long activityCount = 0L;

    private User dummyUser1;
    private User dummyUser2; // Used when a second user is required
    private final String validToken = "valid";
    private Set<Activity> activityMockTable = new HashSet<>();
    private Set<User> userMockTable = new HashSet<>();

    private boolean defaultAdminIsRegistered;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        dummyUser1 = new User();
        dummyUser1.setToken(validToken);
        dummyUser2 = new User();
        dummyUser2.setToken("validToken");
        defaultAdminIsRegistered = false;

        // Mocking ActivityTypeService
        when(activityTypeService.getMatchingEntitiesFromRepository(Mockito.any())).thenAnswer(i -> i.getArgument(0));
        when(activityRepository.findAllByUserId(Mockito.any(Long.class))).thenAnswer(i -> {
            List<Activity> activities = new ArrayList<>();
            for (Activity activity : activityMockTable) {
                activities.add(activity);
            }
            return activities;
        });
        // Mocking userAuthenticationService
        when(userAuthenticationService.findByUserId(Mockito.any(String.class), Mockito.any(Long.class))).thenAnswer(i -> dummyUser1);
        when(userAuthenticationService.hasAdminPrivileges(Mockito.any())).thenAnswer(i ->
                ((User) i.getArgument(0)).getToken().equals(validToken));
    }

    /**
     * Mock ActivityType repository actions
     */
    @BeforeEach
    void setupActivityRepository() {
        activityCount = 0L;
        // Save
        when(activityRepository.save(Mockito.any(Activity.class))).thenAnswer(i -> {
            Activity newActivity = i.getArgument(0);
            ReflectionTestUtils.setField(newActivity, "creatorUserId", DEFAULT_USER_ID);
            ReflectionTestUtils.setField(newActivity, "activityId", (DEFAULT_ACTIVITY_ID + activityCount++));
            newActivity.setCreatorUserId(DEFAULT_USER_ID);
            activityMockTable.add(i.getArgument(0));
            return newActivity;
        });
        // FindByActivityId
        when(activityRepository.findByActivityId(Mockito.any(Long.class))).thenAnswer(i -> {
            for (Activity activity: activityMockTable) {
                if (activity.getActivityId() == i.getArgument(0)) {
                    return activity;
                }
            }
            return null;
        });
        when(activityRepository.findAllByUserId(Mockito.any(Long.class))).thenAnswer(i -> {
            for (Activity activity : activityMockTable) {
                if (activity.getCreatorUserId() == i.getArgument(0)) {
                    return activity;
                }
            }
            return null;
        });
    }

    private final String newUserJson1 = JsonConverter.toJson(true,
            "lastname", "Cucumber",
            "firstname", "Larry",
            "primary_email", "larry@gmail.com",
            "password", "larrysPassword",
            "date_of_birth", "2002-1-20",
            "gender", "Female");

    private final String newUserJson2 = JsonConverter.toJson(true,
            "lastname", "Pocket",
            "firstname", "Poly",
            "middlename", "Michelle",
            "nickname", "Pino",
            "primary_email", "poly@pocket.com",
            "password", "somepwd0",
            "date_of_birth", "2000-11-11",
            "gender", "Female");

    private void setupMockingNoEmailDummyUser1(String json) throws JsonProcessingException {
        UserRegisterRequest regReq;
        regReq = objectMapper.treeToValue(objectMapper.readTree(json), UserRegisterRequest.class);
        dummyUser1 = dummyUser1.builder(regReq);
        Email dummyEmail = new Email(dummyUser1.getPrimaryEmail(), true, dummyUser1);
        when(userRepository.save(Mockito.any(User.class))).thenAnswer(i -> {
            User user = i.getArgument(0);
            if (user.getRole() == UserRole.DEFAULT_ADMIN) {
                defaultAdminIsRegistered = true;
            }
            return dummyUser1;
        });
    }


    private void setupMockingNoEmailDummyUser2(String json) throws JsonProcessingException {
        UserRegisterRequest regReq;
        regReq = objectMapper.treeToValue(objectMapper.readTree(json), UserRegisterRequest.class);
        dummyUser2 = dummyUser2.builder(regReq);
        Email dummyEmail = new Email(dummyUser2.getPrimaryEmail(), true, dummyUser2);
        when(userRepository.save(Mockito.any(User.class))).thenAnswer(i -> {
            User user = i.getArgument(0);
            if (user.getRole() == UserRole.DEFAULT_ADMIN) {
                defaultAdminIsRegistered = true;
            }
            return dummyUser2;
        });
    }



    // ----------- Tests -----------

    private final String newActivity1Json = JsonConverter.toJson(true,
            "activity_name", "Kaikoura Coast Track race",
            "description", "A big and nice race on a lovely peninsula",
            "activity_type", new Object[]{
                    "Astronomy", "Hiking"
            },
            "continuous", false,
            "start_time", "2020-02-20T08:00:00+1300",
            "end_time", "2020-02-20T08:00:00+1300",
            "location", "Kaikoura, NZ");

    /**
     * Test successful creation of new activity.
     */
    @Test
    void newActivity() throws Exception {

        MockHttpServletRequestBuilder httpReq = MockMvcRequestBuilders.post("/profiles/{profileId}/activities", DEFAULT_USER_ID)
                .content(newActivity1Json)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);

        MvcResult result = mvc.perform(httpReq)
                .andExpect(status().isCreated())
                .andReturn();
        assertNotNull(result.getResponse());
    }

    /**
     * Test successful deletion of an activity.
     */
    @Test
    void deleteActivity() throws Exception {
        Activity activityInRepo = objectMapper.readValue(newActivity1Json, Activity.class);
        activityRepository.save(activityInRepo);

        MockHttpServletRequestBuilder httpReqDelete = MockMvcRequestBuilders.delete("/profiles/{profileId}/activities/{activityId}", DEFAULT_USER_ID, DEFAULT_ACTIVITY_ID)
                .header("Token", validToken);
        MvcResult result = mvc.perform(httpReqDelete).andExpect(status().isOk()).andReturn();
        assertNotNull(result.getResponse());
    }

    private final String newActivityEditJson = JsonConverter.toJson(true,
            "activity_name", "Kaikoura Coast Track race",
            "description", "A big and nice race on a lovely peninsula",
            "activity_type", new Object[]{
                    "Gymnastics",
                    "Hiking"
            },
            "continuous", false,
            "start_time", "2020-02-20T08:00:00+1300",
            "end_time", "2020-02-20T08:00:00+1300",
            "location", "Kaikoura, NZ");

    /**
     * Test successful edit/update of an activity details
     */
    @Test
    void editActivity() throws Exception {
        Activity activityInRepo = objectMapper.readValue(newActivity1Json, Activity.class);
        activityRepository.save(activityInRepo);

        //Activity newActivity = objectMapper.readValue(newActivityEditJson, Activity.class);
        MockHttpServletRequestBuilder httpReqEdit = MockMvcRequestBuilders.put("/profiles/{profileId}/activities/{activityId}", DEFAULT_USER_ID, DEFAULT_ACTIVITY_ID)
                .header("Token", validToken)
                .content(newActivityEditJson)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);

        MvcResult result = mvc.perform(httpReqEdit).andExpect(status().isOk()).andReturn();

        assertNotNull(result.getResponse());
    }

    private final String newActivityWrongDateFormatJson = JsonConverter.toJson(true,
            "activity_name", "Port Hills Rock Climbing",
            "description", "Cattlestop Crag lead climbing",
            "activity_type", new Object[]{
                    "Rock Climbing", "Mountaineering"
            },
            "continuous", false,
            "start_time", "2020-02-20T08:00:00Z",
            "end_time", "2020-02-20T08:00:00Z",
            "location", "Christchurch, NZ");

    /**
     * Tests that Bad Request is returned when the date format is not correct, in this case representing time zone
     * with a 'Z'.  It must be represented with +/- time.  See Java SimpleDateFormat and @JsonFormat in Activity.
     */
    @Test
    void newActivityWrongDateFormat() throws Exception {

        MockHttpServletRequestBuilder httpReq = MockMvcRequestBuilders.post("/profiles/{profileId}/activities", DEFAULT_USER_ID)
                .content(newActivityWrongDateFormatJson)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);

        MvcResult result = mvc.perform(httpReq)
                .andExpect(status().isBadRequest())
                .andReturn();
        assertNotNull(result.getResponse());
    }

    private final String newActivity2Json = JsonConverter.toJson(true,
            "activity_name", "Kaikoura Coast Track race",
            "description", "A big and nice race on a lovely peninsula",
            "activity_type", new Object[]{
                    "Astronomy", "Hiking"
            },
            "continuous", false,
            "start_time", "2020-02-20T08:00:00+1300",
            "end_time", "2020-02-20T08:00:00+1300",
            "location", "Kaikoura, NZ");


    /**
     * Test successful get request of activities created by specific user
     */
    @Test
    void getUserActivities() throws Exception {
        //Todo: Change the assertNotNull to assertEquals(1, repoSize)
        //Save some activities to get
        Activity activity = objectMapper.readValue(newActivity1Json, Activity.class);
        activityRepository.save(activity);

        MockHttpServletRequestBuilder httpReq = MockMvcRequestBuilders.get("/profiles/{profileId}/activities", DEFAULT_USER_ID)
                .header("Token", validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);
        MvcResult result = mvc.perform(httpReq)
                .andExpect(status().isOk())
                .andReturn();
        //int contentLength = result.getResponse().getContentLength();
        assertNotNull(result); //should have one item in this list as saved above
    }


    /**
     * Test successful get request of activity.
     */
    @Test
    void getActivity() throws Exception {
        Activity activityInRepo = objectMapper.readValue(newActivity2Json, Activity.class);
        activityRepository.save(activityInRepo);
        MockHttpServletRequestBuilder httpReq = MockMvcRequestBuilders.get("/activities/{activityId}", DEFAULT_ACTIVITY_ID)
                .content(newActivity2Json)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);

        MvcResult result = mvc.perform(httpReq)
                .andExpect(status().isOk())
                .andReturn();
        String jsonResponseStr = result.getResponse().getContentAsString();
        Activity activityReceived = objectMapper.readValue(jsonResponseStr, Activity.class);  // Convert JSON to Activity obj

        assertEquals(activityRepository.findByActivityId(DEFAULT_ACTIVITY_ID), activityReceived);
    }


    @Test
    void getParticipantsOfActivitySuccess() throws Exception {
        Activity activityInRepo = objectMapper.readValue(newActivity2Json, Activity.class);
        setupMockingNoEmailDummyUser1(newUserJson1);
        setupMockingNoEmailDummyUser2(newUserJson2);
        activityInRepo.addParticipant(dummyUser1);
        activityInRepo.addParticipant(dummyUser2);
        activityRepository.save(activityInRepo);

        MockHttpServletRequestBuilder httpReq = MockMvcRequestBuilders.get("/activities/{activityId}/participants", DEFAULT_ACTIVITY_ID)
                .accept(MediaType.APPLICATION_JSON);

        MvcResult result = mvc.perform(httpReq)
                .andExpect(status().isOk())
                .andReturn();
        String jsonResponseStr = result.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(jsonResponseStr);
        List<User> expectedParticipants = new ArrayList<>(activityRepository.findByActivityId(DEFAULT_ACTIVITY_ID).getParticipants());
        for (int i = 0; i < jsonNode.size(); i++) {
            ((ObjectNode) jsonNode.get(i)).remove("timedOut");
            User participant = objectMapper.treeToValue(jsonNode.get(i), User.class);
            assertEquals(participant.toString(), expectedParticipants.get(i).toString());
        }
    }
}
