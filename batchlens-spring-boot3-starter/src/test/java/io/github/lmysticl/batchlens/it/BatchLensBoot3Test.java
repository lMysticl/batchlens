package io.github.lmysticl.batchlens.it;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    classes = TestBatchApplication.class,
    properties = {
      "spring.batch.job.enabled=false",
      "spring.datasource.url=jdbc:h2:mem:batchlens-boot3;DB_CLOSE_DELAY=-1",
      "batchlens.enabled=true",
      "batchlens.api-token=" + BatchLensBoot3Test.TOKEN,
      "batchlens.operations-enabled=true"
    })
@AutoConfigureMockMvc
class BatchLensBoot3Test {
  static final String TOKEN = "boot3-contract-test-token-0123456789";
  private static final String AUTH = "Bearer " + TOKEN;

  @Autowired MockMvc mvc;

  @Test
  void rejectsRequestsWithoutTheConfiguredToken() throws Exception {
    mvc.perform(get("/batchlens/api/jobs"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string("WWW-Authenticate", "Bearer realm=\"BatchLens\""));
    mvc.perform(get("/batchlens/api/jobs").header("Authorization", "Bearer wrong"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void failedRunRestartsTheSameInstanceFromItsCheckpoint() throws Exception {
    TestBatchApplication.FAIL_NEXT.set(true);
    mvc.perform(get("/batchlens/api/jobs").header("Authorization", AUTH))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.name == 'flaky')].registered").value(true));

    var failed =
        mvc.perform(
                post("/batchlens/api/jobs/flaky/start")
                    .header("Authorization", AUTH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"parameters\":{\"run\":\"restart-contract\"}}"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.status").value("FAILED"))
            .andExpect(jsonPath("$.parameters.run").value("restart-contract"))
            .andExpect(jsonPath("$.steps", hasSize(2)))
            .andExpect(jsonPath("$.steps[0].status").value("COMPLETED"))
            .andExpect(jsonPath("$.steps[1].status").value("FAILED"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String failedId = JsonPath.read(failed, "$.id");
    String instanceId = JsonPath.read(failed, "$.instanceId");

    var restarted =
        mvc.perform(
                post("/batchlens/api/executions/" + failedId + "/restart")
                    .header("Authorization", AUTH))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.instanceId").value(instanceId))
            // The completed first step is not executed again on restart.
            .andExpect(jsonPath("$.steps", hasSize(1)))
            .andExpect(jsonPath("$.steps[0].name").value("load"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String restartedId = JsonPath.read(restarted, "$.id");

    mvc.perform(
            post("/batchlens/api/executions/" + restartedId + "/restart")
                .header("Authorization", AUTH))
        .andExpect(status().isConflict());
    mvc.perform(
            post("/batchlens/api/executions/" + restartedId + "/stop")
                .header("Authorization", AUTH))
        .andExpect(status().isConflict());
    mvc.perform(get("/batchlens/api/executions?job=flaky&limit=5").header("Authorization", AUTH))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(restartedId))
        .andExpect(jsonPath("$[1].id").value(failedId));
  }

  @Test
  void reportsMissingExecutionsAndInvalidInput() throws Exception {
    mvc.perform(get("/batchlens/api/executions/987654321").header("Authorization", AUTH))
        .andExpect(status().isNotFound());
    mvc.perform(get("/batchlens/api/executions?job=flaky&limit=0").header("Authorization", AUTH))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/batchlens/api/jobs/missing/start")
                .header("Authorization", AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"parameters\":{}}"))
        .andExpect(status().isNotFound());
    mvc.perform(get("/batchlens/api/info").header("Authorization", AUTH))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("BatchLens"))
        .andExpect(jsonPath("$.operationsEnabled").value(true));
  }

  @Test
  void servesTheConsolePage() throws Exception {
    mvc.perform(get("/batchlens/"))
        .andExpect(status().is3xxRedirection())
        .andExpect(header().string("Location", "/batchlens/index.html"));
  }
}
