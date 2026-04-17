CREATE TABLE IF NOT EXISTS `exam_schedule` (
  `exam_id` INT(11) NOT NULL AUTO_INCREMENT,
  `course_name` VARCHAR(255) NOT NULL,
  `teacher` VARCHAR(255) NOT NULL,
  `exam_time` DATE NOT NULL,
  `exam_room` VARCHAR(255) NOT NULL,
  `seat_number` INT(11) NOT NULL,
  `exam_ticket` VARCHAR(255) NOT NULL,
  `year_semester_id` INT(11) NOT NULL,
  PRIMARY KEY (`exam_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;