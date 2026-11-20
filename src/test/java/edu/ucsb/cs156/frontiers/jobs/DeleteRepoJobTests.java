package edu.ucsb.cs156.frontiers.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.services.RepositoryService;
import edu.ucsb.cs156.frontiers.services.RepositoryService.GithubRepository;
import edu.ucsb.cs156.jobs.entities.Job;
import edu.ucsb.cs156.jobs.services.JobContext;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DeleteRepoJobTests {

  @Mock private CourseRepository courseRepository;
  @Mock private RepositoryService repositoryService;

  Job jobStarted = Job.builder().build();
  JobContext ctx = new JobContext(null, jobStarted);

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void test_getScope_returnsCourseScope() {
    Long courseId = 1L;

    DeleteRepoJob job =
        DeleteRepoJob.builder().courseId(courseId).courseRepository(courseRepository).build();

    assertEquals("course", job.getScopeType());
    assertEquals(courseId, job.getScopeId());
  }

  @Test
  public void test_accept_courseNotFound() throws Exception {
    Long courseId = 1L;
    when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

    DeleteRepoJob job =
        DeleteRepoJob.builder()
            .courseId(courseId)
            .prefix("lab01")
            .courseRepository(courseRepository)
            .repositoryService(repositoryService)
            .build();

    job.accept(ctx);

    verify(courseRepository).findById(courseId);
    verify(repositoryService, never()).getRepositoriesMatchingPrefix(any(), any());
    assertTrue(jobStarted.getLog().contains("ERROR: Course with ID 1 not found"));
  }

  @Test
  public void test_accept_courseWithoutGithubOrg() throws Exception {
    Long courseId = 1L;
    Course course = Course.builder().id(courseId).courseName("Test Course").build();
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

    DeleteRepoJob job =
        DeleteRepoJob.builder()
            .courseId(courseId)
            .prefix("lab01")
            .courseRepository(courseRepository)
            .repositoryService(repositoryService)
            .build();

    job.accept(ctx);

    verify(courseRepository).findById(courseId);
    verify(repositoryService, never()).getRepositoriesMatchingPrefix(any(), any());
    assertTrue(jobStarted.getLog().contains("ERROR: Course has no linked GitHub organization"));
  }

  @Test
  public void test_accept_courseWithOrgNameButNoInstallationId() throws Exception {
    Long courseId = 1L;
    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .orgName("test-org")
            .installationId(null)
            .build();
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

    DeleteRepoJob job =
        DeleteRepoJob.builder()
            .courseId(courseId)
            .prefix("lab01")
            .courseRepository(courseRepository)
            .repositoryService(repositoryService)
            .build();

    job.accept(ctx);

    verify(courseRepository).findById(courseId);
    verify(repositoryService, never()).getRepositoriesMatchingPrefix(any(), any());
    assertTrue(jobStarted.getLog().contains("ERROR: Course has no linked GitHub organization"));
  }

  @Test
  public void test_accept_allReposEmpty() throws Exception {
    Long courseId = 1L;
    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .orgName("test-org")
            .installationId("123")
            .build();
    List<GithubRepository> repositories =
        List.of(
            new GithubRepository("lab01-student1", "test-org/lab01-student1"),
            new GithubRepository("lab01-student2", "test-org/lab01-student2"));

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(repositoryService.getRepositoriesMatchingPrefix(course, "lab01")).thenReturn(repositories);
    when(repositoryService.deleteRepositoryIfEmpty(course, "lab01-student1")).thenReturn(true);
    when(repositoryService.deleteRepositoryIfEmpty(course, "lab01-student2")).thenReturn(true);

    DeleteRepoJob job =
        DeleteRepoJob.builder()
            .courseId(courseId)
            .prefix("lab01")
            .courseRepository(courseRepository)
            .repositoryService(repositoryService)
            .build();

    job.accept(ctx);

    verify(repositoryService).getRepositoriesMatchingPrefix(course, "lab01");
    verify(repositoryService).deleteRepositoryIfEmpty(course, "lab01-student1");
    verify(repositoryService).deleteRepositoryIfEmpty(course, "lab01-student2");
    assertTrue(
        jobStarted.getLog().contains("Starting delete repository job for course ID: " + courseId));
    assertTrue(jobStarted.getLog().contains("prefix=lab01"));
    assertTrue(jobStarted.getLog().contains("Deleted empty repository: lab01-student1"));
    assertTrue(jobStarted.getLog().contains("Deleted empty repository: lab01-student2"));
    assertTrue(
        jobStarted.getLog().contains("Summary: 2 repos deleted, 0 repos retained, 0 errors"));
    assertTrue(jobStarted.getLog().contains("Done"));
  }

  @Test
  public void test_accept_someReposRetainedBecauseCommitsExist() throws Exception {
    Long courseId = 1L;
    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .orgName("test-org")
            .installationId("123")
            .build();
    List<GithubRepository> repositories =
        List.of(
            new GithubRepository("lab01-student1", "test-org/lab01-student1"),
            new GithubRepository("lab01-student2", "test-org/lab01-student2"));

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(repositoryService.getRepositoriesMatchingPrefix(course, "lab01")).thenReturn(repositories);
    when(repositoryService.deleteRepositoryIfEmpty(course, "lab01-student1")).thenReturn(true);
    when(repositoryService.deleteRepositoryIfEmpty(course, "lab01-student2")).thenReturn(false);

    DeleteRepoJob job =
        DeleteRepoJob.builder()
            .courseId(courseId)
            .prefix("lab01")
            .courseRepository(courseRepository)
            .repositoryService(repositoryService)
            .build();

    job.accept(ctx);

    verify(repositoryService).deleteRepositoryIfEmpty(course, "lab01-student1");
    verify(repositoryService).deleteRepositoryIfEmpty(course, "lab01-student2");
    assertTrue(jobStarted.getLog().contains("Deleted empty repository: lab01-student1"));
    assertTrue(jobStarted.getLog().contains("Retained repository with commits: lab01-student2"));
    assertTrue(
        jobStarted.getLog().contains("Summary: 1 repos deleted, 1 repos retained, 0 errors"));
  }

  @Test
  public void test_accept_githubApiErrorDuringDeleteOrCheck() throws Exception {
    Long courseId = 1L;
    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .orgName("test-org")
            .installationId("123")
            .build();
    List<GithubRepository> repositories =
        List.of(
            new GithubRepository("lab01-student1", "test-org/lab01-student1"),
            new GithubRepository("lab01-student2", "test-org/lab01-student2"));

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(repositoryService.getRepositoriesMatchingPrefix(course, "lab01")).thenReturn(repositories);
    when(repositoryService.deleteRepositoryIfEmpty(course, "lab01-student1"))
        .thenThrow(new RuntimeException("GitHub API error"));
    when(repositoryService.deleteRepositoryIfEmpty(course, "lab01-student2")).thenReturn(true);

    DeleteRepoJob job =
        DeleteRepoJob.builder()
            .courseId(courseId)
            .prefix("lab01")
            .courseRepository(courseRepository)
            .repositoryService(repositoryService)
            .build();

    job.accept(ctx);

    verify(repositoryService).deleteRepositoryIfEmpty(course, "lab01-student1");
    verify(repositoryService).deleteRepositoryIfEmpty(course, "lab01-student2");
    assertTrue(
        jobStarted
            .getLog()
            .contains("ERROR: Failed to delete/check repository lab01-student1: GitHub API error"));
    assertTrue(jobStarted.getLog().contains("Deleted empty repository: lab01-student2"));
    assertTrue(
        jobStarted.getLog().contains("Summary: 1 repos deleted, 0 repos retained, 1 errors"));
  }

  @Test
  public void test_accept_githubApiErrorDuringList() throws Exception {
    Long courseId = 1L;
    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .orgName("test-org")
            .installationId("123")
            .build();

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(repositoryService.getRepositoriesMatchingPrefix(course, "lab01"))
        .thenThrow(new RuntimeException("GitHub API error"));

    DeleteRepoJob job =
        DeleteRepoJob.builder()
            .courseId(courseId)
            .prefix("lab01")
            .courseRepository(courseRepository)
            .repositoryService(repositoryService)
            .build();

    job.accept(ctx);

    verify(repositoryService).getRepositoriesMatchingPrefix(course, "lab01");
    verify(repositoryService, never()).deleteRepositoryIfEmpty(any(), any());
    assertTrue(
        jobStarted.getLog().contains("ERROR: Failed to list repositories: GitHub API error"));
    assertTrue(
        jobStarted.getLog().contains("Summary: 0 repos deleted, 0 repos retained, 1 errors"));
  }

  @Test
  private DeleteRepoJob.Sleeper runWithRepos(int repoCount, long delayMillis) throws Exception {
    Long courseId = 1L;
    Course course =
        Course.builder()
            .id(courseId)
            .courseName("Test Course")
            .orgName("test-org")
            .installationId("123")
            .build();
    List<GithubRepository> repositories = new java.util.ArrayList<>();
    for (int i = 1; i <= repoCount; i++) {
      repositories.add(new GithubRepository("lab01-student" + i, "test-org/lab01-student" + i));
    }
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(repositoryService.getRepositoriesMatchingPrefix(course, "lab01")).thenReturn(repositories);
    when(repositoryService.deleteRepositoryIfEmpty(any(), any())).thenReturn(true);

    DeleteRepoJob.Sleeper sleeper = mock(DeleteRepoJob.Sleeper.class);
    DeleteRepoJob job =
        DeleteRepoJob.builder()
            .courseId(courseId)
            .prefix("lab01")
            .courseRepository(courseRepository)
            .repositoryService(repositoryService)
            .delayMillis(delayMillis)
            .sleeper(sleeper)
            .build();
    job.accept(ctx);
    return sleeper;
  }

  @Test
  public void sleeps_between_repositories_but_not_before_the_first() throws Exception {
    DeleteRepoJob.Sleeper sleeper = runWithRepos(3, 250);
    verify(sleeper, times(2)).sleep(250L); // 3 repos means 2 pauses
  }

  @Test
  public void does_not_sleep_when_delay_is_zero() throws Exception {
    DeleteRepoJob.Sleeper sleeper = runWithRepos(3, 0);
    verifyNoInteractions(sleeper);
  }

  @Test
  public void does_not_sleep_for_a_single_repository() throws Exception {
    DeleteRepoJob.Sleeper sleeper = runWithRepos(1, 250);
    verifyNoInteractions(sleeper);
  }
}
