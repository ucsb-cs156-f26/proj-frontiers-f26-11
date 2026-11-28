import { describe, expect, test, vi, beforeEach } from "vitest";
import { toast } from "react-toastify";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import {
  onSectionMutationError,
  useSectionLabels,
} from "main/utils/sectionsUtils";
import { sectionsFixtures } from "fixtures/sectionsFixtures";

const axiosMock = new AxiosMockAdapter(axios);

vi.mock("react-toastify", async (importOriginal) => {
  const mockToast = vi.fn();
  return {
    ...(await importOriginal()),
    toast: mockToast,
  };
});

describe("sectionsUtils tests", () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  test("onSectionMutationError shows duplicate message on 409", () => {
    const error = new Error("Request failed with status code 409");
    error.response = { status: 409 };

    onSectionMutationError(error);

    expect(toast).toHaveBeenCalledTimes(1);
    expect(toast).toHaveBeenCalledWith(
      "A section with that section value already exists for this course.",
    );
  });

  test("onSectionMutationError shows generic message on other errors", () => {
    const error = new Error("Request failed with status code 500");
    error.response = { status: 500 };

    onSectionMutationError(error);

    expect(toast).toHaveBeenCalledTimes(1);
    expect(toast).toHaveBeenCalledWith(
      "Error: Request failed with status code 500",
    );
  });

  test("useSectionLabels returns the raw section value when disabled, even if a translation is cached", () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });
    // Pre-populate the cache with section labels, so we can be sure that a
    // real, non-equivalent difference in behavior exists between the
    // disabled and enabled cases: if the `enabled` guard were removed, this
    // cached data would be found and returned instead of the raw section.
    queryClient.setQueryData(
      ["/api/courses/1/sections"],
      sectionsFixtures.threeSections,
    );

    const wrapper = ({ children }) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );

    const { result } = renderHook(() => useSectionLabels(1, false), {
      wrapper,
    });

    expect(result.current("0100")).toBe("0100");
  });

  test("useSectionLabels translates a section value to its label when enabled", () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });
    queryClient.setQueryData(
      ["/api/courses/1/sections"],
      sectionsFixtures.threeSections,
    );

    const wrapper = ({ children }) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );

    const { result } = renderHook(() => useSectionLabels(1, true), {
      wrapper,
    });

    expect(result.current("0100")).toBe("Tue 9:00am");
  });

  test("useSectionLabels starts with an empty list of sections in the cache when disabled", () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });
    const wrapper = ({ children }) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );

    renderHook(() => useSectionLabels(1, false), { wrapper });

    expect(queryClient.getQueryData(["/api/courses/1/sections"])).toEqual([]);
  });

  test("useSectionLabels does not show an error toast when the sections cannot be loaded", async () => {
    axiosMock.reset();
    axiosMock.onGet("/api/courses/2/sections").reply(500);
    const consoleErrorSpy = vi
      .spyOn(console, "error")
      .mockImplementation(() => {});
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });
    const wrapper = ({ children }) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );

    const { result } = renderHook(() => useSectionLabels(2, true), {
      wrapper,
    });

    await waitFor(() => expect(consoleErrorSpy).toHaveBeenCalled());
    expect(consoleErrorSpy.mock.calls[0][0]).toBe(
      "Error communicating with backend via GET on /api/courses/2/sections",
    );
    expect(toast).not.toHaveBeenCalled();
    expect(result.current("0100")).toBe("0100");
    consoleErrorSpy.mockRestore();
  });
});
