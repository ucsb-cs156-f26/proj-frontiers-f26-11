import {
  Button,
  Col,
  Form,
  ModalBody,
  ModalHeader,
  Row,
  OverlayTrigger,
  Tooltip,
} from "react-bootstrap";
import { BsInfoCircle } from "react-icons/bs";
import Modal from "react-bootstrap/Modal";
import CourseEmailsCard from "main/components/Emails/CourseEmailsCard";

export default function EmailsTabComponent({ courseId, testIdPrefix }) {
  return (
    <div data-testid={`${testIdPrefix}-EmailsTabComponent`}>
      <CourseEmailsCard
        courseId={courseId}
        type="ALL"
        showTypeDropdown={true}
        testIdPrefix={`${testIdPrefix}-EmailsCard`}
      />
    </div>
  );
}
