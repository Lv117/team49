package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.exception.BusinessException;
import cn.edu.sdu.java.server.exception.ErrorCodes;
import cn.edu.sdu.java.server.models.Person;
import cn.edu.sdu.java.server.models.Student;
import cn.edu.sdu.java.server.models.User;
import cn.edu.sdu.java.server.models.UserType;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.repositorys.AttendanceRepository;
import cn.edu.sdu.java.server.repositorys.DevelopmentRepository;
import cn.edu.sdu.java.server.repositorys.FamilyMemberRepository;
import cn.edu.sdu.java.server.repositorys.FeeRepository;
import cn.edu.sdu.java.server.repositorys.PersonRepository;
import cn.edu.sdu.java.server.repositorys.ScoreRepository;
import cn.edu.sdu.java.server.repositorys.StudentRepository;
import cn.edu.sdu.java.server.repositorys.UserRepository;
import cn.edu.sdu.java.server.repositorys.UserTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {
    @Mock
    private PersonRepository personRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserTypeRepository userTypeRepository;
    @Mock
    private PasswordEncoder encoder;
    @Mock
    private FeeRepository feeRepository;
    @Mock
    private FamilyMemberRepository familyMemberRepository;
    @Mock
    private SystemService systemService;
    @Mock
    private ScoreRepository scoreRepository;
    @Mock
    private DevelopmentRepository developmentRepository;
    @Mock
    private AttendanceRepository attendanceRepository;

    @InjectMocks
    private StudentService studentService;

    private DataRequest buildCreateRequest(String num) {
        DataRequest request = new DataRequest();
        Map<String, Object> form = new HashMap<>();
        form.put("num", num);
        form.put("name", "张三");
        form.put("dept", "计算机学院");
        form.put("major", "软件工程");
        form.put("className", "软工1班");
        form.put("email", "zhangsan@example.com");
        request.add("form", form);
        return request;
    }

    @BeforeEach
    void setUpCommonMocks() {
        when(encoder.encode("123456")).thenReturn("encoded-password");
    }

    @Test
    void addsStudentSuccessfully() {
        DataRequest request = buildCreateRequest("20260001");
        UserType studentType = new UserType();
        studentType.setId(2);
        studentType.setName("ROLE_STUDENT");

        when(personRepository.findByNum("20260001")).thenReturn(Optional.empty());
        when(userTypeRepository.findByName("ROLE_STUDENT")).thenReturn(studentType);
        when(personRepository.saveAndFlush(any(Person.class))).thenAnswer(invocation -> {
            Person person = invocation.getArgument(0);
            person.setPersonId(101);
            return person;
        });
        when(userRepository.findByPersonPersonId(101)).thenReturn(Optional.empty());
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(studentRepository.findById(101)).thenReturn(Optional.empty());
        when(studentRepository.saveAndFlush(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DataResponse response = studentService.studentEditSave(request);

        assertEquals(0, response.getCode());
        assertEquals(101, response.getData());
        ArgumentCaptor<Person> personCaptor = ArgumentCaptor.forClass(Person.class);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<Student> studentCaptor = ArgumentCaptor.forClass(Student.class);
        verify(personRepository).saveAndFlush(personCaptor.capture());
        verify(userRepository).saveAndFlush(userCaptor.capture());
        verify(studentRepository).saveAndFlush(studentCaptor.capture());
        assertEquals("20260001", personCaptor.getValue().getNum());
        assertEquals("zhangsan@example.com", personCaptor.getValue().getEmail());
        assertEquals(101, userCaptor.getValue().getPersonId());
        assertNull(userCaptor.getValue().getPerson());
        assertEquals(101, studentCaptor.getValue().getPersonId());
        assertNull(studentCaptor.getValue().getPerson());
        verify(systemService).modifyLog(any(Student.class), org.mockito.ArgumentMatchers.eq(true));
    }

    @Test
    void reusesOrphanPersonRecordInsteadOfReportingFalseConflict() {
        DataRequest request = buildCreateRequest("20260002");
        Person orphanPerson = new Person();
        orphanPerson.setPersonId(202);
        orphanPerson.setNum("20260002");
        orphanPerson.setType("1");

        UserType studentType = new UserType();
        studentType.setId(2);
        studentType.setName("ROLE_STUDENT");

        when(personRepository.findByNum("20260002")).thenReturn(Optional.of(orphanPerson));
        when(studentRepository.findById(202)).thenReturn(Optional.empty());
        when(userTypeRepository.findByName("ROLE_STUDENT")).thenReturn(studentType);
        when(personRepository.saveAndFlush(orphanPerson)).thenReturn(orphanPerson);
        when(userRepository.findByPersonPersonId(202)).thenReturn(Optional.empty());
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(studentRepository.saveAndFlush(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DataResponse response = studentService.studentEditSave(request);

        assertEquals(0, response.getCode());
        assertEquals(202, response.getData());
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(personRepository).saveAndFlush(orphanPerson);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        assertEquals(202, userCaptor.getValue().getPersonId());
        assertNull(userCaptor.getValue().getPerson());
        verify(studentRepository, never()).findById(org.mockito.ArgumentMatchers.isNull());
    }

    @Test
    void returnsConflictWhenStudentNumberAlreadyBelongsToAnotherStudent() {
        DataRequest request = buildCreateRequest("20260003");
        Person existingPerson = new Person();
        existingPerson.setPersonId(303);
        existingPerson.setNum("20260003");

        when(personRepository.findByNum("20260003")).thenReturn(Optional.of(existingPerson));
        when(studentRepository.findById(303)).thenReturn(Optional.of(new Student()));

        DataResponse response = studentService.studentEditSave(request);

        assertEquals(1, response.getCode());
        assertEquals(ErrorCodes.STUDENT_NUM_CONFLICT, response.getErrorCode());
        verify(personRepository, never()).saveAndFlush(any(Person.class));
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(studentRepository, never()).saveAndFlush(any(Student.class));
    }

    @Test
    void returnsRequiredErrorWhenStudentNumberMissing() {
        DataRequest request = buildCreateRequest("   ");

        DataResponse response = studentService.studentEditSave(request);

        assertEquals(1, response.getCode());
        assertEquals(ErrorCodes.STUDENT_NUM_REQUIRED, response.getErrorCode());
        assertNull(response.getData());
        verify(personRepository, never()).saveAndFlush(any(Person.class));
    }

    @Test
    void throwsTimeoutBusinessExceptionWhenUserInsertTimesOut() {
        DataRequest request = buildCreateRequest("20260004");
        UserType studentType = new UserType();
        studentType.setId(2);
        studentType.setName("ROLE_STUDENT");

        when(personRepository.findByNum("20260004")).thenReturn(Optional.empty());
        when(userTypeRepository.findByName("ROLE_STUDENT")).thenReturn(studentType);
        when(personRepository.saveAndFlush(any(Person.class))).thenAnswer(invocation -> {
            Person person = invocation.getArgument(0);
            person.setPersonId(404);
            return person;
        });
        when(userRepository.findByPersonPersonId(404)).thenReturn(Optional.empty());
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new PessimisticLockingFailureException("Lock wait timeout exceeded"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> studentService.studentEditSave(request));

        assertEquals(ErrorCodes.STUDENT_SAVE_TIMEOUT, exception.getErrorCode());
        assertNotNull(exception.getCause());
        verify(studentRepository, never()).saveAndFlush(any(Student.class));
    }
}
