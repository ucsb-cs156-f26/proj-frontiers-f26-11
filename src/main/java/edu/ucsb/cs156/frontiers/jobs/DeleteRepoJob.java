package edu.ucsb.cs156.frontiers.jobs;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.services.RepositoryService;
import edu.ucsb.cs156.frontiers.services.RepositoryService.GithubRepository;
import edu.ucsb.cs156.jobs.services.JobContext;
import edu.ucsb.cs156.jobs.services.JobContextConsumer;
import java.util.List;
import java.util.Optional;
import lombok.Builder;

@Builder
public class DeleteRepoJob implements JobContextConsumer {
  Long courseId;
  String prefix;
  CourseRepository courseRepository;
  RepositoryService repositoryService;

  // Pause between deleting repositories to avoid ratelimit
  public static final long DEFAULT_DELAY_MILLIS = 1000L;
  // Tests can set this to zero to avoid waiting
  @Builder.Default long delayMillis = DEFAULT_DELAY_MILLIS;

  /** Pauses the job. Injectable so tests can observe the delay without actually waiting. */
  @FunctionalInterface
  public interface Sleeper {
    void sleep(long millis) throws InterruptedException;
  }

  @Builder.Default Sleeper sleeper = Thread::sleep;

  @Override
  public String getScopeType() {
    return "course";
  }

  @Override
  public Long getScopeId() {
    return courseId;
  }

  @Override
  public void accept(JobContext ctx) throws Exception {
    ctx.log(String.format("Starting delete repository job for course ID: %s", courseId));
    ctx.log(String.format("prefix=%s", prefix));

    Optional<Course> courseOpt = courseRepository.findById(courseId);
    if (courseOpt.isEmpty()) {
      ctx.log(String.format("ERROR: Course with ID %s not found", courseId));
      return;
    }

    Course course = courseOpt.get();
    if (course.getOrgName() == null || course.getInstallationId() == null) {
      ctx.log("ERROR: Course has no linked GitHub organization");
      return;
    }

    List<GithubRepository> repositories;
    int errors = 0;
    try {
      repositories = repositoryService.getRepositoriesMatchingPrefix(course, prefix);
    } catch (Exception e) {
      ctx.log(String.format("ERROR: Failed to list repositories: %s", e.getMessage()));
      errors++;
      logSummary(ctx, 0, 0, 0, errors);
      return;
    }

    int deleted = 0;
    int retained = 0;

    for (int i = 0; i < repositories.size(); i++) {
      if (i > 0) {
        pause();
      }
      GithubRepository repository = repositories.get(i);
      try {
        boolean deletedRepository =
            repositoryService.deleteRepositoryIfEmpty(course, repository.name());
        if (deletedRepository) {
          deleted++;
          ctx.log(String.format("Deleted empty repository: %s", repository.name()));
        } else {
          retained++;
          ctx.log(String.format("Retained repository with commits: %s", repository.name()));
        }
      } catch (Exception e) {
        errors++;
        ctx.log(
            String.format(
                "ERROR: Failed to delete/check repository %s: %s",
                repository.name(), e.getMessage()));
      }
    }

    logSummary(ctx, repositories.size(), deleted, retained, errors);
    ctx.log("Done");
  }

  private void pause() throws InterruptedException {
    if (delayMillis > 0) {
      sleeper.sleep(delayMillis);
    }
  }

  private static void logSummary(JobContext ctx, int found, int deleted, int retained, int errors) {

    ctx.log(
        String.format(
            "Summary: %d repos deleted, %d repos retained, %d errors", deleted, retained, errors));
  }
}
