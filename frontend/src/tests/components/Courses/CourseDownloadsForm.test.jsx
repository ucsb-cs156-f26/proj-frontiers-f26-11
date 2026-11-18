import { fireEvent, render, screen } from "@testing-library/react";
import { expect, vi } from "vitest";
import CourseDownloadsForm from "main/components/Courses/CourseDownloadsForm";

const PREFIX = "InstructorCourseShowPage";
const BTN = `${PREFIX}-btn-download-students-csv`;
const HEADER = `${PREFIX}-downloads-header`;

describe("CourseDownloadsForm tests", () => {
  test("renders a button with the correct name", () => {
    render(<CourseDownloadsForm testIdPrefix={PREFIX} />);

    expect(
      screen.getByText("Download Formatted Student/Teams Chart as CSV"),
    ).toBeInTheDocument();
  });

  test("renders the header and helper text with the expected test id", () => {
    render(<CourseDownloadsForm testIdPrefix={PREFIX} />);

    // Kills mutants that empty the header's test id template string
    expect(screen.getByTestId(HEADER)).toHaveTextContent("Course Downloads");
    expect(
      screen.getByText(
        "Export utility options available for this course instance.",
      ),
    ).toBeInTheDocument();
  });

  test("renders the submit button with the expected test id and styling", () => {
    render(<CourseDownloadsForm testIdPrefix={PREFIX} />);

    // Kills mutants that empty the button's test id template string
    const button = screen.getByTestId(BTN);
    expect(button).toHaveAttribute("type", "submit");
    expect(button).toHaveClass("btn-primary");
  });

  test("uses the testIdPrefix prop when building test ids", () => {
    render(<CourseDownloadsForm testIdPrefix="OtherPrefix" />);

    expect(
      screen.getByTestId("OtherPrefix-downloads-header"),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId("OtherPrefix-btn-download-students-csv"),
    ).toBeInTheDocument();
    expect(screen.queryByTestId(HEADER)).not.toBeInTheDocument();
    expect(screen.queryByTestId(BTN)).not.toBeInTheDocument();
  });

  test("calls downloadAction exactly once when the button is clicked", () => {
    const downloadAction = vi.fn();
    render(
      <CourseDownloadsForm
        downloadAction={downloadAction}
        testIdPrefix={PREFIX}
      />,
    );

    fireEvent.click(screen.getByTestId(BTN));

    // Kills mutants that empty handleSubmit, remove downloadAction(),
    // or remove onSubmit={handleSubmit}
    expect(downloadAction).toHaveBeenCalledTimes(1);
  });

  test("does not call downloadAction before the form is submitted", () => {
    const downloadAction = vi.fn();
    render(
      <CourseDownloadsForm
        downloadAction={downloadAction}
        testIdPrefix={PREFIX}
      />,
    );

    expect(downloadAction).not.toHaveBeenCalled();
  });

  test("prevents the default form submission", () => {
    const downloadAction = vi.fn();
    render(
      <CourseDownloadsForm
        downloadAction={downloadAction}
        testIdPrefix={PREFIX}
      />,
    );

    const form = screen.getByTestId(BTN).closest("form");

    // fireEvent returns false when preventDefault() was called on the event.
    // Kills mutants that remove event.preventDefault()
    const notPrevented = fireEvent.submit(form);
    expect(notPrevented).toBe(false);
    expect(downloadAction).toHaveBeenCalledTimes(1);
  });
});
