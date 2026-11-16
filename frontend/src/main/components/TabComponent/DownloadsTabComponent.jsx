import axios from "axios";
import CourseDownloadsForm from "../Courses/CourseDownloadsForm";
import { toast } from "react-toastify";

export default function DownloadsTabComponent({ courseId, testIdPrefix }) {
  const handleSubmit = async () => {
    try {
      const response = await axios.get("/api/csv/formattedteams", {
        params: { courseId },
        responseType: "blob",
      });

      const blob = new Blob([response.data], { type: "text/csv" });
      const url = window.URL.createObjectURL(blob);

      const link = document.createElement("a");
      link.href = url;
      link.download = `formatted_teams_${courseId}.csv`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);

      toast("Download successfully initiated.");
    } catch (error) {
      console.error("CSV download error:", error);
      toast.error("Failed to download the CSV. Please try again.");
    }
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
