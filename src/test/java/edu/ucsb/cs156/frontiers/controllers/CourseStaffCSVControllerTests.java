package edu.ucsb.cs156.frontiers.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.annotations.WithInstructorCoursePermissions;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.CourseStaff;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.enums.School;
import edu.ucsb.cs156.frontiers.models.LoadResult;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseStaffRepository;
import edu.ucsb.cs156.frontiers.services.CurrentUserService;
import edu.ucsb.cs156.frontiers.services.UpdateUserService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@Slf4j
@WebMvcTest(controllers = CourseStaffCSVController.class)
public class CourseStaffCSVControllerTests extends ControllerTestCase {

  @MockitoBean private CourseRepository courseRepository;

  @MockitoBean private CourseStaffRepository courseStaffRepository;

  @Autowired private CurrentUserService currentUserService;

  @MockitoBean private UpdateUserService updateUserService;

  @Autowired private ObjectMapper objectMapper;

  private final String sampleCSV =
      """
      firstName,lastName,email
      Chris,Gaucho,cgaucho@example.org
      Lauren,Del Playa,ldelplaya@ucsb.edu
      """;

  private final String sampleCSVWithExtraSpaces =
      """
      firstName,lastName,email
       Chris , Gaucho , cgaucho@example.org
      """;

  private final String sampleCSVContentsStaff =
      """
      firstName,lastName,email
      CHRIS FAKE,GAUCHO,cgaucho@ucsb.edu
      LAUREN,DEL PLAYA,ldelplaya@umail.ucsb.edu
      SABADO,TARDE,sabadotarde@umail.ucsb.edu
      """;

  private final String sampleCSVContentsStaffWithDuplicateRow =
      """
      firstName,lastName,email
      Marge,Simpson,msimpson@csuchico.edu
      Ralph,Wiggum,rwiggum@csuchico.edu
      Ralph,Wiggum,rwiggum@csuchico.edu
      """;

