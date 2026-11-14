import { render, screen, waitFor } from "@testing-library/react";
import AxiosMockAdapter from "axios-mock-adapter";
import axios from "axios";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import EmailsTabComponent from "main/components/TabComponent/EmailsTabComponent";
import { vi } from "vitest";

const queryClient = new QueryClient();
const testId = "InstructorCourseShowPage";
const mockToast = vi.fn();
const mockToastError = vi.fn();
vi.mock("react-toastify", async (importOriginal) => {
  const toast = (x) => mockToast(x);
  toast.error = (x) => mockToastError(x);
  return {
    ...(await importOriginal()),
    toast,
  };
});

let axiosMock;

describe("EmailsTabComponent Tests", () => {
  beforeEach(() => {
    axiosMock = new AxiosMockAdapter(axios);
    axiosMock.reset();
    axiosMock.resetHistory();
    queryClient.clear();
    mockToast.mockClear();
    mockToastError.mockClear();
  });

  afterEach(() => {
    axiosMock.restore();
  });

  test("EmailsTabComponent Renders", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <EmailsTabComponent courseId={1} testIdPrefix={testId} />
      </QueryClientProvider>,
    );

    expect(
      screen.getByTestId(`InstructorCourseShowPage-EmailsTabComponent`),
    ).toBeInTheDocument();
  });

  test("shows the Emails card, with ALL emails of this course", async () => {
    axiosMock
      .onGet("/api/courses/emails")
      .reply(200, "a@ucsb.edu\r\nb@ucsb.edu");

    render(
      <QueryClientProvider client={queryClient}>
        <EmailsTabComponent courseId={7} testIdPrefix={testId} />
      </QueryClientProvider>,
    );

    const cardId = `${testId}-EmailsCard`;
    expect(screen.getByRole("button", { name: "Emails" })).toHaveAttribute(
      "aria-expanded",
      "true",
    );
    await waitFor(() =>
      expect(screen.getByTestId(`${cardId}-emails`)).toHaveValue(
        "a@ucsb.edu\nb@ucsb.edu",
      ),
    );

    const emailsRequests = axiosMock.history.get.filter(
      (request) => request.url === "/api/courses/emails",
    );
    expect(emailsRequests.length).toBe(1);
    expect(emailsRequests[0].params).toEqual({
      courseId: 7,
      type: "ALL",
      format: "ONE_PER_LINE",
    });
    expect(emailsRequests[0].params).not.toHaveProperty("team");
  });

  test("shows the Emails card to everyone", async () => {
    axiosMock.onGet("/api/courses/emails").reply(200, "a@ucsb.edu");

    render(
      <QueryClientProvider client={queryClient}>
        <EmailsTabComponent courseId={7} testIdPrefix={testId} />
      </QueryClientProvider>,
    );

    expect(
      await screen.findByTestId(`${testId}-EmailsCard-card`),
    ).toBeInTheDocument();
  });

  test("shows the Type dropdown on the Emails card", async () => {
    axiosMock.onGet("/api/courses/emails").reply(200, "a@ucsb.edu");

    render(
      <QueryClientProvider client={queryClient}>
        <EmailsTabComponent courseId={7} testIdPrefix={testId} />
      </QueryClientProvider>,
    );

    expect(
      await screen.findByTestId(`${testId}-EmailsCard-type`),
    ).toBeInTheDocument();
  });
});
