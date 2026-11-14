package edu.ucsb.cs156.frontiers.controllers;

import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.repositories.CourseStaffRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/github")
public class GithubController extends ApiController {

  @Autowired private UserRepository userRepository;
  @Autowired private CourseStaffRepository courseStaffRepository;
  @Autowired private RosterStudentRepository rosterStudentRepository;

  @PreAuthorize("hasRole('ROLE_GITHUB')")
  @DeleteMapping("/disconnect")
  public Object disconnect(SecurityContext context) {
    User currentUser = getCurrentUser().getUser();
    currentUser.setGithubId(null);
    currentUser.setGithubLogin(null);
    userRepository.save(currentUser);

    rosterStudentRepository
        .findAllByEmail(currentUser.getEmail())
        .forEach(
            rs -> {
              if (rs.getOrgStatus() == OrgStatus.INVITED) {
                rs.setGithubId(null);
                rs.setGithubLogin(null);
                rs.setOrgStatus(OrgStatus.JOINCOURSE);
                rosterStudentRepository.save(rs);
              }
            });

    courseStaffRepository
        .findAllByEmail(currentUser.getEmail())
        .forEach(
            cs -> {
              if (cs.getOrgStatus() == OrgStatus.INVITED) {
                cs.setGithubId(null);
                cs.setGithubLogin(null);
                cs.setOrgStatus(OrgStatus.JOINCOURSE);
                courseStaffRepository.save(cs);
              }
            });

    Authentication auth = context.getAuthentication();
    List<? extends GrantedAuthority> removedAuthority =
        auth.getAuthorities().stream()
            .filter(r -> !"ROLE_GITHUB".equals(r.getAuthority()))
            .toList();
    OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) context.getAuthentication();
    context.setAuthentication(
        new OAuth2AuthenticationToken(
            (OidcUser) auth.getPrincipal(),
            removedAuthority,
            token.getAuthorizedClientRegistrationId()));
    SecurityContextHolder.setContext(context);
    return genericMessage("Disconnected from GitHub. You may now log in with a different account.");
  }
}
