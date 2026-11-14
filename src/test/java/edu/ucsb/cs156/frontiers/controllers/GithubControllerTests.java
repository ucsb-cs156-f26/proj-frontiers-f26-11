package edu.ucsb.cs156.frontiers.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.entities.CourseStaff;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.repositories.CourseStaffRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = GithubController.class)
public class GithubControllerTests extends ControllerTestCase {

  @MockitoBean private UserRepository userRepository;

  @MockitoBean private RosterStudentRepository rosterStudentRepository;

  @MockitoBean private CourseStaffRepository courseStaffRepository;

  @Test
  public void successfulDisconnect() throws Exception {

    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

    MvcResult response = performDisconnect("user@example.org");

    verify(userRepository).save(userCaptor.capture());

    assertNull(userCaptor.getValue().getGithubId());
    assertNull(userCaptor.getValue().getGithubLogin());

    Map<String, Object> json = responseToJson(response);
    assertEquals(
        "Disconnected from GitHub. You may now log in with a different account.",
        json.get("message"));
  }

  @Test
  public void disconnectResetsInvitedStudent() throws Exception {
    RosterStudent student =
        RosterStudent.builder()
            .email("student@ucsb.edu")
            .orgStatus(OrgStatus.INVITED)
            .githubId(123)
            .githubLogin("student-account")
            .build();
    when(rosterStudentRepository.findAllByEmail("student@ucsb.edu")).thenReturn(List.of(student));

    performDisconnect("student@ucsb.edu");

    assertEquals(OrgStatus.JOINCOURSE, student.getOrgStatus());
    assertNull(student.getGithubId());
    assertNull(student.getGithubLogin());
    verify(rosterStudentRepository).save(student);
  }

  @Test
  public void disconnectResetsInvitedStaff() throws Exception {
    CourseStaff staff =
        CourseStaff.builder()
            .email("staff@ucsb.edu")
            .orgStatus(OrgStatus.INVITED)
            .githubId(456)
            .githubLogin("staff-account")
            .build();
    when(courseStaffRepository.findAllByEmail("staff@ucsb.edu")).thenReturn(List.of(staff));

    performDisconnect("staff@ucsb.edu");

    assertEquals(OrgStatus.JOINCOURSE, staff.getOrgStatus());
    assertNull(staff.getGithubId());
    assertNull(staff.getGithubLogin());
    verify(courseStaffRepository).save(staff);
  }

  @Test
  public void disconnectPreservesStudentMembership() throws Exception {
    RosterStudent student =
        RosterStudent.builder()
            .email("student@ucsb.edu")
            .orgStatus(OrgStatus.MEMBER)
            .githubId(123)
            .githubLogin("student-account")
            .build();
    when(rosterStudentRepository.findAllByEmail("student@ucsb.edu")).thenReturn(List.of(student));

    performDisconnect("student@ucsb.edu");

    assertEquals(OrgStatus.MEMBER, student.getOrgStatus());
    assertEquals(Integer.valueOf(123), student.getGithubId());
    assertEquals("student-account", student.getGithubLogin());
    verify(rosterStudentRepository, never()).save(any());
  }

  @Test
  public void disconnectPreservesStaffOwnership() throws Exception {
    CourseStaff staff =
        CourseStaff.builder()
            .email("staff@ucsb.edu")
            .orgStatus(OrgStatus.OWNER)
            .githubId(456)
            .githubLogin("staff-account")
            .build();
    when(courseStaffRepository.findAllByEmail("staff@ucsb.edu")).thenReturn(List.of(staff));

    performDisconnect("staff@ucsb.edu");

    assertEquals(OrgStatus.OWNER, staff.getOrgStatus());
    assertEquals(Integer.valueOf(456), staff.getGithubId());
    assertEquals("staff-account", staff.getGithubLogin());
    verify(courseStaffRepository, never()).save(any());
  }

  private MvcResult performDisconnect(String email) throws Exception {
    return mockMvc
        .perform(
            delete("/api/github/disconnect")
                .with(csrf())
                .with(
                    oidcLogin()
                        .idToken(token -> token.claim("email", email))
                        .authorities(
                            new SimpleGrantedAuthority("ROLE_USER"),
                            new SimpleGrantedAuthority("ROLE_GITHUB"))))
        .andExpect(status().isOk())
        .andExpect(authenticated().withRoles("USER"))
        .andReturn();
  }
}
