import axios from "axios";
import { fireEvent, render, waitFor, screen } from "@testing-library/react";
import DownloadsTabComponent from "main/components/TabComponent/DownloadsTabComponent";
import AxiosMockAdapter from "axios-mock-adapter";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { expect, vi } from "vitest";
import coursesFixtures from "fixtures/coursesFixtures";
import * as useBackendModule from "main/utils/useBackend";

const axiosMock = new AxiosMockAdapter(axios);
const { mockToast } = vi.hoisted(() => {
  const mockToast = vi.fn();
  mockToast.error = vi.fn();
  return { mockToast };
});

vi.mock("react-toastify", async (importOriginal) => {
  const original = await importOriginal();
  return { ...original, toast: mockToast };
});

describe("DownloadsTabComponent", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    window.URL.createObjectURL = vi.fn(() => "blob:mock-url");
    window.URL.revokeObjectURL = vi.fn();
  });

  test("downloads the CSV and shows a success toast on button click", async () => {
    axiosMock
      .onGet("/api/csv/formattedteams")
      .reply(200, "team,members\nA,Alice\n");

    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <DownloadsTabComponent
          courseId={coursesFixtures.severalCourses[0].id}
          testIdPrefix="InstructorCourseShowPage"
        />
      </QueryClientProvider>,
    );

    const submitButton = await screen.findByTestId(
      "InstructorCourseShowPage-btn-download-students-csv",
    );
    fireEvent.click(submitButton);

    await waitFor(() =>
      expect(mockToast).toHaveBeenCalledWith(
        "Download successfully initiated.",
      ),
    );
    expect(window.URL.createObjectURL).toHaveBeenCalled();
    expect(axiosMock.history.get[0].params).toEqual({
      courseId: coursesFixtures.severalCourses[0].id,
    });
  });

  test("shows an error toast when the request fails", async () => {
    axiosMock.onGet("/api/csv/formattedteams").reply(500);

    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <DownloadsTabComponent
          courseId={1}
          testIdPrefix="InstructorCourseShowPage"
        />
      </QueryClientProvider>,
    );

    fireEvent.click(
      await screen.findByTestId(
        "InstructorCourseShowPage-btn-download-students-csv",
      ),
    );

    await waitFor(() =>
      expect(mockToast.error).toHaveBeenCalledWith(
        "Failed to download the CSV. Please try again.",
      ),
    );
  });
});
