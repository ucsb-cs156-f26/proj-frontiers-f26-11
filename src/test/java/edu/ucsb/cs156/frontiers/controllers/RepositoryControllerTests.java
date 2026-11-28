package edu.ucsb.cs156.frontiers.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.annotations.WithInstructorCoursePermissions;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.jobs.CreateStudentOrStaffRepositoriesJob;
import edu.ucsb.cs156.frontiers.jobs.CreateTeamRepositoriesJob;
import edu.ucsb.cs156.frontiers.jobs.DeleteRepoJob;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.services.CurrentUserService;
import edu.ucsb.cs156.frontiers.services.GithubTeamService;
import edu.ucsb.cs156.frontiers.services.RepositoryService;
import edu.ucsb.cs156.jobs.entities.Job;
import edu.ucsb.cs156.jobs.services.JobService;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MvcResult;

@Slf4j
@WebMvcTest(controllers = RepositoryController.class)
public class RepositoryControllerTests extends ControllerTestCase {
  @MockitoBean private CourseRepository courseRepository;

  @MockitoBean private JobService service;

  @MockitoBean private RepositoryService repositoryService;

  @MockitoBean private GithubTeamService githubTeamService;

  @Autowired private CurrentUserService currentUserService;
  @Autowired private ObjectMapper objectMapper;

