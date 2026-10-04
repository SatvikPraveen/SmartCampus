package com.smartcampus.api;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Boots the full application context and exercises the HTTP contract. */
@SpringBootTest
@AutoConfigureMockMvc
class TimetablingControllerTest {

    private static final String TINY = """
            {
              "problem": {
                "days": 1, "periodsPerDay": 3,
                "rooms": [{"id": "small", "capacity": 2}, {"id": "big", "capacity": 3}],
                "events": [
                  {"id": "e0", "instructorId": "X", "studentIds": ["a", "b"]},
                  {"id": "e1", "instructorId": "Y", "studentIds": ["b", "c"]},
                  {"id": "e2", "instructorId": "X", "studentIds": ["d"]},
                  {"id": "e3", "studentIds": ["e", "f", "g"]}
                ]
              },
              "solver": "%s", "seed": 7, "iterations": 2000
            }
            """;

    @Autowired
    private MockMvc mvc;

    @Test
    void listsSolvers() throws Exception {
        mvc.perform(get("/api/v1/timetabling/solvers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("dsatur")))
                .andExpect(jsonPath("$[*].name", hasItem("sa")));
    }

    @Test
    void solvesTinyInstanceFeasibly() throws Exception {
        for (String solver : new String[] {"greedy", "largest-degree", "dsatur", "sa"}) {
            mvc.perform(post("/api/v1/timetabling/solve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(TINY.formatted(solver)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.solver").value(solver))
                    .andExpect(jsonPath("$.cost.feasible").value(true))
                    .andExpect(jsonPath("$.assignments", hasSize(4)))
                    .andExpect(jsonPath("$.assignments[*].day", everyItem(greaterThanOrEqualTo(0))));
        }
    }

    @Test
    void rejectsUnknownSolverWithProblemDetail() throws Exception {
        mvc.perform(post("/api/v1/timetabling/solve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TINY.formatted("quantum")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void rejectsInvalidProblem() throws Exception {
        mvc.perform(post("/api/v1/timetabling/solve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"problem\": {\"days\": 0, \"periodsPerDay\": 3, \"rooms\": [], \"events\": []}}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void generatesBenchmarkInstance() throws Exception {
        mvc.perform(get("/api/v1/timetabling/instances/small").param("seed", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events", hasSize(100)))
                .andExpect(jsonPath("$.rooms", hasSize(5)));
        mvc.perform(get("/api/v1/timetabling/instances/huge"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actuatorHealthIsUp() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
