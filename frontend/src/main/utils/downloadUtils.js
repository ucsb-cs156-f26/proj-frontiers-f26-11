import axios from "axios";
import { toast } from "react-toastify";

/**
 * Download a CSV file from the backend and trigger a browser download.
 *
 * @param url the backend endpoint to GET
 * @param params query parameters, e.g. { courseId }
 * @param filename the name the downloaded file should get
 */
export async function downloadCsv({ url, params, filename }) {
  try {
    const response = await axios.get(url, {
      params,
      responseType: "blob",
    });

    const blob = new Blob([response.data], { type: "text/csv" });
    const objectUrl = window.URL.createObjectURL(blob);

    const link = document.createElement("a");
    link.href = objectUrl;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(objectUrl);

    toast("Download successfully initiated.");
  } catch (error) {
    console.error("CSV download error:", error);
    toast.error("Failed to download the CSV. Please try again.");
  }
}
