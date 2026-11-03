# Lab 8 — Mock объектууд (Mockito)

**Оюутны нэр:** Номунзаяа
**Оюутны код:** B232270804

**Орчин:**
openjdk version “17.0.20.1” 2026-08-18
OpenJDK Runtime Environment (build 17.0.20.1+1-1-26.04-Ubuntu)
OpenJDK 64-Bit Server VM (build 17.0.20.1+1-1-26.04-Ubuntu, mixed mode, sharing)

Apache Maven 3.9.12
Maven home: /usr/share/maven
Java version: 17.0.20.1, vendor: Ubuntu, runtime: /usr/lib/jvm/java-17-openjdk-amd64
Default locale: en, platform encoding: UTF-8
OS name: “linux”, version: “6.18.33.2-microsoft-standard-wsl2”, arch: “amd64”, family: “unix”

## Агуулга

- `src/main/java/mn/edu/must/sqat/` — тестлэгдэх код (`CourseRegistrationService`,
  `StudentRepository`, `EmailSender`, `Student`)
- `src/test/java/mn/edu/must/sqat/CourseRegistrationServiceTest.java` — Хэсэг A-ийн 8 тест
- `results/mvn-test.txt` — эцсийн, ногоон тестийн гаралт
- `results/mvn-test-mutant-1..3.txt` — Хэсэг B-ийн 3 мутацийн гаралт

## Хэсэг A — Тестийн тоо

`results/mvn-test.txt`-ийн сүүлийн мөр: **Tests run: 10, Failures: 0, Errors: 0, Skipped: 0**
(7 энгийн тест A1–A7 + A8-ийн 3 мөр).

A1–A8 бүгд бичигдсэн: `when`/`thenReturn` stub, `verify`/`never()`,
`verifyNoInteractions`, `doThrow(...).when(...)`, `assertThrows`,
`ArgumentCaptor`, `@ParameterizedTest` + `@CsvSource`.

### A8-ийн `lenient()` тайлбар

A8 нь нэг параметржүүлсэн тестээр 3 өөр сценари (`missing`, `inactive`,
`noCourse`) шалгадаг бөгөөд мөр бүрт зөвхөн зарим stub ашиглагддаг
(жишээ нь `missing` мөрөнд `courseExists` stub хэзээ ч дуудагдахгүй,
учир нь оюутан олдохгүй үед урсгал нэн даруй зогсдог). Mockito-гийн
strict stubs горим ийм "ашиглагдаагүй stub"-ыг алдаа гэж үзэх тул,
`lenient()` ашиглан энэ шалгалтыг чөлөөлсөн.

## Хэсэг B — Мутацийн тест

Мутаци бүрийн дараа кодыг сэргээж, эцэст нь `results/mvn-test.txt`
дахин ногоон (`BUILD SUCCESS`) болсон.

| № | Мутаци | Унасан тест(үүд) | Файл |
|---|--------|-------------------|------|
| 1 | `if (!student.active()) return "ERROR_INACTIVE_STUDENT";` мөрийг комментлосон | A3 (`register_идэвхгүйОюутанд...`) — expected ERROR_INACTIVE_STUDENT, actual ERROR_NO_COURSE; A8[2] (inactive мөр) — expected ERROR_INACTIVE_STUDENT, actual OK | `results/mvn-test-mutant-1.txt` (Failures: 2, BUILD FAILURE) |
| 2 | `repository.saveRegistration(...)` дуудалтыг комментлосон | A1 (`register_идэвхтэйОюутан...`) — "Wanted but not invoked: saveRegistration"; A5 (`register_иМэйлИлгээхэдException...`) — мөн адил | `results/mvn-test-mutant-2.txt` (Failures: 2, BUILD FAILURE) |
| 3 | Эхний if-ээс `\|\| courseId == null`-г устгаж, зөвхөн `studentId`-г шалгадаг болгосон | A6 (`register_studentIdNullOrCourseIdNull...`) — "Expected IllegalArgumentException... but nothing was thrown" | `results/mvn-test-mutant-3.txt` (Failures: 1, BUILD FAILURE) |

**Дүгнэлт (мутаци):** Гурван мутаци тус бүр дор хаяж нэг тестийг
унагаасан тул тестийн багц "цоорхойгүй" гэдгийг нотолж чадсан. A6 тест
эхнээсээ `studentId` болон `courseId` хоёрыг тус тусад нь
`assertThrows`-аар шалгадаг байсан тул 3-р мутацийг нэмэлт засваргүйгээр
шууд илрүүлсэн — тестийн цоорхой олдоогүй.

## Дүгнэлт

Mock объект нь гараар бичсэн fake-ээс ялгаатай нь — mock нь зөвхөн
тухайн тестэд хэрэгтэй, тодорхой тохиолдлыг зохиомлоор stub хийдэг
бол fake нь бодит хэрэгжүүлэлтийн логикийг багасгаж хуулбарладаг
("жижиг бодит систем" шиг ажилладаг), өөрөөр хэлбэл fake-ийг зөв
бичихийн тулд нэмэлт код, засвар шаардагддаг. Mockito ашигласнаар
`StudentRepository` болон `EmailSender`-ийн бодит хэрэгжүүлэлт (сан,
SMTP) огт байхгүйгээр, `CourseRegistrationService`-ийн цэвэр бизнес
логикийг хурдан, тусгаарлагдсан байдлаар тестлэх боломжтой боллоо.
Гэхдээ бүх зүйлийг mock хийх ёсгүй — тухайлбал `Student` record эсвэл
`ArrayList`-ийн `removeAll` мэт value object, стандарт сангийн
функцүүдийг mock хийх шаардлагагүй, учир нь тэдгээр нь аль хэдийн
энгийн, детерминистик бөгөөд mock хийх нь тестийг зөвхөн улам
төвөгтэй, уншихад хэцүү болгоно. `verify`-г хэтрүүлж, шаардлагагүй
бүх дуудалтыг (жишээ нь дотоод, тестийн зорилгод хамааралгүй
interaction-уудыг) нарийн шалгавал тест нь хэт "хэврэг" (brittle)
болж, код дотоод хэрэгжүүлэлтээ өөрчлөх бүрт (гадаад зан төлөв хэвээр
байсан ч) тест унах эрсдэлтэй болно — ингэснээр тест нь спецификацийг
биш, харин хэрэгжүүлэлтийн нарийн ширийнийг шалгах болж, рефакторинг
хийхэд саад болдог. Иймээс `verify`-г зөвхөн чухал, ажиглагдах ёстой
зан төлөвт (жишээ нь "хадгалагдсан эсэх", "и-мэйл илгээгдсэн эсэх")
л хэрэглэх нь зөв арга юм.
