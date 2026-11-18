import AxiosMockAdapter from "axios-mock-adapter";
import axios from "axios";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import CourseDownloadsForm from "main/components/Courses/CourseDownloadsForm";


const axiosMock = new AxiosMockAdapter(axios);
const queryClient = new QueryClient();

describe("CourseDownloadForm tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    queryClient.clear();
  });
  test("renders a button with the correct name", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <CourseDownloadsForm />
      </QueryClientProvider>,
    );

    expect(await screen.findByText("Download Formatted Student/Teams Chart as CSV")).toBeInTheDocument();
    });
});