-- 移除exam_schedule表中的座位号和准考证号字段
ALTER TABLE `exam_schedule` DROP COLUMN `seat_number`;
ALTER TABLE `exam_schedule` DROP COLUMN `exam_ticket`;

-- 创建student_exam_schedule表
CREATE TABLE IF NOT EXISTS `student_exam_schedule` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `exam_id` INT(11) NOT NULL,
  `student_id` VARCHAR(50) NOT NULL,
  `seat_number` INT(11) NOT NULL,
  `exam_ticket` VARCHAR(50) NOT NULL,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`exam_id`) REFERENCES `exam_schedule` (`exam_id`) ON DELETE CASCADE,
  UNIQUE KEY `unique_student_exam` (`exam_id`, `student_id`),
  UNIQUE KEY `unique_seat_exam` (`exam_id`, `seat_number`),
  UNIQUE KEY `unique_ticket_exam` (`exam_id`, `exam_ticket`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;