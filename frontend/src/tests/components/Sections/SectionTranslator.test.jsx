import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { describe, expect, test, vi } from "vitest";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";

import SectionTranslator from "main/components/Sections/SectionTranslator";
import { sectionsFixtures } from "fixtures/sectionsFixtures";

const axiosMock = new AxiosMockAdapter(axios);

const mockToast = vi.fn();
vi.mock("react-toastify", async (importOriginal) => {
  return {
    ...(await importOriginal()),
    toast: (x) => mockToast(x),
  };
});

const freshQueryClient = () =>
  new QueryClient({ defaultOptions: { queries: { retry: false } } });

describe("SectionTranslator tests", () => {
  const queryClient = new QueryClient();

  beforeEach(() => {
    axiosMock.reset();
    axiosMock
      .onGet("/api/courses/7/sections")
      .reply(200, sectionsFixtures.threeSections);
  });

  test("renders the translation for the currently selected section", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <SectionTranslator courseId={7} section="0200" onChange={vi.fn()} />
      </QueryClientProvider>,
    );

    await waitFor(() =>
      expect(screen.getByTestId("SectionTranslator-translation")).toHaveValue(
        "Tue 10:00am",
      ),
    );
  });

  test("renders a blank translation when there is no matching section", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <SectionTranslator
          courseId={7}
          section="does-not-exist"
          onChange={vi.fn()}
        />
      </QueryClientProvider>,
    );

    await screen.findByTestId("SectionTranslator-select");
    expect(screen.getByTestId("SectionTranslator-translation")).toHaveValue("");
  });

  test("defaults the select to an empty value when section is undefined", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <SectionTranslator courseId={7} onChange={vi.fn()} />
      </QueryClientProvider>,
    );

    const select = await screen.findByTestId("SectionTranslator-select");
    expect(select).toHaveValue("");
  });

  test("calls onChange with the raw section value when a dropdown option is selected", async () => {
    const onChange = vi.fn();
    render(
      <QueryClientProvider client={queryClient}>
        <SectionTranslator courseId={7} section="" onChange={onChange} />
      </QueryClientProvider>,
    );

    const select = await screen.findByTestId("SectionTranslator-select");
    await screen.findByRole("option", { name: "0300 - Tue 11:00am" });

    fireEvent.change(select, { target: { value: "0300" } });

    expect(onChange).toHaveBeenCalledWith("0300");
  });

  test("lists all sections for the course as dropdown options", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <SectionTranslator courseId={7} section="" onChange={vi.fn()} />
      </QueryClientProvider>,
    );

    const select = await screen.findByTestId("SectionTranslator-select");
    sectionsFixtures.threeSections.forEach((s) => {
      expect(
        screen.getByRole("option", { name: `${s.section} - ${s.label}` }),
      ).toBeInTheDocument();
    });
    expect(select).toBeInTheDocument();
  });

  test("selects the current section in the dropdown once the sections load", async () => {
    render(
      <QueryClientProvider client={freshQueryClient()}>
        <SectionTranslator courseId={7} section="0200" onChange={vi.fn()} />
      </QueryClientProvider>,
    );

    await screen.findByRole("option", { name: "0200 - Tue 10:00am" });
    expect(screen.getByTestId("SectionTranslator-select")).toHaveValue("0200");
  });

  test("links each label to its field, using the testIdPrefix for the ids", async () => {
    render(
      <QueryClientProvider client={freshQueryClient()}>
        <SectionTranslator
          courseId={7}
          section="0200"
          onChange={vi.fn()}
          testIdPrefix="Prefix"
        />
      </QueryClientProvider>,
    );

    const translation = await screen.findByTestId("Prefix-translation");
    const select = screen.getByTestId("Prefix-select");
    expect(translation).toHaveAttribute("id", "Prefix-translation");
    expect(select).toHaveAttribute("id", "Prefix-select");
    expect(screen.getByLabelText("Section Translation")).toBe(translation);
    expect(screen.getByLabelText("Select Section")).toBe(select);
  });

  test("caches the sections under the course's sections query key", async () => {
    const queryClient = freshQueryClient();
    render(
      <QueryClientProvider client={queryClient}>
        <SectionTranslator courseId={7} section="" onChange={vi.fn()} />
      </QueryClientProvider>,
    );

    await screen.findByRole("option", { name: "0100 - Tue 9:00am" });
    expect(queryClient.getQueryData(["/api/courses/7/sections"])).toEqual(
      sectionsFixtures.threeSections,
    );
  });

  test("does not fetch sections, and only shows the placeholder option, when there is no courseId", async () => {
    const queryClient = freshQueryClient();
    render(
      <QueryClientProvider client={queryClient}>
        <SectionTranslator section="" onChange={vi.fn()} />
      </QueryClientProvider>,
    );

    expect(
      queryClient.getQueryState(["/api/courses/undefined/sections"])
        .fetchStatus,
    ).toBe("idle");
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(axiosMock.history.get).toHaveLength(0);

    const options = screen.getAllByRole("option");
    expect(options).toHaveLength(1);
    expect(options[0]).toHaveTextContent(/^-- Select a Section --$/);
    expect(screen.getByTestId("SectionTranslator-translation")).toHaveValue("");
  });

  test("does not show an error toast when the sections cannot be loaded", async () => {
    mockToast.mockClear();
    const consoleErrorSpy = vi
      .spyOn(console, "error")
      .mockImplementation(() => {});
    axiosMock.onGet("/api/courses/8/sections").reply(500);

    render(
      <QueryClientProvider client={freshQueryClient()}>
        <SectionTranslator courseId={8} section="" onChange={vi.fn()} />
      </QueryClientProvider>,
    );

    await waitFor(() => expect(consoleErrorSpy).toHaveBeenCalled());
    expect(consoleErrorSpy.mock.calls[0][0]).toBe(
      "Error communicating with backend via GET on /api/courses/8/sections",
    );
    expect(mockToast).not.toHaveBeenCalled();
    consoleErrorSpy.mockRestore();
  });
});
