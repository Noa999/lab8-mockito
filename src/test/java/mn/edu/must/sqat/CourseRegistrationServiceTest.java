package mn.edu.must.sqat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseRegistrationServiceTest {

    @Mock StudentRepository repository;
    @Mock EmailSender emailSender;

    CourseRegistrationService service;

    private static final String STUDENT_ID = "S1";
    private static final String COURSE_ID = "CS101";
    private static final String EMAIL = "s1@example.mn";

    @BeforeEach
    void setUp() {
        service = new CourseRegistrationService(repository, emailSender);
    }

    private Student activeStudent(List<String> coursesTaken) {
        return new Student(STUDENT_ID, EMAIL, true, coursesTaken);
    }

    private Student inactiveStudent() {
        return new Student(STUDENT_ID, EMAIL, false, List.of());
    }

    // A1 — Идэвхтэй оюутан бүртгүүлэхэд "OK" буцаж, saveRegistration дуудагдаж, и-мэйл илгээгдэнэ
    @Test
    void register_идэвхтэйОюутанБолонХангалттайПрериквизитэдOKБуцаана() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(activeStudent(List.of("CS100")));
        when(repository.courseExists(COURSE_ID)).thenReturn(true);
        when(repository.prerequisites(COURSE_ID)).thenReturn(List.of("CS100"));

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("OK", result);
        verify(repository).saveRegistration(STUDENT_ID, COURSE_ID);
        verify(emailSender).send(eq(EMAIL), anyString(), contains(COURSE_ID));
    }

    // A2 — Байхгүй оюутанд "ERROR_NO_STUDENT"; сан руу хадгалаагүй, и-мэйл яваагүй
    @Test
    void register_байхгүйОюутанд_ERROR_NO_STUDENTБуцаанаТаХадгалахгүй() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(null);

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("ERROR_NO_STUDENT", result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }

    // A3 — Идэвхгүй оюутанд "ERROR_INACTIVE_STUDENT"; и-мэйлтэй огт харьцаагүй
    @Test
    void register_идэвхгүйОюутанд_ERROR_INACTIVE_STUDENTБуцаанаИМэйлтэйХарьцаагүй() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(inactiveStudent());

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("ERROR_INACTIVE_STUDENT", result);
        verifyNoInteractions(emailSender);
    }

    // A4 — Өмнөх холбоо хичээл дутуу бол "ERROR_PREREQUISITES"; хадгалаагүй
    @Test
    void register_дутууПрериквизитэд_ERROR_PREREQUISITESБуцаанаХадгалахгүй() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(activeStudent(List.of()));
        when(repository.courseExists(COURSE_ID)).thenReturn(true);
        when(repository.prerequisites(COURSE_ID)).thenReturn(List.of("CS100"));

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("ERROR_PREREQUISITES", result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
    }

    // A5 — И-мэйл илгээгч exception шидсэн ч бүртгэл "OK", хадгалалт хийгдсэн
    @Test
    void register_иМэйлИлгээхэдExceptionШидэгдэвч_OKБуцаажХадгална() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(activeStudent(List.of()));
        when(repository.courseExists(COURSE_ID)).thenReturn(true);
        when(repository.prerequisites(COURSE_ID)).thenReturn(List.of());
        doThrow(new RuntimeException("SMTP алдаа")).when(emailSender)
                .send(anyString(), anyString(), anyString());

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("OK", result);
        verify(repository).saveRegistration(STUDENT_ID, COURSE_ID);
    }

    // A6 — null оролтод IllegalArgumentException; хамаарлуудтай огт харьцаагүй
    @Test
    void register_studentIdNullOrCourseIdNull_IllegalArgumentExceptionШидэгдэнэХамаарлуудтайХарьцаагүй() {
        assertThrows(IllegalArgumentException.class, () -> service.register(null, COURSE_ID));
        assertThrows(IllegalArgumentException.class, () -> service.register(STUDENT_ID, null));

        verifyNoInteractions(repository, emailSender);
    }

    // A7 — Илгээгдсэн и-мэйлийн биед хичээлийн код орсныг шалгах
    @Test
    void register_амжилттайБүртгэлд_иМэйлийнБиедХичээлийнКодОрсонБайна() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(activeStudent(List.of()));
        when(repository.courseExists(COURSE_ID)).thenReturn(true);
        when(repository.prerequisites(COURSE_ID)).thenReturn(List.of());

        service.register(STUDENT_ID, COURSE_ID);

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(eq(EMAIL), anyString(), bodyCaptor.capture());
        assertTrue(bodyCaptor.getValue().contains(COURSE_ID));
    }

    // A8 — Гурван алдааны ангийг нэг параметржүүлсэн тестээр
    @ParameterizedTest(name = "{0} тохиолдолд {1} буцаана")
    @CsvSource({
            "missing,    ERROR_NO_STUDENT",
            "inactive,   ERROR_INACTIVE_STUDENT",
            "noCourse,   ERROR_NO_COURSE"
    })
    void register_гурванТөрлийнАлдаа_зөвERRORКодБуцаана(String scenario, String expected) {
        lenient().when(repository.findStudent(STUDENT_ID)).thenReturn(switch (scenario) {
            case "missing" -> null;
            case "inactive" -> inactiveStudent();
            default -> activeStudent(List.of());
        });
        lenient().when(repository.courseExists(COURSE_ID)).thenReturn(!scenario.equals("noCourse"));

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals(expected, result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }
}
