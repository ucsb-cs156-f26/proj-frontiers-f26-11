import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import { fireEvent, render, waitFor, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import coursesFixtures from "fixtures/coursesFixtures";

import CatmeTabComponent from "main/components/TabComponent/CatmeTabComponent";

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

const BTN = "InstructorCourseShowPage-btn-download-catme-csv";
const courseId = coursesFixtures.severalCourses[0].id;

const renderComponent = (id = courseId) =>
  render(
    <CatmeTabComponent courseId={id} testIdPrefix="InstructorCourseShowPage" />,
  );

describe("CatmeTabComponent tests", () => {
  let clickedAnchor;

  beforeEach(() => {
    vi.clearAllMocks();
    axiosMock.reset(); // clears handlers AND history
    clickedAnchor = undefined;

    window.URL.createObjectURL = vi.fn(() => "blob:mock-url");
    window.URL.revokeObjectURL = vi.fn();

    // Capture the anchor instead of letting jsdom try to navigate
    vi.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(
      function () {
        clickedAnchor = this;
      },
    );
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  test("renders the CATME roster download form", () => {
    renderComponent();

    expect(
      screen.getByTestId("InstructorCourseShowPage-catme-tab-component"),
    ).toBeInTheDocument();
    expect(screen.getByTestId(BTN)).toHaveTextContent(
      "Download CSV Roster in CATME format",
    );
  });

  test("requests the CATME CSV with the correct config", async () => {
    axiosMock.onGet("/api/csv/catme").reply(200, "a,b\n1,2\n");
    renderComponent();
    fireEvent.click(await screen.findByTestId(BTN));

    await waitFor(() =>
      expect(mockToast).toHaveBeenCalledWith(
        "Download successfully initiated.",
      ),
    );

    const config = axiosMock.history.get[0];
    expect(axiosMock.history.get).toHaveLength(1);
    expect(config.url).toBe("/api/csv/catme");
    expect(config.params).toEqual({ courseId });
    expect(config.responseType).toBe("blob");
  });

  test("downloads the CATME CSV with the expected filename", async () => {
    axiosMock.onGet("/api/csv/catme").reply(200, "a,b\n1,2\n");
    renderComponent();
    fireEvent.click(await screen.findByTestId(BTN));

    await waitFor(() => expect(clickedAnchor).toBeDefined());

    expect(clickedAnchor.download).toBe(`catme_roster_${courseId}.csv`);
    expect(window.URL.createObjectURL).toHaveBeenCalledTimes(1);
    expect(window.URL.revokeObjectURL).toHaveBeenCalledTimes(1);
  });

  test("shows an error toast and does no download work when the request fails", async () => {
    vi.spyOn(console, "error").mockImplementation(() => {});
    axiosMock.onGet("/api/csv/catme").reply(500);
    renderComponent();
    fireEvent.click(await screen.findByTestId(BTN));

    await waitFor(() =>
      expect(mockToast.error).toHaveBeenCalledWith(
        "Failed to download the CSV. Please try again.",
      ),
    );

    expect(mockToast).not.toHaveBeenCalled();
    expect(window.URL.createObjectURL).not.toHaveBeenCalled();
    expect(clickedAnchor).toBeUndefined();
  });
});
