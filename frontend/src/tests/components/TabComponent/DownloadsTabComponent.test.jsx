import axios from "axios";
import { fireEvent, render, waitFor, screen } from "@testing-library/react";
import DownloadsTabComponent from "main/components/TabComponent/DownloadsTabComponent";
import AxiosMockAdapter from "axios-mock-adapter";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { expect, vi } from "vitest";
import coursesFixtures from "fixtures/coursesFixtures";

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

const BTN = "InstructorCourseShowPage-btn-download-students-csv";
const courseId = coursesFixtures.severalCourses[0].id;

const renderComponent = (id = courseId) =>
  render(
    <QueryClientProvider client={new QueryClient()}>
      <DownloadsTabComponent
        courseId={id}
        testIdPrefix="InstructorCourseShowPage"
      />
    </QueryClientProvider>,
  );

describe("DownloadsTabComponent", () => {
  let clickedAnchor;
  let wasInDomOnClick;

  beforeEach(() => {
    vi.clearAllMocks();
    axiosMock.reset(); // clears handlers AND history
    clickedAnchor = undefined;
    wasInDomOnClick = undefined;

    window.URL.createObjectURL = vi.fn(() => "blob:mock-url");
    window.URL.revokeObjectURL = vi.fn();

    // Capture the anchor instead of letting jsdom try to navigate
    vi.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(
      function () {
        clickedAnchor = this;
        wasInDomOnClick = document.body.contains(this);
      },
    );
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  test("renders the wrapper with the expected test id", () => {
    renderComponent();
    expect(
      screen.getByTestId("InstructorCourseShowPage-downloadsTab"),
    ).toBeInTheDocument();
  });

  test("requests the CSV with the correct config", async () => {
    axiosMock.onGet("/api/csv/formattedteams").reply(200, "a,b\n1,2\n");
    renderComponent();
    fireEvent.click(await screen.findByTestId(BTN));

    await waitFor(() =>
      expect(mockToast).toHaveBeenCalledWith(
        "Download successfully initiated.",
      ),
    );

    const config = axiosMock.history.get[0];
    expect(axiosMock.history.get).toHaveLength(1);
    expect(config.url).toBe("/api/csv/formattedteams");
    expect(config.params).toEqual({ courseId });
    expect(config.responseType).toBe("blob");
  });

  test("builds a csv blob, clicks an attached link, then cleans up", async () => {
    const CSV = "a,b\n1,2\n";
    axiosMock.onGet("/api/csv/formattedteams").reply(200, CSV);
    renderComponent();
    fireEvent.click(await screen.findByTestId(BTN));

    await waitFor(() => expect(clickedAnchor).toBeDefined());

    // Blob
    expect(window.URL.createObjectURL).toHaveBeenCalledTimes(1);
    const blob = window.URL.createObjectURL.mock.calls[0][0];
    expect(blob).toBeInstanceOf(Blob);
    expect(blob.type).toBe("text/csv");
    expect(blob.size).toBe(CSV.length);
    expect(await blob.text()).toBe(CSV);

    // Anchor
    expect(clickedAnchor.href).toBe("blob:mock-url");
    expect(clickedAnchor.download).toBe(`formatted_teams_${courseId}.csv`);
    expect(wasInDomOnClick).toBe(true); // appendChild happened before click
    expect(document.body.contains(clickedAnchor)).toBe(false); // removed after

    // Cleanup
    expect(window.URL.revokeObjectURL).toHaveBeenCalledTimes(1);
    expect(window.URL.revokeObjectURL).toHaveBeenCalledWith("blob:mock-url");

    // Success path only
    expect(mockToast).toHaveBeenCalledTimes(1);
    expect(mockToast.error).not.toHaveBeenCalled();
  });

  test("logs and shows an error toast when the request fails", async () => {
    const errorSpy = vi.spyOn(console, "error").mockImplementation(() => {});
    axiosMock.onGet("/api/csv/formattedteams").reply(500);
    renderComponent(1);
    fireEvent.click(await screen.findByTestId(BTN));

    await waitFor(() =>
      expect(mockToast.error).toHaveBeenCalledWith(
        "Failed to download the CSV. Please try again.",
      ),
    );
    expect(errorSpy).toHaveBeenCalledWith(
      "CSV download error:",
      expect.anything(),
    );

    // Failure path must not do any download work or show success
    expect(mockToast).not.toHaveBeenCalled();
    expect(window.URL.createObjectURL).not.toHaveBeenCalled();
    expect(clickedAnchor).toBeUndefined();
  });

  test("re-enables the button after a failed download", async () => {
    vi.spyOn(console, "error").mockImplementation(() => {});
    axiosMock.onGet("/api/csv/formattedteams").reply(500);
    renderComponent();
    const button = await screen.findByTestId(BTN);

    fireEvent.click(button);
    await waitFor(() => expect(mockToast.error).toHaveBeenCalled());
    await waitFor(() => expect(button).not.toBeDisabled());
  });
});