import CourseDownloadsForm from "../Courses/CourseDownloadsForm";
import { downloadCsv } from "main/utils/downloadUtils";

export default function DownloadsTabComponent({ courseId, testIdPrefix }) {
  const handleSubmit = () => {
    downloadCsv({
      url: "/api/csv/formattedteams",
      params: { courseId },
      filename: `formatted_teams_${courseId}.csv`,
    });
  };

  return (
    <div data-testid={`${testIdPrefix}-downloadsTab`}>
      <CourseDownloadsForm
        downloadAction={handleSubmit}
        testIdPrefix={testIdPrefix}
      />
    </div>
  );
}