  @Test
  @WithInstructorCoursePermissions
  public void updates_in_upsert_correctly() throws Exception {
    CourseStaff staff1Email = CourseStaff.builder().id(2L).email("cgaucho@ucsb.edu").build();
    CourseStaff staff2 = CourseStaff.builder().id(3L).email("ldelplaya@ucsb.edu").build();
    Course course1 =
        Course.builder().id(1L).courseStaff(new ArrayList<>(List.of(staff1Email, staff2))).build();
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course1));
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "roster.csv", MediaType.TEXT_PLAIN_VALUE, sampleCSVContentsStaff.getBytes());

    ArgumentCaptor<List<CourseStaff>> courseStaffCaptor = ArgumentCaptor.forClass(List.class);
    MvcResult response =
        mockMvc
            .perform(
                multipart("/api/coursestaff/upload/csv")
                    .file(file)
                    .param("courseId", "1")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    CourseStaff courseStaff1Updated =
        CourseStaff.builder()
            .id(2L)
            .firstName("CHRIS FAKE")
            .lastName("GAUCHO")
            .email("cgaucho@ucsb.edu")
            .build();

    CourseStaff courseStaff2Updated =
        CourseStaff.builder()
            .id(3L)
            .firstName("LAUREN")
            .lastName("DEL PLAYA")
            .email("ldelplaya@ucsb.edu")
            .build();

    verify(courseStaffRepository, times(1)).saveAll(courseStaffCaptor.capture());
    assertTrue(courseStaffCaptor.getValue().contains(courseStaff1Updated));
    assertTrue(courseStaffCaptor.getValue().contains(courseStaff2Updated));
    String responseString = response.getResponse().getContentAsString();
    LoadResult<CourseStaff> expectedResult = new LoadResult<>(1, 2, 0, List.of());
    String expectedJson = mapper.writeValueAsString(expectedResult);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithInstructorCoursePermissions
  public void instructor_can_upload_staff_csv_with_duplicate_row_for_new_staff() throws Exception {

    // arrange

    Course course1 =
        Course.builder()
            .id(1L)
            .courseName("CSED 500")
            .orgName("csed-500-s25")
            .term("S25")
            .school(School.CHICO_STATE)
            .build();

    CourseStaff cs1BeforeWithId =
        CourseStaff.builder()
            .id(1L)
            .firstName("MARGE")
            .lastName("SIMPSON")
            .email("msimpson@csuchico.edu")
            .course(course1)
            .orgStatus(OrgStatus.PENDING)
            .build();

    CourseStaff cs1AfterWithId =
        CourseStaff.builder()
            .id(1L)
            .firstName("Marge")
            .lastName("Simpson")
            .email("msimpson@csuchico.edu")
            .course(course1)
            .orgStatus(OrgStatus.PENDING)
            .build();

    CourseStaff cs3NoId =
        CourseStaff.builder()
            .course(course1)
            .firstName("Ralph")
            .lastName("Wiggum")
            .email("rwiggum@csuchico.edu")
            .orgStatus(OrgStatus.PENDING)
            .build();

    CourseStaff cs3WithId =
        CourseStaff.builder()
            .id(3L)
            .course(course1)
            .firstName("Ralph")
            .lastName("Wiggum")
            .email("rwiggum@csuchico.edu")
            .orgStatus(OrgStatus.PENDING)
            .build();

    course1.setCourseStaff(new ArrayList<>(List.of(cs1BeforeWithId)));

    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            "roster.csv",
            MediaType.TEXT_PLAIN_VALUE,
            sampleCSVContentsStaffWithDuplicateRow.getBytes());

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course1));

    when(courseStaffRepository.saveAll(List.of(cs1AfterWithId, cs3NoId)))
        .thenReturn(List.of(cs1AfterWithId, cs3WithId));

    // act

    MvcResult response =
        mockMvc
            .perform(
                multipart("/api/coursestaff/upload/csv")
                    .file(file)
                    .param("courseId", "1")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert

    // Ralph is saved exactly once, even though he was in the CSV twice
    verify(courseStaffRepository, times(1))
        .saveAll(new ArrayList<>(List.of(cs1AfterWithId, cs3NoId)));
    verify(updateUserService, times(1)).attachUserToCourseStaff(cs1AfterWithId);
    verify(updateUserService, times(1)).attachUserToCourseStaff(cs3NoId);

    String responseString = response.getResponse().getContentAsString();
    // 1 inserted (Ralph, first row), 2 updated (Marge, and Ralph's duplicate second row)
    LoadResult<CourseStaff> expectedResult = new LoadResult<>(1, 2, 0, List.of());
    String expectedJson = mapper.writeValueAsString(expectedResult);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithInstructorCoursePermissions
  public void instructor_can_upload_staff_csv() throws Exception {

    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .courseStaff(new ArrayList<>())
            .orgName("ucsb-cs156-s25")
            .term("S25")
            .school(School.UCSB)
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));

    when(courseStaffRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "staff.csv", MediaType.TEXT_PLAIN_VALUE, sampleCSV.getBytes());

    MvcResult response =
        mockMvc
            .perform(
                multipart("/api/coursestaff/upload/csv")
                    .file(file)
                    .param("courseId", "1")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    verify(courseStaffRepository, times(1)).saveAll(any());
    verify(updateUserService, times(2)).attachUserToCourseStaff(any(CourseStaff.class));

    // Verify the saved staff members
    ArgumentCaptor<List<CourseStaff>> captor = ArgumentCaptor.forClass(List.class);
    verify(courseStaffRepository, times(1)).saveAll(captor.capture());

    CourseStaff first = captor.getValue().get(0);
    assertEquals("Chris", first.getFirstName());
    assertEquals("Gaucho", first.getLastName());
    assertEquals("cgaucho@example.org", first.getEmail());
    assertEquals(OrgStatus.PENDING, first.getOrgStatus());

    CourseStaff second = captor.getValue().get(1);
    assertEquals("Lauren", second.getFirstName());
    assertEquals("Del Playa", second.getLastName());
    assertEquals("ldelplaya@ucsb.edu", second.getEmail());

    String responseString = response.getResponse().getContentAsString();
    String expectedJson = objectMapper.writeValueAsString(new LoadResult<>(2, 0, 0, List.of()));
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithInstructorCoursePermissions
  public void instructor_can_upload_staff_csv_with_installation_id() throws Exception {

    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .courseStaff(new ArrayList<>())
            .orgName("ucsb-cs156-s25")
            .installationId("12345")
            .term("S25")
            .school(School.UCSB)
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));

    when(courseStaffRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "staff.csv", MediaType.TEXT_PLAIN_VALUE, sampleCSV.getBytes());

    mockMvc
        .perform(
            multipart("/api/coursestaff/upload/csv").file(file).param("courseId", "1").with(csrf()))
        .andExpect(status().isOk());

    ArgumentCaptor<List<CourseStaff>> captor = ArgumentCaptor.forClass(List.class);
    verify(courseStaffRepository, times(1)).saveAll(captor.capture());

    // When installationId is present, orgStatus should be JOINCOURSE
    assertEquals(OrgStatus.JOINCOURSE, captor.getValue().get(0).getOrgStatus());
    assertEquals(OrgStatus.JOINCOURSE, captor.getValue().get(1).getOrgStatus());
  }

  @Test
  @WithInstructorCoursePermissions
  public void upload_strips_whitespace_from_fields() throws Exception {

    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .courseStaff(new ArrayList<>())
            .orgName("ucsb-cs156-s25")
            .term("S25")
            .school(School.UCSB)
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));

    when(courseStaffRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "staff.csv", MediaType.TEXT_PLAIN_VALUE, sampleCSVWithExtraSpaces.getBytes());

    mockMvc
        .perform(
            multipart("/api/coursestaff/upload/csv").file(file).param("courseId", "1").with(csrf()))
        .andExpect(status().isOk());

    ArgumentCaptor<List<CourseStaff>> captor = ArgumentCaptor.forClass(List.class);
    verify(courseStaffRepository, times(1)).saveAll(captor.capture());

    CourseStaff saved = captor.getValue().get(0);
    assertEquals("Chris", saved.getFirstName());
    assertEquals("Gaucho", saved.getLastName());
    assertEquals("cgaucho@example.org", saved.getEmail());
  }

  @Test
  @WithInstructorCoursePermissions
  public void upload_returns_bad_request_for_invalid_headers() throws Exception {

    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .courseStaff(new ArrayList<>())
            .orgName("ucsb-cs156-s25")
            .term("S25")
            .school(School.UCSB)
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));

    String badCSV = "name,address,phone\nChris,123 Main,555-1234\n";

    MockMultipartFile file =
        new MockMultipartFile("file", "staff.csv", MediaType.TEXT_PLAIN_VALUE, badCSV.getBytes());

    mockMvc
        .perform(
            multipart("/api/coursestaff/upload/csv").file(file).param("courseId", "1").with(csrf()))
        .andExpect(status().isBadRequest());

    verify(courseStaffRepository, never()).saveAll(any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void upload_returns_bad_request_for_empty_csv() throws Exception {

    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .courseStaff(new ArrayList<>())
            .orgName("ucsb-cs156-s25")
            .term("S25")
            .school(School.UCSB)
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));

    MockMultipartFile file =
        new MockMultipartFile("file", "staff.csv", MediaType.TEXT_PLAIN_VALUE, "".getBytes());

    mockMvc
        .perform(
            multipart("/api/coursestaff/upload/csv").file(file).param("courseId", "1").with(csrf()))
        .andExpect(status().isBadRequest());

    verify(courseStaffRepository, never()).saveAll(any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void upload_returns_bad_request_for_short_row() throws Exception {

    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .courseStaff(new ArrayList<>())
            .orgName("ucsb-cs156-s25")
            .term("S25")
            .school(School.UCSB)
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));

    String shortRowCSV = "firstName,lastName,email\nChris,Gaucho\n";

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "staff.csv", MediaType.TEXT_PLAIN_VALUE, shortRowCSV.getBytes());

    mockMvc
        .perform(
            multipart("/api/coursestaff/upload/csv").file(file).param("courseId", "1").with(csrf()))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithInstructorCoursePermissions
  public void upload_returns_not_found_for_missing_course() throws Exception {

    when(courseRepository.findById(eq(99L))).thenReturn(Optional.empty());

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "staff.csv", MediaType.TEXT_PLAIN_VALUE, sampleCSV.getBytes());

    mockMvc
        .perform(
            multipart("/api/coursestaff/upload/csv")
                .file(file)
                .param("courseId", "99")
                .with(csrf()))
        .andExpect(status().isNotFound());

    verify(courseStaffRepository, never()).saveAll(any());
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void regular_user_cannot_upload_staff_csv() throws Exception {

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "staff.csv", MediaType.TEXT_PLAIN_VALUE, sampleCSV.getBytes());

    mockMvc
        .perform(
            multipart("/api/coursestaff/upload/csv").file(file).param("courseId", "1").with(csrf()))
        .andExpect(status().isForbidden());

    verify(courseStaffRepository, never()).saveAll(any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void upload_returns_bad_request_for_too_few_headers() throws Exception {

    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .courseStaff(new ArrayList<>())
            .orgName("ucsb-cs156-s25")
            .term("S25")
            .school(School.UCSB)
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));

    String fewHeadersCSV = "firstName,lastName\nChris,Gaucho\n";

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "staff.csv", MediaType.TEXT_PLAIN_VALUE, fewHeadersCSV.getBytes());

    mockMvc
        .perform(
            multipart("/api/coursestaff/upload/csv").file(file).param("courseId", "1").with(csrf()))
        .andExpect(status().isBadRequest());

    verify(courseStaffRepository, never()).saveAll(any());
  }
}
