import React from "react";
import CourseEmailsCard from "main/components/Emails/CourseEmailsCard";
import { http, HttpResponse } from "msw";

export default {
  title: "components/Emails/CourseEmailsCard",
  component: CourseEmailsCard,
};

const Template = (args) => {
  return <CourseEmailsCard {...args} />;
};

const staffEmails = [
  "cgaucho@ucsb.edu",
  "ldelplaya@ucsb.edu",
  "phtcon@ucsb.edu",
];
const studentEmails = [
  "student1@ucsb.edu",
  "student2@ucsb.edu",
  "student3@ucsb.edu",
];
const allEmails = [
  "cgaucho@ucsb.edu",
  "ldelplaya@ucsb.edu",
  "phtcon@ucsb.edu",
  "student1@ucsb.edu",
  "student2@ucsb.edu",
  "student3@ucsb.edu",
];

// Answers in whichever format the dropdown asks for
const emailsHandler = http.get("/api/courses/emails", ({ request }) => {
  const format = new URL(request.url).searchParams.get("format");
  const type = new URL(request.url).searchParams.get("type");

  const emailsByType = {
    STAFF: staffEmails,
    STUDENTS: studentEmails,
    ALL: allEmails,
  };
  const emails = emailsByType[type] || "";

  return HttpResponse.text(
    format === "COMMA_SEPARATED" ? emails.join(",") : emails.join("\r\n"),
    { status: 200 },
  );
});

export const Students = Template.bind({});
Students.args = {
  courseId: 7,
  type: "STUDENTS",
};
Students.parameters = {
  msw: [emailsHandler],
};

export const Staff = Template.bind({});
Staff.args = {
  courseId: 7,
  type: "STAFF",
};
Staff.parameters = {
  msw: [emailsHandler],
};

export const All = Template.bind({});
All.args = {
  courseId: 7,
  type: "ALL",
};
All.parameters = {
  msw: [emailsHandler],
};

export const ShowTypeDropdown = Template.bind({});
ShowTypeDropdown.args = {
  courseId: 7,
  type: "ALL",
  showTypeDropdown: true,
};
ShowTypeDropdown.parameters = {
  msw: [emailsHandler],
};

export const Empty = Template.bind({});
Empty.args = {
  courseId: 7,
  type: "ALL",
};
Empty.parameters = {
  msw: [
    http.get("/api/courses/emails", () => {
      return HttpResponse.text("", { status: 200 });
    }),
  ],
};