  @Test
  @WithMockUser(roles = {"INSTRUCTOR"})
  public void not_the_creator() throws Exception {
    Course course = Course.builder().instructorEmail("test@example.com").build();
    doReturn(Optional.of(course)).when(courseRepository).findById(eq(2L));
    MvcResult response =
        mockMvc
            .perform(
                post("/api/repos/createRepos")
                    .with(csrf())
                    .param("courseId", "2")
                    .param("repoPrefix", "repo1")
                    .param("permissions", "WRITE"))
            .andExpect(status().isForbidden())
            .andReturn();
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void not_registered_org() throws Exception {
    Course course =
        Course.builder()
            .courseName("course")
            .instructorEmail(currentUserService.getUser().getEmail())
            .build();
    doReturn(Optional.of(course)).when(courseRepository).findById(eq(2L));
    MvcResult response =
        mockMvc
            .perform(
                post("/api/repos/createRepos")
                    .with(csrf())
                    .param("courseId", "2")
                    .param("repoPrefix", "repo1")
                    .param("permissions", "WRITE"))
            .andExpect(status().isBadRequest())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals("NoLinkedOrganizationException", json.get("type"));
    assertEquals(
        "No linked GitHub Organization to course. Please link a GitHub Organization first.",
        json.get("message"));
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void just_no_install_id() throws Exception {
    Course course =
        Course.builder()
            .courseName("course")
            .orgName("ucsb-cs156")
            .instructorEmail(currentUserService.getUser().getEmail())
            .build();
    doReturn(Optional.of(course)).when(courseRepository).findById(eq(2L));
    MvcResult response =
        mockMvc
            .perform(
                post("/api/repos/createRepos")
                    .with(csrf())
                    .param("courseId", "2")
                    .param("repoPrefix", "repo1")
                    .param("permissions", "WRITE"))
            .andExpect(status().isBadRequest())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals("NoLinkedOrganizationException", json.get("type"));
    assertEquals(
        "No linked GitHub Organization to course. Please link a GitHub Organization first.",
        json.get("message"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void job_actually_fires_with_instructor() throws Exception {
    Course course =
        Course.builder()
            .id(2L)
            .orgName("ucsb-cs156")
            .installationId("1234")
            .courseName("course")
            .instructorEmail(currentUserService.getUser().getEmail())
            .build();
    doReturn(Optional.of(course)).when(courseRepository).findById(eq(2L));
    Job job = Job.builder().status("processing").build();
    doReturn(job).when(service).runAsJob(any(CreateStudentOrStaffRepositoriesJob.class));
    MvcResult response =
        mockMvc
            .perform(
                post("/api/repos/createRepos")
                    .with(csrf())
                    .param("courseId", "2")
                    .param("repoPrefix", "repo1")
                    .param("permissions", "WRITE"))
            .andExpect(status().isOk())
            .andReturn();

    String expectedJson = objectMapper.writeValueAsString(job);
    String actualJson = response.getResponse().getContentAsString();
    assertEquals(expectedJson, actualJson);
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void job_actually_fires() throws Exception {
    Course course =
        Course.builder()
            .id(2L)
            .orgName("ucsb-cs156")
            .installationId("1234")
            .courseName("course")
            .instructorEmail(currentUserService.getUser().getEmail())
            .build();
    doReturn(Optional.of(course)).when(courseRepository).findById(eq(2L));
    Job job = Job.builder().status("processing").build();
    doReturn(job).when(service).runAsJob(any(CreateStudentOrStaffRepositoriesJob.class));
    MvcResult response =
        mockMvc
            .perform(
                post("/api/repos/createRepos")
                    .with(csrf())
                    .param("courseId", "2")
                    .param("repoPrefix", "repo1")
                    .param("permissions", "WRITE"))
            .andExpect(status().isOk())
            .andReturn();

    String expectedJson = objectMapper.writeValueAsString(job);
    String actualJson = response.getResponse().getContentAsString();
    assertEquals(expectedJson, actualJson);
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void notFound() throws Exception {
    Course course =
        Course.builder()
            .courseName("course")
            .instructorEmail(currentUserService.getUser().getEmail())
            .build();
    doReturn(Optional.empty()).when(courseRepository).findById(eq(2L));
    MvcResult response =
        mockMvc
            .perform(
                post("/api/repos/createRepos")
                    .with(csrf())
                    .param("courseId", "2")
                    .param("repoPrefix", "repo1")
                    .param("permissions", "WRITE"))
            .andExpect(status().isNotFound())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("Course with id 2 not found", json.get("message"));
  }

  @Test
  @WithMockUser(roles = {"INSTRUCTOR"})
  public void team_repo_job_not_the_creator() throws Exception {
    Course course = Course.builder().instructorEmail("test@example.com").build();
    doReturn(Optional.of(course)).when(courseRepository).findById(eq(2L));
    MvcResult response =
        mockMvc
            .perform(
                post("/api/repos/createTeamRepos")
                    .with(csrf())
                    .param("courseId", "2")
                    .param("repoPrefix", "repo1")
                    .param("permissions", "WRITE"))
            .andExpect(status().isForbidden())
            .andReturn();
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void team_repo_job_not_registered_org() throws Exception {
    Course course =
        Course.builder()
            .courseName("course")
            .instructorEmail(currentUserService.getUser().getEmail())
            .build();
    doReturn(Optional.of(course)).when(courseRepository).findById(eq(2L));
    MvcResult response =
        mockMvc
            .perform(
                post("/api/repos/createTeamRepos")
                    .with(csrf())
                    .param("courseId", "2")
                    .param("repoPrefix", "repo1")
                    .param("permissions", "WRITE"))
            .andExpect(status().isBadRequest())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals("NoLinkedOrganizationException", json.get("type"));
    assertEquals(
        "No linked GitHub Organization to course. Please link a GitHub Organization first.",
        json.get("message"));
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void team_repo_job_just_no_install_id() throws Exception {
    Course course =
        Course.builder()
            .courseName("course")
            .orgName("ucsb-cs156")
            .instructorEmail(currentUserService.getUser().getEmail())
            .build();
    doReturn(Optional.of(course)).when(courseRepository).findById(eq(2L));
    MvcResult response =
        mockMvc
            .perform(
                post("/api/repos/createTeamRepos")
                    .with(csrf())
                    .param("courseId", "2")
                    .param("repoPrefix", "repo1")
                    .param("permissions", "WRITE"))
            .andExpect(status().isBadRequest())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals("NoLinkedOrganizationException", json.get("type"));
    assertEquals(
        "No linked GitHub Organization to course. Please link a GitHub Organization first.",
        json.get("message"));
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void team_repo_job_actually_fires() throws Exception {
    Course course =
        Course.builder()
            .id(2L)
            .orgName("ucsb-cs156")
            .installationId("1234")
            .courseName("course")
            .instructorEmail(currentUserService.getUser().getEmail())
            .build();
    doReturn(Optional.of(course)).when(courseRepository).findById(eq(2L));
    Job job = Job.builder().status("processing").build();
    doReturn(job).when(service).runAsJob(any(CreateTeamRepositoriesJob.class));
    MvcResult response =
        mockMvc
            .perform(
                post("/api/repos/createTeamRepos")
                    .with(csrf())
                    .param("courseId", "2")
                    .param("repoPrefix", "repo1")
                    .param("permissions", "WRITE"))
            .andExpect(status().isOk())
            .andReturn();

    String expectedJson = objectMapper.writeValueAsString(job);
    String actualJson = response.getResponse().getContentAsString();
    assertEquals(expectedJson, actualJson);
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void team_repo_job_notFound() throws Exception {
    Course course =
        Course.builder()
            .courseName("course")
            .instructorEmail(currentUserService.getUser().getEmail())
            .build();
    doReturn(Optional.empty()).when(courseRepository).findById(eq(2L));
    MvcResult response =
        mockMvc
            .perform(
                post("/api/repos/createTeamRepos")
                    .with(csrf())
                    .param("courseId", "2")
                    .param("repoPrefix", "repo1")
                    .param("permissions", "WRITE"))
            .andExpect(status().isNotFound())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("Course with id 2 not found", json.get("message"));
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void team_repo_job_fires_with_teamRegex() throws Exception {
    Course course =
        Course.builder()
            .id(2L)
            .orgName("ucsb-cs156")
            .installationId("1234")
            .courseName("course")
            .instructorEmail(currentUserService.getUser().getEmail())
            .build();
    doReturn(Optional.of(course)).when(courseRepository).findById(eq(2L));
    Job job = Job.builder().status("processing").build();
    doReturn(job).when(service).runAsJob(any(CreateTeamRepositoriesJob.class));
    MvcResult response =
        mockMvc
            .perform(
                post("/api/repos/createTeamRepos")
                    .with(csrf())
                    .param("courseId", "2")
                    .param("repoPrefix", "repo1")
                    .param("permissions", "WRITE")
                    .param("teamRegex", "test-team.*"))
            .andExpect(status().isOk())
            .andReturn();

    String expectedJson = objectMapper.writeValueAsString(job);
    String actualJson = response.getResponse().getContentAsString();
    assertEquals(expectedJson, actualJson);
  }

  @Test
  @WithMockUser(roles = {"INSTRUCTOR"})
  public void delete_repos_not_the_creator() throws Exception {
    Course course = Course.builder().instructorEmail("test@example.com").build();
    doReturn(Optional.of(course)).when(courseRepository).findById(eq(2L));
    mockMvc
        .perform(delete("/api/repos").with(csrf()).param("courseId", "2").param("prefix", "repo1"))
        .andExpect(status().isForbidden());
    verify(service, never()).runAsJob(any(DeleteRepoJob.class));
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void delete_repos_not_registered_org() throws Exception {
    Course course =
        Course.builder()
            .courseName("course")
            .instructorEmail(currentUserService.getUser().getEmail())
            .build();
    doReturn(Optional.of(course)).when(courseRepository).findById(eq(2L));
    MvcResult response =
        mockMvc
            .perform(
                delete("/api/repos").with(csrf()).param("courseId", "2").param("prefix", "repo1"))
            .andExpect(status().isBadRequest())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals("NoLinkedOrganizationException", json.get("type"));
    verify(service, never()).runAsJob(any(DeleteRepoJob.class));
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void delete_repos_just_no_install_id() throws Exception {
    Course course =
        Course.builder()
            .courseName("course")
            .orgName("ucsb-cs156")
            .instructorEmail(currentUserService.getUser().getEmail())
            .build();
    doReturn(Optional.of(course)).when(courseRepository).findById(eq(2L));
    MvcResult response =
        mockMvc
            .perform(
                delete("/api/repos").with(csrf()).param("courseId", "2").param("prefix", "repo1"))
            .andExpect(status().isBadRequest())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals("NoLinkedOrganizationException", json.get("type"));
    verify(service, never()).runAsJob(any(DeleteRepoJob.class));
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void delete_repos_course_not_found() throws Exception {
    doReturn(Optional.empty()).when(courseRepository).findById(eq(2L));
    MvcResult response =
        mockMvc
            .perform(
                delete("/api/repos").with(csrf()).param("courseId", "2").param("prefix", "repo1"))
            .andExpect(status().isNotFound())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("Course with id 2 not found", json.get("message"));
    verify(service, never()).runAsJob(any(DeleteRepoJob.class));
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void delete_repos_rejects_blank_prefix() throws Exception {
    for (String blank : new String[] {"", "   "}) {
      mockMvc
          .perform(delete("/api/repos").with(csrf()).param("courseId", "2").param("prefix", blank))
          .andExpect(status().isBadRequest());
    }
    verify(service, never()).runAsJob(any(DeleteRepoJob.class));
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void delete_repos_job_actually_fires() throws Exception {
    Course course =
        Course.builder()
            .id(2L)
            .orgName("ucsb-cs156")
            .installationId("1234")
            .courseName("course")
            .instructorEmail(currentUserService.getUser().getEmail())
            .build();
    doReturn(Optional.of(course)).when(courseRepository).findById(eq(2L));
    Job job = Job.builder().status("processing").build();
    doReturn(job).when(service).runAsJob(any(DeleteRepoJob.class));

    MvcResult response =
        mockMvc
            .perform(
                delete("/api/repos").with(csrf()).param("courseId", "2").param("prefix", "repo1"))
            .andExpect(status().isOk())
            .andReturn();

    assertEquals(objectMapper.writeValueAsString(job), response.getResponse().getContentAsString());

    ArgumentCaptor<DeleteRepoJob> captor = ArgumentCaptor.forClass(DeleteRepoJob.class);
    verify(service).runAsJob(captor.capture());
    DeleteRepoJob launched = captor.getValue();
    assertEquals(2L, launched.getScopeId());
    assertEquals("repo1", ReflectionTestUtils.getField(launched, "prefix"));
    assertSame(courseRepository, ReflectionTestUtils.getField(launched, "courseRepository"));
    assertSame(repositoryService, ReflectionTestUtils.getField(launched, "repositoryService"));
  }
}
