import { describe, expect, test } from "vitest";
import {
  EMAIL_FORMATS,
  EMAIL_TYPES,
  courseEmailsQueryKey,
} from "main/utils/courseEmailsUtils";

describe("courseEmailsUtils tests", () => {
  test("courseEmailsQueryKey includes the course id", () => {
    expect(courseEmailsQueryKey(7)).toBe("/api/courses/emails?courseId=7");
    expect(courseEmailsQueryKey("12")).toBe("/api/courses/emails?courseId=12");
  });

  test("EMAIL_FORMATS lists the endpoint's formats, One per line first", () => {
    expect(EMAIL_FORMATS).toEqual([
      { value: "ONE_PER_LINE", label: "One per line" },
      { value: "COMMA_SEPARATED", label: "Comma separated" },
    ]);
  });

  test("EMAIL_TYPES lists the endpoint's types, in the order of Students, Staff, All", () => {
    expect(EMAIL_TYPES).toEqual([
      { value: "STUDENTS", label: "Students" },
      { value: "STAFF", label: "Staff" },
      { value: "ALL", label: "All" },
    ]);
  });
});
